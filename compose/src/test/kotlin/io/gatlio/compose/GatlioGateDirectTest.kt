package io.gatlio.compose

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.gatlio.core.GatlioStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GatlioGateDirectTest {

    @get:Rule val composeRule = createComposeRule()

    @Test fun rendersChildrenOnActive() {
        composeRule.setContent {
            GatlioGate(
                tenantSlug = "test", customerId = "cus_test",
                publishableKey = "pk_test", apiBase = "https://example.com", hmac = "test_hmac",
                forcedStatus = GatlioStatus.Active,
            ) { Text("protected content") }
        }
        composeRule.onNodeWithText("protected content").assertIsDisplayed()
        composeRule.onNodeWithText("Payment method declined").assertDoesNotExist()
    }

    @Test fun rendersLockoutScreenOnLockout() {
        composeRule.setContent {
            GatlioGate(
                tenantSlug = "test", customerId = "cus_test",
                publishableKey = "pk_test", apiBase = "https://example.com", hmac = "test_hmac",
                forcedStatus = GatlioStatus.Lockout,
            ) { Text("protected content") }
        }
        composeRule.onNodeWithText("Payment method declined").assertIsDisplayed()
        composeRule.onNodeWithText("protected content").assertDoesNotExist()
    }

    @Test fun rendersWarningBannerAndChildrenOnWarning() {
        composeRule.setContent {
            GatlioGate(
                tenantSlug = "test", customerId = "cus_test",
                publishableKey = "pk_test", apiBase = "https://example.com", hmac = "test_hmac",
                forcedStatus = GatlioStatus.Warning,
            ) { Text("protected content") }
        }
        composeRule.onNodeWithText("please ensure funds are available.", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("protected content").assertIsDisplayed()
    }

    @Test fun customLockoutScreenCalledOnLockout() {
        var builderCalled = false
        composeRule.setContent {
            GatlioGate(
                tenantSlug = "test", customerId = "cus_test",
                publishableKey = "pk_test", apiBase = "https://example.com", hmac = "test_hmac",
                forcedStatus = GatlioStatus.Lockout,
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
            GatlioGate(
                tenantSlug = "test", customerId = "cus_test",
                publishableKey = "pk_test", apiBase = "https://example.com", hmac = "test_hmac",
                forcedStatus = GatlioStatus.Warning,
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
