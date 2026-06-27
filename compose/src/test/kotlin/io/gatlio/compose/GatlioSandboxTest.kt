package io.gatlio.compose

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
class GatlioSandboxTest {

    @get:Rule val composeRule = createComposeRule()

    @Test fun `starts in Active - children render, lockout not present`() {
        composeRule.setContent {
            GatlioSandbox { Text("app content") }
        }
        composeRule.onNodeWithText("app content").assertIsDisplayed()
        composeRule.onNodeWithText("Payment method declined").assertDoesNotExist()
    }

    @Test fun `lockout pill shows lockout screen`() {
        composeRule.setContent {
            GatlioSandbox { Text("app content") }
        }
        composeRule.onNodeWithTag("sandbox-dev-badge").performClick()
        composeRule.onNodeWithTag("sandbox-pill-lockout").performClick()
        composeRule.onNodeWithText("Payment method declined").assertIsDisplayed()
        composeRule.onNodeWithText("app content").assertDoesNotExist()
    }

    @Test fun `callback fires when pill is tapped`() {
        var lockoutCalled = false
        composeRule.setContent {
            GatlioSandbox(onLockout = { lockoutCalled = true }) { Text("content") }
        }
        composeRule.onNodeWithTag("sandbox-dev-badge").performClick()
        composeRule.onNodeWithTag("sandbox-pill-lockout").performClick()
        assert(lockoutCalled)
    }

    @Test fun `log appends entry on transition`() {
        composeRule.setContent {
            GatlioSandbox { Text("content") }
        }
        composeRule.onNodeWithTag("sandbox-dev-badge").performClick()
        composeRule.onNodeWithTag("sandbox-pill-lockout").performClick()
        // Panel closes on status change; reopen to inspect the log
        composeRule.onNodeWithTag("sandbox-dev-badge").performClick()
        composeRule.onNodeWithText("OnLockout()", substring = true).assertExists()
    }

    @Test fun `onRecovered note is present in sheet`() {
        composeRule.setContent {
            GatlioSandbox { Text("content") }
        }
        composeRule.onNodeWithTag("sandbox-dev-badge").performClick()
        composeRule.onNodeWithTag("sandbox-recovered-note").assertExists()
    }
}
