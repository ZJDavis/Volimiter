# Volimiter

Volimiter is an Android utility app that limits media volume while the device is using the built-in speaker. It lets the user choose a maximum media volume, start a foreground service that enforces that limit, and protect stopping the limiter with a PIN.

> Current status: early Android project / pre-release. The app is not yet published on Google Play.

## Features

- Set a maximum allowed media volume with a simple slider
- Runs as a foreground service while active
- Restarts after device boot when previously enabled
- Allows higher volume when wired, Bluetooth, or USB headphones are connected
- PIN protection for stopping the limiter
- Required, explicitly granted Device Admin activation for uninstall protection
- Optional launcher icon hiding/restoring from inside the app

## How It Works

Volimiter monitors the device's `STREAM_MUSIC` volume while the service is running. If no supported headset device is detected and the current media volume exceeds the configured maximum, the app lowers it back to the selected limit.

The app stores the selected volume locally using Android `SharedPreferences`. The PIN is encrypted using the Android Keystore with AES/GCM before being saved locally.

## Tech Stack

- Kotlin
- Android Studio
- Gradle Kotlin DSL
- Android foreground service
- Android Device Admin API
- Android Keystore
- SharedPreferences

## Project Structure

```text
Volimiter/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/volimiter/
│   │   │   ├── MainActivity.kt
│   │   │   ├── VolimiterService.kt
│   │   │   ├── BootReceiver.kt
│   │   │   ├── PinManager.kt
│   │   │   ├── IconManager.kt
│   │   │   └── VolimiterDeviceAdmin.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml
```

## Getting Started

### Prerequisites

- Android Studio
- Android SDK installed through Android Studio
- JDK configured for Android development
- Android emulator or physical Android device

### Clone the Repository

```bash
git clone https://github.com/ZJDavis/Volimiter.git
cd Volimiter
```

### Open in Android Studio

1. Open Android Studio.
2. Choose **Open**.
3. Select the `Volimiter` project folder.
4. Let Gradle sync.
5. Run the app on an emulator or physical device.

## Basic Usage

1. Open Volimiter.
2. Choose the maximum media volume using the slider.
3. Tap **Start Volimiter**.
4. Set a PIN when prompted.
5. Explicitly grant the required Device Admin access.
6. Tap **Stop Volimiter** and enter the PIN to stop the limiter.

## Permissions and Sensitive Features

Volimiter currently uses the following Android capabilities:

- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `RECEIVE_BOOT_COMPLETED`
- Device Admin receiver

The Device Admin behavior and app icon hiding feature should be clearly explained to users before release. These features affect user control and may require careful policy review before publishing on Google Play.

## Known Limitations

- Current UI is functional but minimal.
- Tests are still the default Android Studio sample tests.
- The service monitors volume on a dedicated background thread and responds to audio-route changes.
- Device Admin and hidden-icon behavior may create Google Play policy risk if not handled transparently.
- The current package name is still the default-style `com.example.volimiter`.

## Roadmap

- Improve UI design and onboarding
- Add a clear explanation screen before Device Admin activation
- Add a PIN reset/recovery strategy
- Add tests for PIN storage and volume-limiting behavior
- Add notification action to open the app
- Add settings screen
- Add Play Store screenshots and listing text
- Review Google Play policy risk around uninstall protection and icon hiding

## Screenshot list for later

_Once the app is ready, take screenshots of:_

1. Main screen with the volume slider.
2. First-run onboarding dialog.
3. PIN setup dialog.
4. Running state / notification.
5. Device Admin permission explanation.
6. Privacy policy dialog.

## License

No license has been selected yet.

If this project will be public, consider adding a license such as MIT, Apache-2.0, or keeping it as All Rights Reserved.
