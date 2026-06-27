package io.gatlio.core

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertThrows

class GatlioConfigTest {

    private fun validConfig(pollIntervalMs: Long = 60_000L) = GatlioConfig(
        apiBase = "https://app.gatlio.io",
        tenantSlug = "acme",
        customerId = "cus_123",
        publishableKey = "pk_live_abc",
        hmac = "test_hmac",
        pollIntervalMs = pollIntervalMs,
    )

    @Test fun pollIntervalBelowMinimumThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            validConfig(pollIntervalMs = 59_999L)
        }
    }

    @Test fun pollIntervalAtMinimumIsAllowed() {
        val config = validConfig(pollIntervalMs = 60_000L)
        assertEquals(60_000L, config.pollIntervalMs)
    }

    @Test fun pollIntervalAboveMinimumIsAllowed() {
        val config = validConfig(pollIntervalMs = 600_000L)
        assertEquals(600_000L, config.pollIntervalMs)
    }
}
