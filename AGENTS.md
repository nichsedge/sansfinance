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

- `./gradlew assembleDebug` — build debug APK
- `./gradlew assembleRelease` — build an optimized, minified release APK (using debug signing)
- `./build_release.sh` — clean build and copy the release APK to `./release/`
- `./gradlew testDebugUnitTest` — run JVM unit tests
- `./gradlew connectedDebugAndroidTest` — run instrumentation tests on a device/emulator
- `./gradlew tasks` — list available tasks

Min/target SDK is 36; ensure your emulator/device matches.

## High-Level Architecture

This is a single-module Android app (`app/`) following **Clean Architecture** with MVVM presentation. Source lives under `app/src/main/java/com/sans/finance/`.

### Layers

**Domain** (`domain/`) — pure Kotlin, no Android dependencies.
- `model/` — core data classes: `Expense`, `Installment`, `InstallmentItem`
- `repository/` — interfaces: `ExpenseRepository`, `InstallmentRepository`
- `preferences/` — interfaces: `BudgetPreferences`
- `usecase/` — one use case per operation (e.g. `AddExpenseUseCase`, `GetExpensesUseCase`)

**Data** (`data/`) — implements domain interfaces.
- `local/entity/` — Room entities mapping to DB tables
- `local/dao/` — DAOs; `ExpenseDao` has complex UNION queries combining regular expenses and installment items for budget aggregations and chart data
- `repository/` — `ExpenseRepositoryImpl`, `InstallmentRepositoryImpl`; maps entities→domain models, handles tag syncing
- `preferences/` — DataStore-backed `BudgetPreferencesImpl`
- `util/` — `CsvParser`/`CsvExporter` for import/export, `LocaleManager` for runtime locale switching

**Presentation** (`presentation/`) — Compose + ViewModel.
- Each screen has a colocated `*Screen.kt` + `*ViewModel.kt`
- Screens: `ExpenseList`, `AddExpense`, `EditExpense`, `Installments`, `Stats`, `Settings`, `RecurringExpenses`
- `navigation/Screen.kt` — type-safe nav destinations using Kotlinx Serialization
- `components/` — shared composables (e.g. `CategoryIcon`)

**DI** (`di/AppModule.kt`) — Hilt `@Module` providing Room database, DAOs, repository implementations, and preferences as application-scoped singletons.

**Core utilities** (`core/util/`) — `CalendarUtils` (thread-safe calendar cloning), `CurrencyFormatter` (Rupiah), `DateFormatterUtils`.

**Theme** (`ui/theme/`) — Material 3 with custom dark/light color scheme (DeepViolet/Mint/Rose palette).

### Data Flow

```
Compose Screen → ViewModel (StateFlow) → Use Case → Repository interface
                                                          ↓
                                              Room DAOs / DataStore
```

ViewModels use `MutableStateFlow` for state; repositories return `Flow<T>`. Complex filter combinations in `ExpenseListViewModel` use `flatMapLatest()` over 7 parameters; `StatsViewModel` aggregates across multiple time ranges.

### Database

Room database at version 7 with migrations from 5→6 (tag system) and 6→7 (category/tag ordering). A `RoomDatabase.Callback` seeds default categories on first create; CSV seed data is injected if the database is empty. Do not edit `sans_finance_db_snapshot.sqlite` in-place (it is a reference snapshot).

## Coding Style & Naming Conventions

- Kotlin, JDK 17, 4-space indentation.
- Packages follow `com.sans.finance.<layer>` — keep new code in the correct layer.
- Compose: small, focused `@Composable` functions with descriptive names (e.g. `SpendingTrendChart`).
- No repo-level formatter/linter is configured; keep changes consistent with nearby code.

## Testing Guidelines

- Unit tests use JUnit (see `app/src/test/...`).
- Instrumentation tests use AndroidX Test + Espresso (see `app/src/androidTest/...`).
- Test class naming: `*Test.kt`.

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

## Debugging Library APIs

When library docs are unclear, inspect cached Gradle artifacts:

```bash
# Find artifact under ~/.gradle/caches/modules-2/files-2.1/<group>/<name>/<version>/
jar tf <artifact>.aar           # list contents
jar xf <artifact>.aar classes.jar
javap -classpath /tmp/classes.jar <package.ClassName> | head -n 80
```

Useful for third-party chart APIs (e.g. Vico layers).
