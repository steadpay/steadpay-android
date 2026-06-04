package io.steadpay.compose

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalContext
import io.steadpay.core.*
import kotlinx.coroutines.flow.collectLatest

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

    val controller = remember(customerId) {
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

    DisposableEffect(customerId) {
        controller.start()
        onDispose { controller.stop() }
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
