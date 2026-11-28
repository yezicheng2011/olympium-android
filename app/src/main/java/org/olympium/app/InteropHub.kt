// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Olympium Contributors
//
// Cross-app interoperability hub for Olympium on Android:
//   - Native Android ClipboardManager <-> Wine/Linux/OpenHarmony container shared clipboard
//   - AppWidgetManager widget discovery bridge for cross-platform widget sync
//   - Open-source microG GmsCore detection (Apache-2.0, NO proprietary Play Store bundled)
//   - Persistent Settings toggle (SharedPreferences) to hide/show the Google Play manual setup guide

package org.olympium.app

import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import java.io.File

object InteropHub {
    private const val PREFS_NAME = "olympium_settings"
    private const val KEY_SHOW_PLAY_GUIDE = "show_google_play_guide"
    private const val KEY_CLIPBOARD_SYNC = "clipboard_sync_enabled"

    fun isPlayGuideVisible(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_PLAY_GUIDE, true)

    fun setPlayGuideVisible(context: Context, visible: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_PLAY_GUIDE, visible).apply()
    }

    fun isClipboardSyncEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CLIPBOARD_SYNC, true)

    fun setClipboardSyncEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CLIPBOARD_SYNC, enabled).apply()
    }

    fun isMicroGInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.google.android.gms", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun syncSharedClipboard(context: Context): String {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val sharedDir = File(context.filesDir, "olympium/shared").apply { mkdirs() }
        val clipFile = File(sharedDir, ".olympium-clipboard.txt")
        val current = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
        return if (!current.isNullOrEmpty()) {
            clipFile.writeText(current)
            "Synced Android clipboard (${current.length} chars) -> Wine/Linux/OpenHarmony shared bridge."
        } else if (clipFile.exists()) {
            val bridged = clipFile.readText()
            cm.setPrimaryClip(ClipData.newPlainText("Olympium", bridged))
            "Pulled bridged clipboard (${bridged.length} chars) into native Android clipboard."
        } else {
            "Shared clipboard ready (Android <-> Wine <-> Linux <-> OpenHarmony)."
        }
    }

    fun listInstalledWidgets(context: Context): Int {
        return try {
            AppWidgetManager.getInstance(context).installedProviders.size
        } catch (_: Exception) {
            0
        }
    }

    fun googlePlayGuideText(): String = """
        Android GMS Core uses open-source microG Services Core (Apache-2.0) — no proprietary Google Play Store is bundled.
        Optional manual Google Play setup:
          1. Open microG Settings -> Self-Check and enable Signature Spoofing & Google Device Registration.
          2. Recommended open-source store client: Install Aurora Store (GPL-3.0) by tapping its .apk.
          3. Or obtain the official Google Play Store APK from your own licensed Android device and tap to install.
        (Switch off 'Show Google Play manual setup guide' above anytime to hide this card.)
    """.trimIndent()
}
