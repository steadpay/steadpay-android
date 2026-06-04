package io.steadpay.core

data class SteadpayState(
    val status: SteadpayStatus = SteadpayStatus.Loading,
    val cardUpdateUrl: String? = null,
    val entitlements: Entitlements? = null,
)
