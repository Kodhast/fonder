package se.kodhast.fonder.ui.imports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.R
import se.kodhast.fonder.data.common.AppErrors
import se.kodhast.fonder.data.common.DataError
import se.kodhast.fonder.ui.theme.FonderTheme
import se.kodhast.ui.common.ListUiState
import se.kodhast.ui.components.ItemRowText

/**
 * Importernas gemensamma skärm [ImportFrame] (IMP-4, IMP-6, #266 F6) i JVM med Robolectric: listramens lägen
 * med importens rubrik på läsfelet och filväljaren som tomt läge (NFR-6), filväljarknappen, importknappen som bara
 * finns när raderna visas, och [importListState]. Påhittade rader.
 */
@RunWith(RobolectricTestRunner::class)
class ImportFrameTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val pick = ImportPick(R.drawable.ic_upload, "Välj filen", "Filen läses här.", "Välj fil")

    @Composable
    private fun Frame(state: ListUiState<String>, onPick: () -> Unit = {}, onCommit: () -> Unit = {}) = ImportFrame(
        title = "Importera",
        state = state,
        pick = pick,
        onPick = onPick,
        onBack = {},
        group = "Fonder i filen",
        commit = "Importera 1",
        canCommit = true,
        onCommit = onCommit,
        key = { it }
    ) { ItemRowText(title = it, subtitle = null) }

    /**
     * Listramens lägen med importens egna val. Bibliotekets `runListScreenContract` går inte att använda: importen
     * har sin rubrik både i toppraden och på läsfelet, och kontraktet kräver att felrubriken finns en gång.
     */
    @Test
    fun `laddning, filväljaren, läsfelet under importens rubrik och raderna`() {
        var state by mutableStateOf<ListUiState<String>>(ListUiState.Loading)
        var picks = 0
        composeRule.setContent { FonderTheme { Frame(state, onPick = { picks++ }) } }
        composeRule.onNodeWithContentDescription("Laddar").assertExists()

        state = ListUiState.Empty
        composeRule.onNodeWithText(pick.title).assertIsDisplayed()
        composeRule.onNodeWithText(pick.message).assertIsDisplayed()

        state = ListUiState.Error(AppErrors.map(DataError.ImportUnreadable))
        composeRule.onAllNodesWithText("Importera").assertCountEquals(2)
        composeRule.onNodeWithText("Försök igen").performClick()
        assertEquals("Försök igen öppnar filväljaren", 1, picks)

        state = ListUiState.Content(persistentListOf("Global Indexfond A"))
        composeRule.onNodeWithText("Global Indexfond A").assertIsDisplayed()
        composeRule.onNodeWithText("Fonder i filen", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `filväljaren och importknappen anropar sina åtgärder`() {
        var state by mutableStateOf<ListUiState<String>>(ListUiState.Empty)
        var picks = 0
        var commits = 0
        composeRule.setContent { FonderTheme { Frame(state, onPick = { picks++ }, onCommit = { commits++ }) } }
        composeRule.onNodeWithText("Importera 1").assertDoesNotExist()
        composeRule.onNodeWithText(pick.button).performClick()
        assertEquals(1, picks)

        state = ListUiState.Content(persistentListOf("Global Indexfond A"))
        composeRule.onNodeWithText("Importera 1").assertIsDisplayed().performClick()
        assertEquals(1, commits)
    }

    @Test
    fun `listtillståndet följer filvalet, felet och läsningen`() {
        val rows = persistentListOf("A")
        val error = AppErrors.map(DataError.ImportEmpty)
        assertEquals(ListUiState.Empty, importListState(selected = false, loading = false, error = error, rows = rows))
        assertEquals(ListUiState.Error(error), importListState(selected = true, loading = false, error = error, rows = rows))
        assertEquals(ListUiState.Loading, importListState(selected = true, loading = true, error = null, rows = persistentListOf<String>()))
        assertEquals(ListUiState.Content(rows), importListState(selected = true, loading = true, error = null, rows = rows))
    }
}
