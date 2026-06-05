package io.steadpay.compose

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SteadpaySandboxTest {

    @get:Rule val composeRule = createComposeRule()

    @Test fun `starts in Active - children render, lockout not present`() {
        composeRule.setContent {
            SteadpaySandbox { Text("app content") }
        }
        composeRule.onNodeWithText("app content").assertIsDisplayed()
        composeRule.onNodeWithText("Payment method declined").assertDoesNotExist()
    }

    @Test fun `lockout pill shows lockout screen`() {
        composeRule.setContent {
            SteadpaySandbox { Text("app content") }
        }
        composeRule.onNodeWithTag("sandbox-dev-badge").performClick()
        composeRule.onNodeWithTag("sandbox-pill-lockout").performClick()
        composeRule.onNodeWithText("Payment method declined").assertIsDisplayed()
        composeRule.onNodeWithText("app content").assertDoesNotExist()
    }

    @Test fun `callback fires when pill is tapped`() {
        var lockoutCalled = false
        composeRule.setContent {
            SteadpaySandbox(onLockout = { lockoutCalled = true }) { Text("content") }
        }
        composeRule.onNodeWithTag("sandbox-dev-badge").performClick()
        composeRule.onNodeWithTag("sandbox-pill-lockout").performClick()
        assert(lockoutCalled)
    }

    @Test fun `log appends entry on transition`() {
        composeRule.setContent {
            SteadpaySandbox { Text("content") }
        }
        composeRule.onNodeWithTag("sandbox-dev-badge").performClick()
        composeRule.onNodeWithTag("sandbox-pill-lockout").performClick()
        composeRule.onNodeWithText("OnLockout()", substring = true).assertExists()
    }

    @Test fun `onRecovered note is present in sheet`() {
        composeRule.setContent {
            SteadpaySandbox { Text("content") }
        }
        composeRule.onNodeWithTag("sandbox-dev-badge").performClick()
        composeRule.onNodeWithTag("sandbox-recovered-note").assertExists()
    }
}
