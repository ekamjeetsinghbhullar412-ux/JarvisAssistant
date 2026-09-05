# Jarvis — Android Voice Assistant

A real, buildable Android Studio project: a JARVIS-style voice assistant with a futuristic
Compose UI, on-device command parsing for common phone actions, and an AI conversation
fallback powered by your own backend (so your API key never ships inside the app).

## What this can and cannot do on real Android (read this first)

Android's security model does not allow a normal app to:
- Draw over or bypass the actual lock screen PIN/pattern/biometric.
- Continuously listen to the microphone while the screen is off and the device is locked.
- Silently send messages, place calls, or change settings without the permissions dialog
  and (in this app's case) an explicit in-app confirmation step.

What Jarvis *does* do, using officially supported APIs:
- Shows its UI **on top of** the lock screen (before you unlock), like an incoming call
  screen — via `showWhenLocked`/`turnScreenOn`. You still need to unlock for anything
  sensitive underneath.
- Runs a foreground service with a visible "Jarvis is active" notification so a listening
  session can continue while the app/service is alive and the screen is on.
- Can be set as your device's default **Assistant app** (Settings → Apps → Default apps →
  Digital assistant app) so long-pressing the home button / assistant gesture opens it —
  the closest legitimate path to deeper OS integration.
- Executes real actions — opening apps, setting alarms/timers, calling/texting (after you
  say "yes" to confirm), reading battery %, searching the web, basic calculator, calendar
  event creation, media controls — via public Android Intents and APIs.
- Falls back to an AI conversation (via your backend) for anything that isn't a recognized
  device command.

A true always-on "Hey Jarvis" wake word with the screen off is not something a third-party
app can implement on stock Android — that's reserved for the OS's own assistant framework.

## Project structure

```
JarvisAssistant/
├── app/                                 Android app module
│   └── src/main/java/com/jarvis/assistant/
│       ├── MainActivity.kt              Entry point, permissions, lock-screen flags
│       ├── JarvisApplication.kt         Notification channel setup
│       ├── ui/
│       │   ├── theme/                   Colors, type, dark HUD theme
│       │   ├── components/              AICore (animated ring), WaveformVisualizer
│       │   ├── screens/                 MainScreen (HUD), SettingsScreen
│       │   └── navigation/              Compose NavHost
│       ├── viewmodel/                   AssistantViewModel, SettingsViewModel
│       ├── voice/                       SpeechRecognizerManager, TextToSpeechManager
│       ├── commands/                    Command, CommandParser, CommandExecutor, ContactLookup
│       ├── ai/                          Retrofit ApiService, NetworkModule, AIRepository
│       ├── data/local/                  Room (conversation history)
│       ├── data/datastore/              DataStore (settings)
│       ├── service/                     JarvisForegroundService
│       └── util/                        PermissionUtils
├── backend/                             Node/Express proxy (keeps your API key off-device)
│   ├── server.js
│   ├── package.json
│   └── .env.example
├── build.gradle.kts / settings.gradle.kts
└── README.md (this file)
```

## Setup instructions

### 1. Backend first
```
cd backend
npm install
cp .env.example .env
# edit .env: add your ANTHROPIC_API_KEY and a random JARVIS_DEVICE_TOKEN
npm start
```
Deploy it somewhere reachable from your phone (see backend/README.md) and note the HTTPS URL.

### 2. Open the Android project
1. Install **Android Studio** (Koala or newer recommended).
2. `File → Open` and select the `JarvisAssistant` folder.
3. Let Gradle sync (it will download dependencies — needs internet the first time).

### 3. Configure the backend URL and token
In `JarvisAssistant/local.properties` (create it if it doesn't exist — it's git-ignored by
default in Android projects), add:
```
JARVIS_BACKEND_URL=https://your-deployed-backend.example.com/
JARVIS_DEVICE_TOKEN=the-same-random-string-you-put-in-backend/.env
```
Gradle reads these as project properties automatically via `gradle.properties`/
`local.properties`. If you're using Gradle command line instead, pass them as:
```
./gradlew assembleDebug -PJARVIS_BACKEND_URL=https://... -PJARVIS_DEVICE_TOKEN=...
```

### 4. Build and run
- Click **Run ▶** in Android Studio with a physical device (API 26+) connected via USB
  debugging, or an emulator (note: emulators often have poor/no mic input — a real phone
  is strongly recommended for voice testing).
- Or from the command line: `./gradlew installDebug`

### 5. Grant permissions on first launch
The app will ask for microphone access when you first tap the mic button. Contacts, call,
and SMS permissions are requested only when you first try a command that needs them
(e.g. "call Mom").

### 6. (Optional) Set as default assistant
Settings → Apps → Default apps → Digital assistant app → Jarvis. This lets the system
assistant gesture open Jarvis directly.

## Testing commands to try
- "What time is it"
- "What's my battery"
- "Open Chrome"
- "Set an alarm for 7 30 am"
- "Start a timer for 5 minutes"
- "Search the web for pixel 9 review"
- "Call Mom" → will ask you to confirm by voice or in the dialog
- "What's the capital of France" → falls through to the AI backend

## Troubleshooting
- **"AI provider request failed" / network errors**: confirm the backend is running and
  reachable, and that `JARVIS_BACKEND_URL`/`JARVIS_DEVICE_TOKEN` match on both sides.
- **Mic button does nothing**: check the app has microphone permission in Android Settings.
- **No spoken responses**: make sure a TTS engine is installed and has your language's voice
  data (Settings → Accessibility → Text-to-speech output → install voice data).
- **"Speech recognition isn't available"**: some emulator images and custom ROMs lack Google's
  speech services; test on a real device with Google apps installed.
- **Calls/texts don't work**: these require CALL_PHONE/SEND_SMS permission grants and only
  fire after you confirm — check you said/tapped "yes" and that permissions were granted.
- **Gradle sync fails on KSP/Compose versions**: this project pins compatible versions
  (Kotlin 1.9.24 / Compose compiler 1.5.14 / AGP 8.5.2); if you upgrade one, check
  Google's Compose-Kotlin compatibility map and update the others together.

## Extending it
- Add more phrases/intents in `commands/CommandParser.kt` + a case in `CommandExecutor.kt`.
- Swap the AI provider by editing only `backend/server.js` — the app never changes.
- Add a persistent local "Reminders" feature (currently reminders route to a calendar
  event) by adding a Room entity + a notification-based scheduler.
