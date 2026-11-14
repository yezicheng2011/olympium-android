#!/bin/sh
# Copyright 2026 Ye Zicheng
# SPDX-License-Identifier: Apache-2.0
# Pack a locally built AOSP user image. This script does not download an
# Android system image, and it does not invent one.
set -eu

cd "$(CDPATH= cd -- "$(dirname "$0")" && pwd)"

for arg in "$@"; do
  case "$arg" in
    *harmonyos*|*HarmonyOS*|*huawei.com*)
      echo "Refusing a HarmonyOS or Huawei commercial input." >&2
      exit 2
      ;;
    *gapps*|*GApps*|*playstore*|*PlayStore*)
      echo "Refusing a Google Play or GApps input." >&2
      exit 2
      ;;
    *macos*|*darwin*|*iOS*|*ios*)
      echo "Refusing an Apple system image." >&2
      exit 2
      ;;
  esac
done

out=${AOSP_PRODUCT_OUT:-}
if [ -z "$out" ] || [ ! -d "$out" ]; then
  cat >&2 <<'EOF'
No AOSP product output is available (AOSP_PRODUCT_OUT is unset).

This script will not download a Google SDK system image or a Generic System
Image. Those files are published under licenses that restrict redistribution.
It will not write a placeholder archive.

A from-source AOSP user image (no Play services) was not built in this
environment. Building aosp_x86_64 needs a full AOSP tree and more CPU and
memory than this host has (4 vCPU, 16 GiB RAM). Until that build exists,
olympium-android stays unpublished.
EOF
  exit 2
fi

for need in system.img userdata.img; do
  if [ ! -f "$out/$need" ]; then
    echo "AOSP output is missing $need. Refusing to pack a partial image." >&2
    exit 2
  fi
done

if grep -a -q -m 1 'com.google.android.gms' "$out/system.img"; then
  echo "Refusing an image that contains Google Play services." >&2
  exit 2
fi

arch=${1:-$(uname -m)}
case "$arch" in
  x86_64|aarch64) ;;
  *)
    echo "No Android slim asset is defined for $arch." >&2
    exit 2
    ;;
esac

stage=$(mktemp -d)
cleanup() { rm -rf "$stage"; }
trap cleanup EXIT
mkdir -p "$stage/bin"
cp "$out/system.img" "$out/userdata.img" "$stage/"
if [ -f "$out/vendor.img" ]; then
  cp "$out/vendor.img" "$stage/"
fi
gcc -O2 -o "$stage/bin/linkspan-surface" bridge/linkspan-surface.c
cp bridge/linkspan-runtime.sh "$stage/bin/linkspan-runtime"
chmod 755 "$stage/bin/linkspan-runtime"
cat > "$stage/EDITION" <<EOF
android-slim 0.1.0
aosp
arch ${arch}
EOF
cat > "$stage/NOTICE" <<'EOF'
The Android images in this archive keep the licenses of AOSP and its
third-party components. They are not relicensed as Apache-2.0.
There is no Google Play and no proprietary Windows or HarmonyOS image.
EOF
mkdir -p dist
archive="dist/android-slim-${arch}.tar.gz"
tar -C "$stage" -czf "$archive" .
sha256sum "$archive" | tee "dist/android-slim-${arch}.sha256"
echo "Wrote $archive"
echo "Boot this image before publishing it. This script does not claim the image boots."
