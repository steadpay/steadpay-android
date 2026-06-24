package io.steadpay.compose

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gate rendering tests. SteadpaySandbox starts in Active by default;
 * status transitions are driven via the DEV badge + pills.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SteadpayGateTest {

    @get:Rule val composeRule = createComposeRule()

    @Test fun rendersChildrenOnActive() {
        composeRule.setContent {
            SteadpaySandbox { Text("protected content") }
        }
        composeRule.onNodeWithText("protected content").assertIsDisplayed()
        composeRule.onNodeWithText("Payment method declined").assertDoesNotExist()
    }

    @Test fun rendersLockoutScreenOnLockout() {
        composeRule.setContent {
            SteadpaySandbox { Text("protected content") }
        }
        composeRule.onNodeWithTag("sandbox-dev-badge").performClick()
        composeRule.onNodeWithTag("sandbox-pill-lockout").performClick()
        composeRule.onNodeWithText("Payment method declined").assertIsDisplayed()
        composeRule.onNodeWithText("protected content").assertDoesNotExist()
    }

    @Test fun rendersWarningBannerAndChildrenOnWarning() {
        composeRule.setContent {
            SteadpaySandbox { Text("protected content") }
        }
        composeRule.onNodeWithTag("sandbox-dev-badge").performClick()
        composeRule.onNodeWithTag("sandbox-pill-warning").performClick()
        composeRule.onNodeWithText("please ensure funds are available.", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("protected content").assertIsDisplayed()
    }

    @Test fun customLockoutScreenBuilderCalledOnLockout() {
        composeRule.setContent {
            SteadpaySandbox(
                lockoutScreen = { _, _, _, _ ->
                    Text("custom lockout")
                },
            ) {
                Text("protected content")
            }
        }
        composeRule.onNodeWithTag("sandbox-dev-badge").performClick()
        composeRule.onNodeWithTag("sandbox-pill-lockout").performClick()
        composeRule.onNodeWithText("custom lockout").assertIsDisplayed()
    }

    @Test fun customWarningBannerBuilderCalledOnWarning() {
        composeRule.setContent {
            SteadpaySandbox(
                warningBanner = { _, _ ->
                    Text("custom banner")
                },
            ) {
                Text("protected content")
            }
        }
        composeRule.onNodeWithTag("sandbox-dev-badge").performClick()
        composeRule.onNodeWithTag("sandbox-pill-warning").performClick()
        composeRule.onNodeWithText("custom banner").assertIsDisplayed()
    }
}
