# Repository Guidelines

## 🏛️ Ecosystem Role & Financial Data Contracts

Sans Finance is the **Single Source of Truth (SSOT) for Cash Accounts, Daily Expenses, and Budgeting** in the workstation's Personal Data Architecture ([`~/Projects/DATA_ARCHITECTURE.md`](file:///home/al/Projects/DATA_ARCHITECTURE.md)):

- **Primary Domain**: Daily cash flow pacing, expenses, accounts, budgets, debt/installment horizons, and Monte Carlo FIRE simulations (Android Room DB v38).
- **Export to `portfolio-integration`**: Cloudflare R2 database snapshots provide off-chain cash and P2P lending balances to `portfolio-integration` via `sansfinance-fetch` (portfolio holdings in the app originate upstream from KSEI/DeBank and are excluded from re-import to avoid double-counting).
- **Alignment with `ierp`**: High-level recurring commitments and normalized monthly burn inform `ierp commitments` for sovereign runway tracking.
- **Boundary**: Do NOT create ad-hoc scrapers for stock or crypto exchanges inside this repository; investment assets are handled upstream by `portfolio-integration`.

## Project Structure & Module Organization

Sans Finance is an Android project consisting of the following module:

- `:app` — Android application module (Jetpack Compose, Hilt, Room).

### Module Details

- **app**: 
    - Source code: `app/src/main/java/com/sans/finance`.
    - UI: Jetpack Compose under `presentation/`.
    - Data: Room database and Hilt DI.
    - Domain: Business logic and models under `domain/`.
- **scripts**: Utility scripts like `backup.sh`, `sync.sh`, `push_portfolio.sh`.
- **Makefile**: Common tasks for building and running.

## Build, Test, and Development Commands

You can use the `Makefile` for convenience:

- `make run` — build and run on device
- `make build` — build debug APK
- `make release` — build and package release APK
- `make test-unit` — run JVM unit tests
- `make test-android` — run instrumentation tests

Alternatively, use the Gradle wrapper:

- `./gradlew :app:assembleDebug` — build Android debug APK
- `./gradlew test` — run all tests

Min/target SDK is 36.

## Remote Android Debugging & Deployment (Tailscale + Wireless ADB)

To deploy and debug on physical Android devices remotely without a USB cable:

1. **Verify Tailscale Connection**:
   - Check device IP via `tailscale status` (e.g. `100.110.101.84 xiaomi-14t-pro`).
2. **Wireless Debugging & Pairing**:
   - On Android (Developer Options): Enable **Wireless debugging** (and **Install via USB** on Xiaomi/HyperOS).
   - Tap *Pair device with pairing code*.
   - Run `adb pair <TAILSCALE_IP>:<PAIRING_PORT> <6_DIGIT_CODE>`.
3. **ADB Connect & Port Discovery**:
   - Android uses a dynamic port for the ADB daemon (separate from the pairing port). Connect via `adb connect <TAILSCALE_IP>:<PORT>`.
4. **Build & Install**:
   - Build: `./gradlew :app:assembleDebug`
   - Install: `adb -s <TAILSCALE_IP>:<PORT> install -r app/build/outputs/apk/debug/app-debug.apk`

## High-Level Architecture

The project follows **Clean Architecture** with a Kotlin Multiplatform core.

### Layers (App)

**Domain** (`app/src/main/java/com/sans/finance/domain/`) — Pure Kotlin.
- `model/` — Core models: `Expense`, `Account`, `PortfolioHolding`, `Goal`, `Budget`, `DividendYieldSummary`, `DailySafeToSpend`, `InstallmentHorizonRoadmap`.
- `repository/` — Interfaces for data access.
- `usecase/` — Business logic:
  - `MonteCarloFireSimulator` — Stochastic geometric Brownian motion simulation (1,000 iterations, 10th/50th/90th percentile fan chart, FIRE probability score).
  - `GetDividendYieldSummaryUseCase` — Aggregated passive yield, weighted yield-on-cost, and lifestyle expense coverage.
  - `GetCashInjectionRebalanceUseCase` — Optimal capital allocation across underweight asset classes without triggering asset sales.
  - `GetCashFlowPacingUseCase` — Safe-to-spend daily discretionary allowance and billing cycle runway pacing.
  - `GetInstallmentHorizonUseCase` — Future installment commitments and debt payoff liberation matrix.
  - `GetEmergencyFundStressTestUseCase` — Emergency fund safety runway and stress scenario testing (Job loss, 50% pay cut, +25% cost shock, -30% market drawdown).
  - `GetSavingsRateVelocityUseCase` — Savings rate acceleration tracking, 3/6-month velocity averages, and momentum trends.
  - `MaintainDatabaseUseCase` — SQLite `VACUUM` defragmentation, `PRAGMA optimize`, `ANALYZE`, and orphaned tag cleanup.

**Data** (`app/src/main/java/com/sans/finance/data/`)
- `local/entity/` — Room entities (database version 37).
- `local/dao/` — Room DAOs with complex queries for analytics.
- `repository/` — Implementations mapping entities to domain models.

**Presentation** (`app/presentation/`) — Compose + ViewModel + Jetpack Glance.
- ViewModels use `StateFlow` to expose UI state.
- Screen list: `Dashboard`, `ExpenseList`, `AddTransaction`, `Wealth`, `Portfolio` (Overview, Health, Yield), `Goals`, `Budgets` (Safe-to-Spend runway), `Installments` (Horizon timeline), `MonthlyReview`, `DataManagement` (Database Optimization & Audit), `WealthForecasting` (Monte Carlo Simulation), etc.
- Navigation: Type-safe routes using Kotlinx Serialization in `Screen.kt` with fluid Material 3 enter/exit motion transitions.
- AppWidgets: Jetpack Glance-powered home screen widgets (`FinancialSummaryGlanceWidget`, `QuickAddGlanceWidget`) alongside legacy RemoteViews.

## Database

Room database is at **version 38**. It includes:
- Recurring expense projection with end conditions (`recurrence_end_type`, `recurrence_end_date`, `recurrence_total_occurrences`, `recurrence_interval_multiplier`, `recurrence_status`)
- Compound indices on `installment_items` (`due_date`, `status`) and `expenses` (`is_recurring`, `date`)
- Multi-currency valuation with historical FX rates (`fx_rates` table via `FxRateEntity`)
- Configurable tag visibility and ordering (`tags` table via `TagEntity`)
- Custom account ordering and liability classifications (`account_types` and `accounts`)
- Account alias mapping (`account_aliases`)
- Support for portfolio tracking, targets (`portfolio_targets`), goals, and budgets.
Reference snapshot: `sans_finance_db_snapshot.sqlite`.

## Cloud Sync & Backup

- **Cloudflare R2 (Default)**: Pure Kotlin AWS SigV4 signed requests for S3-compatible cloud snapshot downloads and SQLite database backups.
- **Google Cloud Storage (GCS, Optional)**: Service-account JWT authentication for snapshots and SQLite database backup uploads.
- Background sync and automated backups scheduled via Android `WorkManager` with exponential backoff retry policies.

## AI Integration Strategy

- **Cloud AI Only**: Support for **OpenAI** and OpenAI-compatible APIs (e.g., **OpenRouter**). Used strictly for high-value on-demand analysis (e.g. "Analyze with AI" in Monthly Review and Portfolio Health Insights).
- **No On-Device AI / LLM**: Do not implement or suggest on-device LLMs or on-device AI engines (such as LiteRT-LM / edge SLMs). They introduce excessive battery drain, thermal throttling, and large binary footprints with negligible user benefit for personal finance. All core calculations must remain pure deterministic Kotlin algorithms, while complex LLM summaries use cloud APIs.

## Coding Style

- Kotlin, JDK 17, 4-space indentation.
- Follow Clean Architecture patterns—keep business logic in Use Cases.
- Use `MutableStateFlow` in ViewModels for state management.
- Small, focused `@Composable` functions.

## Testing

- Unit tests: `app/src/test`.
- Instrumentation: `app/src/androidTest`.
- Test naming: `*Test.kt`.
