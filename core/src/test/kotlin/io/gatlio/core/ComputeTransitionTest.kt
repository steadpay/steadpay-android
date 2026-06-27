package io.gatlio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComputeTransitionTest {

    // null → status (initial load)

    @Test fun nullToLockoutFiresOnLockout() {
        assertEquals(CallbackName.OnLockout, computeTransition(null, GatlioStatus.Lockout, false))
    }

    @Test fun nullToWarningSuppressed() {
        assertNull(computeTransition(null, GatlioStatus.Warning, false))
    }

    @Test fun nullToActiveSuppressed() {
        assertNull(computeTransition(null, GatlioStatus.Active, false))
    }

    // transitions to lockout

    @Test fun warningToLockoutFiresOnLockout() {
        assertEquals(CallbackName.OnLockout, computeTransition(GatlioStatus.Warning, GatlioStatus.Lockout, false))
    }

    @Test fun activeToLockoutFiresOnLockout() {
        assertEquals(CallbackName.OnLockout, computeTransition(GatlioStatus.Active, GatlioStatus.Lockout, false))
    }

    // transitions to warning

    @Test fun activeToWarningFiresOnWarning() {
        assertEquals(CallbackName.OnWarning, computeTransition(GatlioStatus.Active, GatlioStatus.Warning, false))
    }

    @Test fun lockoutToWarningFiresOnWarning() {
        assertEquals(CallbackName.OnWarning, computeTransition(GatlioStatus.Lockout, GatlioStatus.Warning, false))
    }

    // transitions to active

    @Test fun lockoutToActiveFiresOnActive() {
        assertEquals(CallbackName.OnActive, computeTransition(GatlioStatus.Lockout, GatlioStatus.Active, false))
    }

    @Test fun lockoutToActiveOnRecoveryFiresOnRecovered() {
        assertEquals(CallbackName.OnRecovered, computeTransition(GatlioStatus.Lockout, GatlioStatus.Active, true))
    }

    @Test fun warningToActiveFiresOnActive() {
        assertEquals(CallbackName.OnActive, computeTransition(GatlioStatus.Warning, GatlioStatus.Active, false))
    }

    @Test fun warningToActiveOnRecoveryFiresOnActive() {
        assertEquals(CallbackName.OnActive, computeTransition(GatlioStatus.Warning, GatlioStatus.Active, true))
    }

    // same → same (no transition)

    @Test fun activeToActiveSuppressed() {
        assertNull(computeTransition(GatlioStatus.Active, GatlioStatus.Active, false))
    }

    @Test fun warningToWarningSuppressed() {
        assertNull(computeTransition(GatlioStatus.Warning, GatlioStatus.Warning, false))
    }

    @Test fun lockoutToLockoutSuppressed() {
        assertNull(computeTransition(GatlioStatus.Lockout, GatlioStatus.Lockout, false))
    }

    // non-billing statuses as newStatus

    @Test fun activeToLoadingSuppressed() {
        assertNull(computeTransition(GatlioStatus.Active, GatlioStatus.Loading, false))
    }

    @Test fun activeToErrorSuppressed() {
        assertNull(computeTransition(GatlioStatus.Active, GatlioStatus.Error, false))
    }
}
