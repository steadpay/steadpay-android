package io.steadpay.compose

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalContext
import io.steadpay.core.*

@Composable
fun SteadpayGate(
    tenantSlug: String,
    customerId: String,
    publishableKey: String,
    apiBase: String,
    pollIntervalMs: Long = 600_000L,
    callbacks: SteadpayCallbacks? = null,
    lockoutScreen: (@Composable (triggerCardUpdate: () -> Unit, entitlements: Entitlements?) -> Unit)? = null,
    warningBanner: (@Composable (triggerCardUpdate: () -> Unit, dismissWarning: () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    // Key on all config params so a rotated publishableKey or changed tenantSlug
    // disposes the stale controller and creates a fresh one.
    val controller = remember(tenantSlug, customerId, publishableKey, apiBase, pollIntervalMs) {
        SteadpayController(
            config = SteadpayConfig(
                apiBase = apiBase,
                tenantSlug = tenantSlug,
                customerId = customerId,
                publishableKey = publishableKey,
                pollIntervalMs = pollIntervalMs,
            ),
            callbacks = callbacks,
            urlLauncher = { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
        )
    }

    // dispose() cancels the coroutine scope; keys match remember() so the effect
    // re-runs (disposes old, starts new) whenever any config param changes.
    DisposableEffect(tenantSlug, customerId, publishableKey, apiBase, pollIntervalMs) {
        controller.start()
        onDispose { controller.dispose() }
    }

    val state by controller.stateFlow.collectAsState()
    val dismissed by controller.dismissedFlow.collectAsState()

    when (state.status) {
        SteadpayStatus.Lockout -> {
            if (lockoutScreen != null) {
                lockoutScreen(controller::triggerCardUpdate, state.entitlements)
            } else {
                LockoutScreen(
                    poweredByWatermark = state.entitlements?.poweredByWatermark ?: true,
                    entitlements = state.entitlements,
                    onTriggerCardUpdate = controller::triggerCardUpdate,
                )
            }
        }
        else -> {
            Column {
                if (state.status == SteadpayStatus.Warning && !dismissed) {
                    if (warningBanner != null) {
                        warningBanner(controller::triggerCardUpdate, controller::dismissWarning)
                    } else {
                        WarningBanner(
                            onTriggerCardUpdate = controller::triggerCardUpdate,
                            onDismiss = controller::dismissWarning,
                        )
                    }
                }
                content()
            }
        }
    }
}
