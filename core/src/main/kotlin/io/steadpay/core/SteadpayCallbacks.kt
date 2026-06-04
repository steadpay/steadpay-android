package io.steadpay.core

data class SteadpayCallbacks(
    val onLockout: ((String) -> Unit)? = null,
    val onWarning: ((String) -> Unit)? = null,
    val onActive: ((String) -> Unit)? = null,
    val onRecovered: ((String) -> Unit)? = null,
    val onError: ((Throwable) -> Unit)? = null,
)
