package se.kodhast.fonder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlinx.datetime.LocalDate as KotlinLocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.domain.usecase.FundAnalysisCalc
import se.kodhast.fonder.domain.usecase.FundAnalysisCalc.SignalLevel
import se.kodhast.fonder.testing.captureLightAndDark
import se.kodhast.ui.components.AppSegmentedChoice
import se.kodhast.ui.components.ChoiceChips
import se.kodhast.ui.components.DateField
import se.kodhast.ui.components.Foldout
import se.kodhast.ui.components.SelectField

/**
 * Skärmdumpar ljust + mörkt av varje delad komponent i `ui/components` (UI-12, skill
 * testing-strategy) med påhittad men realistisk svensk data. Beteendet testas i respektive
 * komponenttest; här är målet att utseendet i M3 Expressive-temat syns i skärmdumpsdiffen.
 * I en vanlig `testDebugUnitTest` renderas komponenterna utan att bilder skrivs.
 */
@RunWith(RobolectricTestRunner::class)
class ComponentScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `ValueAsOfRow`() = composeRule.captureLightAndDark("ValueAsOfRow_default") {
        ValueAsOfRow(navEpochDay = LocalDate.of(2026, 10, 10).toEpochDay())
    }

    @Test
    fun `val som segment och chips`() = composeRule.captureLightAndDark("ChoiceControls_default") {
        val modes = listOf("Ljust", "Mörkt", "Auto")
        val answers = listOf("Under 1 år", "1–3 år", "3–10 år", "Mer än 10 år", "Vet inte")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppSegmentedChoice(options = modes, selectedIndex = 2, onSelect = {})
            ChoiceChips(options = answers, selected = "3–10 år", onSelect = {}, label = { it })
        }
    }

    @Test
    fun `PeriodRow vinst förlust och otillräcklig data`() = composeRule.captureLightAndDark("PeriodRow_variants") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PeriodRow(label = "I år", amount = 29_870.0, fraction = 0.078)
            PeriodRow(label = "1 år", amount = -4_520.0, fraction = -0.011)
            PeriodRow(label = "3 år", amount = null, fraction = null)
            PeriodRow(label = "Sedan köp", amount = 38_214.0, fraction = 0.102, stackValue = true)
            PeriodRow(label = "Volatilitet", amount = null, fraction = null, valueText = "14,8 %")
            PeriodRow(label = "Varav orealiserat", amount = 13_248.0, fraction = 0.12, muted = true)
            PeriodRow(label = "Årsavkastning (pengaviktad)", amount = null, fraction = null, insufficientText = "Visas efter ett år")
        }
    }

    @Test
    fun `PeriodRow med olika tecken pa procent och kronor`() = composeRule.captureLightAndDark("PeriodRow_mixedSigns") {
        // Issue #193: procenten är fondens kursutveckling, kronorna värdeförändringen på ägda andelar —
        // köpt efter en uppgång ger + % men − kr. Varje tal färgas efter sitt eget tecken.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PeriodRow(label = "Senaste månaden", amount = -412.0, fraction = 0.021)
            PeriodRow(label = "Senaste veckan", amount = 156.0, fraction = -0.004)
        }
    }

    @Test
    fun `RiskBadge och ProfitTakeBadge`() = composeRule.captureLightAndDark("Badges_default") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RiskBadge(level = 4)
                RiskBadge(level = 4, toLevel = 6)
                RiskBadge(level = null)
            }
            ProfitTakeBadge(gainFraction = 0.42)
        }
    }

    @Test
    fun `FundFactsCard med märke och alternativrad`() = composeRule.captureLightAndDark("FundFactsCard_default") {
        Column {
            FundFactsCard(
                facts = FundFacts(
                    name = "Räntefond Kort",
                    risk = 2,
                    totalFeePercent = 0.20,
                    oneYearFraction = 0.039,
                    type = "Räntefond, Korta räntor",
                    company = "Nordiska Fonder AB"
                ),
                badge = "Vårt förslag",
                reason = "Vald för att den är billigast bland fonderna på risknivå 2 som gått bäst det senaste året."
            )
            FundFactsRow(FundFacts(name = "Kort Ränta Index", risk = 2, totalFeePercent = 0.10, oneYearFraction = 0.034, type = "Räntefond"))
            FundFactsRow(FundFacts(name = "Nordisk Företagsobligation", risk = 2, totalFeePercent = 0.60, oneYearFraction = -0.008))
        }
    }

    @Test
    fun `FundFactsCard med okända värden`() = composeRule.captureLightAndDark("FundFactsCard_unknown") {
        FundFactsCard(facts = FundFacts(name = "Okänd Fond"))
    }

    @Test
    fun `AnalysisStatus banner och vägledning`() = composeRule.captureLightAndDark("AnalysisStatus_gul") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalysisStatusBanner(analysis = sampleAnalysis)
            AnalysisGuidanceCard(analysis = sampleAnalysis)
            AnalysisStatusBanner(analysis = null)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDot(SignalLevel.GRON)
                StatusDot(SignalLevel.GUL)
                StatusDot(SignalLevel.ROD)
            }
        }
    }

    @Test
    fun `Foldout och ExpandableInfoRow`() = composeRule.captureLightAndDark("Expandable_default") {
        Column {
            Foldout(title = "Avgifter", expanded = true, onToggle = {}) { Text("Visas när sektionen fälls ut.") }
            ExpandableInfoRow(
                explanation = "Risknivån bygger på hur mycket kursen svängt de senaste fem åren, på en skala 1–7.",
                initiallyExpanded = true
            ) {
                Text("Varför Risk 4?")
            }
        }
    }

    @Test
    fun `DateField SelectField och FollowedToggleRow`() = composeRule.captureLightAndDark("FormFields_default") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DateField(label = "Datum", date = KotlinLocalDate(2026, 8, 12), onDateChange = {})
            SelectField(
                label = "Typ",
                options = listOf("Köp", "Sälj"),
                selected = "Köp",
                optionLabel = { it },
                onSelect = {}
            )
            FollowedToggleRow(followed = true, onFollowedChange = {})
        }
    }

    @Test
    fun `ImportCompleteDialog`() = composeRule.captureLightAndDark("ImportCompleteDialog_default", screen = true) {
        ImportCompleteDialog(importedCount = 12, onDismiss = {})
    }

    private companion object {
        val sampleAnalysis = FundAnalysisCalc.Analysis(
            keyFigures = FundAnalysisCalc.KeyFigures(
                periodReturns = emptyList(),
                cagr = 0.064,
                currentNav = 312.45,
                gavPerShare = 298.10,
                gavFraction = 0.048,
                portfolioShareFraction = 0.22,
                annualizedVolatility = 0.148,
                sharpeRatio = 0.7,
                riskWindowYears = 3
            ),
            distanceFromHigh = FundAnalysisCalc.DistanceFromHighSignal(SignalLevel.GUL, -0.142),
            trend = FundAnalysisCalc.TrendSignal(SignalLevel.GUL),
            momentum = null,
            status = SignalLevel.GUL,
            profitTake = null
        )
    }
}
