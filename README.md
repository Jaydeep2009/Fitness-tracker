# Fitness Tracker

A private, local-first progress photo studio for documenting muscle-building progress.

## Features

- Create separate progress albums (front, side, back, or custom poses).
- Capture photos with a live camera and an adjustable previous-photo ghost overlay to align framing and posture.
- Browse and delete photos; review timestamps and notes.
- Play an album as a timelapse and export the sequence as a WebM video where supported.
- Download individual photos and export/import a JSON backup.
- Skeuomorphic, tactile dark-gym interface with embossed controls and analog-inspired details.

## Run locally

This is a static app and does not need a backend:

1. Open `index.html` in a modern browser, or serve this directory using a static server.
2. Allow camera permission when prompted. Camera access generally requires `localhost` or HTTPS.
3. Use the app on the same browser/device to access locally stored albums.

## Privacy and storage

Photos and album data remain in this browser using IndexedDB. They are not sent to GitHub or any server. Browser storage can be cleared by the browser/device; use **Settings → Export backup** regularly if you want a portable copy. Camera permission is used only for live preview and capture.

## Browser notes

Camera capture requires a secure context (HTTPS or localhost). Timelapse video export depends on browser support for `MediaRecorder` and WebM. If video export is unavailable, the app can still play the photo sequence for preview.
