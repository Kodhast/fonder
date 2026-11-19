package se.kodhast.fonder.ui.hem

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.ui.theme.FonderTheme
import se.kodhast.ui.common.DetailUiState

/**
 * Dra-ned-indikatorn på Översikten (UI-11, UI-19, NAV-6, #277): en kursuppdatering som appen startat själv syns bara av
 * sidhuvudets prickar – ingen indikator över innehållet, och aldrig den vågiga formen. Indikatorn
 * (hoppande prickar, "Laddar") kommer bara när man själv drar, vilket bibliotekets `AppPullToRefresh` testar.
 */
@RunWith(RobolectricTestRunner::class)
class HemRefreshIndicatorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `en uppdatering appen startat själv ger ingen dra-ned-indikator över innehållet`() {
        val state = HemUiState(portfolio = DetailUiState.Content(HemPortfolio(hasHoldings = true)), pricesWorking = true)
        composeRule.setContent { FonderTheme { HemContent(state = state) } }

        composeRule.onNodeWithText("Totalt värde").assertExists()
        composeRule.onAllNodesWithContentDescription("Laddar").assertCountEquals(0)
    }
}
