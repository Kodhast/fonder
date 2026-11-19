package se.kodhast.fonder.ui.hem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.kodhast.fonder.R
import se.kodhast.fonder.domain.usecase.BackupReminder
import se.kodhast.fonder.domain.usecase.BackupReminderCalc
import se.kodhast.fonder.domain.usecase.MoneyFormat
import se.kodhast.fonder.domain.usecase.PendingDecisions
import se.kodhast.fonder.domain.usecase.PortfolioFeeCalc
import se.kodhast.fonder.domain.usecase.PortfolioPerformanceCalc
import se.kodhast.fonder.domain.usecase.TaxCalc
import se.kodhast.fonder.ui.components.ApproxTaxRow
import se.kodhast.fonder.ui.components.PeriodRow
import se.kodhast.fonder.ui.components.RiskCard
import se.kodhast.fonder.ui.components.StatusDot
import se.kodhast.fonder.ui.components.TotalCard
import se.kodhast.fonder.ui.components.copyableFundName
import se.kodhast.fonder.ui.components.statusTriggerMessages
import se.kodhast.fonder.ui.diagram.ChartSeries
import se.kodhast.fonder.ui.diagram.ChartValueAxis
import se.kodhast.fonder.ui.diagram.FundLineChart
import se.kodhast.fonder.ui.theme.Copper
import se.kodhast.fonder.ui.theme.MonoAmountStyle
import se.kodhast.fonder.ui.theme.NeutralPrimary
import se.kodhast.ui.common.DetailUiState
import se.kodhast.ui.components.AppDivider
import se.kodhast.ui.components.AppIconButton
import se.kodhast.ui.components.ButtonVariant
import se.kodhast.ui.components.EmptyAction
import se.kodhast.ui.components.EmptyState
import se.kodhast.ui.components.EntityDetailScreen
import se.kodhast.ui.components.IconButtonVariant
import se.kodhast.ui.components.ItemRow
import se.kodhast.ui.components.ItemRowText
import se.kodhast.ui.components.LeadingSlot
import se.kodhast.ui.components.NoticeBanner
import se.kodhast.ui.components.R as UiR
import se.kodhast.ui.components.SectionCard
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Spacing
import se.kodhast.ui.theme.Tone

@Composable
fun HemScreen(
    onFundClick: (String) -> Unit,
    onOpenBeslut: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onWatchFund: () -> Unit = {},
    onImportHoldings: () -> Unit = {},
    viewModel: HemViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HemContent(
        state = state,
        onRefresh = { viewModel.onEvent(HemEvent.Refresh) },
        onFundClick = onFundClick,
        onOpenBeslut = onOpenBeslut,
        onOpenSettings = onOpenSettings,
        onOpenBackup = onOpenBackup,
        onRetry = { viewModel.onEvent(HemEvent.Retry) },
        onWatchFund = onWatchFund,
        onImportHoldings = onImportHoldings,
        modifier = modifier
    )
}

/**
 * Tillståndsdriven, testbar del av [HemScreen] — inget ViewModel/Hilt-beroende (issue #14). Fliken står på
 * android-ui:s detaljram i flikläge (#266 F3): Kodhäst-sidhuvudet med Inställningar som åtgärd, ramens
 * laddning vid första läsningen och läsfelet med "Försök igen" (NFR-6), och korten som sektioner som rullar
 * under sidhuvudet (UI-5). Bakgrundshämtningen syns bara som sidhuvudets prickar (NAV-6) – inga snurror i korten.
 */
@Composable
fun HemContent(
    state: HemUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onFundClick: (String) -> Unit = {},
    onOpenBeslut: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onRetry: () -> Unit = {},
    onWatchFund: () -> Unit = {},
    onImportHoldings: () -> Unit = {}
) {
    EntityDetailScreen(
        state = state.portfolio,
        header = null,
        onBack = null,
        modifier = modifier,
        title = stringResource(R.string.nav_oversikt),
        onRetry = onRetry,
        actions = { SettingsButton(onOpenSettings) },
        // Dra ned uppdaterar (UI-11) — bara med innehav: utan dem finns ingen kurs att hämta, och en gest
        // som inte gör något är värre än ingen gest.
        refreshing = state.pricesWorking,
        // Dra ned bara över innehåll: vid laddning och läsfel är vägen "Försök igen" (NFR-6), inte en kurshämtning.
        onRefresh = onRefresh.takeIf { state.portfolio is DetailUiState.Content && !state.isEmpty }
    ) { portfolio ->
        if (!portfolio.hasHoldings) {
            // Påminnelsen (HEM-12) även här: en portfölj där allt sålts har inga innehav men fortfarande
            // transaktioner att förlora. Det tomma läget har vägarna framåt (HEM-15).
            state.backupReminder?.let { BackupReminderBanner(it, onOpenBackup) }
            EmptyState(
                icon = R.drawable.ic_overview,
                title = stringResource(R.string.hem_empty_title),
                message = stringResource(R.string.hem_empty_body),
                modifier = Modifier.padding(top = Spacing.xl),
                actions = listOf(
                    EmptyAction(stringResource(R.string.transaktionsform_watch_fund), onWatchFund, UiR.drawable.ic_add),
                    EmptyAction(stringResource(R.string.fab_import_holdings), onImportHoldings, R.drawable.ic_upload, ButtonVariant.Secondary),
                    EmptyAction(stringResource(R.string.hem_empty_restore), onOpenBackup, R.drawable.ic_restore, ButtonVariant.Text)
                )
            )
            return@EntityDetailScreen
        }
        // Ordningen enligt mockupen: det som väntar på beslut först, värdet, jämförelsen mot index och
        // påminnelsen om säkerhetskopia; resten av korten under dem.
        if (state.decisions.count > 0) DecisionsBanner(decisions = state.decisions, onOpen = onOpenBeslut)
        TotalCard(
            totalValue = portfolio.totalValue,
            totalInvested = portfolio.totalInvested,
            gainLoss = portfolio.totalGainLoss,
            gainLossFraction = portfolio.totalGainLossFraction,
            total = portfolio.total,
            navEpochDay = portfolio.navEpochDay,
            afterTotalResult = portfolio.iskTax?.let { iskTax -> { IskTaxRow(iskTax) } },
            footer = portfolio.iskTax?.let { iskTax -> { IskTaxExplanation(iskTax) } }
        )
        ReturnChartCard(portfolio = portfolio, fetchingBenchmark = state.benchmarkWorking)
        state.backupReminder?.let { BackupReminderBanner(it, onOpenBackup) }
        PerformanceCard(performance = portfolio.performance)
        AnalysisSummaryCard(summary = portfolio.analysisSummary, onFundClick = onFundClick)
        FeeCard(summary = portfolio.feeSummary, fetching = state.metadataWorking, onFundClick = onFundClick)
        portfolio.risk?.let { risk -> RiskCard(risk = risk, portfolioRisk = portfolio.portfolioRisk) }
    }
}

/** Inställningar (SET) som sidhuvudets åtgärd på Översikt (NAV-7) – Fonder kräver ingen inloggning, så kontot står inte där. */
@Composable
private fun SettingsButton(onClick: () -> Unit) = AppIconButton(
    icon = R.drawable.ic_settings,
    contentDescription = stringResource(R.string.nav_settings),
    onClick = onClick,
    variant = IconButtonVariant.Tonal,
    tone = Tone.NeutralPrimary
)

/**
 * Årets ungefärliga schablonskatt i ISK/KF (HEM-16, issue #199) direkt under totalresultatet, som i
 * mockupen: beloppet dämpat med "ca", snittunderlaget och den skattefria nivån under etiketten.
 */
@Composable
private fun IskTaxRow(iskTax: TaxCalc.IskTax) {
    ApproxTaxRow(
        label = stringResource(R.string.format_hem_isk_tax_title, iskTax.year),
        tax = iskTax.tax,
        modifier = Modifier.padding(vertical = Spacing.xs),
        basis = stringResource(
            R.string.format_hem_isk_tax_basis,
            MoneyFormat.krWhole(iskTax.averageBase),
            MoneyFormat.krWhole(iskTax.parameters.taxFreeAmount)
        )
    )
}

/** Formeln med årets parametrar ur [TaxCalc.ISK_PARAMETERS] i klartext, längst ned i totalkortet (HEM-16). */
@Composable
private fun IskTaxExplanation(iskTax: TaxCalc.IskTax) {
    ItemRowText(
        title = null,
        subtitle = stringResource(
            R.string.format_hem_isk_tax_explain,
            MoneyFormat.krWhole(iskTax.parameters.taxFreeAmount),
            MoneyFormat.rate(iskTax.parameters.standardRate),
            MoneyFormat.rate(TaxCalc.CAPITAL_INCOME_TAX),
            iskTax.year
        ),
        modifier = Modifier.padding(top = Spacing.xs)
    )
}

/** Dag/vecka/månad (HEM-2) i ett [SectionCard]. */
@Composable
private fun PerformanceCard(performance: PortfolioPerformanceCalc.PortfolioPerformance) {
    SectionCard(title = stringResource(R.string.hem_performance_title), modifier = Modifier.fillMaxWidth()) {
        val (dayAmount, dayFraction, dayPartial) = performance.day.toRowArgs()
        PeriodRow(
            label = stringResource(R.string.period_day),
            amount = dayAmount,
            fraction = dayFraction,
            partial = dayPartial,
            modifier = Modifier.padding(vertical = Spacing.xs)
        )
        val (weekAmount, weekFraction, weekPartial) = performance.week.toRowArgs()
        PeriodRow(
            label = stringResource(R.string.period_week),
            amount = weekAmount,
            fraction = weekFraction,
            partial = weekPartial,
            modifier = Modifier.padding(vertical = Spacing.xs)
        )
        val (monthAmount, monthFraction, monthPartial) = performance.month.toRowArgs()
        PeriodRow(
            label = stringResource(R.string.period_month),
            amount = monthAmount,
            fraction = monthFraction,
            partial = monthPartial,
            modifier = Modifier.padding(vertical = Spacing.xs)
        )
    }
}

/**
 * Under den här andelen oklassificerat värde sägs ingenting: en blandning som vilar på 95 % av
 * portföljen är i praktiken exakt, och en notis om varje blandfond hade blivit brus.
 */
private const val UNCLASSIFIED_NOTICE_THRESHOLD = 0.10

/**
 * Portföljens **tidsviktade** avkastning i procent över tid (HEM-9, issue #96/#116) med
 * indexjämförelsen som skuggportfölj (HEM-10) — samma delade [FundLineChart] som Fonddetalj
 * (regel 4), i procentläge ([ChartValueAxis.PROCENT]) så y-axeln alltid visar nollinjen och
 * serierna inte indexeras.
 *
 * Kurvan svarar på "hur gick fonderna", totalkortet på "hur mycket har jag tjänat på insatt
 * kapital". De två talen är därför inte samma sedan issue #116, och texten under diagrammet säger
 * det rakt ut i stället för att låta läsaren upptäcka det. Köpdagarna markeras fortfarande — de
 * flyttar inte längre kurvan, men de är det som förklarar varför de två siffrorna skiljer sig.
 *
 * Tre olika "det går inte"-lägen hålls isär i stället för att bakas ihop till ett tomt diagram:
 * ingen kurva alls (för lite historik), en kurva utan jämförelse (referensfonden inte hämtad än)
 * och en kurva räknad utan något innehav en viss dag (HEM-2:s delvis osäker).
 */
@Composable
private fun ReturnChartCard(portfolio: HemPortfolio, fetchingBenchmark: Boolean) {
    SectionCard(title = stringResource(R.string.hem_return_chart_title), modifier = Modifier.fillMaxWidth()) {
        if (portfolio.returnSeries.isEmpty) {
            Text(
                stringResource(R.string.hem_return_chart_insufficient),
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@SectionCard
        }
        val benchmark = portfolio.benchmarkSeries
        FundLineChart(
            points = portfolio.returnSeries.points,
            purchaseEpochDays = portfolio.purchaseEpochDays,
            primaryLabel = stringResource(R.string.hem_return_chart_series_portfolio),
            comparisonSeries = benchmark?.let { listOf(ChartSeries(it.fundName, it.points)) }.orEmpty(),
            valueAxis = ChartValueAxis.PROCENT
        )
        Text(
            stringResource(R.string.hem_return_chart_explain),
            style = AppTheme.typography.itemSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.s)
        )
        Text(
            when {
                benchmark != null -> stringResource(R.string.format_hem_return_chart_benchmark_explain, benchmark.fundName)

                // "Vet inte än" ser annars ut som "vet inte" (ANA-4-principen).
                fetchingBenchmark -> stringResource(R.string.hem_return_chart_benchmark_fetching)

                else -> stringResource(R.string.hem_return_chart_benchmark_missing)
            },
            style = AppTheme.typography.itemSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs)
        )
        // Vikterna vilar bara på det värde som gick att klassificera som aktier eller
        // räntor. Är resten stor är blandningen en grov approximation, och det ska synas
        // (HEM-10) — inte döljas bakom en exakt procentsats i teckenförklaringen.
        if (benchmark != null && benchmark.unclassifiedFraction >= UNCLASSIFIED_NOTICE_THRESHOLD) {
            Text(
                stringResource(
                    R.string.format_hem_return_chart_benchmark_unclassified,
                    MoneyFormat.percent(benchmark.unclassifiedFraction)
                ),
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs)
            )
        }
        if (portfolio.returnSeries.partial) {
            Text(
                stringResource(R.string.hem_return_chart_partial),
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs)
            )
        }
    }
}

/** [amount]/[fraction]/[partial] — mappar [PortfolioPerformanceCalc.PortfolioPeriodResult] till [PeriodRow]s primitiver (regel 4). Saknas värde blir kr/% null och raden visar "Otillräcklig data". */
private data class PeriodRowArgs(val amount: Double?, val fraction: Double?, val partial: Boolean)

private fun PortfolioPerformanceCalc.PortfolioPeriodResult.toRowArgs(): PeriodRowArgs = when (this) {
    is PortfolioPerformanceCalc.PortfolioPeriodResult.Available ->
        PeriodRowArgs(amount, fraction, partial)

    PortfolioPerformanceCalc.PortfolioPeriodResult.InsufficientHistory ->
        PeriodRowArgs(null, null, partial = false)
}

/** Summeringskort över gul-/rödflaggade fonder (issue #16, HEM-4): en rad per fond som öppnar Fonddetalj. */
@Composable
private fun AnalysisSummaryCard(summary: AnalysisSummary, onFundClick: (String) -> Unit) {
    SectionCard(title = stringResource(R.string.hem_analysis_title), modifier = Modifier.fillMaxWidth()) {
        if (summary.flagged.isEmpty()) {
            Text(
                stringResource(R.string.hem_analysis_empty),
                style = AppTheme.typography.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column {
                summary.flagged.forEachIndexed { index, flagged ->
                    if (index > 0) AppDivider()
                    FlaggedFundRow(flagged = flagged, onClick = { onFundClick(flagged.fund.fundId) })
                }
            }
        }
    }
}

/**
 * Portföljens totala fondavgift per år (HEM-5, issue #60), samlade besparingspotential
 * (HEM-6, issue #61) och en rad per innehav (störst avgift först, redan sorterat av
 * [PortfolioFeeCalc.compute]) som öppnar fonden i Fonddetalj — annars var totalen inte
 * handlingsbar: användaren visste vad avgifterna kostade totalt, men inte vilken fond som
 * gjorde det (issue #63). Besparingen visas bara för innehav med ett känt, färskt
 * jämförelseresultat ([PortfolioFeeCalc.HoldingFee.annualSavingsKr] null annars) — ett
 * innehav som aldrig genomsökts ser ut som just det (via "N av M genomsökta"), aldrig som
 * "inget billigare hittades".
 */
@Composable
private fun FeeCard(summary: PortfolioFeeCalc.Result, fetching: Boolean, onFundClick: (String) -> Unit) {
    SectionCard(title = stringResource(R.string.hem_fee_title), modifier = Modifier.fillMaxWidth()) {
        PeriodRow(
            label = stringResource(R.string.hem_fee_total_label),
            amount = null,
            fraction = null,
            valueText = MoneyFormat.kr(summary.totalAnnualFeeKr)
        )
        Text(
            stringResource(R.string.hem_fee_explain),
            style = AppTheme.typography.itemSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs)
        )
        if (summary.unknownFeeCount > 0) {
            Text(
                // Medan avgifterna slås upp är de okända på väg, inte okända (ANA-4-principen).
                if (fetching) stringResource(R.string.hem_fee_fetching) else stringResource(R.string.format_hem_fee_unknown, summary.unknownFeeCount),
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs)
            )
        }
        if (summary.comparableCount > 0) {
            Text(
                stringResource(
                    R.string.format_hem_fee_savings,
                    MoneyFormat.kr(summary.totalAnnualSavingsKr),
                    summary.comparedCount,
                    summary.comparableCount
                ),
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs)
            )
        }
        if (summary.byHolding.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = Spacing.s)) {
                summary.byHolding.forEachIndexed { index, holdingFee ->
                    if (index > 0) AppDivider()
                    FeeHoldingRow(holdingFee = holdingFee, onClick = { onFundClick(holdingFee.fund.fundId) })
                }
            }
        }
    }
}

/**
 * Ett innehavs avgift som en listrad som öppnar fonden. Aldrig jämfört (ingen undertext) och
 * jämfört-utan-träff ("Redan bland de billigaste", samma text som ANA-9:s kort i Fonddetalj) är skilda
 * tillstånd – annars ser ett outforskat innehav ut som redan optimalt.
 */
@Composable
private fun FeeHoldingRow(holdingFee: PortfolioFeeCalc.HoldingFee, onClick: () -> Unit) {
    val savings = holdingFee.annualSavingsKr
    ItemRow(
        title = holdingFee.fund.name,
        modifier = Modifier.copyableFundName(holdingFee.fund.name),
        subtitle = when {
            savings != null && savings > 0 -> stringResource(R.string.format_hem_fee_holding_savings, MoneyFormat.kr(savings))
            holdingFee.wasCompared -> stringResource(R.string.fee_comparison_none_cheaper)
            else -> null
        },
        trailing = { Text(MoneyFormat.kr(holdingFee.annualFeeKr), style = MonoAmountStyle.merge(AppTheme.typography.body)) },
        navigates = true,
        onClick = onClick
    )
}

@Composable
private fun FlaggedFundRow(flagged: FlaggedHolding, onClick: () -> Unit) {
    ItemRow(
        title = flagged.fund.name,
        modifier = Modifier.copyableFundName(flagged.fund.name),
        subtitle = statusTriggerMessages(flagged.analysis).joinToString(" · "),
        leading = { LeadingSlot { StatusDot(level = flagged.analysis.status!!) } },
        navigates = true,
        onClick = onClick
    )
}

/**
 * Påminnelsen om att säkerhetskopiera (HEM-12) — undertexten säger hur många transaktioner som
 * tillkommit och när senaste kopian togs, eller att ingen finns. Knappen leder till
 * Inställningars säkerhetskopiering; själva kopian tas där (SET-6, SET-7). android-ui:s [NoticeBanner] i kopparton.
 */
@Composable
private fun BackupReminderBanner(reminder: BackupReminder, onOpenBackup: () -> Unit) {
    // Lokal kopia: BackupReminder ligger i :core, och Kotlin smart-castar inte en publik egenskap från en annan modul.
    val lastBackupEpochMillis = reminder.lastBackupEpochMillis
    val body = when {
        lastBackupEpochMillis == null -> stringResource(R.string.hem_backup_reminder_never)

        reminder.newTransactions > 0 -> pluralStringResource(
            R.plurals.format_hem_backup_reminder_new_transactions,
            reminder.newTransactions,
            reminder.newTransactions,
            BackupReminderCalc.formatDate(lastBackupEpochMillis)
        )

        else -> stringResource(
            R.string.format_hem_backup_reminder_last,
            BackupReminderCalc.formatDate(lastBackupEpochMillis)
        )
    }
    NoticeBanner(
        text = stringResource(R.string.hem_backup_reminder_title),
        icon = R.drawable.ic_restore,
        onClick = onOpenBackup,
        tone = Tone.Copper,
        onClickLabel = stringResource(R.string.hem_backup_reminder_action),
        detail = body
    )
}

/**
 * "Att besluta" (HEM-13): "N beslut väntar" med "N föreslagna · M sålda, väntar på köp · K billigare alternativ"
 * ur listan Byten ([se.kodhast.fonder.domain.usecase.SwitchRows], BYT-4) under, i kopparens ton, och hela
 * bannern öppnar fliken Beslut. Visas bara när något väntar — samma antal som flikradens märke, ur samma
 * källa. Undertexten nämner bara de slag som finns.
 */
@Composable
private fun DecisionsBanner(decisions: PendingDecisions, onOpen: () -> Unit) {
    val detail = listOf(
        R.plurals.format_hem_decisions_suggestions to decisions.switches.proposedCount,
        R.plurals.format_hem_decisions_sold to decisions.switches.soldCount,
        R.plurals.format_hem_decisions_cheaper to decisions.switches.feeCount
    )
        .filter { (_, count) -> count > 0 }
        .map { (plural, count) -> pluralStringResource(plural, count, count) }
        .joinToString(" · ")
    NoticeBanner(
        text = pluralStringResource(R.plurals.format_hem_decisions_waiting, decisions.count, decisions.count),
        icon = R.drawable.ic_decisions,
        onClick = onOpen,
        tone = Tone.Copper,
        onClickLabel = stringResource(R.string.hem_decisions_action),
        detail = detail.ifEmpty { null }
    )
}
