package io.steadpay.core

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun config() = SteadpayConfig(
    apiBase = "https://app.steadpay.io",
    tenantSlug = "acme",
    customerId = "cus_123",
    publishableKey = "pk_live_abc",
)

private fun activeState() = SteadpayState(
    status = SteadpayStatus.Active,
    cardUpdateUrl = "https://app.steadpay.io/update-card",
    entitlements = Entitlements(poweredByWatermark = true, customDomain = false, downstreamWebhooks = false),
)

private fun mockFetch(status: SteadpayStatus): FetchFn = { _, _, _, _ ->
    SteadpayState(
        status = status,
        cardUpdateUrl = "https://app.steadpay.io/update-card",
        entitlements = Entitlements(poweredByWatermark = true, customDomain = false, downstreamWebhooks = false),
    )
}

class SteadpayControllerTest {

    @Test fun initialStateIsLoading() {
        val controller = SteadpayController(config(), fetch = mockFetch(SteadpayStatus.Active))
        assertEquals(SteadpayStatus.Loading, controller.stateFlow.value.status)
        controller.dispose()
    }

    @Test fun initialDismissedIsFalse() {
        val controller = SteadpayController(config(), fetch = mockFetch(SteadpayStatus.Active))
        assertFalse(controller.dismissedFlow.value)
        controller.dispose()
    }

    @Test fun forcedStatusEmitsImmediately() {
        val controller = SteadpayController(
            config(),
            forcedStatus = SteadpayStatus.Lockout,
            fetch = mockFetch(SteadpayStatus.Active),
        )
        controller.start()
        assertEquals(SteadpayStatus.Lockout, controller.stateFlow.value.status)
        controller.dispose()
    }

    @Test fun forcedStatusDoesNotCallFetch() {
        var fetchCalled = false
        val controller = SteadpayController(
            config(),
            forcedStatus = SteadpayStatus.Warning,
            fetch = { _, _, _, _ ->
                fetchCalled = true
                activeState()
            },
        )
        controller.start()
        assertFalse(fetchCalled)
        controller.dispose()
    }

    @Test fun startEmitsCorrectStatus() = runTest {
        val controller = SteadpayController(config(), fetch = mockFetch(SteadpayStatus.Active))
        controller.stateFlow.test {
            controller.start()
            val state = awaitItem()
            assertEquals(SteadpayStatus.Active, state.status)
            cancelAndIgnoreRemainingEvents()
        }
        controller.dispose()
    }

    @Test fun dismissWarningEmitsTrueOnDismissedFlow() = runTest {
        val controller = SteadpayController(config(), fetch = mockFetch(SteadpayStatus.Warning))
        controller.dismissedFlow.test {
            skipItems(1) // initial false
            controller.dismissWarning()
            val dismissed = awaitItem()
            assertTrue(dismissed)
            cancelAndIgnoreRemainingEvents()
        }
        controller.dispose()
    }

    @Test fun triggerCardUpdateResetsDismissedToFalse() = runTest {
        var launchedUrl: String? = null
        val controller = SteadpayController(
            config(),
            forcedStatus = SteadpayStatus.Lockout,
            urlLauncher = { launchedUrl = it },
            fetch = mockFetch(SteadpayStatus.Active),
        )
        controller.start()
        controller.dismissWarning()
        assertTrue(controller.dismissedFlow.value)

        controller.dismissedFlow.test {
            skipItems(1) // current true value
            controller.triggerCardUpdate()
            val dismissed = awaitItem()
            assertFalse(dismissed)
            cancelAndIgnoreRemainingEvents()
        }
        assertNotNull(launchedUrl)
        controller.dispose()
    }

    @Test fun stopPreventsAdditionalPolls() = runTest {
        var callCount = 0
        val controller = SteadpayController(
            config(pollIntervalMs = 600_000L),
            fetch = { _, _, _, _ ->
                callCount++
                activeState()
            },
        )
        controller.start()
        controller.stop()
        val countAfterStop = callCount
        // Wait a bit and confirm no additional polls fired
        kotlinx.coroutines.delay(50)
        assertEquals(countAfterStop, callCount)
        controller.dispose()
    }

    @Test fun onErrorCallbackFiredOnFetchFailure() = runTest {
        var capturedError: Throwable? = null
        val callbacks = SteadpayCallbacks(onError = { capturedError = it })
        val controller = SteadpayController(
            config(),
            callbacks = callbacks,
            fetch = { _, _, _, _ -> throw SteadpayApiError("unauthorized") },
        )
        controller.stateFlow.test {
            controller.start()
            val state = awaitItem()
            assertEquals(SteadpayStatus.Error, state.status)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(capturedError is SteadpayApiError)
        controller.dispose()
    }
}

private fun config(pollIntervalMs: Long = 600_000L) = SteadpayConfig(
    apiBase = "https://app.steadpay.io",
    tenantSlug = "acme",
    customerId = "cus_123",
    publishableKey = "pk_live_abc",
    pollIntervalMs = pollIntervalMs,
)
