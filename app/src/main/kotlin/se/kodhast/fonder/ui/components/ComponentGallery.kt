package se.kodhast.fonder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.time.LocalDate
import kotlin.math.cos
import se.kodhast.fonder.domain.usecase.FundAnalysisCalc
import se.kodhast.fonder.domain.usecase.FundAnalysisCalc.SignalLevel
import se.kodhast.fonder.domain.usecase.PortfolioTotalCalc
import se.kodhast.fonder.ui.diagram.ChartSeries
import se.kodhast.fonder.ui.diagram.ComparisonChart
import se.kodhast.fonder.ui.diagram.FundLineChart
import se.kodhast.ui.components.AppButton
import se.kodhast.ui.components.ButtonVariant
import se.kodhast.ui.components.Foldout
import se.kodhast.ui.navigation.gallery.ComponentGallery as LibraryGallery
import se.kodhast.ui.navigation.gallery.GallerySection
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Spacing

/**
 * Komponentgalleriet (debug, Inställningar → "Komponenter"): android-ui:s `ComponentGallery` med bibliotekets
 * alla delar först och Fonders egna sist ([fonderSections]) – det som hör till Fonders domän och inte finns i
 * biblioteket, med påhittad data, aldrig användarens innehav (skill shared-ui-components). Diagrammen ligger i
 * en hopfälld sektion, så att de bara ritas när man fäller ut den (Vico bygger modellen på en annan tråd).
 */
@Composable
fun ComponentGallery(onBack: () -> Unit, modifier: Modifier = Modifier) {
    LibraryGallery(onBack = onBack, modifier = modifier, appSections = fonderSections())
}

private fun fonderSections() = listOf(
    GallerySection("Fonder: belopp och andelar") { Values() },
    GallerySection("Fonder: kort, märken och val") { CardsAndBadges() },
    GallerySection("Fonder: analys och dialog") { Analysis() },
    GallerySection("Fonder: diagram") { Charts() }
)

/** Komponentens namn över exemplet – ett identifierarnamn, ingen användartext. */
@Composable
private fun Label(name: String) {
    Text(
        name,
        style = AppTheme.typography.pill,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.s)
    )
}

@Composable
private fun Values() = Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
    Label("PeriodRow · ApproxTaxRow · ValueAsOfRow")
    PeriodRow(label = "Idag", amount = 845.0, fraction = 0.0021)
    PeriodRow(label = "I år", amount = -1_960.0, fraction = -0.008)
    PeriodRow(label = "5 år", amount = null, fraction = null)
    PeriodRow(label = "Sharpe", amount = null, fraction = null, valueText = "0,6")
    ApproxTaxRow(label = "Varav skatt", tax = 3_000.0, basis = "30 % på vinst efter kvittning")
    ValueAsOfRow(navEpochDay = LocalDate.of(2026, 10, 16).toEpochDay())
    Label("FundFactsCard och FundFactsRow (Besluts infokort)")
    FundFactsCard(facts = galleryFacts, badge = "Vårt förslag", reason = "Vald för att den är billigast bland de bästa.")
    FundFactsRow(facts = galleryFacts.copy(name = "Kort Ränta Index", oneYearFraction = -0.008, company = null))
}

@Composable
private fun CardsAndBadges() = Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
    var level by remember { mutableIntStateOf(4) }
    var followed by remember { mutableStateOf(false) }
    Label("TotalCard")
    TotalCard(
        totalValue = 412_380.5,
        totalInvested = 360_000.0,
        gainLoss = 52_380.5,
        gainLossFraction = 0.1455,
        total = PortfolioTotalCalc.Result(netDepositsKr = 365_000.0, totalGainKr = 47_380.5, totalGainFraction = 0.1298, moneyWeightedAnnualReturn = 0.071, partial = false, ageDays = 812),
        navEpochDay = 20_742
    )
    Label("RiskBadge · ProfitTakeBadge · StatusDot")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
        RiskBadge(level = 3)
        RiskBadge(level = 5, toLevel = 4)
        ProfitTakeBadge(gainFraction = 0.35)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SignalLevel.entries.forEach { StatusDot(it) }
    }
    Label("ExpandableInfoRow")
    ExpandableInfoRow(explanation = "Årstakten är den genomsnittliga avkastningen per år sedan första köpet.") {
        Text("Årstakt 6,1 %")
    }
    Label("RiskLevelPicker · FollowedToggleRow")
    RiskLevelPicker(selected = level, onSelect = { level = it }, marked = 5, markedDescription = "Säljfondens nivå")
    FollowedToggleRow(followed = followed, onFollowedChange = { followed = it })
}

@Composable
private fun Analysis() = Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
    var showImport by remember { mutableStateOf(false) }
    Label("AnalysisStatusBanner · AnalysisGuidanceCard")
    AnalysisStatusBanner(analysis = galleryAnalysis)
    AnalysisGuidanceCard(analysis = galleryAnalysis)
    Label("ImportCompleteDialog")
    AppButton("Import", { showImport = true }, variant = ButtonVariant.Secondary)
    if (showImport) ImportCompleteDialog(importedCount = 7, onDismiss = { showImport = false })
}

@Composable
private fun Charts() {
    var expanded by remember { mutableStateOf(false) }
    val end = LocalDate.of(2026, 10, 16).toEpochDay()
    val primary = (0L until 60L).map { day -> (end - 59 + day) to 210.0 + 0.5 * day + 5 * cos(day / 4.0) }
    val comparison = (0L until 60L).map { day -> (end - 59 + day) to 95.0 + 0.1 * day + 2 * cos(day / 6.0) }
    Foldout(title = "Diagram", expanded = expanded, onToggle = { expanded = !expanded }) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Label("FundLineChart")
            FundLineChart(points = primary, purchaseEpochDays = listOf(end - 40, end - 10))
            Label("ComparisonChart (Pågående byte)")
            ComparisonChart(
                points = primary,
                comparisonSeries = listOf(ChartSeries("Ränta Kort", comparison)),
                available = true,
                unavailableText = "Ingen jämförelse visas.",
                primaryLabel = "Global Indexfond A"
            )
            ComparisonChart(points = primary, comparisonSeries = emptyList(), available = false, unavailableText = "Ingen jämförelse visas: kurshistoriken saknas.")
        }
    }
}

private val galleryFacts = FundFacts(
    name = "Räntefond Kort",
    risk = 2,
    totalFeePercent = 0.20,
    oneYearFraction = 0.039,
    type = "Räntefond, Korta räntor",
    company = "Nordiska Fonder AB"
)

/** Påhittad analys för statusbannern och vägledningen (aldrig riktiga innehav). */
private val galleryAnalysis = FundAnalysisCalc.Analysis(
    keyFigures = FundAnalysisCalc.KeyFigures(
        periodReturns = emptyList(),
        cagr = 0.052,
        currentNav = 184.20,
        gavPerShare = 201.75,
        gavFraction = -0.087,
        portfolioShareFraction = 0.31,
        annualizedVolatility = 0.211,
        sharpeRatio = 0.3,
        riskWindowYears = 3
    ),
    distanceFromHigh = FundAnalysisCalc.DistanceFromHighSignal(SignalLevel.ROD, -0.236),
    trend = FundAnalysisCalc.TrendSignal(SignalLevel.ROD),
    momentum = null,
    status = SignalLevel.ROD,
    profitTake = null
)
