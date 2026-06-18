package io.steadpay.compose

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.steadpay.core.SteadpayStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SteadpayGateDirectTest {

    @get:Rule val composeRule = createComposeRule()

    @Test fun rendersChildrenOnActive() {
        composeRule.setContent {
            SteadpayGate(
                tenantSlug = "test", customerId = "cus_test",
                publishableKey = "pk_test", apiBase = "https://example.com",
                forcedStatus = SteadpayStatus.Active,
            ) { Text("protected content") }
        }
        composeRule.onNodeWithText("protected content").assertIsDisplayed()
        composeRule.onNodeWithText("Payment method declined").assertDoesNotExist()
    }

    @Test fun rendersLockoutScreenOnLockout() {
        composeRule.setContent {
            SteadpayGate(
                tenantSlug = "test", customerId = "cus_test",
                publishableKey = "pk_test", apiBase = "https://example.com",
                forcedStatus = SteadpayStatus.Lockout,
            ) { Text("protected content") }
        }
        composeRule.onNodeWithText("Payment method declined").assertIsDisplayed()
        composeRule.onNodeWithText("protected content").assertDoesNotExist()
    }

    @Test fun rendersWarningBannerAndChildrenOnWarning() {
        composeRule.setContent {
            SteadpayGate(
                tenantSlug = "test", customerId = "cus_test",
                publishableKey = "pk_test", apiBase = "https://example.com",
                forcedStatus = SteadpayStatus.Warning,
            ) { Text("protected content") }
        }
        composeRule.onNodeWithText("Please ensure sufficient funds are available.", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("protected content").assertIsDisplayed()
    }

    @Test fun customLockoutScreenCalledOnLockout() {
        var builderCalled = false
        composeRule.setContent {
            SteadpayGate(
                tenantSlug = "test", customerId = "cus_test",
                publishableKey = "pk_test", apiBase = "https://example.com",
                forcedStatus = SteadpayStatus.Lockout,
                lockoutScreen = { _, _, _, _ ->
                    builderCalled = true
                    Text("custom lockout")
                },
            ) { Text("protected content") }
        }
        assert(builderCalled)
        composeRule.onNodeWithText("custom lockout").assertIsDisplayed()
    }

    @Test fun customWarningBannerCalledOnWarning() {
        var builderCalled = false
        composeRule.setContent {
            SteadpayGate(
                tenantSlug = "test", customerId = "cus_test",
                publishableKey = "pk_test", apiBase = "https://example.com",
                forcedStatus = SteadpayStatus.Warning,
                warningBanner = { _, _ ->
                    builderCalled = true
                    Text("custom banner")
                },
            ) { Text("protected content") }
        }
        assert(builderCalled)
        composeRule.onNodeWithText("custom banner").assertIsDisplayed()
    }
}
