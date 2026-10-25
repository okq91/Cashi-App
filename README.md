# Cashi Payment App

A Kotlin Multiplatform + Compose Multiplatform payments demo for Android and iOS. On launch it signs in
silently with a demo account. You can then send a payment by email and browse your transaction history.

## Tech stack

| Concern | Library |
| --- | --- |
| Shared code | Kotlin Multiplatform (Android, iOS, JVM for tests) |
| UI | Compose Multiplatform + Material 3, Navigation Compose |
| Architecture | MVI (`UiState` / `UiIntent` / `UiEffect` + `MviViewModel`) |
| Networking | Ktor client (OkHttp / Darwin), bearer `Auth` plugin, kotlinx.serialization |
| DI | Koin |
| Storage | multiplatform-settings (SharedPreferences / NSUserDefaults) |
| Dates | kotlinx-datetime |
| Unit tests | JUnit 5 + Mockito (mockito-kotlin) + Ktor `MockEngine` |
| BDD | Cucumber (JUnit Platform) |
| UI tests | Appium (UiAutomator2) + JUnit 5 |

## Project layout

```
shared/        KMP module: networking, DTOs, repositories, use cases, validation, MVI view models
  src/commonMain/kotlin/com/cashi/app/
    core/        MVI base classes, Ktor client factory, ApiException, token storage
    data/        DTOs, AuthApi / PaymentsApi, repositories, mappers
    domain/      Amount, Currency, Payment, PaymentValidator, use cases
    presentation/ AppViewModel, PaymentsViewModel, AddPaymentViewModel, formatter
    di/          Koin modules + initKoin()
  src/jvmTest/   unit tests (JUnit 5) and Cucumber BDD suite (resources/features/*.feature)
composeApp/    Compose Multiplatform UI (screens, theme, navigation), iOS framework "ComposeApp"
androidApp/    Android application (Application + MainActivity)
iosApp/        Xcode project hosting the Compose UI
uiTests/       Appium end-to-end tests for the Android app
```

### MVI

Each screen has an immutable `State`, a sealed `Intent` that the UI sends through `onIntent()`, and
one-shot `Effect`s (navigation, snackbars) delivered through a channel. The view models live in
`shared/presentation` and are provided by Koin (`koinViewModel()` in Compose).

### Authentication

- `AppViewModel` calls `POST /api/auth/login` on launch with the demo account in
  `AppConfig` (`omar@example.com` / `Password123`) and stores the access and refresh tokens.
- Ktor's bearer `Auth` plugin attaches the access token to every payments request. On a `401` it calls
  `POST /api/auth/refresh` and retries. If the refresh token is rejected, it logs in again with the
  demo account. If that also fails, the tokens are cleared and the error reaches the screen.

### Payments API

| Call | Used by |
| --- | --- |
| `POST /api/payments` `{amount, currency, recipientEmail, description}` | New payment screen |
| `GET /api/payments?cursor=<nextCursor>` → `{items, nextCursor}` | History screen (newest first, paged) |

The history uses Paging 3 (`androidx.paging`, multiplatform): `PaymentsPagingSource` follows the server's
`nextCursor` (the server picks the page size), `PaymentsRepositoryImpl` exposes the `Pager` flow, and the
Payments screen reads it with `collectAsLazyPagingItems()`, so the next page loads as the list scrolls, with
loading, error and retry rows. A new payment invalidates the source, which reloads from the top.
The "Sent this month" card is computed from the pages loaded so far.

Amounts are stored as cents (`Amount`) and sent as a JSON number with two decimals (`30.00`).
Before sending, `PaymentValidator` checks the recipient email (required, valid, at most 254 chars),
the amount (required, numeric, greater than 0, at most 2 decimals, at most 12 integer digits),
the currency (JOD, USD, EUR, AED or EGP) and the description (at most 80 chars). Validation errors returned by the
backend (`{status, message, errors}`) are shown on the matching field.

## Requirements

- JDK 17+ (the project is built with JDK 21)
- Android SDK (compileSdk 37) and an emulator; set `sdk.dir` in `local.properties`
- Xcode 26+ for iOS
- The cashi-api backend (`~/SpringProjects/cashi-api`) running locally on port **8080**

The app reaches the backend at `http://10.0.2.2:8080` from the Android emulator and at
`http://localhost:8080` from the iOS simulator (see `Network.android.kt` / `Network.ios.kt`).
Cleartext HTTP is allowed only for those local hosts (`network_security_config.xml` on Android,
an ATS exception in `iosApp/iosApp/Info.plist` on iOS).

## Running the app

**Android**

```bash
./gradlew :androidApp:installDebug          # build and install on the running emulator
```

Or run the `androidApp` configuration from Android Studio.

**iOS**

Open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme on a simulator. The build phase
runs `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`. From the command line:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
```

## Tests

### Unit tests and BDD (shared module)

```bash
./gradlew :shared:jvmTest
```

This runs both suites on the JUnit Platform. Neither needs the backend.

- **Unit tests** (`*Test.kt`): JUnit 5 with Mockito. The repository, use case and view model tests
  mock their direct dependency (`PaymentsApi`, `PaymentsRepository`, `AuthRepository`, the use
  cases), so each layer is tested in isolation. Pure logic (amount parsing and serialization,
  payment validation, API error parsing, formatter, Koin graph) needs no mocks.
- **Integration-style tests**: `ApiResponseHandlingTest` and the BDD suite run the real client,
  repositories and use cases against an in-memory fake of cashi-api served through Ktor's
  `MockEngine` (`shared/src/jvmTest/.../testing/FakeBackend.kt`). They cover decoding, request
  bodies and headers, 4xx/5xx, malformed JSON, offline, token refresh and re-login.
- **BDD** (`src/jvmTest/resources/features`): `payment_creation.feature` and
  `transaction_history.feature`, with step definitions in `bdd/PaymentSteps.kt` and the runner in
  `bdd/CucumberTest.kt`.

Reports: `shared/build/reports/tests/jvmTest/index.html`.

### Appium UI tests (Android)

`uiTests` drives the real Android app on an emulator against the real backend:

- `SendPaymentTest`: sends a payment (email, amount, USD, description) and checks the confirmation
  snackbar and that the payment is at the top of the history. Also checks that an invalid form shows
  field errors and sends nothing.
- `TransactionHistoryTest`: creates payments through the backend API, pulls to refresh and checks
  that they are listed newest first with description, recipient, date, amount and status.

Elements are found by Compose test tags, which are exposed as Android resource ids
(`testTagsAsResourceId`). For example: `add_payment_button`, `recipient_email_input`, `amount_input`,
`currency_dropdown`, `currency_option_USD`, `send_button`, `payment_item`, `payment_item_email`.

One-time setup:

```bash
npm install -g appium
appium driver install uiautomator2
```

Run:

```bash
# 1. Start the backend on :8080 and boot an Android emulator
# 2. Start Appium (ANDROID_HOME must point at your SDK)
appium
# 3. In another terminal: build the APK and run the tests
./gradlew :androidApp:assembleDebug :uiTests:test
```

Options: `-PappiumUrl=http://127.0.0.1:4723`, `-Papk=/path/to/app.apk`,
`-PbackendUrl=http://localhost:8080`, `-Pudid=emulator-5554`. If no Appium server is reachable,
the UI tests are skipped instead of failing. Each test reinstalls the app with its data cleared. The
tests create real payments with unique `appium.*@example.com` recipients.
