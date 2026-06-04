package io.steadpay.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.steadpay.core.Entitlements
import io.steadpay.core.SteadpayStatus

@Composable
fun SteadpaySandbox(
    forcedStatus: SteadpayStatus,
    lockoutScreen: (@Composable (triggerCardUpdate: () -> Unit, entitlements: Entitlements?) -> Unit)? = null,
    warningBanner: (@Composable (triggerCardUpdate: () -> Unit, dismissWarning: () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    SteadpayGate(
        tenantSlug = "sandbox",
        customerId = "cus_sandbox",
        publishableKey = "pk_test_sandbox",
        apiBase = "https://example.com",
        lockoutScreen = lockoutScreen,
        warningBanner = warningBanner,
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
private fun LockoutPreview() {
    LockoutScreen(poweredByWatermark = true, entitlements = null, onTriggerCardUpdate = {})
}

@Preview(showBackground = true)
@Composable
private fun WarningPreview() {
    WarningBanner(onTriggerCardUpdate = {}, onDismiss = {})
}
