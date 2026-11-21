package se.kodhast.fonder.ui.bytesfonster

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.data.common.AppErrors
import se.kodhast.fonder.data.common.DataError
import se.kodhast.fonder.ui.theme.FonderTheme
import se.kodhast.ui.common.DetailUiState

/** Pågående bytes lägen som inte kräver en emulator — "finns inte" och läsfelet (NFR-6). */
@RunWith(RobolectricTestRunner::class)
class SwitchWatchContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `en bevakning som inte finns behaller sin egen text`() {
        composeRule.setContent {
            FonderTheme { SwitchWatchContent(state = SwitchWatchUiState(watch = DetailUiState.Error(AppErrors.notFound()))) }
        }
        composeRule.onNodeWithText(MISSING, substring = true).assertExists()
        composeRule.onNodeWithText("Det finns inte längre.").assertDoesNotExist()
    }

    @Test
    fun `lasfel visar felet i stallet for en tom bevakning (NFR-6)`() {
        composeRule.setContent {
            FonderTheme { SwitchWatchContent(state = SwitchWatchUiState(watch = DetailUiState.Error(AppErrors.map(DataError.Database)))) }
        }
        composeRule.onNodeWithText("Appens data gick inte att läsa. Lämna skärmen och försök igen.").assertExists()
        composeRule.onNodeWithText(MISSING, substring = true).assertDoesNotExist()
    }

    @Test
    fun `Försök igen efter ett läsfel anropar onRetry (NFR-6)`() {
        var retries = 0
        composeRule.setContent {
            FonderTheme {
                SwitchWatchContent(
                    state = SwitchWatchUiState(watch = DetailUiState.Error(AppErrors.map(DataError.Database))),
                    onRetry = { retries++ }
                )
            }
        }

        composeRule.onNodeWithText("Försök igen").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `en bevakning som inte finns har ingen Försök igen`() {
        composeRule.setContent {
            FonderTheme { SwitchWatchContent(state = SwitchWatchUiState(watch = DetailUiState.Error(AppErrors.notFound()))) }
        }
        composeRule.onNodeWithText("Försök igen").assertDoesNotExist()
    }

    @Test
    fun `en bevakning som inte finns har sin egen rubrik, ett läsfel ramens`() {
        var state by mutableStateOf(SwitchWatchUiState(watch = DetailUiState.Error(AppErrors.notFound())))
        composeRule.setContent { FonderTheme { SwitchWatchContent(state = state) } }
        composeRule.onNodeWithText("Bevakningen finns inte").assertIsDisplayed()
        composeRule.onNodeWithText("Kunde inte hämta uppgifterna").assertDoesNotExist()

        state = SwitchWatchUiState(watch = DetailUiState.Error(AppErrors.map(DataError.Database)))
        composeRule.onNodeWithText("Kunde inte hämta uppgifterna").assertIsDisplayed()
        composeRule.onNodeWithText("Bevakningen finns inte").assertDoesNotExist()
    }

    private companion object {
        /** Början av texten för en bevakning som inte finns (switch_watch_missing_body). */
        const val MISSING = "Det pågående bytet har tagits bort"
    }
}
