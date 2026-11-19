package se.kodhast.fonder.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.domain.usecase.FundAnalysisCalc.SignalLevel
import se.kodhast.fonder.ui.theme.FonderTheme

/**
 * Säljsignalens prick (ANA-3) bär aldrig nivån bara med färgen (UI-3): skärmläsaren läser nivåns namn, utom där
 * namnet redan står i text bredvid (statusbannern).
 */
@RunWith(RobolectricTestRunner::class)
class StatusDotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `pricken läses med nivåns namn`() {
        composeRule.setContent {
            FonderTheme {
                StatusDot(SignalLevel.GRON)
                StatusDot(SignalLevel.GUL)
                StatusDot(SignalLevel.ROD)
            }
        }

        composeRule.onNodeWithContentDescription("Säljsignal: Inga signaler").assertExists()
        composeRule.onNodeWithContentDescription("Säljsignal: Värt att se över").assertExists()
        composeRule.onNodeWithContentDescription("Säljsignal: Bör ses över").assertExists()
    }

    @Test
    fun `en enskild indikator läses med bara nivåns namn`() {
        composeRule.setContent { FonderTheme { StatusDot(SignalLevel.GUL, contentDescription = statusTitle(SignalLevel.GUL)) } }

        composeRule.onNodeWithContentDescription("Värt att se över").assertExists()
        composeRule.onAllNodesWithContentDescription("Säljsignal", substring = true).assertCountEquals(0)
    }

    @Test
    fun `pricken bredvid nivåns namn läses inte två gånger`() {
        composeRule.setContent { FonderTheme { StatusDot(SignalLevel.ROD, contentDescription = null) } }

        composeRule.onAllNodesWithContentDescription("Säljsignal", substring = true).assertCountEquals(0)
    }
}
