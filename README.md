App Permission Monitor Service
这是一个基于 AOSP 定制开发的应用权限监控系统项目，目标是在 Android 系统层实现一个可被客户端调用的 SystemService，用于实时采集、保存和查询应用对敏感权限的使用记录。
项目不仅包含一个 Android 客户端界面，还包含完整的 AOSP 系统服务接入代码，包括：
SystemServer 中注册的系统服务
AIDL / Binder 跨进程接口
AppOps 扫描逻辑
SQLite 持久化存储
SEPolicy 与 service_contexts 配置
客户端可视化查询与实时提示页面
它本质上是一个“系统级权限使用监控平台”，适合用于安卓框架开发、课程设计、系统定制和权限审计实验。
项目特点
基于 SystemService 实现系统级权限监控服务
使用 Binder + AIDL 向客户端暴露接口
基于 AppOpsManager 周期性扫描敏感权限使用记录
支持实时回调通知
支持历史记录持久化保存
支持按应用、时间范围查询权限记录
支持客户端展示应用列表、权限详情、统计和设置
包含 AOSP 集成所需的 SystemServer、SEPolicy 和服务注册修改
项目目标
项目希望实现以下能力：
实时监控应用对敏感权限的使用
保存权限调用历史记录
提供系统服务接口供客户端查询
支持客户端注册回调，接收实时权限事件
支持通过系统权限保护服务访问
整体架构
项目可分为三部分：
1. 系统服务端
位于 aosp/frameworks/base/services/core/java/com/android/server/permissionmonitor/
主要负责：
启动权限监控服务
定时扫描权限使用情况
保存记录到数据库
向客户端提供 Binder 接口
通过回调把新事件推送给客户端
2. AOSP 集成层
位于 aosp/ 目录中的系统改动文件，主要包括：
SystemServer.java
Android.bp
core/res/AndroidManifest.xml
strings.xml
service_contexts
service.te
这部分负责把自定义服务真正接入 Android 系统框架。
3. Android 客户端
位于 app/ 目录。
客户端不是独立的数据源，而是系统服务的使用方，主要功能包括：
连接系统服务
查询最近权限使用记录
展示触发过敏感权限的应用列表
查看单个应用的权限详情
接收实时权限使用通知
配置监控权限类型
核心模块说明
PermissionMonitorService
核心系统服务类，继承 SystemService，在系统启动后注册为：
permission_monitor
主要职责：
启动监控线程
发布 Binder 服务
管理客户端回调列表
向外提供查询、清理、状态获取等接口
PermissionMonitorManager
服务内部的协调层，连接扫描器和数据仓库，负责：
初始化数据库
启动/停止扫描器
接收新权限记录
将记录写入仓库
触发上层监听器
AppOpsScanner
权限记录采集模块，基于 AppOpsManager 工作。
当前实现特点：
每 1s 扫描一次
默认监控以下敏感权限相关操作：CAMERA
RECORD_AUDIO
ACCESS_COARSE_LOCATION
ACCESS_FINE_LOCATION
READ_CONTACTS
WRITE_CONTACTS
READ_CALENDAR
WRITE_CALENDAR
READ_SMS
WRITE_SMS

通过比较 lastScanTime 与当前扫描时间窗口识别新增权限访问事件
PermissionDatabase / PermissionRepository
负责持久化保存权限使用记录，底层使用 SQLite。
数据库表主要字段包括：
package_name
permission_name
timestamp
is_blocked
extra_info
同时建立了按包名、时间戳、权限名的索引，便于查询。
AIDL 接口
项目定义了以下跨进程接口：
IPermissionMonitor.aidl
IPermissionCallback.aidl
PermissionRecord.aidl
主要能力包括：
获取某个应用的权限记录
获取全部权限记录
注册/取消注册回调
获取当前监控权限列表
设置监控权限列表
清理旧记录
获取服务状态
客户端功能
客户端代码位于：
app/src/main/java/com/example/apppermissionmonitorservice/client/
主要页面包括：
MainActivity
连接系统服务
展示应用列表
接收实时权限事件
显示实时卡片和通知

PermissionDetailActivity
查看某个应用的权限使用详情

SettingsActivity
选择监控的权限类型

StatsActivity
展示统计信息

目录结构
.
├─ aosp/                         AOSP 系统服务接入代码
│  ├─ frameworks/base/services/core/java/com/android/server/permissionmonitor/
│  ├─ frameworks/base/services/java/com/android/server/SystemServer.java
│  └─ system/sepolicy/
├─ app/                          Android 客户端
│  ├─ src/main/aidl/
│  ├─ src/main/java/
│  └─ src/main/res/
├─ apk/                          已构建 APK
├─ 参考文档/                     项目总结与部署资料
├─ gradle/
└─ build.gradle.kts
关键文件
aosp/frameworks/base/services/core/java/com/android/server/permissionmonitor/PermissionMonitorService.java
系统服务入口

aosp/frameworks/base/services/core/java/com/android/server/permissionmonitor/PermissionMonitorManager.java
服务内部协调器

aosp/frameworks/base/services/core/java/com/android/server/permissionmonitor/AppOpsScanner.java
权限使用扫描器

aosp/frameworks/base/services/core/java/com/android/server/permissionmonitor/PermissionDatabase.java
数据库定义与 DAO

aosp/frameworks/base/services/java/com/android/server/SystemServer.java
系统服务启动接入点

aosp/system/sepolicy/private/service.te
SEPolicy 服务类型声明

aosp/system/sepolicy/private/service_contexts
服务名到 SELinux 上下文映射

app/src/main/java/com/example/apppermissionmonitorservice/client/ui/MainActivity.java
客户端主界面

服务注册与系统集成
从仓库内容看，这个项目已经包含了较完整的 AOSP 集成修改：
1. SystemServer 启动
在 SystemServer.java 中引入并启动：
com.android.server.permissionmonitor.PermissionMonitorService
2. 服务名注册
服务名为：
permission_monitor
3. SEPolicy
项目已经新增：
permission_monitor_service 类型
service_contexts 中的 permission_monitor 映射
4. 权限保护
服务访问依赖自定义权限：
com.android.permission.ACCESS_PERMISSION_MONITOR
这意味着普通第三方应用不能直接调用，通常需要系统签名或系统镜像集成。
数据流说明
整个权限监控流程大致如下：
系统启动后，PermissionMonitorService 注册到 ServiceManager
开机完成后初始化监控逻辑
AppOpsScanner 周期性扫描敏感权限使用记录
新记录交给 PermissionMonitorManager
管理器把数据写入 SQLite
服务通过回调把新事件通知给客户端
客户端刷新列表、弹出提示并发送通知
构建环境
客户端
Android Studio
Gradle Kotlin DSL
AIDL 支持
系统服务端
AOSP 源码环境
支持修改 frameworks/base 与 system/sepolicy
需要重新编译系统镜像或相关模块
客户端构建
可以直接在 Android Studio 中打开项目，或使用 Gradle 构建：
./gradlew assembleDebug
Windows 下：
gradlew.bat assembleDebug
仓库内已包含示例构建产物：
apk/app-debug.apk
apk/app-platform-signed.apk
AOSP 集成思路
根据仓库中的文档和代码，典型接入流程如下：
将 permissionmonitor 目录下的服务代码放入 frameworks/base/services/core/java/com/android/server/
修改 Android.bp，把新服务加入编译
修改 SystemServer.java，在系统启动流程中启动服务
在 core/res/AndroidManifest.xml 中声明自定义权限
在 strings.xml 中补充描述
在 system/sepolicy 中添加服务类型与上下文映射
重新编译 AOSP
刷入系统镜像并验证服务是否注册成功
更细的部署说明可以看：
参考文档/部署参考/部署指南.md
参考文档/部署参考/AOSP自定义系统服务开发指南.md
参考文档/项目总结.md
验证方式
部署完成后，可通过以下方式检查：
adb shell service list | grep permission
adb shell dumpsys permission_monitor
adb logcat -s PermissionMonitorService
也可以主动触发权限使用行为，例如打开相机或录音应用，再观察客户端是否收到实时事件。
适用场景
Android Framework 开发课程设计
自定义系统服务教学示例
AOSP Binder / AIDL / SystemService 实战项目
权限审计与敏感操作监控原型
系统定制 ROM 的安全能力验证
当前已知问题
从源码和项目总结来看，当前版本有一个比较明确的问题：
设置页虽然支持配置“监控哪些权限”
服务端也提供了 setMonitoredPermissions() 接口
但 AppOpsScanner 仍然使用内部默认的监控权限集合进行扫描
也就是说，客户端配置项和底层扫描逻辑还没有完全打通，这是后续最值得优先修复的点。
另外还要注意：
这是系统级项目，不适合当作普通独立 APK 直接部署到商用手机
服务调用依赖系统权限和系统签名
AOSP 版本差异可能影响 AppOps 行为和系统集成细节
后续优化建议
让设置页选择的权限真正驱动 AppOpsScanner
将轮询扫描优化为更低功耗的策略
增加分页查询，改善大规模记录读取性能
增加图表化统计
补充多用户场景支持
明确不同 Android 版本下的适配说明
参考资料
仓库内已经附带了较多参考文档：
参考文档/项目总结.md
参考文档/参考文档.md
参考文档/部署参考/部署指南.md
参考文档/部署参考/AOSP自定义系统服务开发指南.md
参考文档/开发方向选择说明/设置监控权限类型说明.md
