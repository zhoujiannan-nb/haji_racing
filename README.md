# Haji Racing · 跑山记录

一个**无登录、纯本地**的跑山 / 骑行轨迹记录 App（Android 原生 · Kotlin + Jetpack Compose）。

自定义赛道、GPS 记录、成绩分析与排行榜，全部数据保存在本机 SQLite，不上传任何云端。

> 本项目由 v1（Flutter 全功能原型）与 v2（Kotlin/Compose 自定义地图原型）合并重构而来。
> 保留了 v1 的全部记录与分析逻辑（起点围栏自动计时、成绩分析、排行榜），
> 采用 v2 的 Kotlin/Jetpack Compose 技术栈（高德地图集成与前台服务已验证可用），
> 去除了登录账号与车辆管理。

## 功能

- **自定义赛道**
  - 在高德地图上点击绘制「起点围栏」与「终点围栏」（多边形区域）
  - 支持 POI 关键词搜索定位，快速把地图移到目标位置
  - 自动估算赛道长度，保存赛道名称 / 描述
- **两种记录模式**
  - **赛道跟跑**：进入起点围栏且速度 ≥ 15 km/h 自动开始计时；驶入终点围栏自动完成
  - **自由跑**：点击即开始，手动停止
  - 记录中可退出界面，服务在后台继续；锁屏 / 切后台不受影响（前台服务 + 通知实时显示时间与速度）
- **实时记录界面**：大字体计时、270° 速度表盘、实时距离 / 最高速度 / GPS 精度、地图实时轨迹
- **轨迹分析**
  - 速度曲线图（含最高点标注）
  - 轨迹回放地图（按速度着色）
  - 距离 / 平均速度 / 最高速度 / 用时
- **赛道统计与排行榜**：每条赛道的记录数、最快成绩、TOP 5 排行榜（奖牌配色）
- **个人统计**：总距离、总时长、完成数、历史最快成绩
- **数据管理**：删除单条记录 / 单条赛道（级联删除其全部记录与轨迹点），一键清空

## 技术要点

| 项目 | 选型 |
|---|---|
| 语言 / UI | Kotlin 2.1 + Jetpack Compose (Material 3, BOM 2024.12.01) |
| 地图 | 高德 3D 地图 SDK 9.5.0（GCJ-02 坐标系） |
| 架构 | MVVM（Hilt DI + ViewModel + StateFlow） |
| 存储 | Room 2.6.1（tracks / recordings / recording_points），DataStore 存偏好 |
| 录制 | `LifecycleService` 前台服务 + 位置通知，GPS 1Hz 采样（精度 ≤50m、跳变 ≤200m 过滤），批量写库 |
| 地理计算 | 自实现 Haversine 距离、射线法多边形包含、多边形最短距离 |
| 构建 | AGP 8.7.3 / Gradle 8.14 / KSP，compileSdk 35，minSdk 26，targetSdk 34 |

坐标说明：GPS 原始 WGS-84 在采集时即转换为 GCJ-02，与高德地图展示一致，无需二次偏移。

## 构建

```bat
:: 需要 JDK 17+（本机示例路径）
set JAVA_HOME=D:\SOFT\ASCODE\jbr

:: Debug
gradlew.bat :app:assembleDebug

:: Release（已配置本地签名 app/haji_racing_release.jks）
gradlew.bat :app:assembleRelease
```

产物位于 `app/build/outputs/apk/{debug|release}/`。

> 注意：`local.properties` 需指向本机 Android SDK（`sdk.dir=...`），该文件不入库。

## 隐私

- 无账号、无登录、无网络同步（仅 POI 搜索调用高德 Web API）
- 所有轨迹与成绩仅存于本机数据库，删除记录即彻底清除
- 定位权限仅在开始记录时需要；通知权限用于记录中的实时状态栏

## 目录结构

```
app/src/main/java/com/haji/racing/
├── data/          # Room 实体/DAO/数据库、DataStore 偏好、高德 POI 网络层
├── di/            # Hilt 模块
├── domain/        # 领域模型与仓储接口
├── core/
│   ├── geo/       # 距离 / 多边形 / 路径长度 / 速度着色
│   ├── gps/       # GPS 监听（WGS-84 → GCJ-02）
│   └── common/    # 距离 / 速度 / 时间格式化
├── service/       # RecordingService（前台录制服务，计时与围栏判定）
└── ui/
    ├── theme/     # 深色赛车主题
    ├── components/# 设计系统（卡片/按钮/速度表/速度曲线/状态条…）
    ├── navigation/# 底部导航 + 路由
    ├── home/      # 首页（模式选择 / 赛道选择 / 启动）
    ├── record/    # 实时记录
    ├── track/     # 赛道列表 / 创建（地图画围栏）/ 详情（排行榜）
    ├── records/   # 轨迹列表 / 详情（回放 + 速度曲线）
    └── profile/   # 个人统计 / 数据管理
```

## 版本

- v2.0.0 — v1 + v2 合并重构版（无登录 · 自定义地图 · 深色赛车 UI）
