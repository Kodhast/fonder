package se.kodhast.fonder.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.testing.captureLightAndDark
import se.kodhast.fonder.ui.theme.FonderTheme

/**
 * Om appen (SET-9, #280 C): en egen underskärm från avatarmenyn med appens namn, versionen och Kodhästs
 * signatur – inte längre sist i Inställningar, och komponentgalleriet nås från menyn.
 */
@RunWith(RobolectricTestRunner::class)
class AboutScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `visar namn, version och signatur, och bakåt stänger`() {
        var closed = 0
        rule.setContent { FonderTheme { AboutScreen(onBack = { closed++ }, versionName = "1.10.0") } }

        rule.onNodeWithText("Version 1.10.0", substring = true).assertIsDisplayed()
        rule.onNodeWithText("En kodhäst-app").assertIsDisplayed()
        rule.onNodeWithContentDescription("Tillbaka").performClick()
        assertEquals(1, closed)
    }

    @Test
    fun `Inställningar har varken Om appen eller Komponenter`() {
        rule.setContent { FonderTheme { SettingsContent(state = SettingsUiState()) } }

        rule.onNodeWithText("En kodhäst-app").assertDoesNotExist()
        rule.onNodeWithText("Komponenter").assertDoesNotExist()
    }

    @Test
    fun `skärmdump`() = rule.captureLightAndDark("AboutScreen_standard") {
        AboutScreen(onBack = {}, versionName = "1.10.0")
    }
}
