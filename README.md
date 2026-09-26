# Sans Finance 🏦

[![Release](https://img.shields.io/github/v/release/nichsedge/sansfinance?style=for-the-badge&logo=github&color=brightgreen)](https://github.com/nichsedge/sansfinance/releases/latest)
[![Android](https://img.shields.io/badge/Android-SDK%2036%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

Sans Finance is a modern, privacy-first personal finance and wealth management application for Android. Built with 100% deterministic mathematical modeling and Clean Architecture, it provides comprehensive financial intelligence—from daily cash flow pacing and debt horizon timelines to advanced Monte Carlo FIRE simulations, emergency runway stress testing, and optional Cloud AI analysis.

<img width="1376" height="3058" alt="Sans Finance Preview" src="https://github.com/user-attachments/assets/3fabaac4-d548-4f75-a6b2-9857826bda7a" />

## 📥 Download

[![Download Latest APK](https://img.shields.io/badge/Download-Release%20v1.4-brightgreen?style=for-the-badge&logo=android)](https://github.com/nichsedge/sansfinance/releases/latest)

Get the latest signed APK directly from [GitHub Releases](https://github.com/nichsedge/sansfinance/releases/latest).

---

## ✨ Features

### 📊 Wealth & Analytics Engine
- **Multi-Asset Portfolio Explorer:** Track Indonesian equities (KSEI), EVM DeFi (DeBank), CEX crypto (Binance), and Solana (Alchemy) with real-time multi-currency FX valuation, dynamic source filters, ticker search, sort options (Value, Gainers, Yield), expandable category accordions, allocation micro-progress bars, yield pills, interactive holding detail bottom sheets, **Real-Time Streaming AI Strategist** (auto-navigating to Health tab with live SSE token rendering & stop controls), and **In-App Integration Guide with Helper Icon** (two-tier guides for beginners: CSV/JSON format schema & manual file picker; and experts: automated `portfolio-integration` ETL setup & Cloudflare R2 pipeline sync).
- **Monte Carlo FIRE Simulation:** 1,000-run stochastic geometric Brownian motion simulator modeling market volatility, inflation, and sequence-of-returns risk with 10th/50th/90th percentile fan charts and FIRE probability scores.
- **Emergency Fund Runway Stress Testing:** Liquid safety buffer calculator simulating 4 dynamic shock scenarios: Job Loss, 50% Pay Cut & Essential Budget, +25% Cost Shock, and -30% Portfolio Drawdown.
- **Savings Rate & Net Worth Velocity:** Track real-time savings rate acceleration, 3/6-month velocity averages, and monthly accumulation momentum ($\Delta\text{NW}/\text{month}$) with interactive haptic gesture scrubbing.
- **Smart Cash-Injection Rebalancing:** Calculate optimal buy allocations for fresh deposits across underweight asset classes to achieve target allocations without triggering taxable asset sales.
- **Dividend & Cash Yield Tracker:** Dedicated portfolio yield analysis tracking annual passive run-rate, weighted yield-on-cost %, coupon schedules, and lifestyle expense coverage.

### ⚡ Daily Financial Operations
- **SansAI Financial Copilot & Universal Ingestion:** Real-time conversational AI grounded in live SQLite financial metrics (expenses, income, savings rate, liquid cash balances) and smart receipt parsing (CIMB Niaga SBN coupon payouts, BCA transfer slips, QRIS notes) with SSE token streaming and **Human-in-the-Loop (HITL)** proposal confirmation before persisting.
- **Safe-to-Spend Runway Pacing:** Real-time calculation of daily discretionary allowances after accounting for upcoming committed bills and billing cycle days.
- **Installment Horizon Roadmap:** Timeline matrix projecting future monthly debt obligation reductions and freed cash flow milestones.
- **Home Screen Widgets (Jetpack Glance):** Reactive Material 3 widgets for instant financial health glance and one-tap transaction logging.
- **Database Health & Maintenance Utility:** One-tap SQLite `VACUUM` defragmentation, `PRAGMA optimize`, `ANALYZE`, and orphaned tag reference cleanup.

### 🔒 Privacy & Architecture
- **100% Deterministic Core:** All calculations, simulations, valuations, and financial modeling run locally and offline on pure Kotlin algorithms.
- **Optional Cloud AI Connector:** Connect your own **OpenRouter** or **OpenAI** API key on demand for receipt ingestion, monthly closures, and portfolio health commentary.
- **Global Privacy Mode:** One-tap toggle to mask financial amounts and account balances across the entire app.

## 🛠 Tech Stack

- **Architecture:** Clean Architecture (Domain, Data, Presentation)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/compose) & Material 3
- **Widgets:** Jetpack Glance
- **Dependency Injection:** [Hilt](https://dagger.dev/hilt/)
- **Local Database:** [Room](https://developer.android.com/training/data-storage/room) (SQLite) — Schema Version 40
- **Optional AI:** Cloud AI connector (OpenAI / OpenRouter)
- **Data Persistence:** DataStore (Preferences) & Cloudflare R2 Backups (with Immutable Archives)

## 🚀 Project Structure

- `:app` — Android Application (including Domain & Data logic)
- `scripts/` — Database migration, ADB deployment, and Cloudflare R2 backup utilities (`pull_cloud_db.py`, `backup.sh`, `sync.sh`)

## 🚀 Getting Started

### Prerequisites

- Android device/emulator with **API level 36+**.
- [Android Studio Ladybug](https://developer.android.com/studio) or newer.
- JDK 21.

### Build and Install

1. Clone the repository:
   ```bash
   git clone https://github.com/nichsedge/sansfinance.git
   ```
2. Open the project in Android Studio.
3. Build and run the `app` module.

Alternatively, use the Makefile or build script:
```bash
make run            # Build and deploy debug APK directly to device
make release        # Build signed release APK
# or
./scripts/build_release.sh
```
The release APK will be available in the `release/` folder.

## 📄 License

This project is licensed under the MIT License.

---
Built with ❤️ by [nichsedge](https://github.com/nichsedge)
