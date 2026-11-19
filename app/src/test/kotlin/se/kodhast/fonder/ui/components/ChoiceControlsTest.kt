package se.kodhast.fonder.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import se.kodhast.fonder.ui.theme.FonderTheme
import se.kodhast.ui.components.AppSegmentedChoice
import se.kodhast.ui.components.ChoiceChips

/**
 * Fonders val på android-ui:s delar (UI-12) i JVM på en liten skärm (320 dp): perioderna och riskenkätens
 * svar är chips som bryts på flera rader så att alla syns utan att skrolla eller klippas (UI-5), och ett
 * tryck väljer. Segmentvalet bär de få korta valen (tema, filter, Köp/Sälj).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w320dp-h640dp-xxhdpi")
class ChoiceControlsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val periods = listOf("1 mån", "3 mån", "1 år", "3 år", "5 år", "Allt")

    private val answers = listOf(
        "Mindre än ett år",
        "Ett till tre år",
        "Tre till tio år",
        "Mer än tio år",
        "Vet inte än",
        "Ingen plan"
    )

    @Test
    fun `alla sex perioder syns på en smal skärm och den valda läses som valt`() {
        composeRule.setContent {
            FonderTheme { ChoiceChips(options = periods, selected = "1 år", onSelect = {}, label = { it }) }
        }

        periods.forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNodeWithText("1 år").assertIsSelected()
        composeRule.onNodeWithText("Allt").assertIsNotSelected()
    }

    @Test
    fun `ett tryck på en period väljer den`() {
        var chosen = ""
        composeRule.setContent {
            FonderTheme { ChoiceChips(options = periods, selected = "1 mån", onSelect = { chosen = it }, label = { it }) }
        }

        composeRule.onNodeWithText("Allt").performClick()

        assertEquals("Allt", chosen)
    }

    @Test
    fun `enkätens svar bryts på flera rader så att alla syns och det valda är markerat`() {
        composeRule.setContent {
            FonderTheme { ChoiceChips(options = answers, selected = "Ett till tre år", onSelect = {}, label = { it }) }
        }

        answers.forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNodeWithText("Ett till tre år").assertIsSelected()
        composeRule.onNodeWithText("Mindre än ett år").assertIsNotSelected()
    }

    @Test
    fun `ett tryck på ett svar väljer det`() {
        var chosen: String? = null
        composeRule.setContent {
            FonderTheme { ChoiceChips(options = answers, selected = "", onSelect = { chosen = it }, label = { it }) }
        }

        composeRule.onNodeWithText("Ingen plan").performClick()

        assertEquals("Ingen plan", chosen)
    }

    @Test
    fun `segmentvalet visar de få valen och ett tryck väljer`() {
        var chosen = -1
        composeRule.setContent {
            FonderTheme { AppSegmentedChoice(options = listOf("Alla", "Köp", "Sälj"), selectedIndex = 0, onSelect = { chosen = it }) }
        }

        composeRule.onNodeWithText("Alla").assertIsSelected()
        composeRule.onNodeWithText("Sälj").performClick()

        assertEquals(2, chosen)
    }
}
