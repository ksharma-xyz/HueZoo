# HUEZOO &nbsp;<img src="https://upload.wikimedia.org/wikipedia/commons/thumb/7/74/Kotlin_Icon.png/1200px-Kotlin_Icon.png" height="28"> &nbsp;<img src="https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/Android_robot_head.svg/1100px-Android_robot_head.svg.png" height="28"> &nbsp;<img src="https://upload.wikimedia.org/wikipedia/commons/c/ca/IOS_logo.svg" height="28">

> *Can you see the difference?*

Huezoo is a color perception game — and a personal experiment in what happens when you push UI design, interactions, and touch controls as far as they can go on mobile.

The design system was built from imagination with help from [Stitch by Google](https://stitch.withgoogle.com) and iterated entirely using [Claude](https://claude.ai). The goal was simple: build something that feels alive. Every animation, transition, and surface exists to make the act of tapping a color feel satisfying. Color is close to my heart — building a game around it felt like the right way to explore the limits of what Compose Multiplatform can do.

The scoring is based on **ΔE (Delta E)** — the scientific measure of color difference used by designers, display engineers, and color scientists. The lower your ΔE, the harder the difference you spotted. A ΔE below 1.0 is considered imperceptible to the human eye. Chase it.

[![Huezoo App CI](https://github.com/ksharma-xyz/HueZoo/actions/workflows/code-quality.yml/badge.svg)](https://github.com/ksharma-xyz/HueZoo/actions)

---

## Game Modes

**The Threshold** — your personal perception limit. Six color swatches. One is the odd one out. Tap it correctly and the ΔE shrinks. Wrong tap and it's over. How low can you go?

**Daily Challenge** — same puzzle for every player on the planet, every day. Six rounds. One attempt. Your score lives on the leaderboard.

---

## The Experiment

This project is a learning ground — an exploration of what it feels like to build a polished, opinionated mobile product:

- How far can you push **custom shapes, neon glows, and kinetic animations** in Compose before it breaks?
- Can a **shared Compose UI** feel truly native on both Android and iOS?
- What does a **design system built entirely without Material Design** look like?
- How much of a real product — billing, ads, CI/CD, crash reporting — can be built by one person?

The answer to all of the above turned out to be: further than expected.

---

## Tech Stack

| Area | Technology |
|---|---|
| **UI** | Compose Multiplatform (Android + iOS, shared UI) |
| **Language** | Kotlin Multiplatform |
| **DI** | Koin |
| **Local Storage** | SQLDelight |
| **Crash Reporting** | Firebase Crashlytics |
| **Analytics** | Firebase Analytics |
| **Monetisation** | Google Play Billing (Android) · StoreKit (iOS) · AdMob |
| **Color Science** | CIEDE2000 ΔE algorithm — pure Kotlin, no dependencies |
| **Design** | Custom design system — no Material, built with Stitch + Claude |

---

## Architecture

Single shared module (`composeApp`) with clean platform separation:

```
composeApp/
├── commonMain/     — all UI, game logic, color math, view models
├── androidMain/    — Android billing, AdMob rewarded ads
└── iosMain/        — StoreKit billing, iOS-specific platform calls

androidApp/         — thin Android shell (Activity + Firebase config)
iosApp/             — Xcode project + Fastlane for TestFlight CI
```

Color math lives in `commonMain` — the CIEDE2000 formula is implemented in pure Kotlin so it runs identically on both platforms with zero platform divergence.

---

## Getting Started

### Prerequisites

- JDK 21+
- Android Studio (latest stable) or IntelliJ IDEA
- Xcode 16+ (for iOS)
- Firebase project — drop config files in:
  - `androidApp/src/debug/google-services.json`
  - `androidApp/src/release/google-services.json`
  - `iosApp/iosApp/GoogleService-Info.plist`

### Build Android

```bash
./gradlew :androidApp:assembleDebug
```

### Build iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator or device.

### Static Analysis

```bash
./gradlew detekt
```

---

## CI/CD

GitHub Actions workflows handle the full release pipeline:

| Workflow | Trigger |
|---|---|
| Code quality (Detekt + build) | Every push to `main` |
| Distribute to Firebase App Distribution | RC branch |
| Upload to Google Play (internal track) | Release branch |
| Upload to TestFlight | Release branch |

See [`docs/ci_cd/CI_SETUP_CHECKLIST.md`](docs/ci_cd/CI_SETUP_CHECKLIST.md) for the full secrets and variables setup guide.

---

## Download

*Coming soon to the App Store and Google Play.*

---

## License

```
Copyright 2025 Karan Sharma.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## Contact

- 🌐 Website: [huezoo.app](https://huezoo.app)
- 📧 Email: [hey@huezoo.app](mailto:hey@huezoo.app)
