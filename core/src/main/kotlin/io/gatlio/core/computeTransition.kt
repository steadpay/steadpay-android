package io.gatlio.core

enum class CallbackName { OnLockout, OnWarning, OnActive, OnRecovered }

fun computeTransition(
    lastStatus: GatlioStatus?,
    newStatus: GatlioStatus,
    isRecoveryPath: Boolean,
): CallbackName? {
    if (lastStatus == newStatus) return null

    return when (newStatus) {
        GatlioStatus.Lockout -> CallbackName.OnLockout
        GatlioStatus.Warning -> if (lastStatus == null) null else CallbackName.OnWarning
        GatlioStatus.Active -> when {
            lastStatus == null -> null
            lastStatus == GatlioStatus.Lockout && isRecoveryPath -> CallbackName.OnRecovered
            else -> CallbackName.OnActive
        }
        GatlioStatus.Loading, GatlioStatus.Error -> null
    }
}
