package se.kodhast.fonder.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import se.kodhast.fonder.testing.captureLightAndDark
import se.kodhast.fonder.ui.theme.FonderTheme

/**
 * Komponentgalleriet (debug-skärmen, skill shared-ui-components): topprad med bakåtpil, alla avsnitt,
 * dialogerna öppnas och stängs, och diagrammet ritas först när sektionen fälls ut. Skärmdumpen ljust +
 * mörkt tas med diagrammet hopfällt (Vico ritar på en annan tråd) och klockan stegad för indikatorerna.
 */
@RunWith(RobolectricTestRunner::class)
class ComponentGalleryTest {

    @get:Rule
    val rule = createComposeRule()

    private var backs = 0

    private fun setGallery() = rule.setContent { FonderTheme { ComponentGallery(onBack = { backs++ }) } }

    /** Galleriets yttersta lodräta rullning – exemplen i biblioteket har egna. */
    private fun scrollTo(text: String) = rule.onAllNodes(verticalScroll).onFirst().performScrollToNode(hasText(text))

    private val verticalScroll = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    @Test
    fun `topprad med bakåtpil, bibliotekets och Fonders avsnitt`() {
        setGallery()

        rule.onNodeWithText("Komponenter").assertIsDisplayed()
        // Exemplen längre ned har egna bakåtpilar – galleriets är den i toppraden, först i trädet.
        rule.onAllNodes(hasContentDescription("Tillbaka")).onFirst().performClick()
        assertEquals(1, backs)
        listOf(
            "Fonder: belopp och andelar",
            "Fonder: kort, märken och val",
            "Fonder: analys och dialog",
            "Fonder: diagram"
        ).forEach { section ->
            scrollTo(section)
            rule.onNodeWithText(section).assertIsDisplayed()
        }
    }

    @Test
    fun `importdialogen öppnas och stängs`() {
        setGallery()
        scrollTo("Import")

        rule.onNodeWithText("Import").performClick()
        rule.onNode(hasText("7", substring = true) and hasAnyAncestor(isDialog())).assertIsDisplayed()
        rule.onNode(hasText("Stäng") and hasAnyAncestor(isDialog())).performClick()

        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun `diagrammet ritas först när sektionen fälls ut`() {
        setGallery()
        rule.onNode(hasContentDescription("Kursdiagram", substring = true)).assertDoesNotExist()

        scrollTo("Diagram")
        rule.onAllNodes(hasText("Diagram")).onLast().performClick()

        scrollTo("FundLineChart")
        rule.onAllNodes(hasContentDescription("Kursdiagram", substring = true)).onFirst().assertExists()
    }

    @Test
    @Config(qualifiers = "w390dp-h2400dp-xxhdpi")
    fun `skärmdump av galleriet`() = rule.captureLightAndDark("ComponentGallery_default", animated = true) {
        ComponentGallery(onBack = {})
    }
}
