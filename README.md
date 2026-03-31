# ⌨️ Nill Keyboard

Custom Android keyboard with:
- 🇧🇩 Full Bangla keyboard
- 🇬🇧 English QWERTY  
- 🤖 Banglish → Bangla AI (Groq free)
- 🎤 Voice typing in Bangla + English (Groq Whisper free)
- 😊 200+ Emoji
- 🌙 Light & Dark theme
- 📋 Clipboard history
- 👁️ Parental control keylogger (password protected)

Supports **Android 5.1+** (API 22+)

---

## 🚀 Step 1: Build the APK (Free, Online)

### A) Setup GitHub (one time)
1. Go to **github.com** → Sign up (free)
2. Click **+** → **New repository**
3. Name it: `NillKeyboard`
4. Make it **Public**
5. Click **Create repository**

### B) Upload project files
1. Click **uploading an existing file** on GitHub
2. Drag ALL files from this folder into the browser
3. Make sure to upload the hidden `.github` folder too! (or create it manually)
4. Click **Commit changes**

### C) Run the build
1. Go to your repo → Click **Actions** tab
2. Click **Build Nill Keyboard APK**
3. Click **Run workflow** → **Run workflow** (green button)
4. Wait ~3-5 minutes
5. Click the finished workflow → Scroll down to **Artifacts**
6. Download **NillKeyboard-debug-APK**
7. Unzip → you have `app-debug.apk`!

---

## 📱 Step 2: Install on Android

1. Send the APK to your phone (Telegram, USB, Email)
2. On phone: **Settings → Security → Unknown Sources → ON**
3. Open the APK file → Install
4. Open **Nill Keyboard** app
5. Tap **"Step 1: Enable Keyboard"** → Turn it ON
6. Tap **"Step 2: Select This Keyboard"** → Choose Nill Keyboard
7. Tap **"Settings"** → Add your Groq API key

---

## 🔑 Step 3: Get Free Groq API Key

1. Go to **console.groq.com**
2. Sign up (free, no credit card)
3. Click **API Keys** → **Create API Key**
4. Copy the key (starts with `gsk_...`)
5. Open Nill Keyboard app → Settings → Paste the key → Save

---

## ⌨️ Keyboard Features

### Mode Switching
- Tap **ENG** → English QWERTY
- Tap **বাং** → Full Bangla keyboard  
- Tap **BNG🤖** → Banglish mode (type in English, AI converts to Bangla)
- Tap **😊** → Emoji picker
- Or use the **⌨** key in the bottom row to cycle modes

### Banglish AI Mode
1. Select BNG🤖 mode
2. Type in English: `ami tomake bhalobasi`
3. Tap **রূপান্তর ✨** button
4. AI converts to: `আমি তোমাকে ভালোবাসি`

### Voice Typing
1. Tap **🎤** button (bottom row)
2. Speak in Bangla or English
3. Tap **⏹** to stop
4. AI transcribes your speech

### Parental Control
- Settings → Enable Keylogger toggle
- All keystrokes saved to local file
- View with password: **2090718**

---

## ⚙️ Troubleshooting

**Build fails?**
- Make sure ALL files are uploaded (including `app/` folder structure)
- Check the Actions log for the specific error

**Voice not working?**
- Allow microphone permission when the app asks
- Make sure Groq API key is set in Settings

**Banglish not converting?**  
- Check internet connection
- Verify Groq API key in Settings starts with `gsk_`

---

*Built with ❤️ — Android 5.1+ compatible*
