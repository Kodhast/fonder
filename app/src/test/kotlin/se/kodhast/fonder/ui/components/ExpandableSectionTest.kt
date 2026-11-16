package se.kodhast.fonder.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.ui.theme.FonderTheme

/**
 * [ExpandableSection] på android-ui:s `CollapsibleHeader` (UI-3) i JVM: rubrikraden är en knapp med
 * minst 48 dp, läget läses som "Hopfälld"/"Utfälld", ett tryck fäller ut innehållet och `onExpand`
 * körs bara när sektionen fälls ut.
 */
@RunWith(RobolectricTestRunner::class)
class ExpandableSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun state(value: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun `rubrikraden är en knapp som fäller ut och läser upp läget`() {
        var expands = 0
        composeRule.setContent {
            FonderTheme {
                ExpandableSection(title = "Ordlista", onExpand = { expands++ }) { Text("Årstakt betyder …") }
            }
        }

        composeRule.onNodeWithText("Ordlista")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(state("Hopfälld"))
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Årstakt betyder …").assertDoesNotExist()

        composeRule.onNodeWithText("Ordlista").performClick()

        composeRule.onNodeWithText("Ordlista").assert(state("Utfälld"))
        composeRule.onNodeWithText("Årstakt betyder …").assertExists()
        assertEquals(1, expands)

        composeRule.onNodeWithText("Ordlista").performClick()

        composeRule.onNodeWithText("Ordlista").assert(state("Hopfälld"))
        assertEquals(1, expands)
    }

    @Test
    fun `summary syns hopfällt och hör inte till knappen`() {
        composeRule.setContent {
            FonderTheme {
                ExpandableSection(
                    header = { Text("Europa Aktiv → Europa Index") },
                    summary = { Text("Meravkastning +6,5 %") }
                ) { Text("Belopp: 25 000 kr") }
            }
        }

        composeRule.onNodeWithText("Meravkastning +6,5 %").assertIsDisplayed()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertHasNoClickAction()
        composeRule.onNodeWithText("Europa Aktiv → Europa Index")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(AnnotatedString("Europa Aktiv → Europa Index"))))
        composeRule.onNodeWithText("Belopp: 25 000 kr").assertDoesNotExist()
    }
}
