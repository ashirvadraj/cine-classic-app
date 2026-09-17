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

- **Subtitles OFF by Default with Quick CC Toggle**: Embedded soft subtitles are disabled by default for clean, unobstructed viewing on Hindi and classic films, with an instant "CC: OFF / CC: ON" toggle button available in both the top bar and player controls.
- **Reliable 10-Second Fast-Forward & Rewind Controls**: Dedicated rewind and fast-forward controls that stay permanently visible and responsive across all streaming and downloaded titles.
- **Real Offline Downloads**: Direct MP4 resolution for archive preservation titles like *The Lunchbox (2013)*, downloading real files to local storage with verified offline ExoPlayer playback (zero fake simulation).
- **Accurate Release Years**: Smart curated release year extraction for 50+ classic & modern hits (eliminating the legacy fallback 1975 year bug).
- **Google Voice Search**: Instant voice search with mic input to search any movie or series hands-free.
- **Transliteration & Typo-Tolerant Search**: Smart search normalizer effortlessly matches common variations (e.g., *DDLJ*, *Dilwale Dulhaniya Le Jyege*, *Veer Zara*, *Zanzeer*).
- **Strict Anti-Fake & Anti-Review Engine**: Multi-layer filtering rejecting reviews, fake slideshows, reaction videos, explanations, audio songs, shorts, and paid rental paywalls.
- **65-Minute Feature Duration Enforcement**: Enforces a strict $\ge 65$-minute duration floor for full movies ($\ge 25$ minutes for series) and $\ge 250$ MB for Archive preservation records.
- **Verified Full-Length Classic Streams**: Genuine complete prints including *Zanjeer (1973 - 2h 22m)*, *Dilwale Dulhania Le Jayenge (1995 - 3h 10m)*, *The Lunchbox (2013 - 1h 44m)*, and *Veer-Zaara (2004 - 3h 14m)*.
- **Live Download Progress Tracking**: Real-time download percentage and MB downloaded vs total MB directly in the UI and notifications.
- **Dual Playback Engine**: Zero-buffering player with clean audio transitions, fallback stream switching, and offline local playback.
- **Comprehensive Unit Testing**: Automated test coverage verifying search parsing, duration calculations, keyword filtering, year extraction, and download logic.

---

## 🚀 Direct APK Download

You can find the latest pre-compiled APK directly inside the repository under [`apk/cineclassic-v1.5.0.apk`](apk/cineclassic-v1.5.0.apk) or in the [GitHub Releases](../../releases).

### How to Install on Android
1. Download `cineclassic-v1.5.0.apk` onto your Android phone.
2. Tap the APK to install.
3. If prompted, allow "Install from unknown sources" for your file manager or browser.
4. Launch **CineClassic** and enjoy classic cinema ad-free!

---

## 🛠️ Building & Testing From Source

```bash
# Clone the repository
git clone https://github.com/ashirvadraj/cine-classic-app.git
cd cine-classic-app

# Run Unit Tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/cineclassic-v1.5.0.apk
```

---

## ⚖️ Legal & Copyright Compliance

All films featured in this application are preserved in the **Public Domain** or licensed under **Creative Commons** archives (sourced from open Internet Archives). No DRM-restricted media is hosted, scraped, or infringed upon.

