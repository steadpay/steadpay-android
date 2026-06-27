package io.gatlio.core

data class GatlioState(
    val status: GatlioStatus = GatlioStatus.Loading,
    val cardUpdateUrl: String? = null,
    val entitlements: Entitlements? = null,
    // Context-aware copy fields (#041). Null/false when there is no active failure.
    val declineCategory: String? = null,
    val nextRetryAt: String? = null,
    val isFinalRetry: Boolean = false,
    val lockoutReason: String? = null,
) {
    /** Context signals for [warningCopy] / [lockoutCopy]. */
    val enforcementContext: EnforcementContext
        get() = EnforcementContext(declineCategory, nextRetryAt, isFinalRetry, lockoutReason)
}
