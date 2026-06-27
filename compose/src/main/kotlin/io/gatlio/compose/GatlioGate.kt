package io.gatlio.compose

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalContext
import io.gatlio.core.*

@Composable
fun GatlioGate(
    tenantSlug: String,
    customerId: String,
    publishableKey: String,
    apiBase: String,
    hmac: String,
    pollIntervalMs: Long = 600_000L,
    forcedStatus: GatlioStatus? = null,
    callbacks: GatlioCallbacks? = null,
    /** Override the language for enforcement copy. Defaults to the device locale. */
    locale: String? = null,
    lockoutScreen: (@Composable (triggerCardUpdate: () -> Unit, entitlements: Entitlements?, message: String, cta: String) -> Unit)? = null,
    warningBanner: (@Composable (dismissWarning: () -> Unit, message: String) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val resolvedLocale = resolveLocale(locale ?: java.util.Locale.getDefault().language)

    // Key on all config params so a rotated publishableKey or changed tenantSlug
    // disposes the stale controller and creates a fresh one.
    val controller = remember(tenantSlug, customerId, publishableKey, apiBase, hmac, pollIntervalMs, forcedStatus) {
        GatlioController(
            config = GatlioConfig(
                apiBase = apiBase,
                tenantSlug = tenantSlug,
                customerId = customerId,
                publishableKey = publishableKey,
                hmac = hmac,
                pollIntervalMs = pollIntervalMs,
            ),
            forcedStatus = forcedStatus,
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
    DisposableEffect(tenantSlug, customerId, publishableKey, apiBase, hmac, pollIntervalMs, forcedStatus) {
        controller.start()
        onDispose { controller.dispose() }
    }

    val state by controller.stateFlow.collectAsState()
    val dismissed by controller.dismissedFlow.collectAsState()

    when (state.status) {
        GatlioStatus.Lockout -> {
            val copy = lockoutCopy(state.enforcementContext, resolvedLocale)
            if (lockoutScreen != null) {
                lockoutScreen(controller::triggerCardUpdate, state.entitlements, copy.message, copy.cta ?: "")
            } else {
                LockoutScreen(
                    poweredByWatermark = state.entitlements?.poweredByWatermark ?: true,
                    entitlements = state.entitlements,
                    message = copy.message,
                    cta = copy.cta ?: "",
                    onTriggerCardUpdate = controller::triggerCardUpdate,
                )
            }
        }
        else -> {
            Column {
                if (state.status == GatlioStatus.Warning && !dismissed) {
                    val message = warningCopy(state.enforcementContext, resolvedLocale).message
                    if (warningBanner != null) {
                        warningBanner(controller::dismissWarning, message)
                    } else {
                        WarningBanner(
                            message = message,
                            onDismiss = controller::dismissWarning,
                        )
                    }
                }
                content()
            }
        }
    }
}
