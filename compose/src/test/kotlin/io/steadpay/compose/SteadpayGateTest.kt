package io.steadpay.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.steadpay.core.Entitlements
import io.steadpay.core.SteadpayStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SteadpayGateTest {

    @get:Rule val composeRule = createComposeRule()

    @Test fun rendersChildrenOnActive() {
        composeRule.setContent {
            SteadpaySandbox(forcedStatus = SteadpayStatus.Active) {
                Text("protected content")
            }
        }
        composeRule.onNodeWithText("protected content").assertIsDisplayed()
        composeRule.onNodeWithText("Payment method declined").assertDoesNotExist()
    }

    @Test fun rendersLockoutScreenOnLockout() {
        composeRule.setContent {
            SteadpaySandbox(forcedStatus = SteadpayStatus.Lockout) {
                Text("protected content")
            }
        }
        composeRule.onNodeWithText("Payment method declined").assertIsDisplayed()
        composeRule.onNodeWithText("protected content").assertDoesNotExist()
    }

    @Test fun rendersWarningBannerAndChildrenOnWarning() {
        composeRule.setContent {
            SteadpaySandbox(forcedStatus = SteadpayStatus.Warning) {
                Text("protected content")
            }
        }
        composeRule.onNodeWithText("Please update your payment method to avoid interruption.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("protected content").assertIsDisplayed()
    }

    @Test fun customLockoutScreenBuilderCalledOnLockout() {
        var builderCalled = false
        composeRule.setContent {
            SteadpaySandbox(
                forcedStatus = SteadpayStatus.Lockout,
                lockoutScreen = { _, _ ->
                    builderCalled = true
                    Text("custom lockout")
                },
            ) {
                Text("protected content")
            }
        }
        assert(builderCalled)
        composeRule.onNodeWithText("custom lockout").assertIsDisplayed()
    }

    @Test fun customWarningBannerBuilderCalledOnWarning() {
        var builderCalled = false
        composeRule.setContent {
            SteadpaySandbox(
                forcedStatus = SteadpayStatus.Warning,
                warningBanner = { _, _ ->
                    builderCalled = true
                    Text("custom banner")
                },
            ) {
                Text("protected content")
            }
        }
        assert(builderCalled)
        composeRule.onNodeWithText("custom banner").assertIsDisplayed()
    }
}
