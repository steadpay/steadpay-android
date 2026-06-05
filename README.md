# steadpay-android

Android SDK for [Steadpay](https://steadpay.io) billing enforcement. Jetpack Compose composable that enforces subscriber billing states natively — no WebView, no third-party UI framework.

## Installation

Add the `:compose` module to your project (local path or published artifact):

```kotlin
// settings.gradle.kts
includeBuild("path/to/steadpay-android") {
    dependencySubstitution {
        substitute(module("io.steadpay:compose")).using(project(":compose"))
        substitute(module("io.steadpay:core")).using(project(":core"))
    }
}

// app/build.gradle.kts
dependencies {
    implementation("io.steadpay:compose")
}
```

## Quick start

Wrap the authenticated portion of your app in `SteadpayGate`:

```kotlin
import io.steadpay.compose.SteadpayGate

SteadpayGate(
    tenantSlug = "acme",
    customerId = currentUser.stripeCustomerId,
    publishableKey = "pk_live_abc123",
    apiBase = "https://app.steadpay.io",
) {
    YourApp()
}
```

When billing is current, `YourApp` renders normally. In `Warning` state a dismissable banner appears above it. In `Lockout` a full-screen overlay replaces all content until the card is updated.

## `SteadpayGate` — parameters

| Parameter | Type | Required | Default |
|-----------|------|----------|---------|
| `tenantSlug` | `String` | ✓ | — |
| `customerId` | `String` | ✓ | — |
| `publishableKey` | `String` | ✓ | — |
| `apiBase` | `String` | ✓ | — |
| `pollIntervalMs` | `Long` | | `600_000` |
| `forcedStatus` | `SteadpayStatus?` | | `null` |
| `callbacks` | `SteadpayCallbacks?` | | `null` |
| `lockoutScreen` | `@Composable (...)` | | built-in |
| `warningBanner` | `@Composable (...)` | | built-in |
| `content` | `@Composable () -> Unit` | ✓ | — |

## Callbacks

```kotlin
SteadpayGate(
    // ...
    callbacks = SteadpayCallbacks(
        onLockout = { println("locked out") },
        onWarning = { println("warning") },
        onActive = { println("active") },
        onRecovered = { println("recovered after card update") },
        onError = { err -> println("error: $err") },
    ),
) {
    YourApp()
}
```

Callbacks fire on status *transitions*, not on every poll tick.

## Custom enforcement UI

```kotlin
SteadpayGate(
    // ...
    lockoutScreen = { triggerCardUpdate, _ ->
        MyBrandedLockout(onUpdate = triggerCardUpdate)
    },
    warningBanner = { triggerCardUpdate, dismissWarning ->
        MyBrandedBanner(onUpdate = triggerCardUpdate, onDismiss = dismissWarning)
    },
) {
    YourApp()
}
```

## Testing your integration

### Force a state — `forcedStatus`

```kotlin
SteadpayGate(
    tenantSlug = "acme",
    customerId = "cus_test",
    publishableKey = "pk_test_abc",
    apiBase = "https://app.steadpay.io",
    forcedStatus = SteadpayStatus.Lockout,  // no network calls
) {
    YourApp()
}
```

Remove `forcedStatus` before shipping.

### Interactive harness — `SteadpaySandbox`

`SteadpaySandbox` is a drop-in dev composable that lets you switch billing states and verify your callbacks without a real Steadpay account.

**How it works:** your content renders at full size with a small `DEV` badge anchored to the bottom-right corner as a true overlay. Tap the badge to open a `ModalBottomSheet` control panel; tap a state pill to switch states; tap outside the sheet to dismiss.

```kotlin
import io.steadpay.compose.SteadpaySandbox

SteadpaySandbox(
    onLockout = { println("locked out") },
    onWarning = { println("warning") },
    onActive = { println("active") },
    onError = { err -> println("error: $err") },
) {
    YourApp()
}
```

The sandbox accepts custom `lockoutScreen` and `warningBanner` overrides:

```kotlin
SteadpaySandbox(
    lockoutScreen = { triggerCardUpdate, _ ->
        MyBrandedLockout(onUpdate = triggerCardUpdate)
    },
    warningBanner = { triggerCardUpdate, dismissWarning ->
        MyBrandedBanner(onUpdate = triggerCardUpdate, onDismiss = dismissWarning)
    },
) {
    YourApp()
}
```

**What the control sheet shows:**
- Four state pills (`Active`, `Warning`, `Lockout`, `Error`) — tap to transition
- Current-status indicator
- Callback log (last 5 invocations, newest first)
- `onRecovered` limitation note

**Callback rules:**

| Transition | Fires |
|-----------|-------|
| `Active → Warning` | `onWarning` |
| `Active → Lockout` | `onLockout` |
| `Warning → Lockout` | `onLockout` |
| `Lockout → Active` | `onActive` |
| `Warning → Active` | `onActive` |
| any → `Error` | `onError` (first press only) |
| same → same | nothing |

**`onRecovered` is not fired by the sandbox** — it requires the real card update flow. Test it against a live Steadpay environment.

Remove `SteadpaySandbox` before shipping to production.

## Direct controller usage

For custom state management (manual collect, non-Compose UI, etc.):

```kotlin
val controller = SteadpayController(
    config = SteadpayConfig(
        apiBase = "https://app.steadpay.io",
        tenantSlug = "acme",
        customerId = currentUser.stripeCustomerId,
        publishableKey = "pk_live_abc123",
    ),
)
controller.start()

// In a Compose context:
val state by controller.stateFlow.collectAsState()
val dismissed by controller.dismissedFlow.collectAsState()

// In a View-based context:
lifecycleScope.launch {
    controller.stateFlow.collect { state -> /* update UI */ }
}
```

Call `controller.dispose()` in `onDestroy` or when the controller is no longer needed.

## Minimum SDK

API 26 (Android 8.0).

## License

MIT
