# Reel Counter — Android APK build

This is a native Android/Kotlin project.

## Build an installable APK with GitHub Actions

1. Create a new **public or private** repository on GitHub (for example, `ReelCounter`).
2. Extract this ZIP on your computer.
3. Upload the **contents of the `ReelCounter` folder** to the repository root. The `.github/workflows/build.yml` file must remain at `.github/workflows/build.yml`.
4. Open the repository's **Actions** tab. Select **Build APK** and choose **Run workflow**. (A push to `main` or `master` also triggers a build.)
5. When the workflow finishes successfully, open that workflow run and download the **ReelCounter-APK** artifact.
6. Extract the artifact ZIP. It contains `app-debug.apk`.
7. Transfer `app-debug.apk` to your Android phone, open it, and allow installation from that source if Android asks.

## First-time setup on the phone

1. Open **Reel Counter**.
2. Tap **1. Open Accessibility settings**.
3. Enable the **Reel Counter** accessibility service.
4. On Android 13 and later, if Android blocks enabling it, open the app's App info page, tap the three-dot menu, choose **Allow restricted settings**, then return to Accessibility and enable the service.
5. Set a daily limit if desired. A limit of `0` disables the limit.

The app uses Android Accessibility Service events to detect Instagram Reels and YouTube Shorts. Detection depends on the apps' UI identifiers and may need updates if Instagram or YouTube changes its interface. The app is not affiliated with Instagram or YouTube.

## Local build requirements

- JDK 17
- Android SDK with Android API 34
- Gradle 8.7

From this folder, run:

```bash
gradle assembleDebug --no-daemon
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
