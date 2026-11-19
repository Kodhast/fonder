package se.kodhast.fonder.ui.portfolj

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.data.common.AppErrors
import se.kodhast.fonder.data.common.DataError
import se.kodhast.fonder.domain.model.Fund
import se.kodhast.fonder.domain.model.Holding
import se.kodhast.fonder.domain.usecase.PortfolioTotalCalc
import se.kodhast.fonder.testing.captureLightAndDark
import se.kodhast.fonder.ui.theme.FonderTheme
import se.kodhast.ui.common.ListUiState
import se.kodhast.ui.common.toListUiState
import se.kodhast.ui.components.ENTITY_REFRESH_TAG

/**
 * Innehav utan innehav och med läsfel i JVM med Robolectric: det tomma lägets "Ny transaktion" och
 * "Importera avräkningsnota" (POR-10), läsfelets "Försök igen" (NFR-6) och skärmdumpar ljust + mörkt
 * enligt mockupen. Innehaven själva testas instrumenterat i `androidTest/…/ui/portfolj/PortfoljScreenTest`.
 */
@RunWith(RobolectricTestRunner::class)
class PortfoljContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val empty = PortfoljUiState(list = ListUiState.Empty)
    private val failed = PortfoljUiState(list = ListUiState.Error(AppErrors.map(DataError.Database)))

    @Test
    fun `tomt läge öppnar formuläret och avräkningsnotan (POR-10)`() {
        var opened = ""
        composeRule.setContent {
            FonderTheme {
                PortfoljContent(
                    state = empty,
                    onFundClick = {},
                    onNewTransaction = { opened += "ny;" },
                    onImportOrders = { opened += "import;" }
                )
            }
        }
        composeRule.onNodeWithText("Inga innehav ännu").assertIsDisplayed()
        composeRule.onNodeWithText("Ny transaktion").performClick()
        composeRule.onNodeWithText("Importera avräkningsnota").performClick()
        assertEquals("ny;import;", opened)
    }

    @Test
    fun `sidhuvudet räknar fonderna och exponeringen säger hämtar medan fondfakta slås upp (NAV-6)`() {
        val holdings = listOf("A", "B").map { Holding(fund = Fund(fundId = "SE_$it", name = "Fond $it"), netShares = 1.0, netInvested = 100.0, currentValue = 100.0) }
        composeRule.setContent {
            FonderTheme { PortfoljContent(state = PortfoljUiState(list = holdings.toListUiState(), metadataWorking = true), onFundClick = {}) }
        }
        composeRule.onNodeWithText("2 fonder").assertExists()
        composeRule.onNodeWithText("Hämtar fondfakta …").assertExists()
    }

    @Test
    fun `läsfelet har ingen dragning, bara Försök igen (NFR-6, UI-11)`() {
        composeRule.setContent { FonderTheme { PortfoljContent(state = failed, onFundClick = {}) } }
        composeRule.onNodeWithTag(ENTITY_REFRESH_TAG).assertDoesNotExist()
    }

    @Test
    fun `läsfelets Försök igen läser om innehaven (NFR-6)`() {
        var retries = 0
        composeRule.setContent { FonderTheme { PortfoljContent(state = failed, onFundClick = {}, onRetry = { retries++ }) } }
        composeRule.onNodeWithText("Kunde inte hämta listan").assertIsDisplayed()
        composeRule.onNodeWithText("Försök igen").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `långtryck på ett innehav kopierar fondnamnet utan att öppna fonden (UI-16)`() {
        var opened = 0
        composeRule.setContent {
            FonderTheme {
                PortfoljContent(
                    state = PortfoljUiState(
                        list = persistentListOf(
                            Holding(fund = Fund(fundId = "SE_GLOBAL", name = "Global Indexfond A"), netShares = 120.0, netInvested = 30_000.0, currentValue = 34_250.0)
                        ).toListUiState()
                    ),
                    onFundClick = { opened++ }
                )
            }
        }

        composeRule.onNodeWithText("Global Indexfond A", useUnmergedTree = true).performTouchInput { longClick() }
        composeRule.waitForIdle()

        val clipboard = ApplicationProvider.getApplicationContext<Context>().getSystemService(ClipboardManager::class.java)
        assertEquals("Global Indexfond A", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        assertEquals(0, opened)
    }

    @Test
    fun `skärmdump av Innehav utan innehav`() = composeRule.captureLightAndDark("PortfoljScreen_tom") {
        PortfoljContent(state = empty, onFundClick = {})
    }

    @Test
    fun `skärmdump av Innehav med innehav`() = composeRule.captureLightAndDark("PortfoljScreen_innehav") {
        PortfoljContent(
            state = PortfoljUiState(
                list = persistentListOf(
                    Holding(fund = Fund(fundId = "SE_GLOBAL", name = "Global Indexfond A"), netShares = 120.0, netInvested = 30_000.0, currentValue = 34_250.0),
                    Holding(fund = Fund(fundId = "SE_SMB", name = "Sverige Småbolag"), netShares = 40.0, netInvested = 12_000.0, currentValue = 11_380.0)
                ).toListUiState(),
                totalValue = 45_630.0,
                totalInvested = 42_000.0,
                totalGainLoss = 3_630.0,
                totalGainLossFraction = 0.0864,
                total = PortfolioTotalCalc.Result(netDepositsKr = 43_200.0, totalGainKr = 2_430.0, totalGainFraction = 0.0563, moneyWeightedAnnualReturn = null, partial = false, ageDays = 210),
                navEpochDay = 20_742
            ),
            onFundClick = {}
        )
    }

    @Test
    fun `skärmdump av Innehav med läsfel`() = composeRule.captureLightAndDark("PortfoljScreen_fel") {
        PortfoljContent(state = failed, onFundClick = {})
    }
}
