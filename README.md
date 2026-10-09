# IronFrame — Native Android Progress Photo Studio

IronFrame is a **native Android app written in Kotlin**, using Jetpack Compose and CameraX. It is not a web app, website, or PWA.

## Features

- Create separate albums for front, side, back, or custom poses.
- Capture photos using the Android camera.
- Overlay the previous photo on the live camera preview with adjustable opacity to align framing and posture.
- Browse dated photos and notes, and delete unwanted frames.
- Play the album's photos in sequence as a timelapse preview.
- Skeuomorphic, tactile dark-gym interface with embossed surfaces and lime accents.
- Local-first storage: photo files and album metadata stay on the device; no account or server is required.

## Open and run

1. Open this repository in the latest Android Studio.
2. Use JDK 17 and Android SDK 35.
3. Let Android Studio sync the Gradle project.
4. Run the `app` configuration on an Android device or emulator (camera hardware is recommended for capture).
5. Grant camera permission when prompted.

## Project structure

- `app/src/main/java/com/ironframe/fitness/MainActivity.kt` — Jetpack Compose UI, CameraX capture, album/gallery/timelapse flows, and local persistence.
- `app/src/main/AndroidManifest.xml` — Android app declaration and camera permission.
- `app/build.gradle.kts` — Android/Kotlin plugins and AndroidX dependencies.

## Privacy and limitations

Photos are saved in the app's private files directory and album metadata is stored in app preferences. Photos are not uploaded. Uninstalling the app or clearing its data removes local photos.

The current timelapse feature plays the saved sequence inside the app. Encoding and exporting a video file, importing photos from the system gallery, and a dedicated backup/restore flow are not implemented yet.
