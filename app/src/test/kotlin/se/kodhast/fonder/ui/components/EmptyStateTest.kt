package se.kodhast.fonder.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.data.common.AppErrors
import se.kodhast.fonder.data.common.DataError
import se.kodhast.fonder.testing.captureLightAndDark
import se.kodhast.fonder.ui.theme.FonderTheme
import se.kodhast.ui.components.AppBrandStyle
import se.kodhast.ui.components.AppButton
import se.kodhast.ui.components.LocalAppBrandStyle

/**
 * Den delade [EmptyState] i JVM med Robolectric – första Compose-testet utanför emulatorn
 * (skill testing-strategy). Rubrik och brödtext ska båda synas; skärmdumparna spelas in med
 * `recordRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
class EmptyStateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `visar rubrik och brödtext`() {
        composeRule.setContent {
            FonderTheme {
                EmptyState(
                    title = "Ingen portfölj ännu",
                    body = "Lägg till din första fondtransaktion för att se en översikt här."
                )
            }
        }

        composeRule.onNodeWithText("Ingen portfölj ännu").assertExists()
        composeRule.onNodeWithText("Lägg till din första fondtransaktion för att se en översikt här.").assertExists()
    }

    @Test
    fun `ett läsfel visas som tomt tillstånd med felets text (NFR-6)`() {
        composeRule.setContent { FonderTheme { EmptyState(error = AppErrors.map(DataError.Database)) } }

        composeRule.onNodeWithText("Kunde inte visa innehållet").assertExists()
        composeRule.onNodeWithText("Appens data gick inte att läsa. Lämna skärmen och försök igen.").assertExists()
    }

    @Test
    fun `ett läsfel kan få skärmens egen rubrik`() {
        composeRule.setContent { FonderTheme { EmptyState(error = AppErrors.map(DataError.ImportEmpty), title = "Importera innehav") } }

        composeRule.onNodeWithText("Importera innehav").assertExists()
        composeRule.onNodeWithText("Inga rader hittades i filen.").assertExists()
    }

    @Test
    fun `en åtgärd visas som knapp under texten och anropas vid tryck`() {
        var clicks = 0
        composeRule.setContent {
            FonderTheme {
                EmptyState(
                    title = "Inga bevakade fonder",
                    body = "Bevaka en fond först.",
                    action = { AppButton("Bevaka en fond", onClick = { clicks++ }) }
                )
            }
        }

        composeRule.onNodeWithText("Bevaka en fond").assertHasClickAction().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `ikonrutan visar Kodhästs märke i Fonders tema (UI-17)`() {
        composeRule.setContent { FonderTheme { EmptyState(title = "Inga fonder hittades", body = "Prova ett annat namn.", icon = Icons.Outlined.PieChart) } }

        composeRule.onNodeWithTag(EMPTY_STATE_MARK_TAG).assertExists()
    }

    @Test
    fun `utan stilen visar ikonrutan ikonen och inget märke`() {
        composeRule.setContent {
            FonderTheme {
                CompositionLocalProvider(LocalAppBrandStyle provides AppBrandStyle()) {
                    EmptyState(title = "Inga fonder hittades", body = "Prova ett annat namn.", icon = Icons.Outlined.PieChart)
                }
            }
        }

        composeRule.onNodeWithTag(EMPTY_STATE_MARK_TAG).assertDoesNotExist()
    }

    @Test
    fun `utan ikonruta och i felvyn ritas inget märke`() {
        composeRule.setContent {
            FonderTheme {
                EmptyState(title = "Ingen portfölj ännu", body = "Lägg till en fond.")
                EmptyState(error = AppErrors.map(DataError.Database))
            }
        }

        composeRule.onNodeWithTag(EMPTY_STATE_MARK_TAG).assertDoesNotExist()
    }

    @Test
    fun `skärmdump ljust och mörkt`() {
        composeRule.captureLightAndDark("EmptyState_default") {
            EmptyState(
                title = "Inga innehav ännu",
                body = "Lägg till din första fondtransaktion för att se värde och utveckling."
            )
        }
    }
}
