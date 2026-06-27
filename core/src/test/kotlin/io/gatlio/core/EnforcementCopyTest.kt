package io.gatlio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnforcementCopyTest {
    private val iso = "2026-06-20T12:00:00Z"
    private val d = "June 20, 2026"

    private fun ctx(
        declineCategory: String? = null,
        nextRetryAt: String? = null,
        isFinalRetry: Boolean = false,
        lockoutReason: String? = null,
    ) = EnforcementContext(declineCategory, nextRetryAt, isFinalRetry, lockoutReason)

    @Test fun resolveLocaleAcceptsSupportedAndTags() {
        assertEquals("fr", resolveLocale("fr"))
        assertEquals("es", resolveLocale("es-ES"))
        assertEquals("de", resolveLocale("de_DE"))
        assertEquals("en", resolveLocale("jp"))
        assertEquals("en", resolveLocale(null))
    }

    @Test fun formatRetryDatePerLocale() {
        assertEquals(d, formatRetryDate(iso, "en"))
        assertEquals("20 juin 2026", formatRetryDate(iso, "fr"))
        assertEquals("20 de junio de 2026", formatRetryDate(iso, "es"))
        assertEquals("20. Juni 2026", formatRetryDate(iso, "de"))
    }

    @Test fun formatRetryDateEmptyOnInvalid() {
        assertEquals("", formatRetryDate(null, "en"))
        assertEquals("", formatRetryDate("nope", "en"))
    }

    @Test fun warningCopyHasNoCta() {
        assertNull(warningCopy(ctx(declineCategory = "insufficient_funds", nextRetryAt = iso), "en").cta)
    }

    @Test fun warningCopyVariantsEn() {
        assertEquals(
            "Your payment failed. We'll retry on $d — please ensure funds are available.",
            warningCopy(ctx(declineCategory = "insufficient_funds", nextRetryAt = iso), "en").message)
        assertEquals(
            "Your payment failed. Final retry on $d — add funds or your access will be restricted.",
            warningCopy(ctx(declineCategory = "insufficient_funds", nextRetryAt = iso, isFinalRetry = true), "en").message)
        assertEquals(
            "Your payment was held by your bank. We'll retry on $d — you may want to contact them.",
            warningCopy(ctx(declineCategory = "bank_hold", nextRetryAt = iso), "en").message)
        assertEquals(
            "Your payment failed due to a temporary issue. We'll retry on $d.",
            warningCopy(ctx(declineCategory = "processing_error", nextRetryAt = iso), "en").message)
        assertEquals(
            "Your payment failed. We'll retry on $d, but your saved card may need updating.",
            warningCopy(ctx(declineCategory = "card_issue", nextRetryAt = iso), "en").message)
    }

    @Test fun warningCopyFallback() {
        assertEquals(
            "Your payment failed. We'll retry automatically — please keep your payment method up to date.",
            warningCopy(ctx(declineCategory = null), "en").message)
    }

    @Test fun warningCopyLocalized() {
        assertTrue(
            warningCopy(ctx(declineCategory = "insufficient_funds", nextRetryAt = iso, isFinalRetry = true), "fr")
                .message.contains("Dernier essai"))
    }

    @Test fun lockoutCopyEn() {
        val c1 = lockoutCopy(ctx(lockoutReason = "hard_decline", declineCategory = "card_issue"), "en")
        assertEquals("Your payment method needs to be updated to restore access.", c1.message)
        assertEquals("Update card", c1.cta)

        assertEquals(
            "Your payment was declined by your bank. Please update your payment method or contact your bank.",
            lockoutCopy(ctx(lockoutReason = "hard_decline", declineCategory = "bank_hold"), "en").message)
        assertEquals(
            "We were unable to process your payment after multiple attempts. Please add funds or update your payment method.",
            lockoutCopy(ctx(lockoutReason = "retry_exhausted", declineCategory = "insufficient_funds"), "en").message)
        assertEquals(
            "We were unable to process your payment after multiple attempts. Please update your payment method or contact your bank.",
            lockoutCopy(ctx(lockoutReason = "retry_exhausted", declineCategory = "bank_hold"), "en").message)
    }

    @Test fun lockoutCtaLocalized() {
        assertEquals(
            "Karte aktualisieren",
            lockoutCopy(ctx(lockoutReason = "hard_decline", declineCategory = "card_issue"), "de").cta)
    }
}
