# Margaret M - Android reading app for kids (v2.0)

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

## New in v2.0
- `World3D.kt`  pure-Kotlin 3D toolkit (camera, meshes, lighting, culling) + the full **Letter Dash 3D** simulation
- `Gfx3D.kt`    draws that 3D world on a Compose Canvas: glowing road, crystal pylons, lit cubes/asteroids, letters mapped onto cube faces in perspective, shadows, particles
- `Games3D.kt`  **Letter Dash 3D** (3-lane space race that spells words: dodge rocks, shields, slow-mo, hearts, combos, speed choice, adaptive words from the child's weak words), **Letter Cube 3D** (drag/flick a real 3D cube; Explore + "find the letter" Challenge), animated 3D hero card
- `Studio.kt`   **Paint Studio**: finger painting, talking word stickers, day/night/paper scenes
- Home: daily gift chest (grows with streak), word of the day, 3D Zone hero, pulsing current map level, world headers, 👑 boss levels
- 12 new badges (Rocket Rider, Dash scores, Little Artist, Cube Master...)

## Testing the 3D logic without Android
`tools/Sim.kt` plays thousands of frames against `World3D.kt` + `Engine.kt` with simulated players and checks the 3D math:
```
kotlinc app/src/main/java/com/margaretm/app/Engine.kt app/src/main/java/com/margaretm/app/World3D.kt tools/Sim.kt -include-runtime -d sim.jar && java -jar sim.jar
```
