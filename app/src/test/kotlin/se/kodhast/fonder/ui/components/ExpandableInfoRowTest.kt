package se.kodhast.fonder.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.ui.theme.FonderTheme

/**
 * [ExpandableInfoRow] på android-ui:s [se.kodhast.ui.components.Foldout] (UI-3) i JVM: raden är en knapp med
 * minst 48 dp, läget läses som "Hopfälld"/"Utfälld", ett tryck fäller ut förklaringen och det extra innehållet,
 * och `onExpand` körs bara när raden fälls ut.
 */
@RunWith(RobolectricTestRunner::class)
class ExpandableInfoRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun state(value: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun `raden är en knapp som fäller ut och läser upp läget`() {
        var expands = 0
        composeRule.setContent {
            FonderTheme {
                ExpandableInfoRow(explanation = "Årstakt betyder …", onExpand = { expands++ }) { Text("Årstakt 6,1 %") }
            }
        }

        composeRule.onNodeWithText("Årstakt 6,1 %")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(state("Hopfälld"))
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Årstakt betyder …").assertDoesNotExist()

        composeRule.onNodeWithText("Årstakt 6,1 %").performClick()

        composeRule.onNodeWithText("Årstakt 6,1 %").assert(state("Utfälld"))
        composeRule.onNodeWithText("Årstakt betyder …").assertExists()
        assertEquals(1, expands)

        composeRule.onNodeWithText("Årstakt 6,1 %").performClick()

        composeRule.onNodeWithText("Årstakt 6,1 %").assert(state("Hopfälld"))
        assertEquals(1, expands)
    }

    @Test
    fun `extra innehåll visas under förklaringen först när raden är utfälld`() {
        composeRule.setContent {
            FonderTheme {
                ExpandableInfoRow(explanation = "Sparar 240 kr per år.", extraContent = { Text("Jämförelsediagram") }) {
                    Text("Indexfond Global")
                }
            }
        }

        composeRule.onNodeWithText("Jämförelsediagram").assertDoesNotExist()

        composeRule.onNodeWithText("Indexfond Global").performClick()

        composeRule.onNodeWithText("Sparar 240 kr per år.").assertExists()
        composeRule.onNodeWithText("Jämförelsediagram").assertExists()
    }

    @Test
    fun `startar utfälld med initiallyExpanded utan att anropa onExpand`() {
        var expands = 0
        composeRule.setContent {
            FonderTheme {
                ExpandableInfoRow(explanation = "Förklaring", initiallyExpanded = true, onExpand = { expands++ }) { Text("Rubrik") }
            }
        }

        composeRule.onNodeWithText("Rubrik").assert(state("Utfälld"))
        composeRule.onNodeWithText("Förklaring").assertExists()
        assertEquals(0, expands)
    }
}
