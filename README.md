# Sans Finance 🏦

Sans Finance is a premium, AI-powered personal finance and wealth management application. It provides a comprehensive dashboard to track your entire financial life—from daily expenses to long-term net worth, including investments and portfolio tracking.

<img width="1376" height="3058" alt="Gemini_Generated_Image_mbod72mbod72mbod" src="https://github.com/user-attachments/assets/3fabaac4-d548-4f75-a6b2-9857826bda7a" />

## 📥 Download

[![Download Latest APK](https://img.shields.io/badge/Download-Release%20v1.2-brightgreen?style=for-the-badge&logo=android)](https://github.com/nichsedge/sansfinance/releases/tag/v1.2)

Get the latest release directly from [GitHub Releases](https://github.com/nichsedge/sansfinance/releases/latest).

## ✨ Features

- **Comprehensive Dashboard:** Monitor your Net Worth, Total Assets, and Liabilities in a single, beautiful view.
- **Wealth & Portfolio Tracking:** Track your investments across different accounts and visualize your wealth distribution.
- **Monthly Cash Flow:** Track income, expenses, and your monthly savings rate with interactive charts.
- **AI-Powered Insights**: 
    - **Cloud AI**: Advanced monthly reviews and transaction analysis via **OpenAI** or **OpenRouter**.
    - **Local AI (Planned)**: Privacy-focused on-device suggestions (Coming Soon).
- **Budgets & Goals:** Set financial goals and track your progress with dedicated budget management.
- **Installment & Debt tracking:** Manage long-term payment plans and use the **Debt Strategist** to optimize repayments.
- **Advanced Statistics:** Analyze spending and account trends over multiple timeframes.
- **Privacy First:** Core data processing is local, ensuring your financial privacy.

## 🛠 Tech Stack

- **Architecture:** Kotlin Multiplatform (KMP) + Clean Architecture
- **Mobile UI:** [Jetpack Compose](https://developer.android.com/compose)
- **Backend:** [Ktor](https://ktor.io/)
- **Dependency Injection:** [Hilt](https://dagger.dev/hilt/) (Android)
- **Local Database:** [Room](https://developer.android.com/training/data-storage/room) (SQLite) - Version 31
- **AI:** OpenAI & OpenAI-compatible APIs (OpenRouter); Local AI (Planned)
- **Data Persistence:** DataStore (Preferences)

## 🚀 Project Structure

- `:app` — Android Application
- `:shared` — Common Domain & Data Logic (KMP)
- `:server` — Companion Ktor Server

## 🚀 Getting Started

### Prerequisites

- Android device/emulator with **API level 36+**.
- [Android Studio Ladybug](https://developer.android.com/studio) or newer.
- JDK 17.

### Build and Install

1. Clone the repository:
   ```bash
   git clone https://github.com/nichsedge/sansfinance.git
   ```
2. Open the project in Android Studio.
3. Build and run the `app` module.

Alternatively, use the Makefile or build script:
```bash
make release
# or
./scripts/build_release.sh
```
The release APK will be available in the `release/` folder.

## 📄 License

This project is licensed under the MIT License.

---
Built with ❤️ by [nichsedge](https://github.com/nichsedge)
