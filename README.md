# Slim Android runtime for Linkspan

A minimal AOSP system image for the Android emulator or QEMU. No Google Play, no extra apps, no proprietary services.

The scripts here are Apache-2.0, Copyright 2026 Ye Zicheng. The image they build is not committed. Linkspan downloads a published GitHub Release of this repository at runtime. A China mirror is selected by Linkspan from the client IP. The mirror must serve the same bytes. Linkspan checks SHA-256.

The official site is https://olympium.net. Do not commit `*.img`, `*.tar.gz`, certificates, or keys.

## Build

```bash
./build-slim.sh
```

The script packs `system.img` and `userdata.img` from `AOSP_PRODUCT_OUT` only when that directory is a local AOSP build. It refuses Google Play, GApps, HarmonyOS, and Apple images. It does not download a Google SDK system image or a Generic System Image, because those downloads are published under licenses that restrict redistribution. It does not write a placeholder archive.

No AOSP image was built for 0.1.0. A from-source `aosp_x86_64` user image needs a full AOSP tree and more CPU and memory than the host used here (4 vCPU, 16 GiB RAM). `published` stays false and `sha256` stays empty.

## Assets

- `android-slim-x86_64.tar.gz`
- `android-slim-aarch64.tar.gz`
