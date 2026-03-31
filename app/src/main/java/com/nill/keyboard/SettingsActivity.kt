package com.nill.keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(KeyboardIMEService.PREFS, Context.MODE_PRIVATE)
        setContentView(buildSettingsUI())
        supportActionBar?.title = "⚙️ Nill Keyboard Settings"
    }

    private fun buildSettingsUI(): ScrollView {
        val sv = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(32))
        }

        // ──── THEME ────
        root.addView(sectionHeader("🎨 Theme"))
        val darkSwitch = Switch(this).apply {
            text = "  Dark Theme"
            textSize = 15f
            isChecked = prefs.getBoolean(KeyboardIMEService.PREF_DARK, false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(KeyboardIMEService.PREF_DARK, checked).apply()
                Toast.makeText(this@SettingsActivity,
                    if (checked) "Dark theme enabled" else "Light theme enabled",
                    Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(darkSwitch)

        root.addView(divider())

        // ──── GROQ API KEY ────
        root.addView(sectionHeader("🔑 Groq API Key (Free AI)"))
        root.addView(infoText("• Go to console.groq.com\n• Sign up free\n• Create API Key\n• Paste it below\n• Powers Banglish AI + Voice typing"))

        val apiKeyInput = EditText(this).apply {
            hint = "Paste your Groq key here: gsk_..."
            setText(prefs.getString(KeyboardIMEService.PREF_GROQ_KEY, ""))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            textSize = 14f
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        root.addView(apiKeyInput)

        root.addView(buildButton("💾 Save API Key", "#007AFF") {
            val key = apiKeyInput.text.toString().trim()
            if (key.startsWith("gsk_") || key.length > 20) {
                prefs.edit().putString(KeyboardIMEService.PREF_GROQ_KEY, key).apply()
                Toast.makeText(this, "✅ API Key saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "⚠️ Key should start with gsk_", Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(divider())

        // ──── PARENTAL CONTROL / KEYLOGGER ────
        root.addView(sectionHeader("👁️ Parental Control (Keylogger)"))
        root.addView(infoText("Logs all keystrokes typed through this keyboard.\nLog file is password-protected."))

        val loggerSwitch = Switch(this).apply {
            text = "  Enable Keylogger"
            textSize = 15f
            isChecked = prefs.getBoolean(KeyboardIMEService.PREF_KEYLOGGER, false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(KeyboardIMEService.PREF_KEYLOGGER, checked).apply()
                Toast.makeText(this@SettingsActivity,
                    if (checked) "🔴 Keylogger ON" else "⚫ Keylogger OFF",
                    Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(loggerSwitch)

        val logSize = KeyLogManager(this).getLogSizeKb()
        root.addView(infoText("Current log size: ${logSize} KB"))

        root.addView(buildButton("📁 View Log File (🔒 Password Required)", "#FF3B30") {
            showPasswordDialog()
        })

        root.addView(divider())

        // ──── CLIPBOARD HISTORY ────
        root.addView(sectionHeader("📋 Clipboard History"))
        root.addView(infoText("Shows all text you've copied while using this keyboard."))

        root.addView(buildButton("📋 View Clipboard History", "#34C759") {
            showClipboardHistory()
        })

        root.addView(buildButton("🗑️ Clear Clipboard History", "#8E8E93") {
            ClipboardHistoryManager(this).clear()
            Toast.makeText(this, "Clipboard history cleared", Toast.LENGTH_SHORT).show()
        })

        root.addView(divider())

        // ──── ABOUT ────
        root.addView(sectionHeader("ℹ️ About"))
        root.addView(infoText(
            "Nill Keyboard v1.0\n" +
            "Built for personal use\n\n" +
            "AI Engine: Groq (Free)\n" +
            "• Banglish→Bangla: llama-3.1-8b-instant\n" +
            "• Voice typing: whisper-large-v3\n\n" +
            "Supports Android 5.1+"
        ))

        sv.addView(root)
        return sv
    }

    // ──── Password Dialog ────
    private fun showPasswordDialog() {
        val input = EditText(this).apply {
            hint = "Enter password"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            textSize = 18f
            gravity = Gravity.CENTER
        }

        AlertDialog.Builder(this)
            .setTitle("🔒 Log Access")
            .setMessage("Enter password to view the keylog:")
            .setView(input)
            .setPositiveButton("Unlock") { _, _ ->
                if (input.text.toString() == "2090718") {
                    showLogViewer()
                } else {
                    Toast.makeText(this, "❌ Wrong password!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogViewer() {
        val logMgr = KeyLogManager(this)
        val content = logMgr.getLogContent()

        val sv = ScrollView(this)
        val tv = TextView(this).apply {
            text = content
            textSize = 11f
            setTextIsSelectable(true)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setTextColor(Color.parseColor("#1C1C1E"))
            typeface = android.graphics.Typeface.MONOSPACE
        }
        sv.addView(tv)

        AlertDialog.Builder(this)
            .setTitle("📁 Keylog (${logMgr.getLogSizeKb()} KB)")
            .setView(sv)
            .setPositiveButton("Close", null)
            .setNeutralButton("Clear All") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Confirm Delete")
                    .setMessage("Delete all keylog entries?")
                    .setPositiveButton("Yes, Delete") { _, _ ->
                        logMgr.clearLog()
                        Toast.makeText(this, "Log cleared", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .show()
    }

    // ──── Clipboard History ────
    private fun showClipboardHistory() {
        val history = ClipboardHistoryManager(this).getHistory()

        if (history.isEmpty()) {
            Toast.makeText(this, "No clipboard history yet", Toast.LENGTH_SHORT).show()
            return
        }

        val items = history.map { entry ->
            if (entry.length > 80) entry.take(80) + "..." else entry
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("📋 Clipboard History (${history.size} items)")
            .setItems(items) { _, idx ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Nill Keyboard", history[idx]))
                Toast.makeText(this, "✅ Copied!", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Close", null)
            .setNegativeButton("Clear All") { _, _ ->
                ClipboardHistoryManager(this).clear()
                Toast.makeText(this, "Clipboard history cleared", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // ──── UI Helpers ────

    private fun sectionHeader(text: String) = TextView(this).apply {
        this.text = text
        textSize = 17f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setTextColor(Color.parseColor("#1C1C1E"))
        setPadding(0, dp(20), 0, dp(8))
    }

    private fun infoText(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(Color.parseColor("#666666"))
        lineSpacingMultiplier = 1.4f
        setPadding(0, dp(4), 0, dp(8))
    }

    private fun divider() = View(this).apply {
        setBackgroundColor(Color.parseColor("#E5E5EA"))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        ).apply { setMargins(0, dp(8), 0, dp(8)) }
    }

    private fun buildButton(label: String, colorHex: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(6), 0, dp(6)) }
            setOnClickListener { onClick() }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
