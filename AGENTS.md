# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

Android demo app showing biometric face registration and sign-in using the Ver-ID SDK (`com.appliedrec`). Users register a face under a name, then sign in via face identification. Single-module Gradle project (`app`), Kotlin-only, Jetpack Compose UI, Room database for persistence.

## Build & Run Commands

```sh
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.appliedrec.veridregistry.SomeTest"

# Run instrumented (on-device) tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Lint check
./gradlew lint
```

Requires Android SDK with compileSdk 36 and minSdk 26. JVM target is 11.

## Architecture

### Navigation

`MainActivity` hosts a single `NavHost` (Jetpack Navigation Compose) with these routes:
- `home` → `HomeView` (sign-in / register entry point)
- `register` → `RegistrationIntroView` (explains capture, starts camera)
- `registration_review` → `RegistrationReviewView` (shows captured face, enter name, save)
- `user/{userName}` → `UserView` (view/manage a user's registered faces)
- `users` → `UsersView` (list all registered users, accessed from Settings)
- `settings` → `SettingsView` (camera, spoof detection, threshold, reset)

### Data Layer

- **Room database** (`AppDatabase`, singleton via `AppDatabaseProvider`) with a single entity `TaggedFaceEntity` (table `tagged_faces`). Stores face template data as `FloatArray` (BLOB via `DatabaseTypeConverters`), user name, and date added.
- **`TaggedFaceDao`** — standard Room DAO exposing `Flow`-based queries.
- **`TaggedFaceRepository`** — wraps the DAO; also manages face image files on disk via `ImageUtils` (JPEG files stored in `{filesDir}/images/{templateId}.jpg`).

### ViewModels

- **`FaceSessionViewModel`** — central ViewModel scoped to the Activity. Manages face capture, registration, and identification workflows. Exposes `sessionState: StateFlow<FaceSessionState>` (sealed class: Idle, Capturing, Registering, IdentificationComplete, RegistrationComplete, RegistrationError, CaptureError) and `capturedFace` for the two-step registration flow.
- **`SettingsViewModel`** — reads/writes user preferences via DataStore (`useBackCamera`, `enableSpoofDetection`, `identificationThreshold`). Loads the SDK's default threshold asynchronously on init.
- **`UserCountViewModel`**, **`UsersViewModel`**, **`UserFacesViewModel`** — lightweight ViewModels that expose repository Flows as StateFlows for their respective screens.

### Ver-ID SDK Integration

All Ver-ID libraries are versioned via a BOM (`verid-bom` in `gradle/libs.versions.toml`). Key SDK components used:
- `FaceCapture` — camera-based face capture session
- `FaceDetectionRetinaFace` — face detection
- `FaceRecognitionR300` (cloud variant) — face template extraction and comparison
- `FaceTemplateRegistry` — manages registration/identification logic with configurable thresholds
- `SpoofDeviceDetection` (cloud) — liveness/spoof detection plugin

The R300 server URL and API key are set as manifest placeholders in `app/build.gradle.kts` and read at runtime via `<meta-data>` in `AndroidManifest.xml`.

### Registration Error Handling

`RegistrationErrorDialog` handles two specific SDK exceptions:
- `SimilarFaceAlreadyRegistered` — offers to add face to the existing user or save under the new name anyway
- `FaceDoesNotMatchExisting` — offers to force-save via `FaceSessionViewModel.forceInsert()`

## Key Patterns

- **Compose + ViewModel**: All screens follow the pattern of a `@Composable` that collects ViewModel StateFlows via `collectAsStateWithLifecycle()`, with a separate stateless `Content` composable for previews.
- **Activity-scoped ViewModel sharing**: `FaceSessionViewModel` is scoped to the Activity (`viewModel(activity)`) so it can be shared across navigation destinations during the registration flow.
- **Version catalog**: All dependency versions are managed in `gradle/libs.versions.toml`. Use `libs.` aliases in `build.gradle.kts`.
- **KSP for Room**: Room annotation processing uses KSP (not kapt). The `ksp` dependency configuration is used for `room-compiler`.
- **mavenLocal in repositories**: `settings.gradle.kts` includes `mavenLocal()` for resolving Ver-ID libraries during development.
