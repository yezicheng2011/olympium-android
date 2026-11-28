# Third-Party Notices — Olympium for Android

## First-Party Code
All first-party Kotlin source code, manifests, and packaging scripts in this repository are licensed under **Apache License, Version 2.0** ([LICENSE](LICENSE), [NOTICE](NOTICE)).

## Third-Party Dependencies & Optional Container Runtimes

| Component | Role on Android | License | Distribution / Link Model |
| :--- | :--- | :--- | :--- |
| **Kotlin Standard Library** | Application language runtime | **Apache-2.0** | Gradle dependency |
| **AndroidX Core KTX** (`androidx.core:core-ktx`) | FileProvider & modern Android APIs | **Apache-2.0** | Gradle dependency |
| **AndroidX AppCompat** (`androidx.appcompat:appcompat`) | Activity & compatibility UI | **Apache-2.0** | Gradle dependency |
| **Material Components for Android** (`com.google.android.material:material`) | Material 3 theme & widgets | **Apache-2.0** | Gradle dependency |
| **Android OS & PackageInstaller** | Native `.apk` installation & SAF document picker | **Apache-2.0** (AOSP) / **GPL-2.0-only** (Android Linux kernel) | Host Android OS; not redistributed |
| **PRoot** (optional user container runtime) | Unprivileged Linux container execution on Android | **GPL-2.0-or-later** | Separate executable in optional user container; not linked into APK |
| **QEMU** (`qemu-user-static`, optional user container) | Cross-arch `x86`/`x86_64`/`arm32`/`arm64` Linux execution | **GPL-2.0-only** | Separate executable in optional user container |
| **Wine** (optional user container) | Automatic Windows `.exe`/`.msi` execution | **LGPL-2.1-or-later** | Separate executable in optional user container |
| **Box64 / Box86** (optional user container on ARM) | x86/x86_64 translation on ARM Android devices | **MIT** | Separate executable in optional user container |
| **Darling** (optional user container) | macOS Intel / Universal2 / ARM64 execution bridge | **GPL-3.0-only** (+ **APSL-2.0** submodules) | Separate executable in optional user container |
| **OpenHarmony Runtime / HDC** (optional bridge) | Slim `.hap` compatibility bridge | **Apache-2.0** | Separate bridge tool |
| **microG Services Core (`GmsCore`)** | Open-source GMS APIs (no proprietary Play Store bundled; dismissible manual guide in Settings) | **Apache-2.0** | Separate APK package |

> **Compliance Note**: All Gradle dependencies compiled into the Android APK are Apache-2.0 compatible. Any optional GPL/LGPL user-space container tools (PRoot, QEMU, Wine, Darling) are distributed separately with their own license notices and never statically linked into `org.olympium.app`.
