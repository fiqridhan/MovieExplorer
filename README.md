# MovieExplorer

MovieExplorer is a native movie discovery app powered by The Movie Database (TMDB). It lets viewers browse the official genre catalog, explore movies by genre, review movie details and community reviews, and play available YouTube trailers. The Android app uses Kotlin, Jetpack Compose, Material 3, Hilt, Coroutines, and StateFlow. Android is the active delivery target; iOS work is deferred until the next instruction. See `docs/REQUIREMENTS_CHECKLIST.md` for current coverage and limitations.

## API setup

Create a TMDB API Read Access Token and provide it locally. Android: copy `android/local.properties.example` to `android/local.properties` and set `TMDB_ACCESS_TOKEN`. iOS: copy `ios/Secrets.example.xcconfig` to `ios/Secrets.xcconfig` and set `TMDB_ACCESS_TOKEN`. Both destination files are ignored by Git. The token is bundled into each mobile binary, so a backend is required to keep it secret in production.

TMDB requires attribution for non-commercial use. See https://www.themoviedb.org.

## Projects

- `android/`: Android Studio Gradle project, Compose UI, StateFlow ViewModel, Hilt, domain/data modules.
- `ios/`: Xcode application target with UIKit views, presenters, interactors, and routers.

## Unit tests

- Android: `cd android && ./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :core:domain:testDebugUnitTest :core:data:testDebugUnitTest`
- Android UI: `cd android && ./gradlew :app:connectedDebugAndroidTest` with an emulator or device connected.
- iOS: run the `CineAtlas` scheme tests in Xcode or use `xcodebuild test` with an available iOS Simulator destination.

The latest Android validation passed 25 unit tests and four Compose UI tests on an Android 16 emulator. The Android debug build and lint passed. Runtime checks confirmed genre, discovery, detail, and review navigation with live TMDB data, an offline error and successful retry, phone and tablet layouts, landscape, large text, and embedded YouTube trailer playback. iOS was built and tested earlier but is not part of the current Android completion pass.

## Requirements

- Android Studio with Android SDK 35 and JDK 21.
- Xcode with an iOS Simulator (iOS 16 or newer target).
- TMDB API Read Access Token in the ignored local files described above.

## Screenshots

Runtime captures are stored in `docs/screenshots/`. They are evidence of the inspected states, not visual parity tests.

## Known limitations

- Android uses StateFlow and Hilt. Pagination is implemented manually with page guards and deduplication rather than Paging 3.
- iOS uses VIPER roles for discover and reviews, while detail and trailer still need their own presenters/interactors.
- Android UI automation covers navigation and scroll restoration; manual runtime checks cover phone, tablet, landscape, large text, offline retry, and trailer playback.
- iOS completion is deferred while Android is prioritized.
