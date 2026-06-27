package io.gatlio.core

import app.cash.turbine.test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun config() = GatlioConfig(
    apiBase = "https://app.gatlio.io",
    tenantSlug = "acme",
    customerId = "cus_123",
    publishableKey = "pk_live_abc",
    hmac = "test_hmac",
)

private fun activeState() = GatlioState(
    status = GatlioStatus.Active,
    cardUpdateUrl = "https://app.gatlio.io/update-card",
    entitlements = Entitlements(poweredByWatermark = true, customDomain = false, downstreamWebhooks = false),
)

private fun mockFetch(status: GatlioStatus): FetchFn = { _, _, _, _, _ ->
    GatlioState(
        status = status,
        cardUpdateUrl = "https://app.gatlio.io/update-card",
        entitlements = Entitlements(poweredByWatermark = true, customDomain = false, downstreamWebhooks = false),
    )
}

class GatlioControllerTest {

    @Test fun initialStateIsLoading() {
        val controller = GatlioController(config(), fetch = mockFetch(GatlioStatus.Active))
        assertEquals(GatlioStatus.Loading, controller.stateFlow.value.status)
        controller.dispose()
    }

    @Test fun initialDismissedIsFalse() {
        val controller = GatlioController(config(), fetch = mockFetch(GatlioStatus.Active))
        assertFalse(controller.dismissedFlow.value)
        controller.dispose()
    }

    @Test fun forcedStatusEmitsImmediately() {
        val controller = GatlioController(
            config(),
            forcedStatus = GatlioStatus.Lockout,
            fetch = mockFetch(GatlioStatus.Active),
        )
        controller.start()
        assertEquals(GatlioStatus.Lockout, controller.stateFlow.value.status)
        controller.dispose()
    }

    @Test fun forcedStatusDoesNotCallFetch() {
        var fetchCalled = false
        val controller = GatlioController(
            config(),
            forcedStatus = GatlioStatus.Warning,
            fetch = { _, _, _, _, _ ->
                fetchCalled = true
                activeState()
            },
        )
        controller.start()
        assertFalse(fetchCalled)
        controller.dispose()
    }

    @Test fun startEmitsCorrectStatus() = runTest {
        val controller = GatlioController(
            config(),
            fetch = mockFetch(GatlioStatus.Active),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        controller.stateFlow.test {
            skipItems(1) // initial Loading
            controller.start()
            val state = awaitItem()
            assertEquals(GatlioStatus.Active, state.status)
            cancelAndIgnoreRemainingEvents()
        }
        controller.dispose()
    }

    @Test fun dismissWarningEmitsTrueOnDismissedFlow() = runTest {
        val controller = GatlioController(config(), fetch = mockFetch(GatlioStatus.Warning))
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
        val controller = GatlioController(
            config(),
            forcedStatus = GatlioStatus.Lockout,
            urlLauncher = { launchedUrl = it },
            fetch = mockFetch(GatlioStatus.Active),
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
        val controller = GatlioController(
            config(pollIntervalMs = 600_000L),
            fetch = { _, _, _, _, _ ->
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

    @Test fun cancellationExceptionFromFetchDoesNotFireOnError() = runTest {
        var errorFired = false
        val callbacks = GatlioCallbacks(onError = { errorFired = true })
        val controller = GatlioController(
            config(),
            callbacks = callbacks,
            fetch = { _, _, _, _, _ -> throw CancellationException("test cancellation") },
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        controller.start()
        advanceUntilIdle()
        assertFalse(errorFired)
        controller.dispose()
    }

@Test fun onErrorCallbackFiredOnFetchFailure() = runTest {
        var capturedError: Throwable? = null
        val callbacks = GatlioCallbacks(onError = { capturedError = it })
        val controller = GatlioController(
            config(),
            callbacks = callbacks,
            fetch = { _, _, _, _, _ -> throw GatlioApiError("unauthorized") },
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        controller.stateFlow.test {
            skipItems(1) // initial Loading
            controller.start()
            val state = awaitItem()
            assertEquals(GatlioStatus.Error, state.status)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(capturedError is GatlioApiError)
        controller.dispose()
    }
}

private fun config(pollIntervalMs: Long = 600_000L) = GatlioConfig(
    apiBase = "https://app.gatlio.io",
    tenantSlug = "acme",
    customerId = "cus_123",
    publishableKey = "pk_live_abc",
    hmac = "test_hmac",
    pollIntervalMs = pollIntervalMs,
)
