# Slim Android runtime for Linkspan

A minimal AOSP system image for the Android emulator or QEMU. No Google Play, no extra apps, no proprietary services.

The scripts here are Apache-2.0, Copyright 2026 Ye Zicheng. The image they build is not committed. Linkspan downloads a published GitHub Release of this repository at runtime. A China mirror is selected by Linkspan from the client IP. The mirror must serve the same bytes. Linkspan checks SHA-256.

The official site is https://olympium.net. Do not commit `*.img`, `*.tar.gz`, certificates, or keys.

## Build

```bash
./build-slim.sh
```


1. Build a user AOSP image for the emulator with no Play services and no extra packages.
2. Pack `system.img`, `userdata.img`, and `bin/linkspan-runtime` into the asset named below.
3. `linkspan-runtime` must start the emulator with no device-manager window and pass `--display host-window` through to a host window bridge.
4. Do not put the image in the Linkspan application repository.


After a build, fill `sha256` in `runtime-lock.json`, set `"published": true` in this file and in Linkspan's `share/runtime-lock.json`, tag `v0.1.0`, and attach the `.tar.gz` assets to the GitHub Release. Until that release exists, Linkspan reports that the image is not published.

## Assets

- `android-slim-x86_64.tar.gz`
- `android-slim-aarch64.tar.gz`
