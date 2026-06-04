package io.steadpay.core

data class SteadpayConfig(
    val apiBase: String,
    val tenantSlug: String,
    val customerId: String,
    val publishableKey: String,
    val pollIntervalMs: Long = 600_000L,
)
