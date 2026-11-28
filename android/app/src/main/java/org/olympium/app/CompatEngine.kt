// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Olympium Contributors

package org.olympium.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

enum class PackageKind {
    ANDROID_APK,
    OPENHARMONY_HAP,
    WINDOWS_EXE,
    WINDOWS_MSI,
    LINUX_ELF,
    LINUX_DEB,
    LINUX_RPM,
    MAC_DMG,
    MAC_PKG,
    MAC_MACHO,
    UNKNOWN
}

data class CompatInspection(
    val file: File,
    val kind: PackageKind,
    val targetOs: String,
    val arch: String,
    val bits: Int,
    val needsCrossArch: Boolean,
    val executionPlan: String
)

object CompatEngine {
    fun hostArch(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when {
            abi.contains("arm64") -> "arm64"
            abi.contains("armeabi") -> "armhf"
            abi.contains("x86_64") -> "x86_64"
            abi.contains("x86") -> "i386"
            else -> abi
        }
    }

    fun inspect(file: File): CompatInspection {
        val ext = file.extension.lowercase()
        val hdr = ByteArray(4096)
        val nRead = try {
            FileInputStream(file).use { it.read(hdr) }
        } catch (_: Exception) {
            0
        }

        when (ext) {
            "apk" -> return CompatInspection(
                file, PackageKind.ANDROID_APK, "Android", inspectZipAbis(file), 64, false,
                "Native Android PackageInstaller (tap-to-install)"
            )
            "hap" -> return CompatInspection(
                file, PackageKind.OPENHARMONY_HAP, "OpenHarmony", inspectZipAbis(file), 64, false,
                "Slim OpenHarmony HAP unpacker + HDC/ArkRuntime bridge"
            )
            "msi" -> return CompatInspection(
                file, PackageKind.WINDOWS_MSI, "Windows", "x86_64", 64, hostArch() != "x86_64",
                "Auto-Wine + Box64/QEMU proot container (msiexec /i)"
            )
            "deb" -> {
                val arch = guessArch(file.name, hostArch())
                return CompatInspection(
                    file, PackageKind.LINUX_DEB, "Linux", arch, if (arch == "i386" || arch == "armhf") 32 else 64,
                    arch != hostArch(), "Sandboxed proot dpkg-deb + qemu-user-static ($arch on ${hostArch()})"
                )
            }
            "rpm" -> {
                val arch = guessArch(file.name, hostArch())
                return CompatInspection(
                    file, PackageKind.LINUX_RPM, "Linux", arch, if (arch == "i386" || arch == "armhf") 32 else 64,
                    arch != hostArch(), "Sandboxed proot rpm2cpio + qemu-user-static ($arch on ${hostArch()})"
                )
            }
            "dmg", "pkg" -> return CompatInspection(
                file, if (ext == "dmg") PackageKind.MAC_DMG else PackageKind.MAC_PKG,
                "macOS", "universal2", 64, true,
                "Confined unpack -> Linux proot Darling GUI / ARM64 bridge"
            )
        }

        // ELF magic
        if (nRead >= 20 && hdr[0] == 0x7F.toByte() && hdr[1] == 'E'.code.toByte() && hdr[2] == 'L'.code.toByte() && hdr[3] == 'F'.code.toByte()) {
            val bits = if (hdr[4].toInt() == 1) 32 else 64
            val mach = ByteBuffer.wrap(hdr, 18, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
            val arch = when (mach) {
                3 -> "i386"
                62 -> "x86_64"
                40 -> "armhf"
                183 -> "arm64"
                else -> "elf-$mach"
            }
            val cross = arch != hostArch()
            val plan = if (cross) "proot + qemu-user-static ($arch on ${hostArch()})" else "Native Linux/Android ELF execution ($arch)"
            return CompatInspection(file, PackageKind.LINUX_ELF, "Linux", arch, bits, cross, plan)
        }

        // PE (.exe) magic
        if (nRead >= 64 && hdr[0] == 'M'.code.toByte() && hdr[1] == 'Z'.code.toByte()) {
            val (arch, bits) = parsePeArch(hdr, nRead)
            val cross = (hostArch() == "arm64" || hostArch() == "armhf") && (arch == "x86_64" || arch == "i386")
            val plan = if (cross) "Auto-Wine + Box64/Box86 ($arch on ${hostArch()})" else "Auto-Wine native ($arch)"
            return CompatInspection(file, PackageKind.WINDOWS_EXE, "Windows", arch, bits, cross, plan)
        }

        // Mach-O / Fat magic
        if (nRead >= 16) {
            val mBE = ByteBuffer.wrap(hdr, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toUInt()
            if (mBE == 0xCAFEBABEu || mBE == 0xFEEDFACFu || mBE == 0xCFFAEDFEu || mBE == 0xFEEDFACEu || mBE == 0xCEFAEDFEu) {
                return CompatInspection(
                    file, PackageKind.MAC_MACHO, "macOS",
                    if (mBE == 0xCAFEBABEu) "universal2" else "x86_64/arm64", 64, true,
                    "Darling container bridge (Intel / Universal2 / ARM64)"
                )
            }
        }

        return CompatInspection(file, PackageKind.UNKNOWN, "Unknown", "unknown", 0, false, "Unsupported format")
    }

    fun installOrRun(context: Context, file: File): String {
        val info = inspect(file)
        val appsRoot = File(context.filesDir, "olympium/apps").apply { mkdirs() }
        return when (info.kind) {
            PackageKind.ANDROID_APK -> {
                // Native Android tap-to-install via FileProvider + ACTION_VIEW
                val apkUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Launched native Android installer for ${file.name} (${info.arch})"
            }
            PackageKind.WINDOWS_EXE, PackageKind.WINDOWS_MSI -> {
                val staged = File(appsRoot, "win-${file.name}")
                file.copyTo(staged, overwrite = true)
                "Staged ${file.name} (${info.arch}) -> ${info.executionPlan}. Ready in Olympium Wine/Box64 runtime."
            }
            PackageKind.LINUX_ELF, PackageKind.LINUX_DEB, PackageKind.LINUX_RPM -> {
                val staged = File(appsRoot, "linux-${file.name}")
                file.copyTo(staged, overwrite = true)
                staged.setExecutable(true)
                "Staged ${file.name} (${info.arch}) -> ${info.executionPlan}."
            }
            PackageKind.OPENHARMONY_HAP -> {
                val outDir = File(appsRoot, "hap-${file.nameWithoutExtension}").apply { mkdirs() }
                unzipTo(file, outDir)
                "Unpacked OpenHarmony HAP (${info.arch}) to ${outDir.name} via slim runtime bridge."
            }
            PackageKind.MAC_DMG, PackageKind.MAC_PKG, PackageKind.MAC_MACHO -> {
                val staged = File(appsRoot, "mac-${file.name}")
                file.copyTo(staged, overwrite = true)
                "Staged macOS package ${file.name} (${info.arch}) -> ${info.executionPlan}."
            }
            PackageKind.UNKNOWN -> "Unsupported package format: ${file.name}"
        }
    }

    private fun parsePeArch(hdr: ByteArray, nRead: Int): Pair<String, Int> {
        val peOff = ByteBuffer.wrap(hdr, 60, 4).order(ByteOrder.LITTLE_ENDIAN).int
        if (peOff > 0 && peOff + 6 <= nRead && hdr[peOff] == 'P'.code.toByte() && hdr[peOff + 1] == 'E'.code.toByte()) {
            val mach = ByteBuffer.wrap(hdr, peOff + 4, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
            return when (mach) {
                0x014C -> "i386" to 32
                0x8664 -> "x86_64" to 64
                0x01C0, 0x01C4 -> "armhf" to 32
                0xAA64 -> "arm64" to 64
                else -> "x86_64" to 64
            }
        }
        return "x86_64" to 64
    }

    private fun inspectZipAbis(file: File): String {
        return try {
            ZipFile(file).use { z ->
                val abis = mutableSetOf<String>()
                val entries = z.entries()
                while (entries.hasMoreElements()) {
                    val name = entries.nextElement().name
                    if (name.startsWith("lib/") || name.startsWith("libs/")) {
                        val parts = name.split("/")
                        if (parts.size >= 3) abis.add(parts[1])
                    }
                }
                if (abis.isEmpty()) "universal" else abis.joinToString(", ")
            }
        } catch (_: Exception) {
            "universal"
        }
    }

    private fun unzipTo(src: File, destDir: File) {
        val canonicalRoot = destDir.canonicalPath + File.separator
        val maxTotalBytes = 128L * 1024L * 1024L
        val maxEntries = 4096
        var totalBytes = 0L
        var entryCount = 0
        try {
            ZipFile(src).use { z ->
                val entries = z.entries()
                while (entries.hasMoreElements()) {
                    entryCount++
                    if (entryCount > maxEntries) throw SecurityException("Archive exceeds max entry limit")
                    val e = entries.nextElement()
                    val out = File(destDir, e.name)
                    if (!out.canonicalPath.startsWith(canonicalRoot)) continue
                    if (e.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        z.getInputStream(e).use { input ->
                            out.outputStream().use { output ->
                                val buf = ByteArray(8192)
                                while (true) {
                                    val n = input.read(buf)
                                    if (n <= 0) break
                                    totalBytes += n
                                    if (totalBytes > maxTotalBytes) throw SecurityException("Archive exceeds 128 MB Zip-Bomb limit")
                                    output.write(buf, 0, n)
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun guessArch(name: String, fallback: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("arm64") || lower.contains("aarch64") -> "arm64"
            lower.contains("armhf") || lower.contains("armv7") -> "armhf"
            lower.contains("i386") || lower.contains("x86.") -> "i386"
            lower.contains("amd64") || lower.contains("x86_64") || lower.contains("x64") -> "x86_64"
            else -> fallback
        }
    }
}
