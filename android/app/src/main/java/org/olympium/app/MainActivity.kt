// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Olympium Contributors

package org.olympium.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var statusView: TextView
    private lateinit var logView: TextView

    private val pickPackage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) handleIncomingUri(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 72, 48, 48)
        }

        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_olympium_dragon)
            layoutParams = LinearLayout.LayoutParams(192, 192)
        }
        root.addView(icon)

        val title = TextView(this).apply {
            text = "Olympium for Android (${CompatEngine.hostArch()})"
            textSize = 22f
            setPadding(0, 24, 0, 12)
        }
        root.addView(title)

        statusView = TextView(this).apply {
            text = buildStatusText()
            textSize = 14f
            setPadding(0, 0, 0, 24)
        }
        root.addView(statusView)

        val btnPick = Button(this).apply {
            text = "Tap to Install & Run App (.apk / .exe / .deb / .rpm / .hap / .dmg)"
            setOnClickListener { pickPackage.launch(arrayOf("*/*")) }
        }
        root.addView(btnPick)

        val btnClip = Button(this).apply {
            text = "Sync Shared Clipboard (Android <-> Wine / Linux / OpenHarmony)"
            setOnClickListener { logView.text = InteropHub.syncSharedClipboard(this@MainActivity) }
        }
        root.addView(btnClip)

        val guideCard = TextView(this).apply {
            text = InteropHub.googlePlayGuideText()
            textSize = 13f
            setPadding(24, 24, 24, 24)
            visibility = if (InteropHub.isPlayGuideVisible(this@MainActivity)) View.VISIBLE else View.GONE
        }

        val guideSwitch = SwitchCompat(this).apply {
            text = "Show Google Play manual setup guide (microG Core built-in; no Play Store bundled)"
            isChecked = InteropHub.isPlayGuideVisible(this@MainActivity)
            setPadding(0, 20, 0, 8)
            setOnCheckedChangeListener { _, isChecked ->
                InteropHub.setPlayGuideVisible(this@MainActivity, isChecked)
                guideCard.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }
        root.addView(guideSwitch)
        root.addView(guideCard)

        logView = TextView(this).apply {
            text = "Ready. Tap any supported package in Files or Downloads to install natively via Olympium."
            textSize = 14f
            setPadding(0, 24, 0, 0)
        }
        root.addView(logView)

        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            handleIncomingUri(intent.data!!)
        }
    }

    private fun handleIncomingUri(uri: Uri) {
        val fileName = resolveFileName(uri)
        val stageDir = File(filesDir, "olympium/inbox").apply { mkdirs() }
        val localFile = File(stageDir, fileName)
        // Strict CWE-22 path-traversal guard against malicious ContentProvider DISPLAY_NAME
        val canonicalRoot = stageDir.canonicalPath + File.separator
        if (!localFile.canonicalPath.startsWith(canonicalRoot)) {
            logView.text = "Blocked unsafe filename from content provider."
            return
        }
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                localFile.outputStream().use { output -> input.copyTo(output) }
            }
            val info = CompatEngine.inspect(localFile)
            val result = CompatEngine.installOrRun(this, localFile)
            logView.text = """
                Detected : ${info.targetOs} / ${info.arch} (${info.bits}-bit)
                Plan     : ${info.executionPlan}
                Result   : $result
            """.trimIndent()
        } catch (ex: Exception) {
            logView.text = "Error opening package: ${ex.message}"
        }
    }

    private fun resolveFileName(uri: Uri): String {
        var raw: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                raw = cursor.getString(idx)
            }
        }
        val base = File(raw ?: uri.lastPathSegment ?: "package.bin").name
        val clean = base.replace(Regex("[^A-Za-z0-9._-]"), "_").trimStart('.')
        return if (clean.isBlank()) "package.bin" else clean.take(128)
    }

    private fun buildStatusText(): String {
        val microgState = if (InteropHub.isMicroGInstalled(this)) "active (GmsCore)" else "ready for setup (Apache-2.0, no Play Store)"
        val widgetCount = InteropHub.listInstalledWidgets(this)
        return """
            Device ABIs      : ${Build.SUPPORTED_ABIS.joinToString(", ")}
            Android Native   : PackageInstaller (native tap-to-install .apk)
            microG GMS Core  : $microgState
            Shared Clipboard : Bidirectional (Android <-> Wine <-> Linux <-> OpenHarmony)
            Widgets Bridge   : $widgetCount native AppWidget provider(s) exposed
            Windows AutoWine : Box64/Box86 + Wine proot bridge (.exe / .msi)
            Linux Cross-Arch : proot + qemu-user-static (x86/x86_64/arm32/arm64)
            OpenHarmony Slim : HAP unpacker + ArkRuntime/HDC bridge (.hap)
            macOS Darling    : Darling container bridge (.dmg / .pkg / Mach-O)
        """.trimIndent()
    }
}
