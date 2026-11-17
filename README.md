# MovieExplorer

MovieExplorer is an Android movie discovery app powered by The Movie Database (TMDB). Browse the official movie genres, discover films by genre, inspect movie details and community reviews, and watch available YouTube trailers in the app.

Built with Kotlin, Jetpack Compose, Material 3, Hilt, Coroutines, and StateFlow. The app uses a layered domain and data architecture, guarded page-based scrolling, adaptive poster grids, and explicit loading, empty, and error states.

## API setup

Create a TMDB API Read Access Token, copy `android/local.properties.example` to `android/local.properties`, and set `TMDB_ACCESS_TOKEN`. The local properties file is ignored by Git. The token is bundled into the Android binary, so a backend is required to keep it secret in production.

TMDB requires attribution for non-commercial use. See https://www.themoviedb.org.

## Build and tests

- Build and unit tests: `cd android && ./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :core:domain:testDebugUnitTest :core:data:testDebugUnitTest`
- Compose UI tests: `cd android && ./gradlew :app:connectedDebugAndroidTest` with an emulator or device connected.

The latest full Android validation passed 25 unit tests and four Compose UI tests on an Android 16 emulator. The debug build and lint passed. Runtime checks confirmed genre, discovery, details, and review navigation with live TMDB data, offline retry, adaptive phone and tablet layouts, landscape, large text, and YouTube trailer playback.

## Requirements

- Android Studio with Android SDK 35 and JDK 21.
- TMDB API Read Access Token configured in the ignored local properties file.

## Known limitations

- Android uses StateFlow and Hilt. Pagination is implemented manually with page guards and deduplication rather than Paging 3.
- Android UI automation covers navigation and scroll restoration; manual runtime checks cover phone, tablet, landscape, large text, offline retry, and trailer playback.
