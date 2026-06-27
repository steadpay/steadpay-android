# Arcta Example — gatlio-android

A fictional analytics SaaS app that demonstrates a complete `gatlio-android` integration. Arcta is used as the host app throughout Gatlio's example suite so you can compare SDK behaviour across platforms side by side.

## Screens

| Screen | File | Purpose |
|--------|------|---------|
| Login | `app/src/main/kotlin/io/arcta/example/LoginScreen.kt` | Fake login form — any credentials navigate to Home |
| Home | `app/src/main/kotlin/io/arcta/example/HomeScreen.kt` | Main content wrapped in `GatlioSandbox` |
| Settings | `app/src/main/kotlin/io/arcta/example/SettingsScreen.kt` | Static account info screen |
| Content | `app/src/main/kotlin/io/arcta/example/ArctaContent.kt` | Fake analytics dashboard — the "protected" content |

`GatlioSandbox` lives in `HomeScreen.kt`. It wraps `ArctaContent` and surfaces a `DEV` badge in the bottom-right corner of the screen.

## Running

### Prerequisites

- Android Studio (Hedgehog or later)
- JDK 17
- An emulator or physical device (API 26+)

The example references the local `:compose` and `:core` modules — no extra configuration needed when opened from Android Studio.

### Open in Android Studio

```
File → Open → select steadpay-android/ (the repo folder is intentionally kept as steadpay-android on disk)
```

Android Studio detects both the library modules and the example app. Select the **app** run configuration, choose your emulator or device, and press **▶**.

### Command line

From the repo root (requires the `gradle` CLI — no wrapper is checked in):

```sh
gradle :example:app:installDebug
```

Or build only (no device needed):

```sh
gradle :example:app:assembleDebug
```

### Physical device

Enable Developer Options (Settings → About Phone → tap Build Number 7×), enable USB Debugging, plug in, then confirm the device is visible:

```sh
adb devices
```

Then run `gradle :example:app:installDebug`.

> Physical devices and the Android Emulator cannot reach `localhost` on the host machine directly. Use `10.0.2.2` for the emulator, or ngrok (`ngrok http 3000`) for physical devices, and enter that URL in Live mode.

## Testing with GatlioSandbox

### Sandbox mode (no server needed)

1. Sign in with any credentials and navigate to Home.
2. Tap the **DEV** badge in the bottom-right corner.
3. A bottom sheet slides up showing four state pills: `active`, `warning`, `lockout`, `error`.
4. Tap any pill — the UI transitions immediately. No network calls, no rebuild.

What to verify in each state:

| State | Expected behaviour |
|-------|--------------------|
| `active` | Content renders normally; no banner, no gate |
| `warning` | Amber banner appears above content; tap **Dismiss** to hide for the session |
| `lockout` | Full-screen gate covers all content; nothing behind it is tappable |
| `error` | Content still renders (fail open); no crash |

The sheet also shows a **callback log** (last 5 invocations). Confirm `onLockout`, `onWarning`, and `onActive` fire on the correct transitions without adding `Log.d` calls.

### Compose Previews — instant visual iteration

Every composable ships with `@Preview` annotations for each state. Open any screen file in Android Studio and switch to the **Design** or **Split** panel to see all states without launching an emulator.

### Sandbox mode — what fires and what doesn't

| Transition | Callback fired |
|-----------|---------------|
| `active → warning` | `onWarning` |
| `active → lockout` | `onLockout` |
| `warning → lockout` | `onLockout` |
| `lockout → active` | `onActive` |
| `warning → active` | `onActive` |
| any → `error` | `onError` |
| same → same | nothing |

`onRecovered` is **not** fired by the sandbox — it requires the real card update flow. Test it in Live mode with a Stripe test card.

### Live mode (real Gatlio instance)

1. Start Gatlio locally (`npm run dev`) and expose it via ngrok (physical device) or use `http://10.0.2.2:3000` (emulator).
2. Run `npm run seed` to create the `test-harness` tenant and seeded subscribers.
3. In the example app, tap the **DEV** badge and switch to **Live** mode.
4. Enter:
   - **API base**: ngrok URL or `http://10.0.2.2:3000`
   - **Tenant slug**: `test-harness`
   - **Publishable key**: from the seed script output
   - **Customer ID**: one of `cus_harness_active`, `cus_harness_warning`, `cus_harness_lockout`
5. Tap **Connect** — the SDK polls the real status API.

To force a state transition mid-test, update the subscriber in Postgres:

```sql
UPDATE subscribers SET status = 'lockout'
WHERE stripe_customer_id = 'cus_harness_warning';
```

Background the app and foreground it — `DefaultLifecycleObserver.onStart` fires a poll and the UI updates via `StateFlow`.

## Integration reference

```kotlin
// HomeScreen.kt
import io.gatlio.compose.GatlioSandbox

GatlioSandbox(
    onLockout = {},
    onWarning = {},
    onActive = {},
) {
    ArctaContent()
}
```

For production, replace `GatlioSandbox` with `GatlioGate`:

```kotlin
import io.gatlio.compose.GatlioGate

GatlioGate(
    apiBase = "https://api.gatlio.io",
    tenantSlug = "your-slug",
    publishableKey = "pk_live_xxx",
    customerId = currentUser.stripeCustomerId,
    hmac = currentUser.gatlioHmac, // HMAC-SHA256(identity_hmac_secret, stripe_customer_id) — computed server-side
    onLockout = { analytics.track("billing_lockout") },
    onRecovered = { analytics.track("billing_recovered") },
) {
    YourApp()
}
```

View-based (non-Compose) apps use `GatlioViewModel` from the `:core` module directly and collect `StateFlow` in `lifecycleScope` — see the main SDK README for the XML/View integration path.
