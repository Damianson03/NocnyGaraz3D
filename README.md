# Nocny Garaż 3D — test build source

Native Android / OpenGL ES 2.0 prototype. No external runtime dependencies.

## Gameplay
- One player car, one night drag-strip scene.
- Garage with 6 upgrade categories, 5 levels each.
- Career mode: opponent gets stronger after each win; reward grows.
- Cash Run: opponent stays close to player's current performance; generous farming reward.
- Hold GAS before the start and keep RPM in the green launch window.
- Tap SHIFT while RPM is in the green shift window.
- Local save via SharedPreferences.

## Build
Requires Android SDK 36 and Android Gradle Plugin compatible Gradle.
Open in Android Studio and Build > Build APK(s), or run `./gradlew assembleDebug` once a Gradle wrapper is generated.
