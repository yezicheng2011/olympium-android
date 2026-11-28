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

## 已知安全风险与残余限制

```text
====================================================================
Olympium for Android — 已知安全风险与残余限制 (KNOWN_VULNERABILITIES.txt)
====================================================================

一、已在本次审计中直接修复的代码漏洞
--------------------------------------------------------------------
1. [已修复] ContentProvider DISPLAY_NAME 路径穿越 (CWE-22):
   - android/app/src/main/java/org/olympium/app/MainActivity.kt 原本直接使用
     外部 ContentProvider 返回的 DISPLAY_NAME 构造本地文件，恶意应用可构造
     ../../ 覆写本应用私有目录；现已做字符白名单清洗并强制 canonicalPath 前缀校验。
2. [已修复] Zip-Slip 前缀绕过与 Zip-Bomb (CWE-22 / CWE-409):
   - android/app/src/main/java/org/olympium/app/CompatEngine.kt 修复了
     startsWith(destDir.canonicalPath) 缺少 File.separator 的经典绕过缺陷，
     并增加 128 MB 解压上限与 4096 文件数上限。
3. [已修复] ADB 备份暴露 (CWE-921):
   - AndroidManifest.xml 已将 android:allowBackup 改为 false。

二、无法单靠本项目代码根除的已知风险
--------------------------------------------------------------------
1. [高] REQUEST_INSTALL_PACKAGES 高危权限滥用面
   - 风险：为实现“点击 .apk 像原生一样安装”，应用申请了安装未知应用权限；若用户
     下载并点击恶意 APK，Android 系统安装器仍需用户确认，但误点存在木马风险。
   - 缓解建议：仅安装来源可信且校验过签名的安装包。

2. [中] microG 签名伪装与非官方系统兼容性
   - 风险：在部分未解锁/未支持 Signature Spoofing 的原厂 Android ROM 上，
     microG 部分 GMS 接口可能受限；若授予非受信任应用签名伪装权限存在安全隐患。
   - 缓解建议：仅在受信任系统/容器内为官方开源 microG GmsCore 启用签名伪装。

3. [中] Android 10+ 后台剪贴板访问限制与共享桥接风险
   - 风险：Android 10+ 限制后台读取剪贴板，且将剪贴板写入容器共享文件时，同容器
     内的 Wine/Linux 程序均可读取该文本。
   - 缓解建议：仅在前台手动触发或确认同步，避免复制银行卡/助记词后同步。

4. [低] 无 Root 环境下 proot/QEMU 性能与 SELinux/W^X 限制
   - 风险：Android 10+ 对应用私有目录执行未签名原生二进制有 SELinux W^X 限制，
     部分设备需依赖宿主容器或特定 exec 路径。
```
