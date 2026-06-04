package io.steadpay.core

data class Entitlements(
    val poweredByWatermark: Boolean,
    val customDomain: Boolean,
    val downstreamWebhooks: Boolean,
)
