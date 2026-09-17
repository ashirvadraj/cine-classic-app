# CineClassic 🎬

> **100% Ad-Free Classic Cinema Streaming & Offline Download App for Android**

CineClassic is a high-performance, ad-free Android application dedicated to golden-era and public-domain Hindi (Bollywood) and English (Hollywood) cinema. Built with modern Android standards: **Kotlin**, **AndroidX Media3 (ExoPlayer)**, **Material Design 3**, and **Coroutines**.

---

## 🌟 Key Features

- **Ad-Free Playback**: Zero advertisements, zero trackers, pure cinematic experience.
- **Classic Bollywood & Vintage Hollywood**: Curated catalog featuring legends like Raj Kapoor, Nargis, Guru Dutt, Dilip Kumar, Cary Grant, Audrey Hepburn, and Orson Welles.
- **High-Definition Streaming**: Crisp 1080p and 720p streams powered by AndroidX Media3 (ExoPlayer).
- **Offline Download Manager**: Download movies directly to device storage for offline playback anywhere.
- **Instant Search & Genre Filters**: Instant filtering by era, language (Hindi/English), and genres (Drama, Noir, Romance, Comedy, Thriller).
- **Personal Watchlist & History**: Save favorites to your collection and auto-resume playback from where you left off.
- **Custom Video Player**:
  - 10-second fast-forward and rewind controls.
  - Interactive scrub bar with real-time buffering indicator.
  - Immersive fullscreen landscape mode with system UI auto-hiding.

---

## 📱 Tech Stack & Architecture

- **Language**: Kotlin 1.9.22
- **UI Framework**: Android Views with Material Design 3 and ViewBinding
- **Media Engine**: AndroidX Media3 (ExoPlayer 1.3.1)
- **Image Caching**: Glide 4.16
- **Download Management**: Android DownloadManager with background progress tracking
- **Asynchronous Execution**: Kotlin Coroutines & Lifecycle KTX
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 / 15 (API 34)

---

## 🚀 Direct APK Download

You can find the pre-compiled APK directly inside the repository under [`apk/cineclassic-v1.0.apk`](apk/cineclassic-v1.0.apk) or in the [GitHub Releases](../../releases).

### How to Install on Android
1. Download `cineclassic-v1.0.apk` onto your Android phone.
2. Tap the APK to install.
3. If prompted, allow "Install from unknown sources" for your file manager or browser.
4. Launch **CineClassic** and enjoy classic cinema ad-free!

---

## 🛠️ Building From Source

```bash
# Clone the repository
git clone https://github.com/ashirvadraj/cine-classic-app.git
cd cine-classic-app

# Build debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚖️ Legal & Copyright Compliance

All films featured in this application are preserved in the **Public Domain** or licensed under **Creative Commons** archives (sourced from the open Internet Archive). No copyrighted or DRM-restricted media is hosted, scraped, or infringed upon.
