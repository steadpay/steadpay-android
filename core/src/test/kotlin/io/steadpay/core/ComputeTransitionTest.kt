package io.steadpay.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComputeTransitionTest {

    // null → status (initial load)

    @Test fun nullToLockoutFiresOnLockout() {
        assertEquals(CallbackName.OnLockout, computeTransition(null, SteadpayStatus.Lockout, false))
    }

    @Test fun nullToWarningSuppressed() {
        assertNull(computeTransition(null, SteadpayStatus.Warning, false))
    }

    @Test fun nullToActiveSuppressed() {
        assertNull(computeTransition(null, SteadpayStatus.Active, false))
    }

    // transitions to lockout

    @Test fun warningToLockoutFiresOnLockout() {
        assertEquals(CallbackName.OnLockout, computeTransition(SteadpayStatus.Warning, SteadpayStatus.Lockout, false))
    }

    @Test fun activeToLockoutFiresOnLockout() {
        assertEquals(CallbackName.OnLockout, computeTransition(SteadpayStatus.Active, SteadpayStatus.Lockout, false))
    }

    // transitions to warning

    @Test fun activeToWarningFiresOnWarning() {
        assertEquals(CallbackName.OnWarning, computeTransition(SteadpayStatus.Active, SteadpayStatus.Warning, false))
    }

    @Test fun lockoutToWarningFiresOnWarning() {
        assertEquals(CallbackName.OnWarning, computeTransition(SteadpayStatus.Lockout, SteadpayStatus.Warning, false))
    }

    // transitions to active

    @Test fun lockoutToActiveFiresOnActive() {
        assertEquals(CallbackName.OnActive, computeTransition(SteadpayStatus.Lockout, SteadpayStatus.Active, false))
    }

    @Test fun lockoutToActiveOnRecoveryFiresOnRecovered() {
        assertEquals(CallbackName.OnRecovered, computeTransition(SteadpayStatus.Lockout, SteadpayStatus.Active, true))
    }

    @Test fun warningToActiveFiresOnActive() {
        assertEquals(CallbackName.OnActive, computeTransition(SteadpayStatus.Warning, SteadpayStatus.Active, false))
    }

    @Test fun warningToActiveOnRecoveryFiresOnActive() {
        assertEquals(CallbackName.OnActive, computeTransition(SteadpayStatus.Warning, SteadpayStatus.Active, true))
    }

    // same → same (no transition)

    @Test fun activeToActiveSuppressed() {
        assertNull(computeTransition(SteadpayStatus.Active, SteadpayStatus.Active, false))
    }

    @Test fun warningToWarningSuppressed() {
        assertNull(computeTransition(SteadpayStatus.Warning, SteadpayStatus.Warning, false))
    }

    @Test fun lockoutToLockoutSuppressed() {
        assertNull(computeTransition(SteadpayStatus.Lockout, SteadpayStatus.Lockout, false))
    }

    // non-billing statuses as newStatus

    @Test fun activeToLoadingSuppressed() {
        assertNull(computeTransition(SteadpayStatus.Active, SteadpayStatus.Loading, false))
    }

    @Test fun activeToErrorSuppressed() {
        assertNull(computeTransition(SteadpayStatus.Active, SteadpayStatus.Error, false))
    }
}
