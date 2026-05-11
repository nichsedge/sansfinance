# Repository Guidelines

## Project Structure & Module Organization

- `app/` is the single Android application module.
- Source code lives in `app/src/main/java/com/sans/finance`.
- UI and resources are in `app/src/main/res` (Compose UI lives in Kotlin under `presentation/`).
- Unit tests are in `app/src/test/java/com/sans/finance`.
- Instrumentation tests are in `app/src/androidTest/java/com/sans/finance`.
- Utility scripts at repo root: `backup.sh`, `restore.sh`, `sync.sh`, `push_portfolio.sh`.
- A sample database snapshot exists at `sans_finance_db_snapshot.sqlite` (do not edit in-place).

## Build, Test, and Development Commands

Use the Gradle wrapper from the repo root:

- `./gradlew assembleDebug` builds a debug APK.
- `./gradlew assembleRelease` builds an optimized, minified release APK (using debug signing).
- `./build_release.sh` clean builds and copies the release APK to `./release/`.
- `./gradlew testDebugUnitTest` runs JVM unit tests.
- `./gradlew connectedDebugAndroidTest` runs instrumentation tests on a device/emulator.
- `./gradlew tasks` lists available tasks.

## Coding Style & Naming Conventions

- Language: Kotlin (JDK 17). Use 4-space indentation and Kotlin standard style.
- Packages follow `com.sans.finance.<layer>` conventions.
- Compose: prefer small, focused `@Composable` functions with clear names like `SpendingTrendChart`.
- No repo-level formatter/linter is configured; keep changes consistent with nearby code.

## Testing Guidelines

- Unit tests use JUnit (see `app/src/test/...`).
- Instrumentation tests use AndroidX Test + Espresso (see `app/src/androidTest/...`).
- Test class naming: `*Test.kt` (examples are in the test folders above).

## Commit & Pull Request Guidelines

- Commit messages follow a lightweight conventional style: `feat: ...`, `refactor: ...`, `fix: ...`.
- PRs should include:
    - A short summary of changes.
    - Steps to test (commands or manual flows).
    - Screenshots or screen recordings for UI changes.
    - Linked issues if applicable.

## Configuration Notes

- `local.properties` may be required for Android SDK paths.
- Min/target SDK are set to 36; ensure your emulator/device matches.

## Debugging Tips (Jar Inspection)

When library docs are unclear, you can inspect cached Gradle artifacts locally:

- Locate the artifact under `~/.gradle/caches/modules-2/files-2.1/<group>/<name>/<version>/`.
- Extract classes: `jar tf <artifact>.aar` (or `.jar`), then `jar xf <artifact>.aar classes.jar`.
- Inspect APIs: `javap -classpath /tmp/classes.jar <package.ClassName> | head -n 80`.
  This is useful for understanding third-party APIs (e.g., chart layers in Vico).
