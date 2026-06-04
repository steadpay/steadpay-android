package io.steadpay.core

enum class CallbackName { OnLockout, OnWarning, OnActive, OnRecovered }

fun computeTransition(
    lastStatus: SteadpayStatus?,
    newStatus: SteadpayStatus,
    isRecoveryPath: Boolean,
): CallbackName? {
    if (lastStatus == newStatus) return null

    return when (newStatus) {
        SteadpayStatus.Lockout -> CallbackName.OnLockout
        SteadpayStatus.Warning -> if (lastStatus == null) null else CallbackName.OnWarning
        SteadpayStatus.Active -> when {
            lastStatus == null -> null
            lastStatus == SteadpayStatus.Lockout && isRecoveryPath -> CallbackName.OnRecovered
            else -> CallbackName.OnActive
        }
        SteadpayStatus.Loading, SteadpayStatus.Error -> null
    }
}
