# Money Tracker

Money Tracker is a modern Android personal finance app that helps you record expenses and income, track monthly budgets, visualize spending patterns, and manage your money — all offline, on your device.

---

## 📱 Overview

Money Tracker lets you:

- Add and edit expenses and income with categories, dates, times, and wallet types.
- Track total balance across Cash, Bank, and Credit Card wallets.
- Monitor monthly spending, remaining budget, and daily budget usage.
- View detailed statistics with charts (line, pie, and bar charts).
- Search, filter, and manage all transactions in one place.
- Customize currency and theme (Light, Dark, System).
- Backup and restore your data via CSV export/import.
- Reset all data when needed.

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|------------|
| **Language** | Kotlin 1.9.23 |
| **Build System** | Gradle with Kotlin DSL |
| **UI Framework** | Jetpack Compose (BOM 2024.05.00) |
| **Dependency Injection** | Dagger Hilt 2.50 |
| **Local Database** | Room 2.6.1 with KSP |
| **Preferences** | Jetpack DataStore |
| **Async Programming** | Kotlin Coroutines & Flow |
| **Charts** | MPAndroidChart 3.1.0 |
| **Min SDK** | 24 (Android 7.0) |
| **Target / Compile SDK** | 34 (Android 14) |
| **Java Version** | 17 |

---

## ✨ Features

### Dashboard
- Monthly navigation with previous/current/next month controls.
- Total balance card plus per-wallet summaries (Cash, Bank, Credit Card).
- Today spent, month spent, and remaining budget overview.
- Budget usage warnings at 80%, 90%, and 100% thresholds.
- Recent transactions list with quick edit access.
- Dynamic greeting in the top app bar.

### Add / Edit Transaction
- Toggle between Expense and Income.
- Amount input with currency formatting.
- Category dropdown for expenses (10 built-in categories).
- Optional description.
- Date and time pickers.
- Wallet selection: Cash, Bank, or Credit Card.
- Edit existing expenses by navigating from dashboard or transaction list.

### Transaction List
- Combined view of expenses and income.
- Search by description or category name.
- Filter by transaction type (Expenses / Income).
- Filter by category.
- Filter by month.
- Swipe-to-delete with undo snackbar.
- Tap an expense to edit it.

### Statistics
- Monthly summary cards: total income, total expenses, remaining balance, average daily spending, transaction count, highest spending day, and highest spending category.
- **Line chart**: daily spending trend.
- **Pie chart**: spending breakdown by category.
- **Bar chart**: weekly spending (Monday to Sunday).

### Settings
- Currency selection: USD, EUR, EGP, SAR, AED.
- Theme selection: Light, Dark, System.
- Export data to CSV.
- Import data from CSV.
- Reset all data with confirmation dialog.

---

## 🏗 Architecture

The project follows a clean architecture style with clear separation between UI, domain, and data layers.

```
com.moneytracker
├── data
│   ├── local          # Room database, entities, DAOs, and DataStore prefs
│   ├── mapper         # Entity ↔ Domain model mapping
│   └── repository     # Data sources exposed to the domain layer
├── di                 # Hilt modules (Database, Preferences)
├── domain
│   ├── model          # Domain models (Expense, Income, Category, Budget, etc.)
│   └── usecase        # Business logic (e.g., GetStatisticsUseCase)
├── ui
│   ├── components     # Reusable Compose UI components (BalanceCard, Charts, etc.)
│   ├── navigation     # Navigation graph and screen definitions
│   ├── screens        # Feature screens with their ViewModels
│   └── theme          # Material 3 color scheme, typography, and theme
├── MainActivity.kt
├── MainViewModel.kt
└── MoneyTrackerApplication.kt
```

### State Management
- Each screen has a dedicated Hilt `ViewModel`.
- UI state is exposed as `StateFlow` and collected in Compose using `collectAsState()`.
- One-time events (navigation, error toasts) are emitted via `SharedFlow`.

### Dependency Injection
- Hilt is used to inject repositories, DAOs, use cases, and preferences into ViewModels.

---

## 🗄 Database

Room database with the following tables:

| Table | Purpose |
|-------|---------|
| `expenses` | Stores expense records |
| `incomes` | Stores income records |
| `categories` | Predefined expense categories |
| `budget` | Stores monthly budget settings |

### Built-in Categories

1. Food
2. Coffee
3. Shopping
4. Transportation
5. Bills
6. Entertainment
7. Health
8. Education
9. Gifts
10. Other

Database is seeded on first launch with the default categories and an initial budget of `0.0`.

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34

### Build & Run

1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle files.
4. Run the `app` module on an emulator or physical device.

Or from the command line:

```bash
./gradlew :app:assembleDebug
```

### Run Tests

Unit tests and instrumentation tests can be run with:

```bash
./gradlew test
./gradlew connectedAndroidTest
```

> Currently, the project does not include test files, but the test infrastructure is configured in Gradle.

---

## 📲 Install the APK on an Android Device

After a successful build, the debug APK is generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

You have several ways to install it on a physical device or emulator.

### Option 1 — Install via Gradle (recommended, fastest)

This single command builds the debug APK (if needed) **and** installs it on every connected device:

```bash
./gradlew :app:installDebug
```

Gradle will invoke `adb install -r app-debug.apk` under the hood.

### Option 2 — Install via ADB manually

Useful when you want full control or the device isn't picked up by Gradle.

```bash
# 1. Verify your device is detected
adb devices
# Expected output:
#   1A2B3C4D5E6F   device        <-- ready
#   1A2B3C4D5E6F   unauthorized  <-- accept the popup on the phone
#   (no devices)    <-- check USB cable / USB debugging toggle

# 2. Install (use -s to target a specific device if you have more than one)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or for a specific serial
adb -s 1A2B3C4D5E6F install -r app/build/outputs/apk/debug/app-debug.apk
```

Flags explained:
- `-r` — reinstall, keeping existing data
- `-d` — allow downgrade (if versionCode is lower than the installed one)
- `-t` — allow test-only APKs

### Option 3 — Sideload by copying the APK to the phone

If you don't have ADB or are installing on someone else's device:

1. Copy the APK to the phone using one of:
   - **USB cable**: `adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/`
   - **Cloud / Email / Drive**: upload the APK, then download it on the phone
2. On the phone, open the file manager and tap the APK file.
3. Android will ask you to **enable "Install unknown apps"** for that file manager. Approve it.
4. Tap **Install**.

> ⚠️ If the phone refuses to install and you see *"App not installed"*, check that **"Install via USB"** is enabled in Developer options and that your current USB mode is **File Transfer / MTP** (not "Charging only").

### Option 4 — Install on a wireless / emulator device

For an emulator (or a phone connected over Wi-Fi via `adb pair` / `adb connect`):

```bash
# List all devices (including emulators)
adb devices

# The emulator usually appears as emulator-5554
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```

### 🔧 Troubleshooting ADB Device Authorization

If `adb devices` shows **`unauthorized`** or **`no permissions (missing udev rules)`**:

| Symptom | Cause | Fix |
|---|---|---|
| `unauthorized` | Phone hasn't accepted USB debugging for this PC | Tap **"Allow USB debugging?"** on the phone; tick "Always allow from this computer" |
| `no permissions (missing udev rules)` | Linux doesn't know how to talk to your phone's USB vendor | Install udev rules: `sudo apt install android-sdk-platform-tools-common` then re-plug. If still failing, add a custom rule for your vendor (e.g., `0x2d95` for Vivo) to `/etc/udev/rules.d/51-android.rules` and run `sudo udevadm control --reload-rules && sudo udevadm trigger` |
| `(no devices)` | USB debugging disabled, bad cable, or USB mode set to "Charging only" | Enable **Developer options → USB debugging** on the phone; use a data cable; pull down the notification shade and switch USB mode to **File Transfer (MTP)** |
| `offline` | Stale ADB session | `adb kill-server && adb start-server`, then re-plug the cable |

### 🚀 Launching the App After Install

Once installed, the app appears in the launcher as **Money Tracker** (`com.moneytracker`).

You can also launch it from the command line:

```bash
adb shell am start -n com.moneytracker/.MainActivity
```

To uninstall:

```bash
adb uninstall com.moneytracker
```

---

## 📦 Key Dependencies

```kotlin
// AndroidX Core
implementation(libs.androidx.core.ktx)
implementation(libs.androidx.appcompat)

// Jetpack Compose
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.ui)
implementation(libs.androidx.material3)
implementation(libs.androidx.activity.compose)
implementation(libs.androidx.navigation.compose)
implementation(libs.androidx.lifecycle.viewmodel.compose)

// Architecture & DI
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
implementation(libs.hilt.navigation.compose)

// Database
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)

// Preferences
implementation(libs.androidx.datastore.preferences)

// Coroutines
implementation(libs.kotlinx.coroutines.android)

// Charts
implementation(libs.mpandroidchart)

// Desugaring
coreLibraryDesugaring(libs.android.desugarJdkLibs)
```

---

## 🧩 Navigation

The app uses a single-activity architecture with Jetpack Navigation Compose.

| Screen | Route | Description |
|--------|-------|-------------|
| Dashboard | `dashboard` | Main overview and recent transactions |
| Add Expense | `add_expense` | Add a new expense or income |
| Edit Expense | `edit_expense/{expenseId}` | Edit an existing expense |
| Transactions | `expenses_list` | Full transaction list with filters |
| Statistics | `statistics` | Charts and monthly insights |
| Settings | `settings` | Currency, theme, backup, and reset |

The bottom navigation bar includes Dashboard, Statistics, and Settings. A floating action button opens the Add Expense screen.

---

## 🎨 Theme & Customization

- Material 3 design system.
- Custom light and dark color schemes.
- Dynamic color support on Android 12+.
- Theme selection persisted in DataStore.

---

## 📄 License

This project is provided as-is for educational and personal use.

---

## 🙋 Author

Developed by **MoneyTracker**.
