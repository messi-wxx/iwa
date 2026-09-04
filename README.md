# IWA — Kotlin MVVM 标准框架

包名：`com.cq.iwa`  
路径：`E:\TestCursor\iwa`

## 模块结构

```
iwa/
├── app/                    # 应用入口
└── core/
    ├── common/             # P0  UiState / ApiResult / UiEvent / Dispatchers
    ├── ui/                 # P0  BaseActivity / BaseFragment / BaseViewModel
    ├── network/            # P0  Retrofit + OkHttp + BaseResponse + TokenInterceptor
    ├── database/           # P0  Room + BaseDao + TypeConverter
    ├── logger/             # P0  Timber DebugTree + ReleaseTree
    ├── storage/            # P1  MMKV mmkvDelegate + TokenProvider 实现
    ├── image/              # P1  Coil ImageLoader 单例
    ├── permission/         # P1  挂起函数权限申请
    ├── media/              # P2  相册选取 + 图片压缩 + CameraX 依赖
    ├── dialog/             # P2  Material Dialog 链式调用 + BottomSheet 基类
    └── monitor/            # P2  NetworkMonitor (Flow<Boolean>)
```

## 技术栈

| 层级 | 选型 |
|------|------|
| 语言 | Kotlin 2.0 + Coroutines |
| 架构 | MVVM + 单 Activity（可扩展 Navigation） |
| DI | Hilt |
| UI | ViewBinding + Material3 + sdp/ssp |
| 网络 | Retrofit + OkHttp + kotlinx.serialization |
| 数据库 | Room |
| 日志 | Timber |
| 存储 | MMKV |
| 图片 | Coil |

## 快速开始

1. 用 Android Studio 打开 `E:\TestCursor\iwa`
2. **若 缺少 `gradle/wrapper/gradle-wrapper.jar`**（误点 Skip 时常见），在工程根目录 PowerShell 执行：
   ```powershell
   .\setup-wrapper.ps1
   ```
   或手动：`gradle wrapper --gradle-version 8.10.2`
3. 复制 `local.properties.example` 为 `local.properties`，填入本机 Android SDK 路径
4. Sync Gradle → Run `app`

## 业务扩展指引

- **新增 API**：在 `core/network` 定义接口，Repository 调用 `ApiExceptionHandler.safeApiCall`
- **新增页面**：继承 `BaseActivity` / `BaseFragment`，ViewModel 继承 `BaseViewModel`
- **Token**：通过 `MmkvTokenProvider` 自动注入请求头
- **环境切换**：修改 `core/network` 中 `BASE_URL` buildConfigField

## 待接入（按需）

- Navigation Component 路由图
- productFlavor dev/prod 多环境
- Paging3 列表分页
- Crash 上报（Bugly / Sentry）
