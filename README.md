<p align="center">
  <img src="logo.png" width="100" alt="Calculator icon"/>
</p>

<h1 align="center">Calculator</h1>

<p align="center">
  <b>A clean, expressive calculator for Android</b><br/>
  Scientific functions, calculation history, dynamic theming, and OLED dark mode.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white" alt="Min SDK 26"/>
  <img src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack_Compose-1.11-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose"/>
  <img src="https://img.shields.io/badge/Material_3-Expressive-E8DEF8" alt="M3 Expressive"/>
</p>

---

## About

A lightweight calculator built with Jetpack Compose and Material 3 Expressive. Supports standard arithmetic, scientific functions (sin, cos, tan, log, ln, sqrt, powers, factorials), parentheses, and percentages. Keeps a full history of past calculations that you can tap to reuse.

The app follows Material 3 Expressive guidelines throughout: expressive button shapes with press animations, M3E motion scheme tokens, dynamic color from your wallpaper, and an adaptive layout that shows a side-by-side history panel on tablets.

## Features

- **Standard & scientific calculator** with trig, log, sqrt, powers, factorial, and constants
- **Calculation history** with swipe-down gesture or tap to open, stored locally with Room
- **Dynamic color** (Material You) follows your wallpaper theme
- **Dark mode** with system, light, and dark options
- **OLED dark theme** with pure black surfaces
- **Adaptive layout** single-pane on phones, dual-pane with history on tablets
- **Expressive animations** using M3E motion scheme and shape morphing on press
- **Edge-to-edge** with proper system bar handling

## Building from source

**Requirements:** Java 21, Android SDK 36

```bash
git clone https://github.com/Pingasmaster/calculator.git
cd calculator

# Generate signing keys for release builds (skip for debug)
keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 4096 -validity 10000 -alias calculator
echo "your-password-here" > .password-signing-keys

# Build (runs lint, assembles debug + release, copies APK to project root)
./build.sh
```

## Contributing

Contributions welcome. Open an issue or PR.
