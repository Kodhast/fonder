package se.kodhast.fonder.ui.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.ui.theme.FonderTheme

/**
 * Inställningarnas väg till komponentgalleriet: kortet finns bara när navigeringen ger en väg dit
 * (debug-bygget, `BuildConfig.DEBUG` i `AppNavigation`) och öppnar galleriet. Kodhästs signatur
 * står sist (SET-9).
 */
@RunWith(RobolectricTestRunner::class)
class SettingsGalleryEntryTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `utan väg till galleriet visas inget kort`() {
        rule.setContent { FonderTheme { SettingsContent(state = SettingsUiState()) } }

        rule.onNodeWithText("Visa komponenter").assertDoesNotExist()
    }

    @Test
    fun `kortet öppnar galleriet`() {
        var opened = 0
        rule.setContent { FonderTheme { SettingsContent(state = SettingsUiState(), onOpenGallery = { opened++ }) } }

        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasText("Visa komponenter"))
        rule.onNodeWithText("Visa komponenter").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun `Kodhästs signatur står sist i Inställningar (SET-9)`() {
        rule.setContent { FonderTheme { SettingsContent(state = SettingsUiState()) } }

        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasText("En kodhäst-app"))
        val signature = rule.onNodeWithText("En kodhäst-app").assertIsDisplayed().getUnclippedBoundsInRoot()
        val dangerButton = rule.onNodeWithText("Töm databasen").getUnclippedBoundsInRoot()
        assertTrue("signaturen ska stå under farozonens knapp", signature.top >= dangerButton.bottom)
    }
}
