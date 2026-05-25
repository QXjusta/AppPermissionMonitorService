---
inclusion: always
---
# 项目结构与组织

## 项目根目录结构
```
AppPermissionMonitorService/
├── .gradle/                    # Gradle缓存目录
├── .idea/                      # IDE配置文件
├── .kiro/                      # Kiro配置目录
│   └── steering/              # Steering文档
├── app/                        # 主应用模块
│   ├── src/
│   │   ├── main/              # 主源代码
│   │   ├── androidTest/       # Android测试
│   │   └── test/              # 单元测试
│   ├── build.gradle.kts       # 模块构建配置
│   └── proguard-rules.pro     # 混淆规则
├── gradle/                     # Gradle包装器
├── build.gradle.kts           # 项目级构建配置
├── settings.gradle.kts        # 项目设置
├── gradle.properties          # Gradle属性
├── local.properties           # 本地SDK配置
└── gradlew/gradlew.bat        # Gradle包装器脚本
```

## 源代码组织规范

### 包结构
```
com.example.apppermissionmonitorservice/
├── service/                   # 系统服务相关
│   ├── PermissionMonitorService.java  # 主服务类
│   ├── IPermissionMonitor.aidl        # AIDL接口定义
│   ├── IPermissionCallback.aidl       # 回调接口定义
│   └── PermissionRecord.java          # 数据实体类
├── data/                      # 数据层
│   ├── database/             # 数据库相关
│   │   ├── PermissionDatabase.java    # 数据库帮助类
│   │   └── PermissionDao.java         # 数据访问对象
│   └── repository/           # 仓库层
│       └── PermissionRepository.java  # 数据仓库
├── monitor/                  # 监控模块
│   ├── PermissionMonitor.java         # 权限监控器
│   └── AppOpsScanner.java             # AppOps扫描器
├── client/                   # 客户端模块
│   ├── ui/                  # 用户界面
│   │   ├── MainActivity.java          # 主界面
│   │   ├── AppListFragment.java       # 应用列表
│   │   └── PermissionDetailFragment.java # 权限详情
│   └── adapter/             # 适配器
│       └── PermissionAdapter.java     # RecyclerView适配器
└── utils/                   # 工具类
    ├── LogUtils.java                  # 日志工具
    └── PermissionUtils.java           # 权限工具
```

## 模块职责

### 服务端模块（系统服务）
- **PermissionMonitorService**：系统服务主类，继承SystemService
- **IPermissionMonitor**：AIDL接口定义，提供查询、注册回调等方法
- **PermissionMonitor**：权限监控核心逻辑，管理数据采集和回调
- **PermissionDatabase**：SQLite数据库管理，存储权限使用记录

### 客户端模块（应用）
- **MainActivity**：应用主界面，展示应用列表
- **PermissionDetailFragment**：权限使用详情页面
- **PermissionAdapter**：RecyclerView适配器，展示权限记录
- **ServiceBinder**：服务绑定和通信管理

### 数据模型
- **PermissionRecord**：权限记录实体，实现Parcelable接口
- 包含字段：id, packageName, permissionName, timestamp, isBlocked等

## 文件命名规范

### Java/Kotlin文件
- 类名使用大驼峰命名法：`PermissionMonitorService`
- 接口名以I开头：`IPermissionMonitor`
- 工具类以Utils结尾：`LogUtils`
- 适配器以Adapter结尾：`PermissionAdapter`

### 资源文件
- 布局文件：`activity_main.xml`, `fragment_permission_detail.xml`
- 菜单文件：`menu_main.xml`
- 字符串资源：在`values/strings.xml`中定义
- 颜色资源：在`values/colors.xml`中定义

### AIDL文件
- 接口文件：`IPermissionMonitor.aidl`
- 回调文件：`IPermissionCallback.aidl`
- 放置在`src/main/aidl/`目录下

## 代码组织原则

1. **分层架构**：遵循清晰的分层结构（服务层、数据层、UI层）
2. **单一职责**：每个类/方法只负责一个明确的功能
3. **接口隔离**：使用AIDL定义清晰的跨进程接口
4. **依赖倒置**：高层模块不依赖低层模块，都依赖抽象
5. **包私有性**：合理使用访问修饰符，保护内部实现

## 开发流程建议

1. **先定义接口**：首先完成AIDL接口和数据模型定义
2. **服务端实现**：实现系统服务核心功能
3. **客户端开发**：基于接口开发客户端应用
4. **集成测试**：测试跨进程通信和功能完整性
5. **系统集成**：将服务集成到SystemServer中