package se.kodhast.fonder.ui.transaktioner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import se.kodhast.fonder.R
import se.kodhast.fonder.domain.model.TransactionType
import se.kodhast.fonder.domain.usecase.MoneyFormat
import se.kodhast.fonder.domain.usecase.RealizedSale
import se.kodhast.fonder.domain.usecase.TaxCalc
import se.kodhast.fonder.ui.components.ApproxTaxRow
import se.kodhast.fonder.ui.components.PeriodRow
import se.kodhast.fonder.ui.theme.MonoAmountStyle
import se.kodhast.fonder.ui.theme.ReturnColors
import se.kodhast.ui.common.ListUiState
import se.kodhast.ui.common.toListUiState
import se.kodhast.ui.components.AppButton
import se.kodhast.ui.components.AppCard
import se.kodhast.ui.components.AppCardStyle
import se.kodhast.ui.components.AppSegmentedChoice
import se.kodhast.ui.components.ButtonVariant
import se.kodhast.ui.components.DateField
import se.kodhast.ui.components.DeleteAction
import se.kodhast.ui.components.EmptyAction
import se.kodhast.ui.components.EmptyContent
import se.kodhast.ui.components.EntityListScreen
import se.kodhast.ui.components.Foldout
import se.kodhast.ui.components.ItemRow
import se.kodhast.ui.components.ItemRowText
import se.kodhast.ui.components.R as UiR
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Spacing

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * Fliken Händelser (NAV-1, NAV-5) på android-ui:s listram `EntityListScreen`: stor rubrik som fälls
 * ihop, filtret Alla/Köp/Sälj (TRX-3) fast under rubriken i alla lägen, årets realiserade resultat
 * (TRX-4) överst i listan och alla transaktioner där sälj visar sitt realiserade resultat
 * (SLD-1…SLD-5). Laddning, tomt läge med "Ny transaktion" och "Importera avräkningsnota" och läsfel
 * med "Försök igen" enligt NFR-6. Ingen lägg till-knapp i ramen – plusmenyn står i skalet (NAV-8).
 */
@Composable
fun TransaktionerScreen(
    onOpenSwitchWatch: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onNewTransaction: () -> Unit = {},
    onImportOrders: () -> Unit = {},
    viewModel: TransaktionerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // En nystartad bevakning öppnas direkt (SLD-5) — samma skäl som på Beslut: den är tom tills
    // kandidaterna hämtats, och att bli kvar här hade sett ut som att knappen inte gjorde något.
    val openSwitchWatch by rememberUpdatedState(onOpenSwitchWatch)
    LaunchedEffect(state.startedSwitchWatchId) {
        state.startedSwitchWatchId?.let { watchId ->
            openSwitchWatch(watchId)
            viewModel.onEvent(TransaktionerEvent.SwitchWatchOpened)
        }
    }

    TransaktionerContent(
        state = state,
        onFilterSelect = { viewModel.onEvent(TransaktionerEvent.FilterSelected(it)) },
        onDeleteTransaction = { viewModel.onEvent(TransaktionerEvent.DeleteTransaction(it)) },
        onStartSwitchWatch = { viewModel.onEvent(TransaktionerEvent.StartSwitchWatch(it)) },
        onRetry = { viewModel.onEvent(TransaktionerEvent.Retry) },
        onCorrectDate = { id, date -> viewModel.onEvent(TransaktionerEvent.CorrectDate(id, date)) },
        onNewTransaction = onNewTransaction,
        onImportOrders = onImportOrders,
        modifier = modifier
    )
}

/** Tillståndsdriven, testbar del av [TransaktionerScreen] — inget ViewModel/Hilt-beroende. */
@Composable
fun TransaktionerContent(
    state: TransaktionerUiState,
    modifier: Modifier = Modifier,
    onFilterSelect: (HandelseFilter) -> Unit = {},
    onDeleteTransaction: (Long) -> Unit = {},
    onStartSwitchWatch: (TransaktionRad) -> Unit = {},
    onRetry: () -> Unit = {},
    onCorrectDate: (Long, LocalDate) -> Unit = { _, _ -> },
    onNewTransaction: () -> Unit = {},
    onImportOrders: () -> Unit = {}
) {
    EntityListScreen(
        title = stringResource(R.string.nav_handelser),
        state = state.shownList(),
        empty = emptyContent(state, onNewTransaction, onImportOrders),
        add = null,
        key = { it.transaction.id },
        modifier = modifier,
        onRetry = onRetry,
        filter = {
            val filters = HandelseFilter.entries
            AppSegmentedChoice(
                options = filters.map { stringResource(it.labelRes()) },
                selectedIndex = filters.indexOf(state.filter),
                onSelect = { onFilterSelect(filters[it]) }
            )
        },
        header = if (state.showRealizedCard) {
            { RealizedCard(state) }
        } else {
            null
        }
    ) { row ->
        HandelseRow(
            row = row,
            onDelete = { onDeleteTransaction(row.transaction.id) },
            onStartSwitchWatch = onStartSwitchWatch,
            onCorrectDate = onCorrectDate
        )
    }
}

/**
 * Listan ramen visar: laddning och fel som de är, annars raderna i det valda filtret (TRX-3) – ett
 * filter utan träffar blir ramens tomma läge, med filtret kvar ovanför.
 */
private fun TransaktionerUiState.shownList(): ListUiState<TransaktionRad> = when (list) {
    is ListUiState.Content -> rows.toListUiState()
    else -> list
}

/**
 * Det tomma läget: inga transaktioner alls ger vägarna framåt, samma som plusmenyns (F1, TRX-7) – ett
 * tomt filter (inga köp, inga sälj, SLD-1) bara sitt budskap.
 */
@Composable
private fun emptyContent(state: TransaktionerUiState, onNewTransaction: () -> Unit, onImportOrders: () -> Unit): EmptyContent = when {
    state.isEmpty -> EmptyContent(
        icon = R.drawable.ic_receipt,
        title = stringResource(R.string.transaktioner_empty_title),
        message = stringResource(R.string.transaktioner_empty_body),
        actions = firstTransactionActions(onNewTransaction, onImportOrders)
    )

    state.filter == HandelseFilter.SALJ -> EmptyContent(
        icon = R.drawable.ic_receipt,
        title = stringResource(R.string.sold_funds_empty_title),
        message = stringResource(R.string.sold_funds_empty_body)
    )

    else -> EmptyContent(
        icon = R.drawable.ic_receipt,
        title = stringResource(R.string.handelser_buy_empty_title),
        message = stringResource(R.string.handelser_buy_empty_body)
    )
}

/**
 * Vägarna till den första transaktionen i ett tomt läge (F1): "Ny transaktion" och "Importera
 * avräkningsnota", samma val och texter som plusmenyn (NAV-8) – Händelser och Innehav (TRX-7, POR-10).
 */
@Composable
fun firstTransactionActions(onNewTransaction: () -> Unit, onImportOrders: () -> Unit): List<EmptyAction> = listOf(
    EmptyAction(stringResource(R.string.transaktioner_add), onNewTransaction, UiR.drawable.ic_add),
    EmptyAction(stringResource(R.string.fab_import_orders), onImportOrders, R.drawable.ic_upload, ButtonVariant.Secondary)
)

private fun HandelseFilter.labelRes(): Int = when (this) {
    HandelseFilter.ALLA -> R.string.handelser_filter_all
    HandelseFilter.KOP -> R.string.transaktion_kop
    HandelseFilter.SALJ -> R.string.transaktion_salj
}

/**
 * "Realiserat resultat i år" (TRX-4) och, i säljfiltret, totalen sedan start (SLD-3). Ett osäkert
 * resultat (SLD-2) sägs ut i text under raderna, aldrig bara med färg. I depå/AF (TRX-8) delas
 * årets resultat upp i vinster och förluster, och den ungefärliga skatten – på vinsten efter
 * kvittning och på schablonintäkten – står under, dämpad och med "ca", följd av en förklaring.
 */
@Composable
private fun RealizedCard(state: TransaktionerUiState, modifier: Modifier = Modifier) {
    val showTotal = state.filter == HandelseFilter.SALJ
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            if (state.showRealizedRow) {
                PeriodRow(
                    label = stringResource(R.string.handelser_realized_year),
                    amount = state.yearRealized.realizedGain,
                    fraction = state.yearRealized.realizedGainFraction,
                    supportingLabel = if (state.showRealizedTax) {
                        stringResource(
                            R.string.format_handelser_realized_split,
                            MoneyFormat.krWhole(state.yearRealizedTax.gains),
                            MoneyFormat.krWhole(state.yearRealizedTax.losses)
                        )
                    } else {
                        null
                    }
                )
            }
            if (showTotal && state.showRealizedRow) {
                PeriodRow(
                    label = stringResource(R.string.sold_funds_total_title),
                    amount = state.totalRealized.realizedGain,
                    fraction = state.totalRealized.realizedGainFraction
                )
            }
            // Osäkert (SLD-2) hör till resultatet, så det står direkt under resultatraderna — före skatten.
            if (state.yearRealized.uncertain || (showTotal && state.totalRealized.uncertain)) {
                Text(
                    stringResource(R.string.handelser_realized_uncertain),
                    style = AppTheme.typography.itemSubtitle,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (state.showRealizedTax) {
                ApproxTaxRow(
                    label = stringResource(R.string.handelser_tax_realized),
                    tax = state.yearRealizedTax.tax,
                    basis = stringResource(R.string.format_handelser_tax_realized_basis, MoneyFormat.rate(TaxCalc.CAPITAL_INCOME_TAX))
                )
            }
            state.standardIncomeTax?.let { standardIncome ->
                ApproxTaxRow(
                    label = stringResource(R.string.handelser_tax_standard_income),
                    tax = standardIncome.tax,
                    basis = stringResource(
                        R.string.format_handelser_tax_standard_income_basis,
                        MoneyFormat.rate(TaxCalc.FUND_STANDARD_INCOME_RATE),
                        MoneyFormat.krWhole(standardIncome.valueAtYearStart),
                        MoneyFormat.rate(TaxCalc.CAPITAL_INCOME_TAX)
                    )
                )
            }
            if (state.depa && (state.showRealizedTax || state.standardIncomeTax != null)) {
                ItemRowText(title = null, subtitle = stringResource(R.string.handelser_tax_explain))
            }
        }
    }
}

/**
 * En transaktion (TRX-1): datum, fondnamn, typ och belopp med tecken — in (sälj) eller ut (köp) — på
 * bibliotekets `Foldout`. Ett sälj visar sitt realiserade resultat i undertexten, osäkert markerat i text
 * (SLD-2), ett uppskattat datum likaså (TRX-9). Tryck fäller ut detaljerna (SLD-4); Radera står sist i det
 * utfällda, och svep åt vänster begär samma radering – båda frågar först (TRX-2, android-ui #118).
 */
@Composable
private fun HandelseRow(
    row: TransaktionRad,
    onDelete: () -> Unit,
    onStartSwitchWatch: (TransaktionRad) -> Unit,
    onCorrectDate: (Long, LocalDate) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Foldout(
        title = row.fundName,
        expanded = expanded,
        onToggle = { expanded = !expanded },
        // Raden dras in lite från kortets kant, så att chevronen inte står mot kanten; svepets
        // bakgrund följer samma rundade rad.
        modifier = Modifier.padding(horizontal = Spacing.s),
        contentSpacing = Spacing.xs,
        header = {
            // I en Box: ItemRow fyller bredden, och vikten måste sitta på radens direkta barn för
            // att chevronen ska få plats.
            Box(Modifier.weight(1f)) { HandelseRowText(row) }
        },
        actions = if (row.canStartSwitchWatch) {
            {
                AppButton(
                    text = stringResource(R.string.switch_watch_start),
                    onClick = { onStartSwitchWatch(row) },
                    variant = ButtonVariant.Text,
                    compact = true
                )
            }
        } else {
            null
        },
        delete = DeleteAction(
            title = stringResource(R.string.transaktion_delete_title),
            message = stringResource(R.string.format_transaktion_delete_confirm, row.fundName),
            onConfirm = onDelete
        )
    ) {
        HandelseDetails(row = row, onCorrectDate = onCorrectDate)
    }
}

/** Radens text: datum, fondnamn, typ och belopp med tecken — in (sälj) eller ut (köp). */
@Composable
private fun HandelseRowText(row: TransaktionRad) {
    val tx = row.transaction
    ItemRow(
        title = row.fundName,
        subtitle = subtitle(tx.type, row.sale, tx.dateEstimated),
        leading = {
            Text(
                LocalDate.ofEpochDay(tx.epochDay).format(dateFormatter),
                style = MonoAmountStyle.merge(AppTheme.typography.itemSubtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailing = {
            val isSell = tx.type == TransactionType.SALJ
            Text(
                MoneyFormat.krSigned(if (isSell) tx.amount else -tx.amount),
                style = MonoAmountStyle.merge(AppTheme.typography.body),
                color = if (isSell) ReturnColors.gain else MaterialTheme.colorScheme.onSurface
            )
        }
    )
}

/**
 * "Köp", eller "Sälj · vinst +2 140,00 kr" med "· osäkert" när köphistoriken inte räcker (SLD-2), och
 * "· uppskattat datum" när importen gissade dagen (TRX-9).
 */
@Composable
private fun subtitle(type: TransactionType, sale: RealizedSale?, dateEstimated: Boolean): String {
    val text = saleSubtitle(type, sale)
    return if (dateEstimated) stringResource(R.string.format_handelse_estimated_date, text) else text
}

@Composable
private fun saleSubtitle(type: TransactionType, sale: RealizedSale?): String {
    val typeLabel = stringResource(type.labelRes())
    if (sale == null) return typeLabel
    val gain = sale.realizedGain
    val text = stringResource(
        if (gain >= 0.0) R.string.format_handelse_sell_gain else R.string.format_handelse_sell_loss,
        typeLabel,
        MoneyFormat.krSigned(gain)
    )
    return if (sale.uncoveredShares > 0.0) stringResource(R.string.format_handelse_uncertain, text) else text
}

/**
 * Utfällda detaljer (SLD-4): andelar och kurs, för ett sälj även belopp, avgift,
 * anskaffningsvärde, resultatet i kr + % och en ev. osäkerhetsvarning (SLD-2). Ett uppskattat datum
 * förklaras och kan rättas här (TRX-9). "Bevaka alternativ" (SLD-5) och Radera står under, i radens knappar.
 */
@Composable
private fun HandelseDetails(row: TransaktionRad, onCorrectDate: (Long, LocalDate) -> Unit) {
    val tx = row.transaction
    val sale = row.sale
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(
            stringResource(R.string.format_transaktion_rad, tx.shares.toString(), MoneyFormat.kr(tx.pricePerShare)),
            style = MonoAmountStyle.merge(AppTheme.typography.itemSubtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (sale == null) {
            if (tx.fee > 0.0) {
                Text(
                    stringResource(R.string.format_transaktion_fee, MoneyFormat.kr(tx.fee)),
                    style = MonoAmountStyle.merge(AppTheme.typography.itemSubtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            SaleDetails(sale)
        }
        if (tx.dateEstimated) {
            Text(
                stringResource(R.string.handelse_estimated_date_explain),
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DateField(
                label = stringResource(R.string.handelse_correct_date),
                date = LocalDate.ofEpochDay(tx.epochDay).toKotlinLocalDate(),
                onDateChange = { onCorrectDate(tx.id, it.toJavaLocalDate()) }
            )
        }
    }
}

/** Säljets belopp, avgift och anskaffningsvärde, resultatet i kr + % och en ev. osäkerhetsvarning (SLD-2, SLD-4). */
@Composable
private fun SaleDetails(sale: RealizedSale) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            stringResource(
                R.string.format_sold_fund_detail,
                MoneyFormat.kr(sale.proceeds),
                MoneyFormat.kr(sale.fee),
                MoneyFormat.kr(sale.costBasis)
            ),
            style = MonoAmountStyle.merge(AppTheme.typography.itemSubtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PeriodRow(
            label = stringResource(R.string.handelser_realized_row),
            amount = sale.realizedGain,
            fraction = sale.realizedGainFraction
        )
        if (sale.uncoveredShares > 0.0) {
            Text(
                stringResource(R.string.sold_fund_uncovered_warning),
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
