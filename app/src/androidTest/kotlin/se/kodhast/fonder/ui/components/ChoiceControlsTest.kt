package se.kodhast.fonder.ui.components

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import se.kodhast.fonder.ui.theme.FonderTheme
import se.kodhast.ui.components.AppSegmentedChoice
import se.kodhast.ui.components.ChoiceChips

/**
 * Fonders val på android-ui:s [ChoiceChips] och [AppSegmentedChoice] (UI-12, regel 4) på riktig enhet:
 * riskenkätens svar och periodvalen visar alla alternativ, markerar det valda och väljer med ett tryck.
 */
@RunWith(AndroidJUnit4::class)
class ChoiceControlsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun visar_alla_alternativ_och_markerar_det_valda() {
        composeRule.setContent {
            FonderTheme {
                ChoiceChips(options = listOf("A", "B", "C"), selected = "B", onSelect = {}, label = { it })
            }
        }

        composeRule.onNodeWithText("A").assertIsNotSelected()
        composeRule.onNodeWithText("B").assertIsSelected()
        composeRule.onNodeWithText("C").assertIsNotSelected()
    }

    @Test
    fun klick_pa_ett_alternativ_anropar_onSelect_med_det_alternativet() {
        var selected: String? = null
        composeRule.setContent {
            FonderTheme {
                ChoiceChips<String?>(options = listOf("A", "B", "C"), selected = null, onSelect = { selected = it }, label = { it.orEmpty() })
            }
        }

        composeRule.onNodeWithText("C").performClick()

        assert(selected == "C")
    }

    @Test
    fun inget_alternativ_markerat_nar_selected_ar_null() {
        composeRule.setContent {
            FonderTheme {
                ChoiceChips<Int?>(options = listOf(1, 2, 3), selected = null, onSelect = {}, label = { it.toString() })
            }
        }

        composeRule.onNodeWithText("1").assertIsNotSelected()
        composeRule.onNodeWithText("2").assertIsNotSelected()
        composeRule.onNodeWithText("3").assertIsNotSelected()
    }

    @Test
    fun segmentval_visar_alla_perioder_och_markerar_den_valda() {
        composeRule.setContent {
            FonderTheme {
                AppSegmentedChoice(options = listOf("1 mån", "3 mån", "1 år", "3 år", "5 år", "Allt"), selectedIndex = 5, onSelect = {})
            }
        }

        composeRule.onNodeWithText("1 mån").assertIsNotSelected()
        composeRule.onNodeWithText("Allt").assertIsSelected()
    }
}
