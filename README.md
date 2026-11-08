# Margaret M - Android reading app for kids (v1.0)

## Get the APK without installing anything
1. Create a free GitHub account, make a new repository, upload ALL files from this folder (including the .github folder).
2. Open the repo > Actions tab > "Build APK" runs automatically (about 5 min).
3. Open the finished run > Artifacts > download MargaretM-apk > unzip > app-debug.apk.
4. Copy the APK to your phone, tap it, allow "install unknown apps".

## Or use Android Studio
File > Open this folder > Build > Build APK(s).

## What's inside
- `Engine.kt`   pure-Kotlin content + question generator + XP/levels + badges + spaced repetition (unit-tested)
- `Store.kt`    local accounts, child profiles, progress, settings
- `Ui.kt`       design system: 4 themes, animated backdrop, buttons, rings, confetti, nav
- `Games.kt`    quiz runner, Memory Match, Sentence Builder
- `Screens.kt`  login, home, adventure map, badges, shop, parent zone, leaderboard
- `Classic.kt`  your original games (alphabet, tracing, basket, word builder, stories, word race)
