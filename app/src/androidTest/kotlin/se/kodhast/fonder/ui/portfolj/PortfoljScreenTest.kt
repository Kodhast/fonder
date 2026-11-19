package se.kodhast.fonder.ui.portfolj

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import se.kodhast.fonder.domain.model.Fund
import se.kodhast.fonder.domain.model.Holding
import se.kodhast.fonder.domain.usecase.FundAnalysisCalc
import se.kodhast.fonder.domain.usecase.PortfolioExposureCalc
import se.kodhast.fonder.domain.usecase.PortfolioPerformanceCalc
import se.kodhast.fonder.ui.theme.FonderTheme
import se.kodhast.ui.common.ListUiState
import se.kodhast.ui.common.toListUiState

/**
 * Instrumenterat test av Portföljens tillståndsdrivna innehåll på listramen (#266 F3), inklusive
 * dag/vecka/månad per innehav (issue #14, POR-5) — bygger [PortfoljUiState]
 * direkt i stället för att gå via ett riktigt [PortfoljViewModel]/Hilt.
 */
@RunWith(AndroidJUnit4::class)
class PortfoljScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val fond = Fund(fundId = "SHB0000442", name = "Fond A")

    /**
     * Innehaven är riktiga lazy-rader under totalkortet och exponeringskortet (listramen, #266 F3); i
     * emulatorns fönster (320×640) är raden inte komponerad förrän listan rullats dit.
     */
    private fun scrollToHolding(name: String = fond.name) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(name, substring = true))
    }

    @Test
    fun tomt_tillstand_visas_utan_innehav() {
        composeRule.setContent {
            FonderTheme { PortfoljContent(state = PortfoljUiState(list = ListUiState.Empty), onFundClick = {}) }
        }

        composeRule.onNodeWithText("Inga innehav ännu").assertExists()
    }

    @Test
    fun innehavsrad_utan_kurs_sager_hamtar_medan_dess_kurs_hamtas() {
        // NAV-6, per fond: en nyss tillagd fond saknar kurs tills den landat. Raden säger "Hämtar …" i stället
        // för en egen snurra (#266 F3: en laddningsindikator i sidhuvudet), och bara för just den fonden.
        val annanFond = Fund(fundId = "SHB0000443", name = "Fond B")
        val state = PortfoljUiState(
            list = persistentListOf(
                Holding(fund = fond, netShares = 10.0, netInvested = 100.0, currentValue = null),
                Holding(fund = annanFond, netShares = 5.0, netInvested = 50.0, currentValue = null)
            ).toListUiState(),
            refreshingFundIds = persistentSetOf(fond.fundId)
        )

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        scrollToHolding()
        // useUnmergedTree: innehavsraden är klickbar och slår ihop sina barns semantik.
        composeRule.onAllNodesWithText("Hämtar …", useUnmergedTree = true).assertCountEquals(1)
        scrollToHolding(annanFond.name)
        // Totalkortet säger också "Kurs saknas ännu" (ingen fond har kurs) – här gäller raden.
        composeRule.onNode(hasText("Kurs saknas ännu") and hasAnyAncestor(hasClickAction()), useUnmergedTree = true).assertExists()
    }

    @Test
    fun innehavsraderna_har_ingen_egen_vantesnurra_under_en_kursuppdatering() {
        val state = PortfoljUiState(
            list = persistentListOf(Holding(fund = fond, netShares = 10.0, netInvested = 1000.0, currentValue = 1100.0)).toListUiState(),
            pricesWorking = true
        )

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        scrollToHolding()
        composeRule.onAllNodesWithContentDescription("Uppdaterar", substring = true, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun innehavsrad_visar_period_badge_per_fond() {
        val holding = Holding(fund = fond, netShares = 10.0, netInvested = 1000.0, currentValue = 1100.0)
        val state = PortfoljUiState(
            list = persistentListOf(holding).toListUiState(),
            performance = persistentMapOf(
                fond.fundId to PortfolioPerformanceCalc.HoldingPerformance(
                    day = PortfolioPerformanceCalc.PeriodResult.Available(amount = 30.0, fraction = 0.03),
                    week = null,
                    month = null
                )
            )
        )

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        scrollToHolding()
        // Perioderna står på en rad; vecka och månad saknar historik och står som "–", inte som en falsk nolla.
        composeRule.onNodeWithText("Dag +3,0 % 30,00 kr · Vecka – · Månad –", useUnmergedTree = true).assertExists()
        composeRule.onAllNodesWithText("Otillräcklig data", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun innehav_utan_performance_visar_otillrackligt_data_for_alla_perioder() {
        val holding = Holding(fund = fond, netShares = 10.0, netInvested = 1000.0, currentValue = 1100.0)
        val state = PortfoljUiState(list = persistentListOf(holding).toListUiState(), performance = persistentMapOf())

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        scrollToHolding()
        composeRule.onAllNodesWithText("Otillräcklig data", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun otillracklig_historik_visar_otillracklig_data_ist_for_falsk_noll() {
        // En period utan tillräcklig historik ska aldrig se ut som "+0,0 % · 0,00 kr".
        val holding = Holding(fund = fond, netShares = 10.0, netInvested = 1000.0, currentValue = 1100.0)
        val state = PortfoljUiState(
            list = persistentListOf(holding).toListUiState(),
            performance = persistentMapOf(
                fond.fundId to PortfolioPerformanceCalc.HoldingPerformance(
                    day = PortfolioPerformanceCalc.PeriodResult.InsufficientHistory,
                    week = PortfolioPerformanceCalc.PeriodResult.InsufficientHistory,
                    month = null
                )
            )
        )

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        scrollToHolding()
        composeRule.onAllNodesWithText("Otillräcklig data", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onNode(hasText("+0,0 %", substring = true) and hasAnyAncestor(hasClickAction()), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun forsta_kop_och_inkopsvarde_visas_for_innehav() {
        // netInvested < 1000 undviker tusentalsavgränsarens tvetydiga blanksteg (vanligt vs
        // hårt) i formaterad text — se MoneyFormatTest.
        val holding = Holding(
            fund = fond,
            netShares = 10.0,
            netInvested = 500.0,
            currentValue = 1100.0,
            firstPurchaseEpochDay = java.time.LocalDate.of(2024, 3, 15).toEpochDay()
        )
        val state = PortfoljUiState(list = persistentListOf(holding).toListUiState(), performance = persistentMapOf())

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        scrollToHolding()
        composeRule.onNodeWithText("Första köp 2024-03-15 · Inköpsvärde 500,00 kr", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun visar_varde_per_nav_datum_for_total_och_innehav() {
        // POR-7, issue #27 — en normal endagsförskjutning mot en extern källa (t.ex. banken)
        // ska vara begriplig, inte se ut som ett fel.
        val holding = Holding(
            fund = fond,
            netShares = 10.0,
            netInvested = 1000.0,
            currentValue = 1100.0,
            navEpochDay = java.time.LocalDate.of(2026, 7, 10).toEpochDay()
        )
        val state = PortfoljUiState(
            list = persistentListOf(holding).toListUiState(),
            totalValue = 1100.0,
            totalGainLossFraction = 0.1,
            navEpochDay = java.time.LocalDate.of(2026, 7, 10).toEpochDay()
        )

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        // Totalkortet står i listans huvud och är inte klickbart; innehavsraden är det (den öppnar Fonddetalj).
        val asOf = hasText("Värde per 2026-07-10")
        composeRule.onNode(asOf and !hasAnyAncestor(hasClickAction()), useUnmergedTree = true).assertExists()
        scrollToHolding()
        composeRule.onNode(asOf and hasAnyAncestor(hasClickAction()), useUnmergedTree = true).assertExists()
    }

    private fun keyFigures(gavFraction: Double? = 0.6) = FundAnalysisCalc.KeyFigures(
        periodReturns = FundAnalysisCalc.Period.entries.map { FundAnalysisCalc.PeriodReturn(it, amount = null, fraction = null) },
        cagr = null,
        currentNav = 160.0,
        gavPerShare = 100.0,
        gavFraction = gavFraction,
        portfolioShareFraction = null,
        annualizedVolatility = null,
        sharpeRatio = null,
        riskWindowYears = null
    )

    @Test
    fun vinstsignal_badge_visas_pa_kort_med_triggad_vinstsignal() {
        // POR-8/ANA-8, issue #26.
        val holding = Holding(fund = fond, netShares = 10.0, netInvested = 1000.0, currentValue = 1600.0)
        val analysis = FundAnalysisCalc.Analysis(
            keyFigures = keyFigures(),
            distanceFromHigh = null,
            trend = null,
            momentum = null,
            status = null,
            profitTake = FundAnalysisCalc.ProfitTakeSignal(triggered = true, gainFraction = 0.6)
        )
        val state = PortfoljUiState(
            list = persistentListOf(holding).toListUiState(),
            analysis = persistentMapOf(fond.fundId to analysis)
        )

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        scrollToHolding()
        composeRule.onNodeWithText("Vinstläge", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun vinstsignal_badge_visas_inte_utan_triggad_signal_eller_analys() {
        val holding = Holding(fund = fond, netShares = 10.0, netInvested = 1000.0, currentValue = 1100.0)
        val analysis = FundAnalysisCalc.Analysis(
            keyFigures = keyFigures(gavFraction = 0.1),
            distanceFromHigh = null,
            trend = null,
            momentum = null,
            status = null,
            profitTake = FundAnalysisCalc.ProfitTakeSignal(triggered = false, gainFraction = 0.1)
        )
        val state = PortfoljUiState(
            list = persistentListOf(holding).toListUiState(),
            analysis = persistentMapOf(fond.fundId to analysis)
        )

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        scrollToHolding()
        composeRule.onNodeWithText("Vinstläge", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    // --- Exponeringskarta (POR-9, issue #66) ---

    private val emptyDimension = PortfolioExposureCalc.Dimension(buckets = emptyList(), unknownValueKr = 0.0, unknownFraction = 0.0, unknownCount = 0)
    private val emptyIndexStatus = PortfolioExposureCalc.IndexStatusSplit(indexValueKr = 0.0, indexFraction = 0.0, activeValueKr = 0.0, activeFraction = 0.0)

    @Test
    fun exponeringskortet_visar_ratt_kategorier_och_procent() {
        val holding = Holding(fund = fond, netShares = 10.0, netInvested = 1000.0, currentValue = 1000.0)
        val exposure = PortfolioExposureCalc.Result(
            byType = PortfolioExposureCalc.Dimension(
                buckets = listOf(
                    PortfolioExposureCalc.Bucket("Aktiefond", 800.0, 0.8),
                    PortfolioExposureCalc.Bucket("Räntefond", 200.0, 0.2)
                ),
                unknownValueKr = 0.0,
                unknownFraction = 0.0,
                unknownCount = 0
            ),
            byRegion = PortfolioExposureCalc.Dimension(
                buckets = listOf(PortfolioExposureCalc.Bucket("Sverige", 1000.0, 1.0)),
                unknownValueKr = 0.0,
                unknownFraction = 0.0,
                unknownCount = 0
            ),
            indexStatus = PortfolioExposureCalc.IndexStatusSplit(indexValueKr = 300.0, indexFraction = 0.3, activeValueKr = 700.0, activeFraction = 0.7),
            includedValueKr = 1000.0,
            excludedCount = 0
        )
        val state = PortfoljUiState(list = persistentListOf(holding).toListUiState(), exposure = exposure)

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        // Varje andel är en ShareBar som läses som en enhet: "etikett, andel".
        composeRule.onNodeWithContentDescription("Aktiefond, 80,0 %").assertExists()
        composeRule.onNodeWithContentDescription("Räntefond, 20,0 %").assertExists()
        composeRule.onNodeWithContentDescription("Sverige, 100,0 %").assertExists()
        composeRule.onNodeWithContentDescription("Indexfond, 30,0 %").assertExists()
        composeRule.onNodeWithContentDescription("Aktivt förvaltad, 70,0 %").assertExists()
    }

    @Test
    fun risknivadimensionen_visas_stigande_pa_niva() {
        // Störst värde ligger medvetet på den högsta nivån — en fallande värde-sortering hade
        // gett omvänd radordning; POR-9 kräver stigande på nivå i stället (issue #71).
        val holding = Holding(fund = fond, netShares = 10.0, netInvested = 1000.0, currentValue = 1000.0)
        val exposure = PortfolioExposureCalc.Result(
            byType = emptyDimension,
            byRegion = emptyDimension,
            byRiskLevel = PortfolioExposureCalc.Dimension(
                buckets = listOf(
                    PortfolioExposureCalc.Bucket("3", 100.0, 0.1),
                    PortfolioExposureCalc.Bucket("6", 900.0, 0.9)
                ),
                unknownValueKr = 0.0,
                unknownFraction = 0.0,
                unknownCount = 0
            ),
            indexStatus = emptyIndexStatus,
            includedValueKr = 1000.0,
            excludedCount = 0
        )
        val state = PortfoljUiState(list = persistentListOf(holding).toListUiState(), exposure = exposure)

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        composeRule.onNodeWithText("Risknivå").assertExists()
        val levels = composeRule.onAllNodes(hasContentDescription("3, ", substring = true) or hasContentDescription("6, ", substring = true))
        levels.assertCountEquals(2)
        levels[0].assertContentDescriptionEquals("3, 10,0 %")
        levels[1].assertContentDescriptionEquals("6, 90,0 %")
    }

    @Test
    fun okand_region_visas_separat_och_tydligt_markt() {
        val holding = Holding(fund = fond, netShares = 10.0, netInvested = 1000.0, currentValue = 1000.0)
        val exposure = PortfolioExposureCalc.Result(
            byType = emptyDimension,
            byRegion = PortfolioExposureCalc.Dimension(
                buckets = listOf(PortfolioExposureCalc.Bucket("Sverige", 600.0, 0.6)),
                unknownValueKr = 400.0,
                unknownFraction = 0.4,
                unknownCount = 1
            ),
            indexStatus = emptyIndexStatus,
            includedValueKr = 1000.0,
            excludedCount = 0
        )
        val state = PortfoljUiState(list = persistentListOf(holding).toListUiState(), exposure = exposure)

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        composeRule.onNodeWithContentDescription("Sverige, 60,0 %").assertExists()
        composeRule.onNodeWithContentDescription("Okänd region, 40,0 %").assertExists()
    }

    @Test
    fun excludedCount_text_visas_nar_over_noll_och_uteblir_nar_noll() {
        val holding = Holding(fund = fond, netShares = 10.0, netInvested = 1000.0, currentValue = 1000.0)
        val exposureUtanExkludering = PortfolioExposureCalc.Result(
            byType = emptyDimension,
            byRegion = emptyDimension,
            indexStatus = emptyIndexStatus,
            includedValueKr = 1000.0,
            excludedCount = 0
        )
        val stateUtan = PortfoljUiState(list = persistentListOf(holding).toListUiState(), exposure = exposureUtanExkludering)

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = stateUtan, onFundClick = {}) }
        }

        composeRule.onNodeWithText("räknas inte in i exponeringen", substring = true).assertDoesNotExist()
    }

    @Test
    fun excludedCount_text_visas_nar_innehav_ar_exkluderade() {
        val holding = Holding(fund = fond, netShares = 10.0, netInvested = 1000.0, currentValue = 1000.0)
        val exposureMedExkludering = PortfolioExposureCalc.Result(
            byType = emptyDimension,
            byRegion = emptyDimension,
            indexStatus = emptyIndexStatus,
            includedValueKr = 1000.0,
            excludedCount = 2
        )
        val stateMed = PortfoljUiState(list = persistentListOf(holding).toListUiState(), exposure = exposureMedExkludering)

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = stateMed, onFundClick = {}) }
        }

        composeRule.onNodeWithText("2 innehav", substring = true).assertExists()
    }

    @Test
    fun portfolj_ar_skrollbar_med_exponeringskort_och_manga_innehav() {
        // Total- och exponeringskortet är listramens huvud och rullar med innehavsraderna — annars
        // skulle de äta skärmhöjd permanent och kunna klippa bort de sista raderna (UI-5, issue #63).
        // Raderna är riktiga lazy-rader: den sista finns inte i semantikträdet förrän listan rullats
        // dit, därför performScrollToNode på listan i stället för performScrollTo på noden.
        val funds = (1..10).map { i -> Holding(fund = Fund(fundId = "F$i", name = "Innehav $i"), netShares = 1.0, netInvested = 100.0, currentValue = 100.0) }
        val state = PortfoljUiState(list = funds.toListUiState())

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        scrollToHolding("Innehav 10")
        composeRule.onNodeWithText("Innehav 10", useUnmergedTree = true).assertIsDisplayed()
    }

    // --- Risknivå på innehavsraden (UI-10, issue #85) ---

    @Test
    fun innehavsrad_visar_risknivan() {
        val holding = Holding(fund = fond, netShares = 10.0, netInvested = 1000.0, currentValue = 1100.0)
        val state = PortfoljUiState(
            list = persistentListOf(holding).toListUiState(),
            riskLevels = persistentMapOf(fond.fundId to 5)
        )

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        scrollToHolding()
        composeRule.onNodeWithText("Risk 5/7", useUnmergedTree = true).assertExists()
    }

    @Test
    fun innehavsrad_utan_kand_risk_markeras_som_okand() {
        val holding = Holding(fund = fond, netShares = 10.0, netInvested = 1000.0, currentValue = 1100.0)
        val state = PortfoljUiState(list = persistentListOf(holding).toListUiState())

        composeRule.setContent {
            FonderTheme { PortfoljContent(state = state, onFundClick = {}) }
        }

        scrollToHolding()
        composeRule.onNodeWithText("Risk okänd", useUnmergedTree = true).assertExists()
    }
}
