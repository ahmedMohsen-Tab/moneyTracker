# Kotlin for the C++ Engineer — A Guided Tour of the MoneyTracker Codebase

> **Audience:** a senior C++ engineer (C++17/20, modern OOP, system architecture).
> **Goal:** become productive in Kotlin by reading one well-structured Android app end-to-end.
> **Repo under study:** `moneyTracker` — a personal-finance Android app, Kotlin 1.9.23, JVM 17, Jetpack Compose, Hilt, Room, Coroutines/Flow, DataStore.

The tutorial is organized in the order you should read the source files:

1. The mental model (Kotlin vs C++)
2. Project & build system (Gradle vs CMake/Meson)
3. Module layout
4. Boot path — `MoneyTrackerApplication`, `MainActivity`, `MainViewModel`
5. Type system tour — `Transaction`, `RecurrenceRule`, `Wallet`, `Category`, `Budget`
6. Persistence — entities, DAOs, `AppDatabase`, migrations
7. Mappers — `EntityMapper.kt`
8. Repositories
9. Use cases — the "domain" layer (`CalculateWalletBalancesUseCase`, `GetDashboardSummaryUseCase`, `GetStatisticsUseCase`)
10. Preferences — `UserPreferences` / DataStore
11. ViewModels — `DashboardViewModel`, `AddExpenseViewModel`
12. UI — `DashboardScreen`, `MoneyTrackerNavigation`, `Theme.kt`
13. Testing — `CalculateWalletBalancesUseCaseTest`, `RecurrenceRuleTest`
14. Idiomatic Kotlin cheat sheet (with C++ parallels)
15. Where to go next

---

## 1. The Mental Model: Kotlin vs Modern C++

Before reading code, internalize the deltas. Everything below is justified somewhere in this codebase.

| Concept | C++ | Kotlin |
|---|---|---|
| Memory model | Manual `new`/`delete`, RAII, smart pointers (`unique_ptr`, `shared_ptr`) | Garbage-collected JVM heap. No `delete`, no destructors. `close()`/`AutoCloseable` for resources. |
| Default mutability | Mutable unless `const` | `val` (immutable) vs `var` (mutable). Prefer `val`. |
| Headers / modules | `#include` / `module foo;` | `package` declaration + `import` statements. One file = one top-level class typically. |
| Nullability | `T*` (maybe null) vs `T&` (never null), `std::optional<T>` | First-class `T?` (nullable) vs `T` (non-null). Enforced by the type system. **No `NullPointerException` unless you opt in with `!!`.** |
| Inheritance | Multiple inheritance, virtual base classes | Single class inheritance, multiple **interface** implementation. `sealed class` for closed hierarchies (think `std::variant` with exhaustiveness). |
| `interface` | Pure-virtual class | Same idea, but can have default implementations (no C++ equivalent until C++26 contracts/auto rules) and `val` properties. |
| `enum class` | `enum` (unscoped) / `enum class` (scoped) | Scoped by default. Can have data, methods, and implement interfaces. Much closer to Rust's enums. |
| Generics | Templates, instantiated per type, duck-typed | **Reified** at runtime via type erasure; checked at compile time. No specialization trickery. |
| Concurrency | `std::thread`, `std::async`, `std::mutex`, atomics, condition vars | **Structured concurrency** via coroutines (`launch`, `async`, `Flow`). No manual thread management in app code. |
| Compile model | Single-pass-ish; heavy template instantiation cost | Multi-pass; KSP/annotation processors generate code at compile time (Room, Hilt). |
| Build | CMake/Meson + Ninja | **Gradle** with Kotlin DSL (`build.gradle.kts`). Multi-module, incremental, daemonized. |
| Preprocessor | `#define`, `#ifdef` | None. Conditional compilation via compiler flags or `expect`/`actual` in KMP. |
| Function pointers / lambdas | Function objects, `std::function` | First-class lambdas, function references (`::foo`), `Function0`..`Function22`. |
| Properties | You write getters/setters manually | `val`/`var` with auto-generated getter (and setter for `var`). Can be overridden with custom get/set. |
| Constructors | Multiple constructors, member-initializer lists | One primary constructor in the class header, plus `init {}` blocks. Secondary constructors via `constructor(...)`. |
| Standard library | STL + your choice | Kotlin stdlib + `kotlinx.coroutines` + AndroidX + JVM stdlib (`java.time`, `java.util`, ...). |

**The single biggest paradigm shift** for a C++ engineer is: there is no value-vs-reference semantics question for object identity in your head anymore. A non-primitive variable is a reference, but **immutability** is the lever for safety (the equivalent of `const`). Coroutines are the lever for concurrency.

---

## 2. Project & Build System — Gradle vs CMake/Meson

### 2.1 Top-level layout

```
moneyTracker/
├── build.gradle.kts          ← root build script (analogous to top-level CMakeLists.txt)
├── settings.gradle.kts       ← project includes + plugin repositories
├── gradle.properties         ← JVM args, AndroidX flags
├── gradle/
│   ├── libs.versions.toml    ← central version catalog (CMake has no analog)
│   └── wrapper/              ← gradle-wrapper.jar + properties (pinned Gradle version)
├── gradlew, gradlew.bat      ← wrapper scripts (no global Gradle install needed)
└── app/                      ← the one Android "module" today
    ├── build.gradle.kts      ← module build script (analogous to a CMakeLists.txt for one lib)
    └── src/
        ├── main/             ← production code
        ├── test/             ← JVM unit tests (JUnit4 + MockK + Turbine)
        └── androidTest/      ← instrumented tests (Espresso)
```

### 2.2 `settings.gradle.kts` — the project root config

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MoneyTracker"
include(":app")
```

**C++ parallel:** This is roughly your top-level `CMakeLists.txt`'s `project()` + `add_subdirectory(app)`. Three things to notice:

1. **`include(":app")`** = `add_subdirectory(app)`. Each Kotlin/Gradle "module" lives in its own subdir and has its own `build.gradle.kts`.
2. **Repositories** are where artifacts come from. `google()` is the Android-specific maven repo, `mavenCentral()` is Maven Central, `jitpack.io` is for GitHub-built libs (used here for MPAndroidChart).
3. **`FAIL_ON_PROJECT_REPOS`** is a hygiene flag — it forbids modules from declaring their own repositories (forces central control, like requiring all C++ deps to go through `vcpkg`/`conan`).

### 2.3 `gradle/libs.versions.toml` — the version catalog

CMake has no analog. This is a single source of truth for every dependency version:

```toml
[versions]
kotlin = "1.9.23"
ksp = "1.9.23-1.0.20"
room = "2.6.1"
hilt = "2.50"
...

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
...

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
jetbrains-kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
devtools-ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- **`[versions]`** — coordinates only (version strings).
- **`[libraries]`** — Maven coordinates (group/name) bound to a version.
- **`[plugins]`** — Gradle plugin coordinates.

The benefit: a single bump of `kotlin = "1.9.24"` propagates everywhere. C++ equivalents: CMake `find_package` arguments, vcpkg manifest, `conanfile.txt`.

### 2.4 `build.gradle.kts` (root) — plugin classpath only

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
}
```

**`apply false`** means "declare the plugin on the classpath, but don't apply it here." Each subproject re-applies it. Analogous to `find_package(... REQUIRED)` at the top of a CMake project without `target_link_libraries`.

### 2.5 `app/build.gradle.kts` — module-level config

This is where the heavy lifting happens. Key sections:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.moneytracker"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.moneytracker"
        minSdk = 24           // Android 7.0
        targetSdk = 34        // Android 14
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes { release { isMinifyEnabled = false ; proguardFiles(...) } }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true   // backport java.time to old Android
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.13" }
}
```

**C++ parallel:**

| Gradle DSL | CMake/Meson |
|---|---|
| `compileSdk`, `minSdk`, `targetSdk` | C++ standard level + minimum platform version |
| `sourceCompatibility = VERSION_17` | `set(CMAKE_CXX_STANDARD 17)` |
| `jvmTarget = "17"` | bytecode target; closest analog is `-target` for clang |
| `isMinifyEnabled = false` (release) | Stripping / `-Os` optimization toggle |
| `compose = true` | enabling a feature flag — like `add_definitions(-DENABLE_FOO)` |
| `buildTypes { release { ... } }` | Build-type-dependent flags (Debug/Release) |
| `packaging { resources { excludes += ... } }` | like setting linker exclude lists |

```kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
    ksp(libs.androidx.room.compiler)        // <-- annotation processor
    coreLibraryDesugaring(libs.android.desugarJdkLibs)
    testImplementation(libs.junit)
    ...
}
```

- **`implementation`** ≈ `target_link_libraries` — brings a lib into compile + runtime. **Not transitive** to consumers.
- **`ksp`** ≈ compile-time annotation processor (Room/Hilt code generation).
- **`testImplementation`** ≈ `target_link_libraries(... PRIVATE)` for the test binary only.
- **`coreLibraryDesugaring`** = a backport shim — modern C++ libraries being made to work on older "platforms."

### 2.6 Build execution

```bash
./gradlew assembleDebug          # like `ninja -C build app-debug.apk`
./gradlew test                   # like `ninja test`
./gradlew installDebug           # like deploying to a device
./gradlew :app:dependencies      # like `cmake --trace-expand` for deps
```

`gradlew` is the wrapper — it downloads the pinned Gradle version on first run, just like `cmake -P` script bootstrapping in vendor trees.

---

## 3. Module Layout (Clean Architecture, one Gradle module)

There is only one Gradle module today (`:app`), but the package layout enforces layered architecture:

```
com.moneytracker
├── data
│   ├── local
│   │   ├── AppDatabase.kt              ← Room DB holder + Callback (seeding)
│   │   ├── dao/                        ← 5 DAO interfaces
│   │   ├── entity/                     ← 6 Room @Entity data classes
│   │   └── preferences/UserPreferences.kt
│   ├── mapper/EntityMapper.kt          ← entity ↔ domain two-way
│   ├── repository/                     ← 6 repositories wrapping DAOs
│   └── backup/                         ← CSV codec
├── di
│   ├── DatabaseModule.kt               ← Hilt @Module: provides DB + DAOs
│   └── PreferencesModule.kt            ← Hilt @Module: provides UserPreferences
├── domain
│   ├── model/                          ← pure Kotlin models (no Android imports)
│   ├── usecase/                        ← business logic classes
│   └── format/MoneyFormatter.kt        ← shared formatters
├── notifications/                      ← WorkManager workers + schedulers
├── ui
│   ├── components/                     ← reusable Compose widgets
│   ├── navigation/                     ← NavHost + Screen routes
│   ├── screens/                        ← one folder per feature
│   │   ├── dashboard/
│   │   ├── addexpense/
│   │   ├── expenseslist/
│   │   ├── settings/
│   │   └── statistics/
│   ├── theme/                          ← Material 3 colors, type, theme
│   └── util/UiText.kt
├── util/LocaleHelper.kt
├── MainActivity.kt
├── MainViewModel.kt
└── MoneyTrackerApplication.kt
```

**Reading rule:** `ui` → `domain` → `data`. Never `data` → `ui` directly. Repositories bridge them. The mapper file is the firewall.

---

## 4. The Boot Path — Three Files to Read First

These three files together define the entire app lifecycle.

### 4.1 `MoneyTrackerApplication.kt`

```kotlin
@HiltAndroidApp
class MoneyTrackerApplication : Application(), Configuration.Provider {

    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var dailySummaryScheduler: DailySummaryScheduler
    @Inject lateinit var recurringTransactionsScheduler: RecurringTransactionsScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Best-effort locale warm-up...
        appScope.launch {
            val saved = userPreferences.language.first()
            LocaleHelper.apply(saved)
        }
        appScope.launch {
            val enabled = userPreferences.dailySummaryEnabled.firstOrNull() ?: return@launch
            if (enabled) dailySummaryScheduler.enable()
        }
        recurringTransactionsScheduler.enable()
    }
}
```

**Syntax breakdown (line-by-line for the C++ engineer):**

- **`@HiltAndroidApp`** — annotation (= C++ attribute / `[[deprecated]]`). It tells Hilt to generate the application-level DI container. **No analog in C++** — closest thing is `main()` calling a builder.
- **`class MoneyTrackerApplication : Application(), Configuration.Provider`** — Kotlin single-inheritance + multiple interfaces. `: Application()` calls the parent constructor. `Configuration.Provider` is an interface (think `virtual class IWorkProvider`); implementing it requires `override val workManagerConfiguration`.
- **`@Inject lateinit var userPreferences: UserPreferences`** — `@Inject` is Hilt saying "ask the DI container to fill this field at construction." `lateinit var` is a Kotlin specialty: a `var` whose non-null type is **promised** to be initialized before first use (the compiler can't prove it). Think `T* userPreferences = nullptr; /* set in onCreate */` — but you must guarantee it's set, or you crash. (`@Inject` is initialized by Hilt before `onCreate`, so this is safe.)
- **`private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)`** — Read this as: *a coroutine context that runs on the IO thread pool, where one failed child doesn't cancel siblings.* This is **`std::thread` plus a fault-tolerant task scheduler** in one line. There is **no equivalent in C++ standard library** — you'd hand-roll a thread pool with `try`/`catch` wrappers.
  - `SupervisorJob()` is a job whose failure doesn't propagate to siblings. Closest analog: launching detached threads where one crashing doesn't kill others.
  - `Dispatchers.IO` selects the IO-optimized thread pool. `Main`, `Default`, `IO`, `Unconfined` are the standard dispatchers.
- **`override val workManagerConfiguration: Configuration`** — `val` = read-only property with auto-generated getter. `override` required because `Configuration.Provider` declared it abstract. The `get() = ...` block makes this a **computed property** — there's no backing field.
- **`appScope.launch { ... }`** — `launch` is `std::async` with fire-and-forget semantics. The block is a **suspend lambda**; `first()`, `firstOrNull()`, `emit()` inside are `suspend` calls. The compiler instruments them so the coroutine can pause/resume on a thread pool.
- **`return@launch`** — non-local return from the lambda (analogous to `goto` out of a lambda — C++ doesn't have this; closest analog would be throwing an exception, which is exactly the wrong pattern here).

### 4.2 `MainActivity.kt`

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val theme by mainViewModel.theme.collectAsState()
            val darkTheme = when (theme) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            MoneyTrackerTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoneyTrackerNavigation()
                }
            }
        }
    }
}
```

**C++ parallel:** Think of `setContent { ... }` as setting your entire UI tree as a lambda that gets called by Compose's runtime whenever state changes. **No XML layouts, no `findViewById`, no `setText()`** — it's declarative. State flows in; render is a function of state.

- **`@AndroidEntryPoint`** — Hilt hook: lets this Activity receive `@Inject` fields. Closest C++ analog: a CRTP base that wires up dependencies for you.
- **`val theme by mainViewModel.theme.collectAsState()`** — Read carefully:
  - `mainViewModel.theme` is a `StateFlow<String>` (cold/hot observable).
  - `.collectAsState()` converts it to Compose state (`State<T>`).
  - `by` is **delegated property syntax**. It means `theme` is now a delegated read-only `val`; every access calls `getValue()` on the underlying `State<String>`. Recomposition is triggered automatically when the value changes. There's no exact C++ analog — it's like a `std::function` that fires a callback every time the source updates, but read like a plain value.
- **`when (theme) { ... }`** — Kotlin's `when` is `switch` on steroids. No `break` needed. Can match values, ranges, types. Exhaustiveness checking in some contexts.
- **`MoneyTrackerTheme(darkTheme = darkTheme) { ... }`** — Trailing lambda convention: if the last parameter is `content: @Composable () -> Unit`, you can write it outside the parens. Very common in Compose.
- **`Surface(modifier = Modifier.fillMaxSize(), ...)`** — `Modifier` is a chainable decorator (CSS-style) — `Modifier.fillMaxSize().padding(16.dp)` reads like a fluent builder. **`dp`** is density-independent pixels (Compose's length unit).

### 4.3 `MainViewModel.kt`

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val initialLocale: String = runBlocking(Dispatchers.IO) {
        settingsRepository.language.first()
    }
    private val initialTheme: String = runBlocking(Dispatchers.IO) {
        settingsRepository.theme.first()
    }

    val theme: StateFlow<String> = settingsRepository.theme.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        initialTheme
    )

    val locale: StateFlow<String> = settingsRepository.language.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        initialLocale
    )
}
```

**C++ parallel:** A `ViewModel` is a class whose lifetime survives Activity recreation (rotation, theme change). Think of it as a presenter that the framework owns. In C++ you'd write a `Pimpl` plus a lifecycle manager. In Android, the framework does it for you.

- **`@HiltViewModel`** + **`@Inject constructor(...)`** — Hilt generates the factory; `settingsRepository` is provided by `SettingsRepository`'s own `@Inject constructor`.
- **`runBlocking(Dispatchers.IO) { settingsRepository.language.first() }`** — Synchronously read the first emission. This is **deliberately blocking the main thread for a few ms** during VM construction. In C++ you'd feel dirty doing this; in Android, DataStore's first read is <10ms and the VM is constructed before the first frame, so the user doesn't perceive it. The big comment block in this file explains why they don't use a placeholder default value (which caused a recreate loop in a previous version).
  - **`runBlocking`** is `std::future::get()` for coroutines — blocks the current thread until the block completes.
- **`viewModelScope`** — a `CoroutineScope` cancelled when the VM is cleared. Replaces manual thread cleanup.
- **`SharingStarted.WhileSubscribed(5000)`** — "share the upstream Flow when there's at least one subscriber; keep it for 5s after the last unsubscribe so configuration changes don't tear down and re-create everything." Think: short-lived `std::shared_ptr` to a hot stream.

---

## 5. Type System Tour — Sealed Classes, Data Classes, Enums

This codebase uses every "algebraic-ish" Kotlin type. Read these four files first.

### 5.1 `domain/model/Transaction.kt`

```kotlin
sealed class Transaction {
    abstract val id: Long
    abstract val amount: Double
    abstract val description: String
    abstract val date: LocalDate
    abstract val time: LocalTime
    abstract val timestamp: Long

    data class ExpenseTransaction(
        override val id: Long,
        override val amount: Double,
        override val description: String,
        override val date: LocalDate,
        override val time: LocalTime,
        override val timestamp: Long,
        val category: Category,
        val wallet: String
    ) : Transaction()

    data class IncomeTransaction(
        override val id: Long,
        override val amount: Double,
        override val description: String,
        override val date: LocalDate,
        override val time: LocalTime,
        override val timestamp: Long,
        val wallet: String
    ) : Transaction()
}

fun Transaction.displaySign(): String = when (this) {
    is Transaction.ExpenseTransaction -> "-"
    is Transaction.IncomeTransaction -> "+"
}

fun Transaction.isExpense(): Boolean = this is Transaction.ExpenseTransaction
```

**C++ parallel:** A `sealed class` is exactly `std::variant<Expense, Income>` with an exhaustive `std::visit`, except the compiler enforces that you handle every subtype. The doc-comment explicitly calls this out: "Using a sealed class ... forces every new transaction kind to be explicitly handled."

- **`sealed class`** — subclasses must live in the **same file** (or, since Kotlin 1.5, the same package inside the same module). The compiler can then prove exhaustiveness.
- **`abstract val ...`** — `abstract` properties on a sealed class work like pure virtuals. Subclasses `override` them.
- **`data class`** — auto-generates `equals/hashCode/toString/copy/componentN`. Two `ExpenseTransaction(1, ...)` with the same field values are equal. **Use these everywhere you would have used a `struct` in C++.**
- **`is Transaction.ExpenseTransaction`** — type check. Inside the `when` arm, smart-cast gives you `tx as Transaction.ExpenseTransaction` automatically.
- **`fun Transaction.displaySign(): String = when (this) { ... }`** — **extension function**. Adds a method to an existing class without subclassing. C++ analog: a free function `displaySign(const Transaction&)` that takes the type as the first parameter; here it's just nicer syntax.

### 5.2 `domain/model/RecurrenceRule.kt`

```kotlin
sealed class RecurrenceRule {
    abstract fun nextOccurrence(after: LocalDate): LocalDate

    fun encode(): String = when (this) {
        is Daily -> "DAILY"
        is Weekly -> "WEEKLY:${dayOfWeek.name}"
        is Monthly -> "MONTHLY:${dayOfMonth}"
        is Yearly -> "YEARLY:${month}:${dayOfMonth}"
    }

    object Daily : RecurrenceRule() {
        override fun nextOccurrence(after: LocalDate): LocalDate = after.plusDays(1)
    }

    data class Weekly(val dayOfWeek: DayOfWeek) : RecurrenceRule() {
        override fun nextOccurrence(after: LocalDate): LocalDate {
            var d = after.plusDays(1)
            while (d.dayOfWeek != dayOfWeek) d = d.plusDays(1)
            return d
        }
    }

    data class Monthly(val dayOfMonth: Int) : RecurrenceRule() { ... }
    data class Yearly(val month: Int, val dayOfMonth: Int) : RecurrenceRule() { ... }

    companion object {
        fun decode(value: String?): RecurrenceRule? { ... }
    }
}
```

Notice:
- **`object Daily : RecurrenceRule()`** — `object` declares a **singleton** (a class with exactly one instance, lazily created). Closest C++ analog: a class with a deleted constructor + a static `getInstance()`. Here it's literally one keyword.
- **`companion object`** — the "static" members of a class. You can call `RecurrenceRule.decode(...)` directly. Replaces Java's `static` methods.
- **`data class Weekly(val dayOfWeek: DayOfWeek) : RecurrenceRule()`** — primary constructor in the class header. The constructor parameters become properties automatically (because of `val`).

### 5.3 `domain/model/Wallet.kt` and `ThemeMode.kt`

```kotlin
enum class Wallet(val displayName: String) {
    CASH("Cash"),
    BANK("Bank");

    companion object {
        fun fromName(value: String?): Wallet =
            entries.firstOrNull { it.displayName.equals(value, ignoreCase = true) || it.name == value }
                ?: CASH
    }
}

enum class ThemeMode {
    LIGHT, DARK, SYSTEM;

    companion object {
        fun fromName(value: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}
```

- **`enum class Wallet(val displayName: String)`** — Kotlin enums can have **properties and methods**, not just labels. The constructor `CASH("Cash")` invokes the primary constructor. Compare C++:
  ```cpp
  enum class Wallet { CASH, BANK };
  // To attach a name, you need a separate map<string, Wallet>.
  ```
- **`entries`** (Kotlin 1.9+) replaces the old `values()`. Returns a `List<E>`.
- **`it.displayName.equals(value, ignoreCase = true)`** — note `ignoreCase = true` is a **named argument**. C++ would be `equals(value, true)` or `iequals(value)`. Kotlin makes boolean flags self-documenting.

### 5.4 `domain/model/Category.kt`

```kotlin
data class Category(
    val id: Int,
    val name: String,
    val iconName: String,
    val color: Int
) {
    companion object {
        val default = Category(10, "Other", "MoreVert", 0xFF607D8B.toInt())
    }
}
```

- **`val id: Int, val name: String, ...`** — primary constructor, all properties. Auto-generates `copy(id = 99)` for partial updates.
- **`0xFF607D8B.toInt()`** — Kotlin `Int` is 32-bit signed; a literal `0xFF607D8B` is `Long`. `.toInt()` truncates to 32-bit. C++ would also need a cast: `static_cast<int32_t>(0xFF607D8B)`.
- **`companion object { val default = ... }`** — `Category.default` is the singleton fallback.

### 5.5 `domain/model/Budget.kt` — defaults in primary constructor

```kotlin
data class Budget(
    val monthlyBudget: Double = 0.0,
    val dailyBudget: Double = 0.0,
    val currency: String = "USD"
)
```

**Kotlin idiom:** `val monthlyBudget: Double = 0.0` declares a property AND gives the constructor parameter a default value. Now `Budget()`, `Budget(monthlyBudget = 100.0)`, and `Budget(100.0, 5.0, "EUR")` all compile. C++ has default arguments too, but they must come **last** and can't be skipped by name (until C++20 designated initializers, but those don't give you the same call-site ergonomics).

---

## 6. Persistence — Room

### 6.1 `data/local/entity/ExpenseEntity.kt`

```kotlin
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val categoryId: Int,
    val description: String,
    val date: String,
    val time: String,
    val timestamp: Long,
    val wallet: String = "Cash",
    val recurrenceRule: String? = null,
    val recurrenceGroupId: String? = null
)
```

- **`@Entity(tableName = "expenses")`** — Room annotation. Room generates the table-creation SQL and the data access code at compile time via KSP. **The closest C++ analog is an ORM like ODB or SQLAlchemy, but compile-time.**
- **`@PrimaryKey(autoGenerate = true)`** — like `INTEGER PRIMARY KEY AUTOINCREMENT` in SQLite.
- **`val date: String, val time: String`** — Room can't store `LocalDate`/`LocalTime` directly, so the entity holds strings (ISO-8601). The mapper translates to/from `java.time`. **The cost of the boundary.**
- **`val recurrenceRule: String? = null`** — `String?` = nullable string. The `?` is the entire type system change. C++ analog: `std::optional<std::string>` or `const char*`. In Kotlin, the compiler will refuse any operation that could produce a null-pointer exception unless you opt in (`!!`, `?.`, `?:`, smart-cast after a null check).
- **`val id: Long = 0`** — default value for inserts (Room generates a fresh id).

### 6.2 `data/local/entity/ExpenseWithCategory.kt` — Room `@Relation`

```kotlin
data class ExpenseWithCategory(
    @Embedded val expense: ExpenseEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity
)
```

- **`@Embedded`** — flattens `ExpenseEntity`'s columns into this row.
- **`@Relation(parentColumn = "categoryId", entityColumn = "id")`** — auto-join: "whenever I read this, also fetch the `CategoryEntity` whose `id` matches the embedded `categoryId`." Generates a second query and stitches the result. The DAO must use `@Transaction` for atomicity.

### 6.3 `data/local/AppDatabase.kt`

```kotlin
@Database(
    entities = [
        ExpenseEntity::class,
        IncomeEntity::class,
        BudgetEntity::class,
        CategoryEntity::class,
        CategoryBudgetEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao

    class Callback @Inject constructor(
        @ApplicationContext private val context: Context,
        private val categoryDao: Provider<CategoryDao>,
        private val budgetDao: Provider<BudgetDao>
    ) : RoomDatabase.Callback() {

        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            applicationScope.launch {
                seedCategories()
                seedBudget()
            }
        }
        ...
    }
}
```

- **`abstract class AppDatabase : RoomDatabase()`** — You extend Room's base class. The compiler can't generate a class with abstract methods, so Room fills in the implementation at compile time.
- **`abstract fun expenseDao(): ExpenseDao`** — pure virtual; Room implements to return a generated DAO impl.
- **`::class`** — class literal. `ExpenseEntity::class` is the `java.lang.Class<ExpenseEntity>` instance. C++ analog: `typeid(ExpenseEntity)`.
- **`class Callback @Inject constructor(...)` — secondary constructor pattern.** When a nested class needs DI, you mark its constructor `@Inject` and Hilt wires it.
- **`Provider<CategoryDao>`** — `javax.inject.Provider<T>` is a deferred factory. The DB callback shouldn't hold a direct DAO reference because the DB itself might not exist yet at callback creation. `Provider<CategoryDao>.get()` is the C++ analog of `std::function<T*()>`.

### 6.4 `data/local/dao/ExpenseDao.kt`

```kotlin
@Dao
interface ExpenseDao {

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ExpenseWithCategory>>

    @Query("SELECT SUM(amount) FROM expenses WHERE date = :date")
    fun getTotalByDate(date: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)
    ...
}

data class HighestSpendingDay(val date: String, val total: Double)
data class HighestSpendingCategory(val categoryId: Int, val total: Double)
```

**Syntax breakdown:**

- **`@Dao interface ExpenseDao`** — an interface (think C++ abstract class with only pure virtuals). Room generates an implementation at compile time. KSP runs the annotation processor; the impl class is wired into Hilt by `DatabaseModule`.
- **`@Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT :limit")` fun getRecent(limit: Int)`** — Room validates the SQL at compile time against the schema (the `data` annotation processor).
  - **`:limit`** is a parameter binding. C++ would use `?` and a separate bind; SQL uses `:name`.
- **`Flow<List<ExpenseWithCategory>>`** — Room returns **a Kotlin Flow that re-emits whenever the underlying tables change**. C++ analog: a `std::function<void(vector<T>)>` you subscribe to, but with cancellation and backpressure built in. **`Flow` is the single most important type in modern Kotlin** — it's an async stream of values.
- **`suspend fun insert(expense: ExpenseEntity): Long`** — `suspend` marks a coroutine-aware function. The compiler instruments the call sites. Room's `suspend` functions automatically run on the IO dispatcher, so they don't block the main thread.
- **`OnConflictStrategy.REPLACE`** — what Room does on primary-key collision. Like SQLite's `INSERT OR REPLACE`.
- **`@Update` / `@Delete`** — Room generates the SQL from the method signature and the entity's primary key.

### 6.5 `di/DatabaseModule.kt` — Hilt + Migrations

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE incomes ADD COLUMN wallet TEXT NOT NULL DEFAULT 'Cash'")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN recurrenceRule TEXT")
        db.execSQL("ALTER TABLE expenses ADD COLUMN recurrenceGroupId TEXT")
        db.execSQL("ALTER TABLE incomes ADD COLUMN recurrenceRule TEXT")
        db.execSQL("ALTER TABLE incomes ADD COLUMN recurrenceGroupId TEXT")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS category_budgets (
                categoryId INTEGER NOT NULL PRIMARY KEY,
                monthlyLimit REAL NOT NULL,
                currency TEXT NOT NULL
            )
        """.trimIndent())
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        callback: AppDatabase.Callback
    ): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "money_tracker.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .addCallback(callback)
            .build()
    }

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao = database.expenseDao()
    ...
}
```

**C++ parallel:** A Hilt `@Module` is a **manual factory container** — the function body is the constructor, the return type is the produced type. Think of `provideExpenseDao` as `ExpenseDao* make_expense_dao(AppDatabase& db) { return &db.expenseDao(); }`.

- **`object DatabaseModule`** — singleton object holding only `static`-style factories.
- **`@Module @InstallIn(SingletonComponent::class)`** — declare that this module's `@Provides` functions are accessible from anywhere in the app. `SingletonComponent` = the application graph.
- **`@Provides`** — a factory function. The return type becomes the contract; the parameters are the dependencies.
- **`@Singleton`** — scope the produced instance to the component's lifetime (i.e., the application's lifetime). Equivalent to a `std::shared_ptr` stored in the container.
- **`@ApplicationContext context: Context`** — Hilt qualifier telling the framework to inject the application-level `Context`, not an activity's.
- **`object : Migration(1, 2) { ... }`** — anonymous object expression. Creates an instance of an abstract class inline. C++ analog: a lambda capturing `this` and overriding the virtual method — but Kotlin lets you do it on classes with multiple virtual methods cleanly.
- **`Room.databaseBuilder(context, AppDatabase::class.java, "money_tracker.db")`** — Room builder pattern. `.java` on the class literal gives you `java.lang.Class<AppDatabase>` (the Java reflect type that Room needs).
- **`.trimIndent()`** on a multi-line string literal — strips the common leading whitespace. Like a heredoc with auto-dedent.

---

## 7. Mappers — `data/mapper/EntityMapper.kt`

```kotlin
fun ExpenseWithCategory.toExpense(): Expense = Expense(
    id = expense.id,
    amount = expense.amount,
    category = category.toCategory(),
    description = expense.description,
    date = LocalDate.parse(expense.date),
    time = LocalTime.parse(expense.time),
    wallet = expense.wallet,
    recurrenceRule = RecurrenceRule.decode(expense.recurrenceRule),
    recurrenceGroupId = expense.recurrenceGroupId
)

fun Expense.toEntity(): com.moneytracker.data.local.entity.ExpenseEntity =
    com.moneytracker.data.local.entity.ExpenseEntity(
        id = id,
        amount = amount,
        categoryId = category.id,
        description = description,
        date = date.toString(),
        time = time.toString(),
        timestamp = LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toEpochSecond(),
        wallet = wallet,
        recurrenceRule = recurrenceRule?.encode(),
        recurrenceGroupId = recurrenceGroupId
    )
```

**Three idioms in 30 lines:**

1. **`fun ExpenseWithCategory.toExpense(): Expense`** — extension function on a Room class. Lets you write `row.toExpense()` instead of `Mapper.toExpense(row)`. C++ analog: a free function `toExpense(const ExpenseWithCategory&)`.
2. **Named arguments everywhere** (`id = id, amount = amount, ...`) — readability at call sites. Also lets you reorder freely.
3. **`recurrenceRule?.encode()`** — the safe-call operator. If `recurrenceRule` is `null`, the entire expression is `null` and Kotlin doesn't crash. C++ would be `recurrenceRule ? recurrenceRule->encode() : nullptr`. Kotlin's `?.` chains: `a?.b?.c?.d` returns `null` if any link is null.
4. **`LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toEpochSecond()`** — the kind of fluent chain you see in `java.time` and `kotlinx.coroutines`. In C++ you'd compose `std::chrono` calls or use `std::format` with custom code.

---

## 8. Repositories — `data/repository/ExpenseRepository.kt`

```kotlin
@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {

    fun getAllExpenses(): Flow<List<Expense>> =
        expenseDao.getAll().map { list -> list.map { it.toExpense() } }

    fun getRecentExpenses(limit: Int): Flow<List<Expense>> =
        expenseDao.getRecent(limit).map { list -> list.map { it.toExpense() } }

    fun getTotalByMonth(month: String): Flow<Double> =
        expenseDao.getTotalByMonth(month).map { it ?: 0.0 }

    suspend fun insertExpense(expense: Expense): Long =
        expenseDao.insert(expense.toEntity())
    ...
}
```

**The repository pattern is essentially the Facade pattern**, but with two Kotlin-specific tricks:

- **`@Inject constructor(private val expenseDao: ExpenseDao)`** — Hilt instantiates `ExpenseRepository` with the DAO. The repository itself is `@Singleton` so the whole app shares one instance.
- **`fun getTotalByMonth(month: String): Flow<Double> = expenseDao.getTotalByMonth(month).map { it ?: 0.0 }`** — `Flow.map` is `std::transform` for streams. `it ?: 0.0` is the Elvis operator: `it ?: 0.0` means "use `it` if non-null, else `0.0`." C++ analog: `it.value_or(0.0)` (for `std::optional`) or a ternary `it ? *it : 0.0`.
- **`private val expenseDao: ExpenseDao`** — primary constructor with property visibility. The compiler generates the field and assigns it from the constructor parameter. There is no separate "constructor body" needed.

---

## 9. Use Cases — The Domain Layer

This is where the "business logic" lives. Read these two files — they showcase idiomatic Kotlin flow plumbing.

### 9.1 `domain/usecase/CalculateWalletBalancesUseCase.kt`

```kotlin
class CalculateWalletBalancesUseCase @Inject constructor() {

    data class Balances(
        val total: Double,
        val cash: Double,
        val bank: Double
    )

    operator fun invoke(transactions: List<Transaction>): Balances {
        var cash = 0.0
        var bank = 0.0

        for (tx in transactions) {
            val wallet = when (tx) {
                is Transaction.ExpenseTransaction -> Wallet.fromName(tx.wallet)
                is Transaction.IncomeTransaction -> Wallet.fromName(tx.wallet)
            }
            val signedAmount = when (tx) {
                is Transaction.ExpenseTransaction -> -tx.amount
                is Transaction.IncomeTransaction -> tx.amount
            }
            when (wallet) {
                Wallet.CASH -> cash += signedAmount
                Wallet.BANK -> bank += signedAmount
            }
        }

        return Balances(total = cash + bank, cash = cash, bank = bank)
    }
}
```

**C++ parallel:** This is a function object (`std::function` / lambda) but registered with the DI container. The interesting Kotlin bits:

- **`operator fun invoke(...)`** — makes instances callable like functions: `useCase(transactions)` instead of `useCase.execute(transactions)`. C++ analog: a class with `operator()`. **This is how the entire use case pattern works** — every use case overloads `invoke`.
- **`for (tx in transactions)`** — for-each over an `Iterable`. C++: `for (const auto& tx : transactions)`.
- **`when (tx) { is Transaction.ExpenseTransaction -> ... }`** — exhaustive when on a sealed type. The compiler checks that every subtype is covered. After `is Transaction.ExpenseTransaction`, `tx` is smart-cast to that subtype and you can access `tx.amount`, `tx.category`, etc., without explicit casts.
- **`var cash = 0.0` / `var bank = 0.0`** — mutable locals. Idiomatic Kotlin uses `val` whenever possible, but accumulators must be `var`. Note the types are inferred from `0.0`.

### 9.2 `domain/usecase/GetDashboardSummaryUseCase.kt` — the heavy stuff

```kotlin
class GetDashboardSummaryUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val budgetRepository: BudgetRepository,
    private val getCategoryBudgetUsage: GetCategoryBudgetUsageUseCase,
    private val calculateWalletBalances: CalculateWalletBalancesUseCase
) {

    data class Summary(
        val recentTransactions: List<Transaction>,
        val spentToday: Double,
        ...
    )

    operator fun invoke(
        month: YearMonth,
        today: LocalDate,
        currency: String
    ): Flow<Summary> {
        val monthString = month.toString()
        val recentLimit = RECENT_TRANSACTION_LIMIT
        return combine(
            expenseRepository.getTotalByMonth(monthString),
            expenseRepository.getTotalByDate(today),
            incomeRepository.getTotalIncomeByMonth(monthString),
            expenseRepository.getExpensesByMonthString(monthString),
            incomeRepository.getIncomeByMonth(monthString),
            expenseRepository.getRecentExpenses(recentLimit),
            incomeRepository.getRecentIncome(recentLimit),
            budgetRepository.getBudget(),
            getCategoryBudgetUsage(monthString)
        ) { values ->
            val spentThisMonth = values[0] as Double
            val spentToday = values[1] as Double
            ...
            Summary(
                recentTransactions = recent,
                spentToday = spentToday,
                spentThisMonth = spentThisMonth,
                ...
            )
        }
    }

    companion object {
        const val RECENT_TRANSACTION_LIMIT = 10
    }
}
```

**The takeaways:**

- **`Flow<Summary>`** is a cold/hot observable stream of `Summary` values.
- **`combine(flow1, flow2, ..., flow9) { values -> ... }`** — the `Flow` equivalent of `std::experimental::when_all`. It waits for **all** upstream flows to emit, then combines the latest values into a `Summary`. Every time any upstream emits a new value, the combiner re-runs and emits a new `Summary`.
- **`values[0] as Double`** — generics on flows are erased at runtime, so the lambda gets `Array<Any?>`. You must cast.
- **`const val RECENT_TRANSACTION_LIMIT = 10`** — compile-time constant. Use for `Int`, `String`, `Double` literals in a `companion object` (or top-level). C++ analog: `static constexpr int RECENT_TRANSACTION_LIMIT = 10;`.

### 9.3 `domain/usecase/GetStatisticsUseCase.kt`

```kotlin
class GetStatisticsUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository
) {
    operator fun invoke(month: String): Flow<Statistics> = combine(
        combine(
            expenseRepository.getExpensesByMonth(month),
            incomeRepository.getIncomeByMonth(month),
            expenseRepository.getTotalByMonth(month)
        ) { a, b, c -> Triple(a, b, c) },
        incomeRepository.getTotalIncomeByMonth(month),
        expenseRepository.getHighestSpendingDay(month),
        expenseRepository.getHighestSpendingCategory(month)
    ) { (expenses, incomes, totalExpense), totalIncome, highestDay, highestCategory ->
        Statistics(...)
    }
    ...
}
```

Notice **nested `combine`** — there's no overload of `combine` for >5 flows, so you group. `Triple(a, b, c)` is a 3-tuple. **Kotlin has `Pair` and `Triple`; for more, use a `data class`** (which is what the codebase does — see `Summary` and `Statistics` data classes).

---

## 10. Preferences — `data/local/preferences/UserPreferences.kt`

```kotlin
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    val currency: Flow<String> = dataStore.data.map { preferences ->
        preferences[CURRENCY_KEY] ?: "USD"
    }

    val theme: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "System"
    }

    suspend fun setCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[CURRENCY_KEY] = currency
        }
    }

    companion object {
        private val CURRENCY_KEY = stringPreferencesKey("currency")
        private val THEME_KEY = stringPreferencesKey("theme")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val DAILY_SUMMARY_KEY = booleanPreferencesKey("daily_summary")
    }
}
```

**Kotlin idioms in this file:**

- **`private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")`** — **extension property** on the `Context` class. `preferencesDataStore` is a Jetpack delegate that wires a DataStore onto a `Context`. `by` is the property delegate. C++ analog: `static DataStore& dataStore(Context& c)` — a free function masquerading as a member.
- **`val currency: Flow<String> = dataStore.data.map { preferences -> preferences[CURRENCY_KEY] ?: "USD" }`** — `dataStore.data` is `Flow<Preferences>`. `.map` transforms. The `preferences[CURRENCY_KEY] ?:` applies the Elvis default. Reads as: "give me a flow of currency strings, defaulting to USD."
- **`preferences[CURRENCY_KEY] = currency`** inside `edit { ... }`** — `Preferences` is a `Map`-like type with `operator get`/`operator set`. You can use index syntax.
- **`stringPreferencesKey("currency")` / `booleanPreferencesKey("...")`** — typed key factories. C++ analog: `constexpr auto CURRENCY_KEY = "currency";` plus a typed wrapper template.

---

## 11. ViewModels — `DashboardViewModel` and `AddExpenseViewModel`

### 11.1 `DashboardViewModel.kt`

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUseCase,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    val currency: StateFlow<String> = settingsRepository.currency.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "USD"
    )

    val uiState: StateFlow<DashboardUiState> = _selectedMonth
        .flatMapLatest { month ->
            combine(
                getDashboardSummary(month, _today.value, "$currency"),
                currency
            ) { summary, ccy -> summary to ccy }
                .mapToUiState()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState()
        )

    private fun kotlinx.coroutines.flow.Flow<Pair<GetDashboardSummaryUseCase.Summary, String>>.mapToUiState() =
        map { (summary, ccy) -> DashboardUiState(...) }

    fun nextMonth() { _selectedMonth.value = _selectedMonth.value.plusMonths(1) }
    fun previousMonth() { _selectedMonth.value = _selectedMonth.value.minusMonths(1) }
    fun resetToCurrentMonth() { _selectedMonth.value = YearMonth.now() }

    fun onResume() {
        val now = LocalDate.now()
        if (_today.value != now) _today.value = now
    }
}

data class DashboardUiState(
    val recentTransactions: List<Transaction> = emptyList(),
    val spentToday: Double = 0.0,
    ...
)
```

**Syntax breakdown:**

- **`@OptIn(ExperimentalCoroutinesApi::class)`** — like a CMake `find_package(... CONFIG ...)` for an unstable API. `flatMapLatest` is currently experimental; the opt-in silences the warning.
- **`MutableStateFlow<YearMonth>(YearMonth.now())`** — a mutable, hot, single-value container with conflation. Think `std::atomic<T>` + a list of subscribers that get the latest value on subscribe.
- **`val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()`** — the `_selectedMonth` is `Mutable` (private), the exposed `selectedMonth` is read-only. **The `_` prefix is the codebase's convention for "this is the backing field, don't expose mutability."**
- **`settingsRepository.currency.stateIn(scope, started, initialValue)`** — converts a cold `Flow` to a hot `StateFlow` that's tied to the VM's scope. The `5_000` here is `5_000` (underscores in numeric literals — Kotlin allows them anywhere in the digits).
- **`.flatMapLatest { month -> ... }`** — every time the source emits a new month, **cancel the previous downstream and start a new one**. C++ has no exact analog; closest would be "tear down the worker and start a fresh one." Crucial for avoiding stale state when the user navigates months quickly.
- **`summary to ccy`** — `to` is an infix function creating a `Pair<Summary, String>`. C++ analog: `std::pair{summary, ccy}` or `std::make_pair(...)`.
- **`{ (summary, ccy) -> ... }`** — destructuring a `Pair` in a lambda. Equivalent to `{ p -> ...; val summary = p.first; val ccy = p.second }`. C++17 has structured bindings: `auto [summary, ccy] = p;`.
- **`val _today.value` and `_today.value = now`** — `MutableStateFlow.value` is a `var` accessor.
- **`data class DashboardUiState(...)` with default values** — every field is optional in the constructor, so `DashboardUiState()` gives you a sensible empty state.

### 11.2 `AddExpenseViewModel.kt` — state, events, side effects

```kotlin
@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddExpenseEvent>()
    val events: SharedFlow<AddExpenseEvent> = _events.asSharedFlow()

    val currency = settingsRepository.currency

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { state ->
                    val pending = state.pendingNewCategoryName
                    val matching = pending?.let { name ->
                        categories.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    }
                    state.copy(
                        categories = categories,
                        selectedCategory = when {
                            matching != null -> matching
                            state.selectedCategory == Category.default && categories.isNotEmpty() ->
                                categories.first()
                            else -> state.selectedCategory
                        },
                        pendingNewCategoryName = null
                    )
                }
            }
        }
    }
    ...

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val amount = state.amount.toDoubleOrNull() ?: 0.0
            if (amount <= 0) {
                emitError(R.string.add_expense_error_amount_zero)
                return@launch
            }
            try {
                if (state.isIncome) {
                    val income = Income(id = state.editId, ...)
                    if (state.isEdit) incomeRepository.updateIncome(income)
                    else incomeRepository.insertIncome(income)
                } else {
                    val expense = Expense(id = state.editId, ...)
                    if (state.isEdit) expenseRepository.updateExpense(expense)
                    else expenseRepository.insertExpense(expense)
                }
                _events.emit(AddExpenseEvent.NavigateBack)
            } catch (e: Exception) {
                _events.emit(AddExpenseEvent.ShowError(R.string.error_unknown))
            }
        }
    }

    private suspend fun emitError(@StringRes messageRes: Int) {
        _events.emit(AddExpenseEvent.ShowError(messageRes))
    }

    data class AddExpenseUiState(
        val amount: String = "",
        val selectedCategory: Category = Category.default,
        ...
        val pendingNewCategoryName: String? = null
    )

    sealed class AddExpenseEvent {
        data object NavigateBack : AddExpenseEvent()
        data class ShowError(@StringRes val messageRes: Int) : AddExpenseEvent()
    }
}
```

**Idioms you must know:**

- **`init { ... }`** — the primary-constructor body. Runs as part of construction. C++ analog: code in the constructor body.
- **`viewModelScope.launch { ... }`** — launches a coroutine tied to the VM lifetime. **All your async work goes here** (or in a use case that returns a `Flow`).
- **`StateFlow` vs `SharedFlow`**:
  - `StateFlow` always has a current value, replays it to new subscribers, conflates duplicate values.
  - `SharedFlow` has no value (it's just events); you can configure replay, buffer size, etc.
  - **`_uiState` is `StateFlow` (continuous state); `_events` is `SharedFlow` (one-shot navigation/error).** This is the canonical split.
- **`categoryRepository.getAllCategories().collect { categories -> ... }`** — `collect` is `for (x in flow)`. Suspends until the flow completes or is cancelled. The lambda is `suspend`.
- **`_uiState.update { state -> state.copy(...) }`** — `update` is the atomic compare-and-set on `StateFlow`. The lambda receives the current state and returns the new one. C++ analog: `while (!state.compare_exchange_weak(...)) {}`.
- **`state.copy(categories = categories, selectedCategory = ...)`** — `data class` auto-generates `copy(...)` for **partial updates**, much like C++ designated initializers (`Expense{...e, .amount = new_amount}`). Always immutable.
- **`pending?.let { name -> ... }`** — `let` is a scope function. `x?.let { it.foo() }` is shorthand for `if (x != null) { val it = x; it.foo() }`. **You will see `?.let { ... }` everywhere.**
- **`state.amount.toDoubleOrNull() ?: 0.0`** — `String.toDoubleOrNull()` returns `Double?`. Elvis defaults it to `0.0` on null. Then `if (amount <= 0)` validates.
- **`@StringRes messageRes: Int`** — an annotation-marked parameter. **No runtime effect**; it's documentation and helps linting.
- **`sealed class AddExpenseEvent`** — closed hierarchy for one-shot events (Navigate, ShowError). Same pattern as `Transaction`.
- **`data object NavigateBack : AddExpenseEvent()`** — Kotlin 1.9's `data object` is a singleton with auto-generated `toString()`/equals. Without `data`, an `object` is fine but has a less useful string representation.

---

## 12. UI — Compose

### 12.1 `DashboardScreen.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddExpense: () -> Unit,
    onViewAll: () -> Unit,
    onEditExpense: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(greeting()) },
                colors = TopAppBarDefaults.topAppBarColors(...)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Row(...) { ... } }
            items(uiState.recentTransactions, key = { transaction -> transaction.stableKey() }) { transaction ->
                TransactionItem(...)
            }
        }
    }
}
```

**C++ parallel:** A `@Composable` function is a tree-building function. Calling it doesn't immediately produce pixels — Compose records the description and a runtime diffs it against the previous tree, applying minimal changes. **This is exactly the "virtual DOM" pattern (e.g., React) translated to a statically-typed language.**

- **`@Composable`** — annotation; the Compose compiler plugin transforms the function to (a) accept a `Composer` parameter and (b) memoize calls.
- **`onAddExpense: () -> Unit, onEditExpense: (Long) -> Unit`** — `() -> Unit` is `std::function<void()>`; `(Long) -> Unit` is `std::function<void(int64_t)>`. Functional types are first-class.
- **`viewModel: DashboardViewModel = hiltViewModel()`** — default value for a composable parameter. `hiltViewModel()` is the Hilt-aware factory that scopes the VM to the current navigation entry. **This pattern is the equivalent of having a constructor with a default argument — call sites can omit the VM and let Compose wire it.**
- **`val uiState by viewModel.uiState.collectAsState()`** — delegated property on a `State<T>`; reads recompose the function when the value changes.
- **`DisposableEffect(lifecycleOwner) { ... onDispose { ... } }`** — a **side-effect with cleanup**. Runs the block once when the composition enters, and the `onDispose` block when it leaves (or the key changes). C++ analog: an RAII guard. **`DisposableEffect` is your `defer {}` / `unique_ptr` for composable side effects.**
- **`Scaffold(topBar = { ... }, bottomBar = { ... }, floatingActionButton = { ... }) { padding -> ... }`** — Material 3's top-level layout container. The trailing lambda is your screen content; `padding` is the inner padding the bar/button areas need.
- **`LazyColumn(...) { item { ... }; items(list, key = { ... }) { ... } }`** — a virtualized list (renders only visible items). `items(list, key = ...)` is the equivalent of `for (auto& x : list)`, but with a stable key for diffing.
- **`Modifier.fillMaxSize().padding(padding).padding(16.dp)`** — modifier chain. `padding(padding)` consumes the Scaffold's safe-area padding; `.padding(16.dp)` adds 16dp of internal spacing. Reads as "fill the available space, then inset by the scaffold padding, then inset by 16dp more." CSS-style.
- **`Text(stringResource(R.string.dashboard_previous_month))`** — `stringResource` is a composable that returns the localized string from `res/values/strings.xml`. C++ analog: `tr("dashboard_previous_month")` with a Qt-style translation system.
- **`LinearProgressIndicator(progress = { usage.percent.coerceIn(0.0, 1.0).toFloat() })`** — note `progress = { ... }` (a lambda). In Compose, lambdas are used to defer reads until draw time, allowing Compose to skip work if the input didn't change. C++ has no analog.
- **`usage.percent.coerceIn(0.0, 1.0)`** — Kotlin stdlib clamps the value. Like `std::clamp` for doubles.

### 12.2 `ui/navigation/MoneyTrackerNavigation.kt`

```kotlin
@Composable
fun MoneyTrackerNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem(R.string.nav_dashboard, Icons.Default.Home, Screen.Dashboard.route),
        BottomNavItem(R.string.nav_statistics, Icons.Default.TrendingUp, Screen.Statistics.route),
        BottomNavItem(R.string.nav_settings, Icons.Default.Settings, Screen.Settings.route)
    )

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = { if (showBottomBar) NavigationBar { ... } },
        floatingActionButton = { if (showBottomBar) FloatingActionButton(...) { ... } }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = Screen.Dashboard.route, ...) {
                DashboardScreen(onAddExpense = { navController.navigate(Screen.AddExpense.route) }, ...)
            }
            composable(route = Screen.EditExpense.route, arguments = listOf(navArgument("expenseId") { type = NavType.LongType })) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: 0L
                AddExpenseScreen(expenseId = expenseId, onNavigateBack = { navController.popBackStack() })
            }
            ...
        }
    }
}

private data class BottomNavItem(
    @androidx.annotation.StringRes val labelRes: Int,
    val icon: ImageVector,
    val route: String
)
```

**Idioms:**

- **`val navController = rememberNavController()`** — `remember { }` is a Compose primitive: caches a value across recompositions. The first time the composable runs, the lambda executes; subsequent recompositions return the cached value. C++ analog: `static` local variable (but per-composition, not per-function). **Use it for non-derived state.**
- **`val navBackStackEntry by navController.currentBackStackEntryAsState()`** — the `by` here works on a `State<NavBackStackEntry?>`.
- **`in bottomNavItems.map { it.route }`** — `in` with a `Collection` is "is this element contained?" C++: `std::find(vec.begin(), vec.end(), x) != vec.end()`.
- **`@androidx.annotation.StringRes`** — fully-qualified annotation usage. Marks the parameter as expecting an R.string.* resource id.
- **`@StringRes val labelRes: Int`** — same thing with an import.
- **`navArgument("expenseId") { type = NavType.LongType }`** — declarative navigation argument specification. The `type =` is named-argument syntax.
- **`backStackEntry.arguments?.getLong("expenseId") ?: 0L`** — null-safe argument read with Elvis fallback.

### 12.3 `ui/theme/Theme.kt`

```kotlin
@Composable
fun MoneyTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
```

- **`content: @Composable () -> Unit`** — the type of a composable lambda. **A lambda marked `@Composable` is itself a composable**; you can call other composables inside it.
- **`SideEffect { ... }`** — runs the block after every successful recomposition. C++ analog: nothing direct — it's a "hook into the render pipeline."
- **`val view = LocalView.current`** — `LocalView` is a `CompositionLocal`, which is a Compose primitive for **dependency injection through the composition tree**. Like an environment/ambient value.
- **`isSystemInDarkTheme()`** — composable that returns the system's current dark-mode setting.
- **`@Composable ... = ...` default arguments** — default values can be composable expressions.

---

## 13. Testing — JVM Unit Tests with JUnit4 + MockK + Turbine

### 13.1 `CalculateWalletBalancesUseCaseTest.kt`

```kotlin
class CalculateWalletBalancesUseCaseTest {

    private val useCase = CalculateWalletBalancesUseCase()

    @Test
    fun `empty list returns all zeros`() {
        val balances = useCase(emptyList())
        assertEquals(0.0, balances.total, 0.0)
        assertEquals(0.0, balances.cash, 0.0)
        assertEquals(0.0, balances.bank, 0.0)
    }

    @Test
    fun `expenses subtract from the wallet and income adds`() {
        val today = LocalDate.now()
        val now = LocalTime.NOON
        val transactions = listOf(
            expense(amount = 100.0, wallet = "Cash", date = today, time = now),
            expense(amount = 50.0, wallet = "Bank", date = today, time = now),
            income(amount = 200.0, wallet = "Bank", date = today, time = now)
        )
        val balances = useCase(transactions)
        assertEquals(50.0, balances.total, 0.0)
        assertEquals(-100.0, balances.cash, 0.0)
        assertEquals(150.0, balances.bank, 0.0)
    }

    private fun expense(amount: Double, wallet: String, date: LocalDate, time: LocalTime) =
        Transaction.ExpenseTransaction(
            id = 1, amount = amount, description = "", date = date,
            time = time, timestamp = 0L, category = Category.default, wallet = wallet
        )

    private fun income(amount: Double, wallet: String, date: LocalDate, time: LocalTime) =
        Transaction.IncomeTransaction(...)
}
```

**Idioms:**

- **Backtick function names** for test names: `` fun `empty list returns all zeros`() ``. They can contain spaces and read as English sentences. C++ has no syntax for this; closest is `void test_empty_list_returns_all_zeros()`.
- **`assertEquals(0.0, balances.total, 0.0)`** — JUnit's `assertEquals(expected, actual, delta)` for floating-point comparisons. The trailing `delta` is the tolerance.
- **Private factory methods** for test fixtures — Kotlin makes these trivial. No `std::make_unique` or builder classes needed.

### 13.2 `RecurrenceRuleTest.kt` — pure logic tests

```kotlin
class RecurrenceRuleTest {
    private val anchor = LocalDate.of(2026, 8, 26) // Wednesday

    @Test
    fun `daily next occurrence is the day after the anchor`() {
        assertEquals(LocalDate.of(2026, 8, 27), RecurrenceRule.Daily.nextOccurrence(anchor))
    }

    @Test
    fun `yearly clamps Feb 29 in a non-leap year`() {
        val rule = RecurrenceRule.Yearly(month = 2, dayOfMonth = 29)
        val from = LocalDate.of(2024, 2, 29)
        assertEquals(LocalDate.of(2025, 2, 28), rule.nextOccurrence(from))
    }

    @Test
    fun `encode then decode round trips every rule`() {
        val rules: List<RecurrenceRule> = listOf(
            RecurrenceRule.Daily,
            RecurrenceRule.Weekly(DayOfWeek.MONDAY),
            ...
        )
        rules.forEach { original ->
            val decoded = RecurrenceRule.decode(original.encode())
            assertEquals(original, decoded)
        }
    }
}
```

- **`rules.forEach { original -> ... }`** — for-each. Same as `std::for_each`.
- **`assertEquals(original, decoded)`** — for `data class`es, `equals` is auto-generated and does structural comparison. **The whole reason `data class` exists.**

---

## 14. Idiomatic Kotlin Cheat Sheet (C++ → Kotlin)

| What you do in C++ | Kotlin equivalent |
|---|---|
| `std::unique_ptr<T> p = std::make_unique<T>(args...)` | `val p = T(args)` — garbage-collected, no manual cleanup |
| `std::shared_ptr<T> p` | `val p: T` — references are shared, but no refcounting visible |
| `T* p = nullptr; if (p) p->foo()` | `val p: T? = null; if (p != null) p.foo()` or `p?.foo()` |
| `T* p; if (!p) p = fallback()` | `val p: T? = maybeNull() ?: fallback` |
| `enum class Color { RED, GREEN }` | `enum class Color { RED, GREEN }` (but add methods!) |
| `std::variant<A, B, C>` | `sealed class Base { class A : Base(); class B : Base(); ... }` + `when` |
| `std::optional<int>` | `Int?` |
| `std::vector<T>` | `List<T>` (read-only) or `MutableList<T>` |
| `std::unordered_map<K, V>` | `Map<K, V>` / `MutableMap<K, V>` |
| `std::string_view` | `String` (immutable; for mutating, use `StringBuilder`/`buildString`) |
| `std::function<void()>` | `() -> Unit` |
| Lambda capture: `[&](int x){ return x*2; }` | `{ x -> x * 2 }` (no capture syntax; captures whatever is referenced) |
| `static_cast<int>(x)` | `x.toInt()` (every numeric type has `to<Type>()`) |
| `std::move(x)` | No equivalent — values are usually immutable references already |
| `std::forward<T>(x)` | No equivalent |
| Template `<typename T>` | `fun <T> foo(x: T)` (erased at runtime, not specialized) |
| `std::condition_variable`, `std::mutex` | `suspend` functions + `Mutex()` from coroutines (almost never needed in app code) |
| `std::async([](...){...})` | `viewModelScope.launch { ... }` |
| `std::future<T>` | `suspend fun foo(): T` (no future object; you just `return` from the suspend) |
| `std::promise<T>` | `CompletableDeferred<T>` |
| `std::async(f).get()` (blocks) | `runBlocking { f() }` (blocks the thread) |
| RAII: destructor closes file | `use { }` extension on `Closeable` / `AutoCloseable` |
| `[[nodiscard]]` | No analog; convention only |
| `noexcept` | No analog; use `runCatching { }` or `try`/`catch (e: Exception)` |
| `const T&` (read-only ref) | `val t: T` |
| `constexpr int X = 10` | `const val X = 10` (in `companion object` or top-level) |
| Macro / `#define` | None. Compiler plugin / annotation instead |
| Multiple constructors | Primary constructor + `constructor(...)` secondaries, or factory functions |
| Operator overloading | `operator fun plus(other: T): T` (per-operator) |
| Default arg `void f(int x = 0)` | `fun f(x: Int = 0)` |
| Designated initializer `S{s.a=1}` | `S(a = 1)` (named arg) or `s.copy(a = 1)` (data class) |
| `dynamic_cast<T*>(x)` | `x as? T` (returns `null` on failure) or `x as T` (throws) |
| `typeid(x).name()` | `x::class.qualifiedName` or `x::class.simpleName` |

### 14.1 Coroutines in 90 seconds

| Pattern | Code | C++ analog |
|---|---|---|
| Launch background work | `scope.launch { ... }` | `std::async(std::launch::async, ...)` |
| Get a result | `val r = scope.async { ... }.await()` | `auto f = std::async(...); f.get()` |
| Sequential awaits | `suspend fun f() { a(); b(); c() }` | `a(); b(); c()` — but you must be in a coroutine |
| Combine streams | `combine(f1, f2) { a, b -> ... }` | Manual subscriber merge |
| Transform a stream | `flow.map { it.toString() }` | `std::transform` on a stream |
| Filter a stream | `flow.filter { it > 0 }` | `std::copy_if` |
| Hot, single-value stream | `MutableStateFlow(initial)` | `std::atomic<T>` + observer list |
| Hot event stream | `MutableSharedFlow()` | An observer list |
| Cold single-shot | `flow { emit(1); emit(2) }` | A generator coroutine |
| Cancel-on-state-change | `flatMapLatest { ... }` | Unsupported in std |
| Thread pool chooser | `withContext(Dispatchers.IO) { ... }` | Picking a thread pool |
| Test runner | `runTest { ... }` | Custom event loop |

### 14.2 Property / `by` cheat sheet

- `val x: Int` — read-only property, getter only.
- `var x: Int` — readable + writable.
- `val x: Int get() = computeX()` — computed property, no backing field.
- `val x: Int by lazy { computeX() }` — lazy-initialized, thread-safe, single computation.
- `val x: T by delegate` — delegated property. Delegates implement `getValue` (and `setValue` for `var`). Used by Compose's `State`, Hilt's `@Inject`, Kotlin's `Delegates.observable`, etc.

---

## 15. Where to Go Next

You now have a working mental model of every Kotlin construct used in this codebase. To deepen it:

1. **Read `AddExpenseScreen.kt` end-to-end** — it's the most complex screen and will show you every Compose primitive: `OutlinedTextField`, `DropdownMenu`, `DatePicker`, `LaunchedEffect`, `Snackbar`, `IconButton`, etc.
2. **Read `notifications/DailySummaryWorker.kt`** — WorkManager + coroutines + DI in one file. Shows how `HiltWorkerFactory` plays with `Configuration.Provider` (you saw the provider in `MoneyTrackerApplication`).
3. **Read `data/backup/CsvCodec.kt`** — pure-Kotlin file I/O, the `use { }` resource pattern, and string building.
4. **Build and run the app** with `./gradlew assembleDebug` and install on an emulator. Iterate on the dashboard.
5. **Add a feature**: e.g., a "savings goal" entity. You'll touch every layer (entity → DAO → repository → use case → ViewModel → Compose screen), reinforcing the architecture.
6. **Try converting a use case to use `Result<T>`** instead of throwing — Kotlin's `Result` is `std::variant<T, std::exception_ptr>` with ergonomic helpers.

The Kotlin docs that matter most for you:
- *Kotlin docs — Basics*: covers everything in this tutorial.
- *kotlinx.coroutines — Guide*: deep dive on `Flow`, `StateFlow`, `SharedFlow`, structured concurrency.
- *Jetpack Compose — Basics & State*: declarative UI.
- *Hilt — User guide*: compile-time DI.

Welcome to Kotlin. The lift from C++ is mostly un-learning manual memory management and learning to express everything in terms of `val`, immutability, sealed hierarchies, and `Flow`. Once that clicks, the codebase reads naturally.
