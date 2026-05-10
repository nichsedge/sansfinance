# Sans Finance 🏦

Sans Finance is a premium, on-device AI-powered personal finance and wealth management application for Android. Designed with privacy and performance in mind, it provides a comprehensive dashboard to track your entire financial life—from daily expenses to long-term net worth.

![Sans Finance Banner](https://raw.githubusercontent.com/nichsedge/sans-finance/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

## ✨ Features

- **Comprehensive Dashboard:** Monitor your Net Worth, Total Assets, and Liabilities in a single, beautiful view.
- **Monthly Cash Flow:** Track income, expenses, and your monthly savings rate with interactive charts.
- **On-Device AI Insights:** Get personalized financial suggestions powered by **LiteRT-LM** (Google Edge AI), running entirely on your device for maximum privacy.
- **AI Receipt Scanning:** Instantly extract transaction details from photos or gallery images using local AI inference.
- **Wealth Distribution:** Visualize how your assets are distributed across different categories and accounts.
- **Installment & Debt Tracking:** Manage long-term payment plans and see your remaining balances clearly.
- **Recurring Expenses:** Automate your fixed costs and upcoming bills.
- **Advanced Statistics:** Analyze spending trends over 7D, 30D, Monthly, and Yearly timeframes.
- **Privacy First:** Your data never leaves your device. All processing is local, and backups are stored on your storage.

## 🛠 Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/) (JDK 17)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/compose)
- **Architecture:** Clean Architecture + MVVM
- **Dependency Injection:** [Hilt](https://dagger.dev/hilt/)
- **Local Database:** [Room](https://developer.android.com/training/data-storage/room) (SQLite)
- **On-Device AI:** [LiteRT-LM](https://ai.google.dev/edge/litert)
- **Data Persistence:** [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences)

## 🚀 Getting Started

### Prerequisites

- Android device/emulator with **API level 36+**.
- [Android Studio Ladybug](https://developer.android.com/studio) or newer.

### Build and Install

1. Clone the repository:
   ```bash
   git clone https://github.com/nichsedge/sans-finance.git
   ```
2. Open the project in Android Studio.
3. Build and run the `app` module.

Alternatively, use the provided build script:
```bash
./build_release.sh
```
The release APK will be available in the `release/` folder.

## 📄 License

This project is licensed under the MIT License.

---
Built with ❤️ by [nichsedge](https://github.com/nichsedge)
