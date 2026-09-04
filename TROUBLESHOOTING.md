# 编译失败但 Android Studio 不显示原因 — 排查指南

## 1. 最常见：缺少 gradle-wrapper.jar

检查文件是否存在：

```
E:\TestCursor\iwa\gradle\wrapper\gradle-wrapper.jar
```

若不存在，双击运行工程根目录：

```
setup-wrapper.bat
```

或在 PowerShell：

```powershell
.\setup-wrapper.ps1
```

然后在 Android Studio：**File → Sync Project with Gradle Files**

---

## 2. 在 Android Studio 里看到真实报错

1. 底部打开 **Build** 窗口（不是 Run）
2. 左侧选 **Sync** 或 **Build Output**
3. 点击 **Toggle view**（切换树形/纯文本），展开所有 `Caused by:`
4. 或菜单 **View → Tool Windows → Problems**

命令行（最准确）：

```bat
cd /d E:\TestCursor\iwa
gradlew.bat assembleDebug --stacktrace --info
```

把最后 30 行红色错误复制出来即可定位。

---

## 3. JDK 版本

AGP 8.6 需要 **JDK 17**：

**File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** → 选 **17**

---

## 4. Android SDK

`local.properties` 中 `sdk.dir` 必须指向有效 SDK 路径。

当前配置：

```
sdk.dir=E\:\\AndroidSDK
```

确认该目录存在，且 SDK Manager 已安装：

- Android SDK Platform **34**
- Android SDK Build-Tools **34.x**

---

## 5. 依赖与 compileSdk 不匹配（已在本工程修复）

`androidx.core:core-ktx:1.15.0` 要求 compileSdk **35**，工程使用 **34** 会导致 Sync 失败。

已降级为与 compileSdk 34 兼容的版本。若仍失败，可尝试：

- SDK Manager 安装 Android 14 (API 34) / 或 API 35 后将各模块 `compileSdk` 改为 35

---

## 6. 网络 / 仓库

首次 Sync 需下载 Gradle 与依赖。若公司网络限制，确认 `settings.gradle.kts` 中阿里云镜像可访问。

---

## 快速自检清单

- [ ] `gradle/wrapper/gradle-wrapper.jar` 存在
- [ ] `local.properties` 中 sdk.dir 正确
- [ ] Gradle JDK = 17
- [ ] Sync 后 Build Output 无红色错误
- [ ] `gradlew.bat assembleDebug` 能跑通
