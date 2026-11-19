package se.kodhast.fonder.ui.portfolj

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import se.kodhast.fonder.R
import se.kodhast.fonder.domain.model.Holding
import se.kodhast.fonder.domain.usecase.FundAnalysisCalc
import se.kodhast.fonder.domain.usecase.MoneyFormat
import se.kodhast.fonder.domain.usecase.PortfolioExposureCalc
import se.kodhast.fonder.domain.usecase.PortfolioPerformanceCalc
import se.kodhast.fonder.ui.components.ProfitTakeBadge
import se.kodhast.fonder.ui.components.RiskBadge
import se.kodhast.fonder.ui.components.StatusDot
import se.kodhast.fonder.ui.components.TotalCard
import se.kodhast.fonder.ui.components.ValueAsOfRow
import se.kodhast.fonder.ui.components.copyableFundName
import se.kodhast.fonder.ui.theme.MonoAmountStyle
import se.kodhast.fonder.ui.theme.ReturnColors
import se.kodhast.fonder.ui.transaktioner.firstTransactionActions
import se.kodhast.ui.common.ListUiState
import se.kodhast.ui.components.EmptyContent
import se.kodhast.ui.components.EntityListScreen
import se.kodhast.ui.components.ItemRow
import se.kodhast.ui.components.ListGroup
import se.kodhast.ui.components.SectionCard
import se.kodhast.ui.components.ShareBar
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Spacing

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@Composable
fun PortfoljScreen(
    onFundClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNewTransaction: () -> Unit = {},
    onImportOrders: () -> Unit = {},
    viewModel: PortfoljViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PortfoljContent(
        state = state,
        onFundClick = onFundClick,
        onRefresh = { viewModel.onEvent(PortfoljEvent.Refresh) },
        onRetry = { viewModel.onEvent(PortfoljEvent.Retry) },
        onNewTransaction = onNewTransaction,
        onImportOrders = onImportOrders,
        modifier = modifier
    )
}

/**
 * Tillståndsdriven, testbar del av [PortfoljScreen] — inget ViewModel/Hilt-beroende (issue #14). Fliken står
 * på android-ui:s listram i flikläge (#266 F3): Kodhäst-sidhuvudet med antalet fonder, totalkortet och
 * exponeringskortet överst och innehaven som listgruppen "Fonder", en rad per fond. Laddning, tomt läge med
 * vägarna framåt, "Ny transaktion" och "Importera avräkningsnota" (POR-10), och läsfel med "Försök igen"
 * (NFR-6) är ramens. Bakgrundshämtningen syns bara som sidhuvudets prickar (NAV-6).
 */
@Composable
fun PortfoljContent(
    state: PortfoljUiState,
    onFundClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onRetry: () -> Unit = {},
    onNewTransaction: () -> Unit = {},
    onImportOrders: () -> Unit = {}
) {
    val holdings = state.holdings
    val group = ListGroup(title = stringResource(R.string.portfolj_group_funds))
    EntityListScreen(
        title = stringResource(R.string.nav_innehav),
        state = state.list,
        empty = EmptyContent(
            icon = R.drawable.ic_holdings,
            title = stringResource(R.string.portfolj_empty_title),
            message = stringResource(R.string.portfolj_empty_body),
            actions = firstTransactionActions(onNewTransaction, onImportOrders)
        ),
        add = null,
        key = { it.fund.fundId },
        modifier = modifier,
        onRetry = onRetry,
        group = { group },
        subtitle = if (state.list is ListUiState.Content) pluralStringResource(R.plurals.format_portfolj_subtitle, holdings.size, holdings.size) else null,
        header = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                TotalCard(
                    totalValue = state.totalValue,
                    totalInvested = state.totalInvested,
                    gainLoss = state.totalGainLoss,
                    gainLossFraction = state.totalGainLossFraction,
                    total = state.total,
                    navEpochDay = state.navEpochDay
                )
                ExposureCard(exposure = state.exposure, fetching = state.metadataWorking)
            }
        },
        // Dra ned uppdaterar (UI-11) — bara med innehav: utan dem finns ingen kurs att hämta.
        refreshing = state.pricesWorking || state.refreshingFundIds.isNotEmpty(),
        onRefresh = onRefresh.takeIf { state.list is ListUiState.Content }
    ) { holding ->
        HoldingRow(
            holding = holding,
            performance = state.performance[holding.fund.fundId],
            analysis = state.analysis[holding.fund.fundId],
            riskLevel = state.riskLevels[holding.fund.fundId],
            fetching = state.holdingWorking(holding.fund.fundId),
            onClick = { onFundClick(holding.fund.fundId) }
        )
    }
}

/**
 * Exponeringskarta (POR-9, issue #66, risknivådimensionen tillagd i issue #71) — andel av portföljens värde
 * per fondtyp, region, risknivå och index/aktivt förvaltat, ren inventering utan köp- eller
 * rebalanseringstext. Risknivå är redan sorterad stigande på nivå av [PortfolioExposureCalc]. Okänd-hinkar
 * visas sist i sin sektion i en dämpad färg (`outline`), android-ui:s [ShareBar] (regel 4).
 */
@Composable
private fun ExposureCard(exposure: PortfolioExposureCalc.Result, fetching: Boolean) {
    SectionCard(title = stringResource(R.string.portfolj_exposure_title), modifier = Modifier.fillMaxWidth()) {
        // Medan fondfakta slås upp är okänd-hinkarna på väg, inte okända (ANA-4-principen).
        if (fetching) {
            Text(stringResource(R.string.portfolj_exposure_fetching), style = AppTheme.typography.itemSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ExposureDimensionSection(
            title = stringResource(R.string.portfolj_exposure_type_title),
            dimension = exposure.byType,
            unknownLabel = stringResource(R.string.portfolj_exposure_unknown_type)
        )
        ExposureDimensionSection(
            title = stringResource(R.string.portfolj_exposure_region_title),
            dimension = exposure.byRegion,
            unknownLabel = stringResource(R.string.portfolj_exposure_unknown_region),
            explain = stringResource(R.string.portfolj_exposure_region_explain)
        )
        ExposureDimensionSection(
            title = stringResource(R.string.portfolj_exposure_risk_title),
            dimension = exposure.byRiskLevel,
            unknownLabel = stringResource(R.string.portfolj_exposure_unknown_risk)
        )

        DimensionTitle(stringResource(R.string.portfolj_exposure_index_title))
        ShareRow(stringResource(R.string.portfolj_exposure_index_label), exposure.indexStatus.indexFraction)
        ShareRow(stringResource(R.string.portfolj_exposure_active_label), exposure.indexStatus.activeFraction)

        if (exposure.excludedCount > 0) {
            Text(
                stringResource(R.string.format_portfolj_exposure_excluded, exposure.excludedCount),
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.s)
            )
        }
    }
}

@Composable
private fun DimensionTitle(title: String) {
    Text(title, style = AppTheme.typography.itemTitle, modifier = Modifier.padding(top = Spacing.m))
}

/** En andel i exponeringskortet: android-ui:s [ShareBar] med Fonders procentformat. */
@Composable
private fun ShareRow(label: String, fraction: Double, unknown: Boolean = false) {
    ShareBar(
        label = label,
        fraction = fraction.toFloat(),
        value = MoneyFormat.percent(fraction),
        color = if (unknown) MaterialTheme.colorScheme.outline else null
    )
}

@Composable
private fun ColumnScope.ExposureDimensionSection(
    title: String,
    dimension: PortfolioExposureCalc.Dimension,
    unknownLabel: String,
    explain: String? = null
) {
    DimensionTitle(title)
    if (explain != null) {
        Text(explain, style = AppTheme.typography.itemSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    dimension.buckets.forEach { bucket -> ShareRow(bucket.label, bucket.fraction) }
    if (dimension.unknownCount > 0) ShareRow(unknownLabel, dimension.unknownFraction, unknown = true)
}

/**
 * En fond i listgruppen "Fonder" (mockupen): namnet, värdet och resultatet, perioderna på en rad (POR-5),
 * första köp och värdedatum (POR-6, POR-7) och säljsignal och vinstsignal (POR-8); risknivån (UI-10) längst
 * till höger och en pil, eftersom raden öppnar Fonddetalj. En fond utan kurs visar inköpsvärdet och "Hämtar …"
 * medan just dess kurs hämtas, annars "Kurs saknas ännu" – aldrig en snurra i raden.
 */
@Composable
private fun HoldingRow(
    holding: Holding,
    performance: PortfolioPerformanceCalc.HoldingPerformance?,
    analysis: FundAnalysisCalc.Analysis?,
    riskLevel: Int?,
    fetching: Boolean,
    onClick: () -> Unit
) {
    ItemRow(
        title = holding.fund.name,
        modifier = Modifier.copyableFundName(holding.fund.name),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                // Säljsignalen (ANA-3) bredvid risknivån, samma StatusDot som Fonddetalj och Översikt (regel 4).
                analysis?.status?.let { StatusDot(it) }
                RiskBadge(level = riskLevel)
            }
        },
        navigates = true,
        onClick = onClick,
        below = { HoldingDetails(holding, performance, analysis, fetching) }
    )
}

@Composable
private fun HoldingDetails(
    holding: Holding,
    performance: PortfolioPerformanceCalc.HoldingPerformance?,
    analysis: FundAnalysisCalc.Analysis?,
    fetching: Boolean
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val amountStyle = MonoAmountStyle.merge(AppTheme.typography.itemSubtitle)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        val value = holding.currentValue
        if (value != null) {
            val fraction = holding.gainLossFraction
            Text(
                buildAnnotatedString {
                    append(MoneyFormat.kr(value))
                    if (fraction != null) {
                        append(" · ")
                        val gain = holding.gainLoss ?: 0.0
                        withStyle(SpanStyle(color = ReturnColors.forAmount(gain))) {
                            append("${MoneyFormat.percentSigned(fraction)} · ${MoneyFormat.kr(gain)}")
                        }
                    }
                },
                style = amountStyle
            )
            Text(periodsText(performance), style = amountStyle, color = muted)
        } else {
            Text(MoneyFormat.kr(holding.netInvested), style = amountStyle, color = muted)
            Text(
                stringResource(if (fetching) R.string.holdings_fetching else R.string.portfolj_price_missing),
                style = AppTheme.typography.itemSubtitle,
                color = muted
            )
        }
        FirstPurchaseRow(holding = holding)
        if (value != null) ValueAsOfRow(navEpochDay = holding.navEpochDay)
        // Vinstsignalen (S4, ANA-8) är ingen risk och visas därför skild från säljsignalens prick (issue #26).
        analysis?.profitTake?.takeIf { it.triggered }?.let { profitTake ->
            ProfitTakeBadge(gainFraction = profitTake.gainFraction, modifier = Modifier.padding(top = Spacing.xs))
        }
    }
}

/**
 * "Dag +0,4 % +137 kr · Vecka +1,2 % +410 kr · Månad −0,8 % −274 kr" (POR-5): procent och kronor per period,
 * varje tal i sin egen vinst- eller förlustfärg (issue #193). Saknas alla tre: "Otillräcklig data"; saknas en
 * står den perioden som "–".
 */
@Composable
private fun periodsText(performance: PortfolioPerformanceCalc.HoldingPerformance?): AnnotatedString {
    val periods = listOf(
        stringResource(R.string.period_day_short) to performance?.day,
        stringResource(R.string.period_week_short) to performance?.week,
        stringResource(R.string.period_month_short) to performance?.month
    )
    val missing = stringResource(R.string.period_missing_short)
    if (periods.none { it.second is PortfolioPerformanceCalc.PeriodResult.Available }) {
        return AnnotatedString(stringResource(R.string.period_insufficient_data))
    }
    return buildAnnotatedString {
        periods.forEachIndexed { index, (label, result) ->
            if (index > 0) append(" · ")
            append(label)
            append(" ")
            when (result) {
                is PortfolioPerformanceCalc.PeriodResult.Available -> {
                    result.fraction?.let { fraction ->
                        withStyle(SpanStyle(color = ReturnColors.forAmount(fraction))) { append(MoneyFormat.percentSigned(fraction)) }
                        append(" ")
                    }
                    withStyle(SpanStyle(color = ReturnColors.forAmount(result.amount))) { append(MoneyFormat.kr(result.amount)) }
                }

                else -> append(missing)
            }
        }
    }
}

/** Första köp-datum + kvarvarande inköpsvärde (genomsnittsmetoden) för innehavet (POR-6, issue #18). */
@Composable
private fun FirstPurchaseRow(holding: Holding) {
    val firstPurchaseEpochDay = holding.firstPurchaseEpochDay ?: return
    Text(
        stringResource(
            R.string.format_holding_first_purchase,
            LocalDate.ofEpochDay(firstPurchaseEpochDay).format(dateFormatter),
            MoneyFormat.kr(holding.netInvested)
        ),
        style = AppTheme.typography.itemSubtitle,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
