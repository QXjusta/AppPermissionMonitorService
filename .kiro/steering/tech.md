---
inclusion: always
---
# 技术栈与构建系统

## 构建系统
- **Gradle**：使用Gradle作为构建工具
- **Kotlin DSL**：使用Kotlin DSL编写构建脚本（.gradle.kts文件）
- **版本管理**：通过gradle/libs.versions.toml管理依赖版本

## 技术栈
### 核心框架
- **Android Framework**：开发系统级服务
- **Binder IPC**：跨进程通信机制
- **AIDL**：定义跨进程接口
- **SystemService**：自定义系统服务基类
- **AppOpsManager**：获取权限操作记录

### 开发语言
- **Java**：主要开发语言（用于系统服务开发）
- **Kotlin**：可选用于客户端应用开发

### 数据存储
- **SQLite**：推荐用于权限记录存储
- **SharedPreferences**：简单配置存储
- **Parcelable**：跨进程数据传输

### 客户端技术
- **AndroidX**：使用AndroidX库
- **Material Design**：UI设计规范
- **RecyclerView**：列表展示
- **图表库**：MPAndroidChart或HelloCharts（可选）

## 开发环境
- **Android Studio**：主要开发IDE
- **Android SDK**：API 29+（minSdk 29）
- **Java 11**：编译目标版本
- **AOSP源码**：需要访问Android开源项目源码（用于方案A）

## 常用命令
### 构建命令
```bash
# 清理项目
./gradlew clean

# 构建APK
./gradlew assembleDebug
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug
```

### 测试命令
```bash
# 运行单元测试
./gradlew test

# 运行Android测试
./gradlew connectedAndroidTest
```

### 系统相关命令
```bash
# 查看服务状态
adb shell dumpsys activity service com.example.apppermissionmonitorservice

# 查看日志
adb logcat -s PermissionMonitor

# 查看应用权限
adb shell pm list permissions
```

## 依赖管理
依赖版本在`gradle/libs.versions.toml`中统一管理：
- androidx.appcompat: 1.6.1
- com.google.android.material: 1.10.0
- junit: 4.13.2
- androidx.test.ext:junit: 1.1.5
- androidx.test.espresso:espresso-core: 3.5.1

## 编译配置
- compileSdk: 36
- minSdk: 29
- targetSdk: 36
- namespace: com.example.apppermissionmonitorservice
- applicationId: com.example.apppermissionmonitorservice