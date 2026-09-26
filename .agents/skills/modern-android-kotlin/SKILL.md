---
name: modern-android-kotlin
description: >-
  Comprehensive guide, patterns, and runbooks for modern Android (API 36+), Kotlin 2.4+, Java 21,
  Jetpack Compose, Hilt, Room, and Gradle 9 in Sans Finance. Use this skill whenever building,
  refactoring, compiling, debugging, or deploying Android features in this workspace.
---

# Modern Android & Kotlin Development (Sans Finance)

This skill provides the architectural conventions, modern API standards, and verification workflows for Sans Finance.

---

## ⚡ Core Philosophy: Bleeding Edge & Zero Deprecations

1. **Zero Deprecation Tolerance (`@Suppress("DEPRECATION")` Banned)**:
   - Do NOT suppress deprecation warnings.
   - Always migrate directly to the modern non-deprecated replacement.
2. **No Backward Compatibility Shims**:
   - `minSdk` is **36** (Android 16), `targetSdk` is **37**.
   - NEVER add version guards like `if (Build.VERSION.SDK_INT >= UPSIDE_DOWN_CAKE)`. Call the newest platform API directly.
3. **Java 21 JVM & Language Target**:
   - Bytecode and compilation targets are pinned to **Java 21** (`JavaVersion.VERSION_21`, `JvmTarget.JVM_21`).
   - Do not target JDK 22–27 as Android D8 / ART does not support bytecode format > 65.

---

## 🛠️ Modern API Cheatsheet & Migration Map

| Legacy / Deprecated Pattern | Modern Standard (Must Use) | Reason / Context |
| :--- | :--- | :--- |
| `LocalClipboardManager.current` | `LocalClipboard.current` + `clipboard.getClipEntry()?.clipData` | Non-blocking async clipboard access inside coroutine scope. |
| `androidx.hilt.navigation.compose.hiltViewModel` | `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` | Directly lifecycle-bound to Compose navigation backstack. |
| `Locale("id", "ID")` | `Locale.of("id", "ID")` | Standard Java 19+ / 21 static factory method. |
| `startActivityAndCollapse(intent)` | `startActivityAndCollapse(pendingIntent)` | Direct PendingIntent invocation on API 34+. |
| `price!!` on smart-casted types | Direct `price` | Remove redundant non-null assertions. |
| Hardcoded string dependencies | Version catalog (`libs.<group>.<artifact>`) | Single Source of Truth in `gradle/libs.versions.toml`. |

---

## 📋 Common Development Workflows

### 1. Verification & Compilation
Always verify changes using Gradle:
```bash
# Rapid Kotlin compilation check
./gradlew compileDebugKotlin

# Full debug build & APK assembly
./gradlew assembleDebug

# Run JVM Unit Tests
./gradlew testDebugUnitTest
# or via Makefile:
make test-unit
```

### 2. Device Deployment (USB & Wireless ADB)
- **Direct USB (Standard)**:
  ```bash
  # Check connected USB devices
  adb devices

  # Build and install directly
  ./gradlew :app:assembleDebug
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  # Or via Makefile:
  make run
  ```
- **Remote Wireless ADB (Tailscale)**:
  1. Verify device on Tailscale: `tailscale status` (e.g. `100.110.101.84 xiaomi-14t-pro`).
  2. Pair device (if needed): `adb pair <TAILSCALE_IP>:<PAIRING_PORT> <6_DIGIT_CODE>`.
  3. Connect to dynamic ADB daemon port: `adb connect <TAILSCALE_IP>:<DAEMON_PORT>`.
  4. Install:
     ```bash
     adb -s <TAILSCALE_IP>:<DAEMON_PORT> install -r app/build/outputs/apk/debug/app-debug.apk
     ```

### 3. Release & Version Bump
Follow `.agents/workflows/release-inc.md`:
1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Run `make release`.
3. Publish release via GitHub CLI: `gh release create v<VERSION> release/sans-finance-release.apk`.

---

## 🏛️ Invariants & SSOT Contracts
- **Epoch Sentinel (`date = 0`)**: Re-sync opening balance records use `date = 0`. NEVER overwrite them with current timestamps.
- **Orphaned Cross-References**: Junction tables (like `expense_tag_ref`) must reference existing `expenses`.
- **Human-in-the-Loop (HITL) for AI**: SansAI never directly mutates the database without user confirmation via `AiProposalCard`.
- **Currency Inheritance**: Transactions must inherit the parent account currency, defaulting to `IDR`.
