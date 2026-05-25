
# AOSP 源码手动修改指南

## 一、概述

本指南详细描述如何**手动修改 AOSP 源码**来集成权限监控服务，适合需要深入理解代码结构的开发者。

**修改目标**：在 AOSP 中添加一个新的 `PermissionMonitorService` 系统服务，用于监控应用权限使用情况。

**涉及模块**：
- `frameworks/base/services/core/` - 服务核心实现
- `frameworks/base/core/res/` - 系统资源和权限定义
- `system/sepolicy/` - SELinux 策略配置

---

## 二、创建权限监控服务核心文件

### 2.1 创建服务主类

**文件路径**：`frameworks/base/services/core/java/com/android/server/permissionmonitor/PermissionMonitorService.java`

**创建步骤**：

1. 创建目录结构：
```bash
mkdir -p frameworks/base/services/core/java/com/android/server/permissionmonitor
```

2. 创建文件内容：

```java
package com.android.server.permissionmonitor;

import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;

import com.android.internal.os.IResultReceiver;
import com.android.server.SystemService;

import java.util.List;

public class PermissionMonitorService extends SystemService {
    private static final String TAG = "PermissionMonitorService";
    
    private PermissionMonitorManager mManager;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    
    public PermissionMonitorService(Context context) {
        super(context);
    }
    
    @Override
    public void onStart() {
        Slog.i(TAG, "Starting PermissionMonitorService");
        
        mHandlerThread = new HandlerThread("PermissionMonitorService");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        
        mManager = new PermissionMonitorManager(getContext(), mHandler);
        
        publishBinderService(Context.PERMISSION_MONITOR_SERVICE, mManager);
    }
    
    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_SYSTEM_SERVICES_READY) {
            mManager.startMonitoring();
        }
    }
}
```

### 2.2 创建监控管理器

**文件路径**：`frameworks/base/services/core/java/com/android/server/permissionmonitor/PermissionMonitorManager.java`

```java
package com.android.server.permissionmonitor;

import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Slog;

import java.util.List;

public class PermissionMonitorManager extends IPermissionMonitor.Stub {
    private static final String TAG = "PermissionMonitorManager";
    
    private final Context mContext;
    private final Handler mHandler;
    private final RemoteCallbackList<IPermissionCallback> mCallbacks = new RemoteCallbackList<>();
    private final PermissionRepository mRepository;
    private final AppOpsScanner mScanner;
    
    public PermissionMonitorManager(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
        mRepository = new PermissionRepository(context);
        mScanner = new AppOpsScanner(context, mRepository, this::onPermissionUsed);
    }
    
    public void startMonitoring() {
        mScanner.startScan();
    }
    
    private void onPermissionUsed(PermissionRecord record) {
        mRepository.insertRecord(record);
        
        final int count = mCallbacks.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                mCallbacks.getBroadcastItem(i).onPermissionUsed(record);
            } catch (RemoteException e) {
                Slog.w(TAG, "Callback failed", e);
            }
        }
        mCallbacks.finishBroadcast();
    }
    
    @Override
    public void registerCallback(IPermissionCallback callback) {
        if (callback != null) {
            mCallbacks.register(callback);
        }
    }
    
    @Override
    public void unregisterCallback(IPermissionCallback callback) {
        if (callback != null) {
            mCallbacks.unregister(callback);
        }
    }
    
    @Override
    public List<PermissionRecord> getPermissionRecords(String packageName) {
        return mRepository.getRecords(packageName);
    }
    
    @Override
    public List<String> getMonitoredPackages() {
        return mRepository.getMonitoredPackages();
    }
}
```

### 2.3 创建 AppOps 扫描器

**文件路径**：`frameworks/base/services/core/java/com/android/server/permissionmonitor/AppOpsScanner.java`

```java
package com.android.server.permissionmonitor;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Slog;

import java.util.List;

public class AppOpsScanner {
    private static final String TAG = "AppOpsScanner";
    private static final long SCAN_INTERVAL_MS = 5000; // 5秒扫描一次
    
    private final Context mContext;
    private final PermissionRepository mRepository;
    private final Handler mHandler;
    private final OnPermissionUsedListener mListener;
    private final AppOpsManager mAppOpsManager;
    
    public interface OnPermissionUsedListener {
        void onPermissionUsed(PermissionRecord record);
    }
    
    public AppOpsScanner(Context context, PermissionRepository repository, 
                        OnPermissionUsedListener listener) {
        mContext = context;
        mRepository = repository;
        mListener = listener;
        mHandler = new Handler();
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
    }
    
    public void startScan() {
        mHandler.post(mScanRunnable);
    }
    
    private final Runnable mScanRunnable = new Runnable() {
        @Override
        public void run() {
            performScan();
            mHandler.postDelayed(this, SCAN_INTERVAL_MS);
        }
    };
    
    private void performScan() {
        // 扫描敏感权限的使用情况
        int[] sensitiveOps = {
            AppOpsManager.OP_CAMERA,
            AppOpsManager.OP_RECORD_AUDIO,
            AppOpsManager.OP_READ_CONTACTS,
            AppOpsManager.OP_ACCESS_FINE_LOCATION,
            AppOpsManager.OP_READ_SMS
        };
        
        for (int op : sensitiveOps) {
            checkOpUsage(op);
        }
    }
    
    private void checkOpUsage(int op) {
        // 获取所有使用该权限的应用
        List<AppOpsManager.PackageOps> packageOps = mAppOpsManager.getPackagesForOps(new int[]{op});
        
        for (AppOpsManager.PackageOps pkgOps : packageOps) {
            String packageName = pkgOps.getPackageName();
            
            for (AppOpsManager.OpEntry opEntry : pkgOps.getOps()) {
                if (opEntry.getOp() == op && opEntry.getLastAccessTime() > 0) {
                    PermissionRecord record = new PermissionRecord(
                        packageName,
                        getPermissionNameFromOp(op),
                        opEntry.getLastAccessTime(),
                        opEntry.getUid()
                    );
                    mListener.onPermissionUsed(record);
                }
            }
        }
    }
    
    private String getPermissionNameFromOp(int op) {
        switch (op) {
            case AppOpsManager.OP_CAMERA:
                return "android.permission.CAMERA";
            case AppOpsManager.OP_RECORD_AUDIO:
                return "android.permission.RECORD_AUDIO";
            case AppOpsManager.OP_READ_CONTACTS:
                return "android.permission.READ_CONTACTS";
            case AppOpsManager.OP_ACCESS_FINE_LOCATION:
                return "android.permission.ACCESS_FINE_LOCATION";
            case AppOpsManager.OP_READ_SMS:
                return "android.permission.READ_SMS";
            default:
                return "unknown";
        }
    }
}
```

### 2.4 创建数据仓库

**文件路径**：`frameworks/base/services/core/java/com/android/server/permissionmonitor/PermissionRepository.java`

```java
package com.android.server.permissionmonitor;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionRepository {
    private final PermissionDatabase mDatabase;
    private final Map<String, List<PermissionRecord>> mCache = new ConcurrentHashMap<>();
    
    public PermissionRepository(Context context) {
        mDatabase = new PermissionDatabase(context);
    }
    
    public void insertRecord(PermissionRecord record) {
        mDatabase.insert(record);
        mCache.clear(); // 清除缓存
    }
    
    public List<PermissionRecord> getRecords(String packageName) {
        if (packageName == null) {
            return mDatabase.getAllRecords();
        }
        
        List<PermissionRecord> cached = mCache.get(packageName);
        if (cached == null) {
            cached = mDatabase.getRecordsByPackage(packageName);
            mCache.put(packageName, cached);
        }
        return cached;
    }
    
    public List<String> getMonitoredPackages() {
        return mDatabase.getDistinctPackages();
    }
    
    public int deleteOldRecords(long thresholdTime) {
        return mDatabase.deleteOldRecords(thresholdTime);
    }
}
```

### 2.5 创建数据库帮助类

**文件路径**：`frameworks/base/services/core/java/com/android/server/permissionmonitor/PermissionDatabase.java`

```java
package com.android.server.permissionmonitor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Slog;

import java.util.ArrayList;
import java.util.List;

public class PermissionDatabase {
    private static final String TAG = "PermissionDatabase";
    private static final String DB_NAME = "permission_monitor.db";
    private static final int DB_VERSION = 1;
    
    private static final String TABLE_RECORDS = "records";
    private static final String COL_ID = "_id";
    private static final String COL_PACKAGE_NAME = "package_name";
    private static final String COL_PERMISSION_NAME = "permission_name";
    private static final String COL_ACCESS_TIME = "access_time";
    private static final String COL_UID = "uid";
    
    private final DatabaseHelper mHelper;
    
    private static class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTable = "CREATE TABLE " + TABLE_RECORDS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PACKAGE_NAME + " TEXT NOT NULL, " +
                COL_PERMISSION_NAME + " TEXT NOT NULL, " +
                COL_ACCESS_TIME + " LONG NOT NULL, " +
                COL_UID + " INTEGER NOT NULL)";
            db.execSQL(createTable);
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // 升级逻辑
        }
    }
    
    public PermissionDatabase(Context context) {
        mHelper = new DatabaseHelper(context);
    }
    
    public void insert(PermissionRecord record) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        try {
            db.insert(TABLE_RECORDS, null, record.toContentValues());
        } finally {
            db.close();
        }
    }
    
    public List<PermissionRecord> getAllRecords() {
        return query(null, null);
    }
    
    public List<PermissionRecord> getRecordsByPackage(String packageName) {
        return query(COL_PACKAGE_NAME + "=?", new String[]{packageName});
    }
    
    private List<PermissionRecord> query(String selection, String[] selectionArgs) {
        List<PermissionRecord> records = new ArrayList<>();
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = null;
        
        try {
            cursor = db.query(TABLE_RECORDS, null, selection, selectionArgs, 
                            null, null, COL_ACCESS_TIME + " DESC");
            
            while (cursor.moveToNext()) {
                PermissionRecord record = new PermissionRecord(
                    cursor.getString(cursor.getColumnIndex(COL_PACKAGE_NAME)),
                    cursor.getString(cursor.getColumnIndex(COL_PERMISSION_NAME)),
                    cursor.getLong(cursor.getColumnIndex(COL_ACCESS_TIME)),
                    cursor.getInt(cursor.getColumnIndex(COL_UID))
                );
                records.add(record);
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return records;
    }
    
    public List<String> getDistinctPackages() {
        List<String> packages = new ArrayList<>();
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = null;
        
        try {
            cursor = db.query(true, TABLE_RECORDS, new String[]{COL_PACKAGE_NAME},
                            null, null, COL_PACKAGE_NAME, null, null, null);
            
            while (cursor.moveToNext()) {
                packages.add(cursor.getString(0));
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return packages;
    }
    
    public int deleteOldRecords(long thresholdTime) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        try {
            return db.delete(TABLE_RECORDS, COL_ACCESS_TIME + " < ?", 
                            new String[]{String.valueOf(thresholdTime)});
        } finally {
            db.close();
        }
    }
}
```

---

## 三、创建 AIDL 接口文件

### 3.1 主接口

**文件路径**：`frameworks/base/services/core/java/com/android/server/permissionmonitor/IPermissionMonitor.aidl`

```aidl
package com.android.server.permissionmonitor;

import com.android.server.permissionmonitor.PermissionRecord;

interface IPermissionMonitor {
    void registerCallback(IPermissionCallback callback);
    void unregisterCallback(IPermissionCallback callback);
    List<PermissionRecord> getPermissionRecords(String packageName);
    List<String> getMonitoredPackages();
}
```

### 3.2 回调接口

**文件路径**：`frameworks/base/services/core/java/com/android/server/permissionmonitor/IPermissionCallback.aidl`

```aidl
package com.android.server.permissionmonitor;

import com.android.server.permissionmonitor.PermissionRecord;

interface IPermissionCallback {
    void onPermissionUsed(in PermissionRecord record);
}
```

### 3.3 数据实体

**文件路径**：`frameworks/base/services/core/java/com/android/server/permissionmonitor/PermissionRecord.aidl`

```aidl
package com.android.server.permissionmonitor;

parcelable PermissionRecord;
```

### 3.4 数据实体实现类

**文件路径**：`frameworks/base/services/core/java/com/android/server/permissionmonitor/PermissionRecord.java`

```java
package com.android.server.permissionmonitor;

import android.os.Parcel;
import android.os.Parcelable;
import android.content.ContentValues;

public class PermissionRecord implements Parcelable {
    private String mPackageName;
    private String mPermissionName;
    private long mAccessTime;
    private int mUid;
    
    public PermissionRecord(String packageName, String permissionName, long accessTime, int uid) {
        mPackageName = packageName;
        mPermissionName = permissionName;
        mAccessTime = accessTime;
        mUid = uid;
    }
    
    protected PermissionRecord(Parcel in) {
        mPackageName = in.readString();
        mPermissionName = in.readString();
        mAccessTime = in.readLong();
        mUid = in.readInt();
    }
    
    public static final Creator<PermissionRecord> CREATOR = new Creator<PermissionRecord>() {
        @Override
        public PermissionRecord createFromParcel(Parcel in) {
            return new PermissionRecord(in);
        }
        
        @Override
        public PermissionRecord[] newArray(int size) {
            return new PermissionRecord[size];
        }
    };
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPackageName);
        dest.writeString(mPermissionName);
        dest.writeLong(mAccessTime);
        dest.writeInt(mUid);
    }
    
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put("package_name", mPackageName);
        values.put("permission_name", mPermissionName);
        values.put("access_time", mAccessTime);
        values.put("uid", mUid);
        return values;
    }
    
    // Getter methods
    public String getPackageName() { return mPackageName; }
    public String getPermissionName() { return mPermissionName; }
    public long getAccessTime() { return mAccessTime; }
    public int getUid() { return mUid; }
}
```

---

## 四、修改系统服务配置

### 4.1 修改 SystemServer

**文件路径**：`frameworks/base/services/java/com/android/server/SystemServer.java`

**修改步骤**：

1. 在 `startOtherServices()` 方法中找到服务启动区域
2. 添加以下代码（通常在 `ActivityManagerService` 之后）：

```java
// 在 try-catch 块中添加
try {
    traceBeginAndSlog("StartPermissionMonitorService");
    mSystemServiceManager.startService(PermissionMonitorService.class);
    traceEnd();
} catch (Throwable e) {
    reportWtf("starting PermissionMonitorService", e);
}
```

### 4.2 修改 Android.bp

**文件路径**：`frameworks/base/services/core/Android.bp`

**修改步骤**：

在 `services.core` 模块的 `srcs` 数组中添加新文件：

```bp
java_library {
    name: "services.core",
    srcs: [
        // ... 已有文件 ...
        "java/com/android/server/permissionmonitor/*.java",
        "java/com/android/server/permissionmonitor/aidl/*.java", // 如果使用单独的 aidl 目录
    ],
    // ... 其他配置 ...
}
```

**注意**：AIDL 文件会自动编译，无需手动添加。

---

## 五、添加系统权限定义

### 5.1 修改 AndroidManifest.xml

**文件路径**：`frameworks/base/core/res/AndroidManifest.xml`

**添加位置**：在其他 `<permission>` 声明附近添加：

```xml
<permission android:name="com.android.permission.ACCESS_PERMISSION_MONITOR"
    android:protectionLevel="signature"
    android:label="@string/permlab_accessPermissionMonitor"
    android:description="@string/permdesc_accessPermissionMonitor" />
```

### 5.2 添加字符串资源

**文件路径**：`frameworks/base/core/res/res/values/strings.xml`

添加以下字符串：

```xml
<!-- Permission Monitor -->
<string name="permlab_accessPermissionMonitor">Access Permission Monitor</string>
<string name="permdesc_accessPermissionMonitor">Allows the application to access the permission monitoring service.</string>
```

---

## 六、配置 SELinux 策略

### 6.1 修改 service.te

**文件路径**：`system/sepolicy/private/service.te`

添加以下内容：

```te
type permission_monitor_service, system_api_service, service_manager_type;
```

### 6.2 修改 service_contexts

**文件路径**：`system/sepolicy/private/service_contexts`

添加以下内容：

```
permission_monitor                    u:object_r:permission_monitor_service:s0
```

### 6.3 同步到 prebuilts

#### 6.3.1 为什么需要同步

AOSP 中的 SELinux 策略文件需要同时存在于两个位置：
- `system/sepolicy/private/` - 开发态源码目录
- `system/sepolicy/prebuilts/api/<version>/private/` - API 基线预构建目录

这是因为：
1. **向后兼容性**：prebuilts 目录包含不同 API 版本的策略基线
2. **编译依赖**：某些模块编译时依赖 prebuilts 中的策略文件
3. **OTA 更新**：SELinux 策略的 prebuilts 用于生成 OTA 更新包

#### 6.3.2 同步操作步骤

**步骤 1：确认 API 版本**

首先确定当前编译目标的 API 版本：

```bash
# 查看当前 target 的 API 级别
getprop ro.build.version.sdk

# 或者从 build 配置中查看
cat build/make/core/version_defaults.mk | grep PLATFORM_SDK_VERSION
```

对于 Android 10 (Q)，API 级别通常是 **29**。

**步骤 2：同步 service.te**

```bash
# 复制策略类型定义
cp system/sepolicy/private/service.te \
    system/sepolicy/prebuilts/api/29.0/private/service.te
```

**步骤 3：同步 service_contexts**

```bash
# 复制服务上下文映射
cp system/sepolicy/private/service_contexts \
    system/sepolicy/prebuilts/api/29.0/private/service_contexts
```

#### 6.3.3 验证同步结果

```bash
# 验证文件内容一致
diff system/sepolicy/private/service.te \
    system/sepolicy/prebuilts/api/29.0/private/service.te

diff system/sepolicy/private/service_contexts \
    system/sepolicy/prebuilts/api/29.0/private/service_contexts

# 如果输出为空，表示文件内容一致
```

#### 6.3.4 注意事项

| 注意事项 | 说明 |
|----------|------|
| **API 版本匹配** | 确保同步到正确的 API 版本目录（如 29.0、30.0 等） |
| **完整复制** | 必须复制整个文件，而不仅仅是新增内容 |
| **版本升级** | 如果同时支持多个 API 版本，需要同步到所有相关目录 |
| **冲突处理** | 如果 prebuilts 中的文件被其他修改占用，需要手动合并 |

#### 6.3.5 多版本同步（可选）

如果需要支持多个 API 版本，可以使用循环批量同步：

```bash
# 同步到所有支持的 API 版本
for api_version in 29.0 30.0 31.0; do
    if [ -d "system/sepolicy/prebuilts/api/${api_version}/private/" ]; then
        cp system/sepolicy/private/service.te \
            system/sepolicy/prebuilts/api/${api_version}/private/service.te
        cp system/sepolicy/private/service_contexts \
            system/sepolicy/prebuilts/api/${api_version}/private/service_contexts
        echo "Synced to API ${api_version}"
    fi
done
```

---

## 七、更新 API 基线

### 7.1 更新 API 定义

**文件路径**：`frameworks/base/api/current.txt`

添加以下内容（如果需要公开 API）：

```
package com.android.server.permissionmonitor {
    public class PermissionRecord implements android.os.Parcelable {
        public PermissionRecord(java.lang.String, java.lang.String, long, int);
        public int describeContents();
        public int getUid();
        public long getAccessTime();
        public java.lang.String getPackageName();
        public java.lang.String getPermissionName();
        public void writeToParcel(android.os.Parcel, int);
        public static final android.os.Parcelable.Creator<com.android.server.permissionmonitor.PermissionRecord> CREATOR;
    }
}
```

### 7.2 编译 API 文档

```bash
cd ~/aosp
source build/envsetup.sh
lunch aosp_x86_64-eng
make api-stubs-docs-update-current-api -j2
```

---

## 八、编译验证

### 8.1 编译服务模块

```bash
cd ~/aosp
source build/envsetup.sh
lunch aosp_x86_64-eng
m services.core -j4
```

### 8.2 编译完整镜像

```bash
make systemimage -j4
```

### 8.3 验证服务是否注册

启动模拟器后执行：

```bash
adb shell service list | grep permission
```

预期输出：
```
50  permission_monitor: [com.android.server.permissionmonitor.IPermissionMonitor]
```

---

## 九、修改清单总结

| 文件路径 | 修改类型 | 说明 |
|----------|----------|------|
| `PermissionMonitorService.java` | 新建 | 服务主类 |
| `PermissionMonitorManager.java` | 新建 | 监控管理器，实现 AIDL 接口 |
| `AppOpsScanner.java` | 新建 | AppOps 扫描器，检测权限使用 |
| `PermissionRepository.java` | 新建 | 数据仓库，管理缓存和数据库 |
| `PermissionDatabase.java` | 新建 | SQLite 数据库操作类 |
| `PermissionRecord.java` | 新建 | 数据实体，实现 Parcelable |
| `IPermissionMonitor.aidl` | 新建 | 主 AIDL 接口 |
| `IPermissionCallback.aidl` | 新建 | 回调 AIDL 接口 |
| `PermissionRecord.aidl` | 新建 | Parcelable 声明 |
| `SystemServer.java` | 修改 | 添加服务启动代码 |
| `Android.bp` | 修改 | 添加新文件到编译配置 |
| `AndroidManifest.xml` | 修改 | 添加自定义权限 |
| `strings.xml` | 修改 | 添加权限描述字符串 |
| `service.te` | 修改 | 添加 SELinux 类型 |
| `service_contexts` | 修改 | 添加服务上下文映射 |
| `current.txt` | 修改 | 更新 API 基线 |

---

## 十、常见问题

### 10.1 编译错误：找不到符号

**原因**：AIDL 文件未正确编译或包路径错误

**解决方案**：
```bash
# 清理并重新编译
make clean
m services.core -j4
```

### 10.2 服务启动失败

**原因**：SELinux 权限不足或服务注册失败

**解决方案**：
```bash
# 检查 SELinux 日志
adb logcat | grep avc:
```

### 10.3 权限被拒绝

**原因**：调用方没有 `com.android.permission.ACCESS_PERMISSION_MONITOR` 权限

**解决方案**：确保客户端应用使用平台签名

---

**更新日期**：2026年5月

**版本**：1.0
