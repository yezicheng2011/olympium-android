# Olympium for Android (全 ABI 通用 APK / AAB)

本目录为 **Olympium for Android** 独立发布包，采用 Kotlin + Android Material 3 构建，单包支持 `arm64-v8a`、`armeabi-v7a`、`x86_64` 与 `x86` 四大 ABI，提供与 Android 系统一致的原生点按安装体验。

## 目录内容

- `android/`：完整 Kotlin/Gradle 工程源码、Manifest 意图过滤配置、统一金色龙图标及 `LICENSE` / `NOTICE` / `THIRD_PARTY_NOTICES.md`
- `BUILD.txt`：Android SDK/Gradle 编译、签名与安装命令手册

## 核心能力

1. **Android 原生交互体验**：在文件管理器或浏览器点击安装包直接唤起 Olympium；`.apk` 调用原生 `PackageInstaller` 安装，跨平台格式（`.exe`/`.msi`/`.deb`/`.rpm`/ELF/``.hap`/`.dmg`/`.pkg`）自动识别架构并调度至 Wine+Box64/Box86、proot+QEMU（x86/x86_64/ARM32/ARM64）、Darling 或 OpenHarmony 精简桥接。
2. **互联互通 + 开源 microG**：支持原生 `ClipboardManager` 跨生态剪切板同步、`AppWidgetManager` 小组件桥接、开源 microG 核心检测（Apache-2.0，不内置闭源 Play 商店）及界面内可持久化关闭的 Google Play 手动安装说明。
3. **开源许可**：第一方代码采用 **Apache License 2.0**；第三方组件均保留原许可证（详见 `android/THIRD_PARTY_NOTICES.md`）。

## 使用方法

本目录已包含源码，请按同级目录的 `BUILD.txt` 执行打包与签名安装。
