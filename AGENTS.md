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
- Screen list: `Dashboard`, `ExpenseList`, `AddTransaction`, `SansAI` (Financial Copilot & Universal Transaction Ingestion with HITL Confirmation), `Wealth`, `Portfolio` (Overview, Health, Yield), `Goals`, `Budgets` (Safe-to-Spend runway), `Installments` (Horizon timeline), `MonthlyReview`, `DataManagement` (Database Optimization & Audit), `WealthForecasting` (Monte Carlo Simulation), etc.
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

### Database Invariants & Maintenance
- **Epoch Sentinel (`date = 0`)**: Records with `date = 0` (or `date <= 0`) are intentional epoch opening balance adjustments created by the Re-Sync utility (`ReSyncDryRunViewModel`). They must **NEVER** be updated to `created_at` or current timestamps by maintenance scripts or migrations, as doing so distorts monthly cashflow pacing and safe-to-spend runway.
- **Orphaned Cross-References**: Junction tables (such as `expense_tag_ref`) may accumulate phantom pointers if referenced expenses are deleted. Database maintenance (`MaintainDatabaseUseCase`) safely cleans orphaned references where `expenseId NOT IN (SELECT id FROM expenses)` while preserving active tag associations.


## Cloud Sync, Backup & Disaster Recovery

- **Cloudflare R2 (SSOT)**: Pure Kotlin AWS SigV4 signed requests for S3-compatible cloud snapshot downloads and SQLite database backups.
- **Zero Data-Loss Safeguard**: `CloudStorageSyncer` strictly inspects database contents prior to upload and refuses to overwrite backups if the `expenses` count is 0.
- **Dual-Destination Archiving**: Every backup operation writes to both `db/sans_finance_latest.sqlite` and an immutable timestamped key `db/archive/sans_finance_yyyyMMdd_HHmmss.sqlite`.
- **In-App Cloud Restore**: Settings screen provides a one-tap `[Restore]` button that verifies SQLite integrity before replacing local files and cleanly restarting the app.
- **CLI Recovery Utility**: `scripts/restore_from_r2.sh` allows pulling and restoring verified snapshots directly to connected devices via ADB.
- Background sync and automated backups scheduled via Android `WorkManager` with exponential backoff retry policies.

## AI Integration Strategy

- **Cloud AI Only**: Support for **OpenAI** and OpenAI-compatible APIs (e.g., **OpenRouter**). Used strictly for high-value on-demand analysis:
  - Monthly Review closing summaries and Portfolio Health Insights.
  - Interactive **AI Chat & Smart Receipt Ingestion**: Parsing raw text receipts (such as CIMB Niaga SBN coupon payouts, bank slips, or expense notes) into structured transaction proposals.
  - **Human-in-the-Loop (HITL) Guarantee**: The AI never directly mutates the database; it presents an interactive confirmation proposal card allowing account/category review and explicit confirmation before persisting via `AddTransactionUseCase`.
- **SSE Streaming Architecture**:
  - **`AiProvider.streamChat()`** returns `Flow<StreamEvent>` (TextDelta / Done / Error) for real-time token-by-token rendering via Server-Sent Events.
  - **OpenRouter**: Uses `callbackFlow` + Okio `BufferedSource` SSE parsing with `stream: true`. A dedicated `streamingClient` with `readTimeout(0)` prevents timeout during long token pauses.
  - **OpenAI**: Falls back to non-streaming via the default `AiProvider.streamChat()` implementation (OpenAI `/v1/responses` endpoint does not support SSE streaming).
  - **Non-streaming retained**: `parseReceiptOrChat()` (blocking, `response_format: json_object`) remains for Monthly Review, Portfolio Analysis, and as a fallback.
  - **Prompt Caching**: The streaming system prompt is static (no `System.currentTimeMillis()` interpolation); dynamic context (timestamp, date) is injected as a separate user context turn to enable OpenRouter prefix caching.
  - **OkHttp**: Base client uses `pingInterval(20s)` for HTTP/2 PING keep-alive to defeat carrier CGNAT idle eviction on cellular networks.
- **Real-time Financial Grounding Snapshot**:
  - `AiChatViewModel` injects a zero-latency (~5ms) `FinancialContextSnapshot` into the dynamic context turn: Current Month totals, Previous Month (M-1) totals/top categories, 3-Month rolling baseline average expense, Top Big-Ticket discrete items (spike drivers), and liquid account balances.
  - This eliminates generic platitudes (e.g. telling user to unplug electricity appliances when the spike is actually rent) and ensures SansAI performs deep historical variance analysis.
- **Receipt Ingestion & Account Resolution Invariants**:
  - **Account Fallback**: If receipt text or user input does not match an identifiable account, the parser and ViewModel must default to the primary/first **Cash** account (e.g. `Wallet`). Never leave the account unassigned or pick an investment account.
  - **Dynamic Timestamp Prompting**: LLM system prompts must always inject dynamic current timestamps (`${System.currentTimeMillis()}`) rather than hardcoded dummy epoch values, preventing the LLM from backdating transactions into prior months. For streaming, timestamps are injected as a separate context turn rather than inline in the system prompt.
  - **Account Currency Inheritance**: Created expenses must inherit the parent account's native currency (defaulting to `IDR`), never falling back to legacy `USD`.
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
