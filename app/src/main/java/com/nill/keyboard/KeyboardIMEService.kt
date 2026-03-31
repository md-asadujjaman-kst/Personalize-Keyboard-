package com.nill.keyboard

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*

class KeyboardIMEService : InputMethodService() {

    companion object {
        const val PREFS = "nill_kb_prefs"
        const val PREF_DARK = "dark_theme"
        const val PREF_GROQ_KEY = "groq_key"
        const val PREF_KEYLOGGER = "keylogger_on"
    }

    enum class KbMode { ENGLISH, BANGLA, BANGLISH, EMOJI }

    private var kbMode = KbMode.ENGLISH
    private var isShift = false
    private var banglishBuffer = StringBuilder()
    private lateinit var prefs: SharedPreferences
    private lateinit var keyLogManager: KeyLogManager
    private lateinit var clipManager: ClipboardHistoryManager
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var rootView: LinearLayout? = null

    // Theme colors
    private var bgColor = 0
    private var keyBg = 0
    private var keySpecialBg = 0
    private var keyText = 0
    private var accentBg = 0
    private var accentText = 0

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        keyLogManager = KeyLogManager(this)
        clipManager = ClipboardHistoryManager(this)
    }

    override fun onCreateInputView(): View {
        loadTheme()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
        }
        rootView = root
        refreshKeyboard()
        return root
    }

    override fun onWindowShown() {
        super.onWindowShown()
        loadTheme()
        refreshKeyboard()
    }

    private fun loadTheme() {
        val dark = prefs.getBoolean(PREF_DARK, false)
        bgColor = if (dark) Color.parseColor("#1C1C1E") else Color.parseColor("#D1D3D4")
        keyBg = if (dark) Color.parseColor("#3A3A3C") else Color.WHITE
        keySpecialBg = if (dark) Color.parseColor("#636366") else Color.parseColor("#AEB4BD")
        keyText = if (dark) Color.WHITE else Color.parseColor("#1C1C1E")
        accentBg = Color.parseColor("#007AFF")
        accentText = Color.WHITE
    }

    private fun refreshKeyboard() {
        val root = rootView ?: return
        root.removeAllViews()
        root.setBackgroundColor(bgColor)
        root.addView(buildModeBar())
        when (kbMode) {
            KbMode.ENGLISH, KbMode.BANGLISH -> buildQwertyKeys(root)
            KbMode.BANGLA -> buildBanglaKeys(root)
            KbMode.EMOJI -> buildEmojiSection(root)
        }
        if (kbMode == KbMode.BANGLISH) {
            root.addView(buildBanglishHint())
        }
    }

    // ===================== MODE BAR =====================

    private fun buildModeBar(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(bgColor)
            setPadding(dp(4), dp(4), dp(4), 0)
        }
        val labels = listOf("ENG", "বাং", "BNG\uD83E\uDD16", "\uD83D\uDE0A")
        val modes  = listOf(KbMode.ENGLISH, KbMode.BANGLA, KbMode.BANGLISH, KbMode.EMOJI)
        modes.forEachIndexed { i, mode ->
            val active = kbMode == mode
            val btn = TextView(this).apply {
                text = labels[i]
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(if (active) accentText else keyText)
                background = makeRound(if (active) accentBg else keySpecialBg, 6)
                layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
                setOnClickListener {
                    kbMode = modes[i]
                    banglishBuffer.clear()
                    isShift = false
                    refreshKeyboard()
                }
            }
            bar.addView(btn)
        }
        return bar
    }

    // ===================== QWERTY (English & Banglish) =====================

    private fun buildQwertyKeys(root: LinearLayout) {
        val row1 = listOf("q","w","e","r","t","y","u","i","o","p")
        val row2 = listOf("a","s","d","f","g","h","j","k","l")
        val row3 = listOf("z","x","c","v","b","n","m")

        // Row 1
        root.addView(makeKeyRow {
            row1.forEach { k -> addLetterKey(this, k) }
            addSpecialKey(this, "⌫", 1.5f) { deleteChar() }
        })

        // Row 2 — slightly indented
        root.addView(makeKeyRow {
            addSpacer(this, 0.3f)
            row2.forEach { k -> addLetterKey(this, k) }
            addSpecialKey(this, "↵", 1.7f) { pressEnter() }
        })

        // Row 3 — shift + letters + backspace
        root.addView(makeKeyRow {
            addSpecialKey(this, if (isShift) "⬆" else "⇧", 1.5f) {
                isShift = !isShift
                refreshKeyboard()
            }
            row3.forEach { k -> addLetterKey(this, k) }
            addSpecialKey(this, ",", 0.8f) { typeChar(",") }
            addSpecialKey(this, ".", 0.8f) { typeChar(".") }
        })

        // Number row
        root.addView(makeKeyRow {
            listOf("1","2","3","4","5","6","7","8","9","0").forEach { n ->
                val tv = makeKey(n, 1f)
                tv.setOnClickListener { typeChar(n) }
                addView(tv)
            }
        })

        // Symbols row
        root.addView(makeKeyRow {
            listOf("!","@","#","$","%","?","&","*","(",")").forEach { s ->
                val tv = makeKey(s, 1f)
                tv.setOnClickListener { typeChar(s) }
                addView(tv)
            }
        })

        // Bottom row
        root.addView(buildBottomRow())
    }

    private fun addLetterKey(row: LinearLayout, key: String) {
        val display = if (isShift) key.uppercase() else key
        val tv = makeKey(display, 1f)
        tv.setOnClickListener {
            performHapticFeedback(tv, HapticFeedbackConstants.KEYBOARD_TAP)
            val ch = if (isShift) key.uppercase() else key
            typeChar(ch)
            if (isShift) {
                isShift = false
                refreshKeyboard()
            }
        }
        row.addView(tv)
    }

    private fun addSpecialKey(row: LinearLayout, label: String, flex: Float, action: () -> Unit) {
        val tv = makeKey(label, flex, isSpecial = true)
        tv.setOnClickListener {
            performHapticFeedback(tv, HapticFeedbackConstants.KEYBOARD_TAP)
            action()
        }
        row.addView(tv)
    }

    private fun addSpacer(row: LinearLayout, flex: Float) {
        val v = View(this)
        v.layoutParams = LinearLayout.LayoutParams(0, dp(44), flex)
        row.addView(v)
    }

    // ===================== BANGLA KEYBOARD =====================

    private fun buildBanglaKeys(root: LinearLayout) {
        // Vowels
        val vowels = listOf("অ","আ","ই","ঈ","উ","ঊ","ঋ","এ","ঐ","ও","ঔ")
        root.addView(makeKeyRow {
            vowels.forEach { v ->
                val tv = makeKey(v, 1f)
                tv.setOnClickListener { typeChar(v) }
                addView(tv)
            }
        })

        // Matras (vowel signs)
        val matras = listOf("া","ি","ী","ু","ূ","ৃ","ে","ৈ","ো","ৌ","্")
        root.addView(makeKeyRow {
            matras.forEach { m ->
                val tv = makeKey(m, 1f)
                tv.setOnClickListener { typeChar(m) }
                addView(tv)
            }
        })

        // Consonants rows
        val cons = listOf(
            listOf("ক","খ","গ","ঘ","ঙ","চ","ছ","জ","ঝ","ঞ","⌫"),
            listOf("ট","ঠ","ড","ঢ","ণ","ত","থ","দ","ধ","ন","↵"),
            listOf("প","ফ","ব","ভ","ম","য","র","ল","শ","ষ","স"),
            listOf("হ","ড়","ঢ়","য়","ৎ","ং","ঃ","ঁ","।","?","!")
        )

        cons.forEachIndexed { ri, row ->
            root.addView(makeKeyRow {
                row.forEach { k ->
                    when (k) {
                        "⌫" -> addSpecialKey(this, k, 1f) { deleteChar() }
                        "↵" -> addSpecialKey(this, k, 1f) { pressEnter() }
                        else -> {
                            val tv = makeKey(k, 1f)
                            tv.setOnClickListener { typeChar(k) }
                            addView(tv)
                        }
                    }
                }
            })
        }

        // Bangla numbers
        root.addView(makeKeyRow {
            listOf("০","১","২","৩","৪","৫","৬","৭","৮","৯").forEach { n ->
                val tv = makeKey(n, 1f)
                tv.setOnClickListener { typeChar(n) }
                addView(tv)
            }
        })

        root.addView(buildBottomRow())
    }

    // ===================== BANGLISH HINT BAR =====================

    private fun buildBanglishHint(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(if (prefs.getBoolean(PREF_DARK, false))
                Color.parseColor("#2C2C2E") else Color.parseColor("#F2F2F7"))
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        val hint = TextView(this).apply {
            text = if (banglishBuffer.isNotEmpty())
                "\"$banglishBuffer\" → রূপান্তর করুন"
            else
                "বাংলিশ: English-এ টাইপ করুন → AI বাংলায় রূপান্তর করবে"
            textSize = 12f
            setTextColor(if (prefs.getBoolean(PREF_DARK, false)) Color.LTGRAY else Color.parseColor("#555555"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val convertBtn = TextView(this).apply {
            text = "রূপান্তর ✨"
            textSize = 12f
            setTextColor(accentText)
            background = makeRound(accentBg, 8)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { convertBanglish() }
        }
        bar.addView(hint)
        bar.addView(convertBtn)
        return bar
    }

    // ===================== EMOJI =====================

    private fun buildEmojiSection(root: LinearLayout) {
        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220))
        }
        val grid = GridLayout(this).apply {
            columnCount = 9
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        getEmojiList().forEach { emoji ->
            val tv = TextView(this).apply {
                text = emoji
                textSize = 22f
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(4), dp(4), dp(4))
                setOnClickListener { typeChar(emoji) }
            }
            grid.addView(tv)
        }
        scrollView.addView(grid)
        root.addView(scrollView)
        root.addView(buildBottomRow())
    }

    // ===================== BOTTOM ROW =====================

    private fun buildBottomRow(): LinearLayout {
        return makeKeyRow {
            // Mode cycle
            addSpecialKey(this, "⌨", 1f) {
                kbMode = when (kbMode) {
                    KbMode.ENGLISH -> KbMode.BANGLA
                    KbMode.BANGLA -> KbMode.BANGLISH
                    KbMode.BANGLISH -> KbMode.EMOJI
                    KbMode.EMOJI -> KbMode.ENGLISH
                }
                banglishBuffer.clear()
                refreshKeyboard()
            }
            // Space
            val space = makeKey("Space", 3f)
            space.setOnClickListener { typeChar(" ") }
            addView(space)
            // Voice (mic)
            val micLabel = if (isRecording) "⏹" else "🎤"
            addSpecialKey(this, micLabel, 1f) { toggleVoice() }
            // Enter
            addSpecialKey(this, "↵", 1f) { pressEnter() }
        }
    }

    // ===================== KEY BUILDERS =====================

    private fun makeKeyRow(block: LinearLayout.() -> Unit): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(3), dp(2), dp(3), dp(2))
        }
        row.block()
        return row
    }

    private fun makeKey(label: String, flex: Float, isSpecial: Boolean = false): TextView {
        return TextView(this).apply {
            text = label
            textSize = when {
                label.length > 3 -> 11f
                label.length > 1 -> 13f
                else -> 17f
            }
            gravity = Gravity.CENTER
            setTextColor(if (isSpecial) keyText else keyText)
            background = makeRound(if (isSpecial) keySpecialBg else keyBg, 6)
            isHapticFeedbackEnabled = true
            isFocusable = false
            val lp = LinearLayout.LayoutParams(
                if (flex == 0f) LinearLayout.LayoutParams.WRAP_CONTENT else 0,
                dp(44),
                flex
            )
            lp.setMargins(dp(2), dp(2), dp(2), dp(2))
            layoutParams = lp
        }
    }

    private fun makeRound(color: Int, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * resources.displayMetrics.density
            setColor(color)
        }
    }

    // ===================== ACTIONS =====================

    private fun typeChar(ch: String) {
        currentInputConnection?.commitText(ch, 1)
        if (kbMode == KbMode.BANGLISH && ch != " ") {
            banglishBuffer.append(ch)
        } else if (ch == " " && kbMode == KbMode.BANGLISH) {
            banglishBuffer.append(" ")
        }
        if (prefs.getBoolean(PREF_KEYLOGGER, false)) {
            keyLogManager.log(ch)
        }
    }

    private fun deleteChar() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        if (kbMode == KbMode.BANGLISH && banglishBuffer.isNotEmpty()) {
            banglishBuffer.deleteCharAt(banglishBuffer.length - 1)
        }
    }

    private fun pressEnter() {
        currentInputConnection?.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
        )
        currentInputConnection?.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
        )
    }

    private fun performHapticFeedback(v: View, type: Int) {
        v.isHapticFeedbackEnabled = true
        v.performHapticFeedback(type, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
    }

    // ===================== BANGLISH AI =====================

    private fun convertBanglish() {
        val text = banglishBuffer.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "কিছু টাইপ করুন আগে", Toast.LENGTH_SHORT).show()
            return
        }
        val key = prefs.getString(PREF_GROQ_KEY, "") ?: ""
        if (key.isEmpty()) {
            Toast.makeText(this, "Settings → Groq API Key দিন প্রথমে", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "⏳ AI রূপান্তর করছে...", Toast.LENGTH_SHORT).show()
        val client = GroqApiClient(key)
        val typedLen = text.length
        client.convertBanglish(text) { result ->
            Handler(Looper.getMainLooper()).post {
                currentInputConnection?.deleteSurroundingText(typedLen, 0)
                currentInputConnection?.commitText(result, 1)
                banglishBuffer.clear()
            }
        }
    }

    // ===================== VOICE TYPING =====================

    private fun toggleVoice() {
        if (isRecording) {
            stopVoiceRecording()
        } else {
            startVoiceRecording()
        }
    }

    private fun startVoiceRecording() {
        try {
            val audioFile = java.io.File(cacheDir, "nk_voice.m4a")
            if (audioFile.exists()) audioFile.delete()

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            refreshKeyboard()
            Toast.makeText(this, "🎤 Recording... press ⏹ to stop", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Mic error: check permissions in app", Toast.LENGTH_SHORT).show()
            isRecording = false
        }
    }

    private fun stopVoiceRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            refreshKeyboard()

            val key = prefs.getString(PREF_GROQ_KEY, "") ?: ""
            if (key.isEmpty()) {
                Toast.makeText(this, "Settings → Groq API Key দিন", Toast.LENGTH_SHORT).show()
                return
            }

            val audioFile = java.io.File(cacheDir, "nk_voice.m4a")
            if (!audioFile.exists() || audioFile.length() < 1000) {
                Toast.makeText(this, "Audio too short, try again", Toast.LENGTH_SHORT).show()
                return
            }

            val lang = when (kbMode) {
                KbMode.BANGLA, KbMode.BANGLISH -> "bn"
                else -> "en"
            }

            Toast.makeText(this, "⏳ AI শুনছে...", Toast.LENGTH_SHORT).show()
            val client = GroqApiClient(key)
            client.transcribeAudio(audioFile, lang) { text ->
                Handler(Looper.getMainLooper()).post {
                    if (text.isNotEmpty()) {
                        currentInputConnection?.commitText(text, 1)
                    } else {
                        Toast.makeText(this, "Voice not recognized", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            mediaRecorder = null
            isRecording = false
            refreshKeyboard()
        }
    }

    // ===================== EMOJI LIST =====================

    private fun getEmojiList(): List<String> = listOf(
        "😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃","😉","😊","😇","🥰","😍","🤩",
        "😘","😚","😋","😛","😜","🤪","😝","🤑","🤗","🤭","🤫","🤔","🤨","😐","😑","😶",
        "😏","😒","🙄","😬","🤥","😌","😔","😪","🤤","😴","😷","🤒","🤕","🤢","🤮","🤧",
        "🥵","🥶","🥴","😵","🤯","🤠","🥸","😎","🤓","🧐","😕","😟","🙁","☹️","😮","😯",
        "😲","😳","🥺","😦","😧","😨","😰","😥","😢","😭","😱","😖","😣","😞","😓","😩",
        "😫","🥱","😤","😡","😠","🤬","😈","👿","💀","☠️","💩","🤡","👻","👽","🤖","😺",
        "👋","🤚","🖐️","✋","✌️","🤞","🤟","🤘","👌","🤌","👈","👉","👆","👇","👍","👎",
        "✊","👊","🤛","🤜","👏","🙌","🤝","🙏","💪","🦵","🦶","👀","👁️","👄","👂","👃",
        "❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❣️","💕","💞","💓","💗","💖",
        "💘","💝","🔥","⭐","🌟","✨","⚡","❄️","🌊","🌈","☀️","🌙","🌸","🌺","🌻","🍀",
        "🎉","🎊","🎈","🎁","🎀","🏆","🥇","🎵","🎶","🎸","🎹","🎤","🎧","📱","💻","📷",
        "✈️","🚀","🚗","🏠","🍎","🍕","🍔","🍟","🍰","🎂","☕","🍵","🥤","🍺","🥂","🍾",
        "⚽","🏀","🏈","⚾","🎾","🏐","🎮","🎲","♟️","🎯","🎳","🏋️","🤸","🧘","🏊","🚴",
        "🇧🇩","💯","✅","❌","⚠️","🔴","🟢","🔵","🟡","🔔","💡","🔑","🔒","🔓","💰","💸",
        "📞","📧","📝","📅","📌","📍","🗺️","🧭","⏰","⌚","📊","📈","📉","🔍","💊","🩺"
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
