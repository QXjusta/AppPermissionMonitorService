# AOSP 自定义系统服务开发指南

## 一、概述

本文档用于教学如何在 AOSP 中 **自定义一个自己的系统服务** 并集成到 Android 系统中。

这份文档是**通用方案**，不绑定当前项目，可用于你以后开发其他系统服务时参考，例如：
- 日志采集服务
- 设备状态监控服务
- 自定义配置管理服务
- 系统级安全控制服务
- 应用行为分析服务

---

## 二、一个系统服务通常包含哪些部分

在 AOSP 中增加一个新的系统服务，通常需要修改以下几类内容：

1. **服务实现类**
   - 真正运行在 system_server 中的 Java 服务代码

2. **Binder 接口**
   - 用于客户端和服务端通信
   - 一般通过 AIDL 定义

3. **SystemServer 启动逻辑**
   - 让系统开机时自动启动这个服务

4. **编译配置**
   - 让新加的源码参与编译

5. **权限定义**
   - 如果客户端要访问服务，通常需要定义权限

6. **SELinux 策略**
   - 如果服务通过 ServiceManager 注册，通常需要补充 sepolicy

7. **客户端调用代码**
   - 应用或 framework 层通过 Binder 获取并使用这个服务

---

## 三、推荐的目录结构

假设你要新增一个叫 `DemoManagerService` 的系统服务，建议结构如下：

```text
frameworks/base/services/core/java/com/android/server/demo/
├── DemoManagerService.java
├── DemoManagerImpl.java
├── IDemoManager.aidl
└── DemoData.java
```

如果需要提供给客户端使用，还可能涉及：

```text
frameworks/base/core/java/android/os/
frameworks/base/core/java/android/app/
frameworks/base/core/java/android/content/
```

具体放在哪个包，要看你的服务定位：

| 场景 | 推荐包路径 |
|------|------------|
| 仅 system_server 内部使用 | `com.android.server.xxx` |
| 要给 framework 层公开 Manager | `android.xxx` |
| 要给 app 层调用 | `android.xxx` + AIDL + 权限 |

---

## 四、开发流程总览

完整流程通常是：

1. 设计服务职责
2. 编写 AIDL 接口
3. 编写服务实现类
4. 在 `SystemServer.java` 中注册启动
5. 修改 `Android.bp`
6. 如有需要，补充权限声明
7. 如有需要，补充 SELinux 策略
8. 编译系统镜像
9. 启动模拟器验证服务是否注册成功
10. 编写客户端调用代码验证功能

---

## 五、第一步：先设计你的服务

在动手写代码前，先明确下面几个问题：

### 5.1 服务是做什么的

例如：
- 保存和读取一组系统级配置
- 返回某种系统统计信息
- 监听系统事件后对外提供查询能力

### 5.2 服务是否需要给应用调用

- **不需要给应用调用**：只在 `system_server` 内部使用，最简单
- **需要给系统应用调用**：要设计 Binder 接口和权限控制
- **需要给普通应用调用**：通常风险较高，需要额外做权限和安全设计

### 5.3 服务数据是否要持久化

- 仅内存保存
- 写数据库
- 写文件
- 读取系统属性

### 5.4 服务是否需要开机启动

大多数系统服务都需要在开机时启动，因此一般会接入 `SystemServer`

---

## 六、第二步：定义 AIDL 接口

如果你的系统服务需要被别的进程调用，就要定义 AIDL。

### 6.1 示例：创建 `IDemoManager.aidl`

**文件路径**：

```text
frameworks/base/services/core/java/com/android/server/demo/IDemoManager.aidl
```

**示例内容**：

```aidl
package com.android.server.demo;

interface IDemoManager {
    String getMessage();
    void setMessage(String value);
}
```

这个接口表示客户端可以：
- 读取一个字符串
- 设置一个字符串

### 6.2 如果需要传复杂对象

如果参数不是基础类型，而是自定义对象，需要：

1. 创建 Java Parcelable 类
2. 创建对应的 `.aidl` 声明

例如：

```aidl
parcelable DemoData;
```

---

## 七、第三步：实现服务端逻辑

通常建议分成两层：

1. **Service 类**：负责挂到 SystemServer 中
2. **Binder 实现类**：真正处理外部调用

### 7.1 创建服务启动类

**文件路径**：

```text
frameworks/base/services/core/java/com/android/server/demo/DemoManagerService.java
```

**示例内容**：

```java
package com.android.server.demo;

import android.content.Context;
import android.util.Slog;

import com.android.server.SystemService;

public class DemoManagerService extends SystemService {
    private static final String TAG = "DemoManagerService";

    private DemoManagerImpl mBinderService;

    public DemoManagerService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        Slog.i(TAG, "Start DemoManagerService");
        mBinderService = new DemoManagerImpl(getContext());
        publishBinderService("demo_manager", mBinderService);
    }
}
```

这里最关键的是：

```java
publishBinderService("demo_manager", mBinderService);
```

它会把服务注册到 ServiceManager，服务名就是：

```text
demo_manager
```

### 7.2 创建 Binder 实现类

**文件路径**：

```text
frameworks/base/services/core/java/com/android/server/demo/DemoManagerImpl.java
```

**示例内容**：

```java
package com.android.server.demo;

import android.content.Context;

public class DemoManagerImpl extends IDemoManager.Stub {
    private final Context mContext;
    private String mMessage = "Hello from system service";

    public DemoManagerImpl(Context context) {
        mContext = context;
    }

    @Override
    public String getMessage() {
        return mMessage;
    }

    @Override
    public void setMessage(String value) {
        mMessage = value;
    }
}
```

---

## 八、第四步：在 SystemServer 中启动服务

**文件路径**：

```text
frameworks/base/services/java/com/android/server/SystemServer.java
```

### 8.1 添加 import

```java
import com.android.server.demo.DemoManagerService;
```

### 8.2 在合适的位置启动服务

通常是在 `startOtherServices()` 里添加。

示例：

```java
try {
    traceBeginAndSlog("StartDemoManagerService");
    mSystemServiceManager.startService(DemoManagerService.class);
    traceEnd();
} catch (Throwable e) {
    reportWtf("starting DemoManagerService", e);
}
```

### 8.3 怎么选启动位置

一般原则：
- 如果服务依赖其他系统服务，要放在依赖服务后面
- 如果服务比较独立，可以放在 `startOtherServices()` 的普通服务区域

例如：
- 依赖 `PackageManager`，就放在 PMS 之后
- 依赖 `ActivityManager`，就放在 AMS 初始化之后

---

## 九、第五步：修改编译配置

**文件路径**：

```text
frameworks/base/services/core/Android.bp
```

你要确保新加的服务源码被编译进去。

示例写法：

```bp
java_library {
    name: "services.core",
    srcs: [
        "java/com/android/server/demo/*.java",
    ],
}
```

如果原文件中已经有大范围通配包含到你的目录，也可能不用改。

所以这里一定要结合你当前 AOSP 的 `Android.bp` 实际内容判断。

---

## 十、第六步：如果要给客户端访问，增加权限控制

如果这是个可被应用访问的系统服务，建议定义权限。

### 10.1 在 `AndroidManifest.xml` 中声明权限

**文件路径**：

```text
frameworks/base/core/res/AndroidManifest.xml
```

添加：

```xml
<permission android:name="com.android.permission.ACCESS_DEMO_SERVICE"
    android:protectionLevel="signature"
    android:label="@string/permlab_accessDemoService"
    android:description="@string/permdesc_accessDemoService" />
```

### 10.2 在 `strings.xml` 中添加描述

**文件路径**：

```text
frameworks/base/core/res/res/values/strings.xml
```

添加：

```xml
<string name="permlab_accessDemoService">access demo service</string>
<string name="permdesc_accessDemoService">Allows the app to access the demo system service.</string>
```

### 10.3 在服务端做权限检查

在 Binder 方法里检查调用方权限：

```java
private void enforceAccessPermission() {
    mContext.enforceCallingOrSelfPermission(
            "com.android.permission.ACCESS_DEMO_SERVICE",
            "Requires ACCESS_DEMO_SERVICE permission");
}
```

然后在每个敏感接口里调用：

```java
@Override
public String getMessage() {
    enforceAccessPermission();
    return mMessage;
}
```

---

## 十一、第七步：补充 SELinux 策略

如果你用 `publishBinderService()` 向 ServiceManager 注册了一个新的服务名，通常需要补充 SELinux。

### 11.1 修改 `service.te`

**文件路径**：

```text
system/sepolicy/private/service.te
```

添加：

```te
type demo_manager_service, system_api_service, service_manager_type;
```

### 11.2 修改 `service_contexts`

**文件路径**：

```text
system/sepolicy/private/service_contexts
```

添加：

```text
demo_manager                    u:object_r:demo_manager_service:s0
```

### 11.3 同步到 prebuilts

如果你当前 Android 版本需要同步 prebuilts，也要在对应 API 目录下的同名文件做相同修改。

例如 Android 10 / API 29：

- `system/sepolicy/prebuilts/api/29.0/private/service.te`
- `system/sepolicy/prebuilts/api/29.0/private/service_contexts`

---

## 十二、第八步：如果要做 framework 层封装

如果你不想让客户端直接拿 Binder 接口，而是想像系统原生服务那样，通过 Manager 类访问，还可以继续做 framework 封装。

这一步不是必须，但更规范。

常见做法：

1. 在 `android.app` 或 `android.os` 下增加 Manager 类
2. 在 `Context.java` 中定义常量
3. 在 `SystemServiceRegistry.java` 中注册
4. 客户端通过 `getSystemService()` 获取

### 12.1 在 `Context.java` 增加常量

```java
public static final String DEMO_SERVICE = "demo_manager";
```

### 12.2 增加 Manager 类

例如：

```java
package android.app;

import android.content.Context;
import android.os.RemoteException;

import com.android.server.demo.IDemoManager;

public class DemoManager {
    private final Context mContext;
    private final IDemoManager mService;

    public DemoManager(Context context, IDemoManager service) {
        mContext = context;
        mService = service;
    }

    public String getMessage() {
        try {
            return mService.getMessage();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
```

### 12.3 在 `SystemServiceRegistry.java` 注册

这样客户端就能：

```java
DemoManager manager = (DemoManager) context.getSystemService(Context.DEMO_SERVICE);
```

---

## 十三、第九步：编译与验证

### 13.1 编译

```bash
cd ~/aosp
source build/envsetup.sh
lunch aosp_x86_64-eng
m -j4
```

### 13.2 启动模拟器

```bash
emulator -verbose -show-kernel
```

### 13.3 检查服务是否注册成功

```bash
adb shell service list | grep demo
```

如果成功，能看到类似：

```text
demo_manager: [com.android.server.demo.IDemoManager]
```

### 13.4 查看服务启动日志

```bash
adb logcat | grep DemoManagerService
```

---

## 十四、第十步：客户端调用方式

### 14.1 直接通过 ServiceManager 调用

适合测试：

```java
IBinder binder = ServiceManager.getService("demo_manager");
IDemoManager service = IDemoManager.Stub.asInterface(binder);
String msg = service.getMessage();
```

### 14.2 通过 Manager 封装调用

适合正式项目：

```java
DemoManager manager = (DemoManager) context.getSystemService(Context.DEMO_SERVICE);
String msg = manager.getMessage();
```

---

## 十五、常见问题

### 15.1 编译通过但服务没起来

重点检查：
- `SystemServer.java` 是否真的加了启动代码
- 新服务类是否真的参与编译
- 启动代码有没有跑到
- logcat 是否有异常

### 15.2 `service list` 看不到服务

重点检查：
- `publishBinderService()` 是否执行到了
- 服务启动前是否抛异常
- SELinux 是否拦截

### 15.3 客户端调用时报权限错误

重点检查：
- 是否定义了自定义权限
- 是否在服务端做了 `enforceCallingOrSelfPermission()`
- 客户端 apk 是否具备对应权限
- 如果是 `signature` 权限，签名是否一致

### 15.4 开机报 SELinux 错误

重点检查：
- `service.te` 是否新增类型
- `service_contexts` 是否新增映射
- `prebuilts/api/xx/private/` 是否同步

### 15.5 明明改了代码但模拟器效果没变

重点检查三件事是否一致：
1. 改的是哪套源码
2. 当前 shell 编的是哪套 target
3. 模拟器跑的是哪套镜像

---

## 十六、最小可行接入清单

如果你只想先把一个最小系统服务跑起来，最少一般要做这些事：

1. 新建 AIDL 接口
2. 新建 Binder 实现类
3. 新建 `SystemService` 子类
4. 在 `SystemServer.java` 里启动它
5. 修改 `Android.bp`
6. 如有新服务名，补充 SELinux
7. 编译并用 `adb shell service list` 验证

---

## 十七、建议的学习顺序

建议你按下面顺序学：

1. 先写一个**只返回字符串**的最小服务
2. 再加入**权限校验**
3. 再加入**Parcelable 数据对象**
4. 再加入 **Manager 封装**
5. 最后再做复杂逻辑（数据库、监听器、回调、持久化等）

这样最不容易在一开始就被 AOSP 编译链、权限链和 SELinux 链同时卡住。

---

## 十八、总结

在 AOSP 里自定义一个系统服务，本质上就是完成下面这条链路：

```text
AIDL 定义
→ Binder 实现
→ SystemService 启动
→ SystemServer 注册
→ Android.bp 编译接入
→ SELinux 放行
→ 客户端访问
```

只要你把这条链路一段一段打通，一个新的系统服务就能成功跑起来。

这份文档是通用教学版，而你当前项目里的 [AOSP源码手动修改指南.md](file:///d:/AppPermissionMonitorService/参考文档/部署参考/AOSP源码手动修改指南.md) 更适合拿来对照当前权限监控服务的具体实现。

---

**更新日期**：2026年5月

**版本**：1.0
