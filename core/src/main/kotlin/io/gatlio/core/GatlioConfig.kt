package io.gatlio.core

data class GatlioConfig(
    val apiBase: String,
    val tenantSlug: String,
    val customerId: String,
    val publishableKey: String,
    val hmac: String,
    val pollIntervalMs: Long = 600_000L,
) {
    init {
        require(apiBase.startsWith("https://")) { "apiBase must start with https://" }
        require(tenantSlug.isNotEmpty()) { "tenantSlug must not be empty" }
        require(customerId.isNotEmpty()) { "customerId must not be empty" }
        require(publishableKey.startsWith("pk_")) { "publishableKey must start with pk_" }
        require(hmac.isNotEmpty()) { "hmac must not be empty" }
        require(pollIntervalMs >= 60_000L) { "pollIntervalMs must be ≥ 60 000 ms" }
    }
}
