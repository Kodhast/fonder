package se.kodhast.fonder.ui.imports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import se.kodhast.fonder.R
import se.kodhast.fonder.domain.model.Fund
import se.kodhast.fonder.domain.model.TransactionType
import se.kodhast.fonder.domain.usecase.MoneyFormat
import se.kodhast.fonder.ui.components.ImportCompleteDialog
import se.kodhast.fonder.ui.transaktioner.labelRes
import se.kodhast.ui.components.AppButton
import se.kodhast.ui.components.AppCard
import se.kodhast.ui.components.AppIconButton
import se.kodhast.ui.components.AppSegmentedChoice
import se.kodhast.ui.components.AppTextField
import se.kodhast.ui.components.ButtonVariant
import se.kodhast.ui.components.DateField
import se.kodhast.ui.components.LoadingDialog
import se.kodhast.ui.components.R as UiR
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Spacing

// Exporten är i praktiken inte en riktig zip-baserad .xlsx utan kalkylbladets råa XML
// (se HoldingsImportParser) — filväljaren behöver därför acceptera båda mime-typerna,
// beroende på vad enheten/webbläsaren råkar rapportera för den nedladdade filen.
private val IMPORT_MIME_TYPES = arrayOf(
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/xml",
    "application/xml"
)

/** Importera befintliga innehav från Handelsbankens "Innehav Fonder"-Excel-export (issue #8). */
@Composable
fun ImportHoldingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportHoldingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.onEvent(ImportHoldingsEvent.FileSelected(uri))
    }

    if (state.imported) {
        ImportCompleteDialog(importedCount = state.rows.count { it.readyToImport }, onDismiss = onBack)
        return
    }

    ImportFrame(
        title = stringResource(R.string.import_title),
        state = importListState(state.fileSelected, state.loading, state.error, state.rows),
        pick = ImportPick(
            icon = R.drawable.ic_holdings,
            title = stringResource(R.string.import_pick_file_title),
            message = stringResource(R.string.import_pick_file_body),
            button = stringResource(R.string.import_pick_file)
        ),
        // "Försök igen" efter ett läsfel väljer en ny fil (NFR-6).
        onPick = { filePicker.launch(IMPORT_MIME_TYPES) },
        onBack = onBack,
        group = stringResource(R.string.import_rows_group),
        commit = stringResource(R.string.import_commit_button, state.rows.count { it.readyToImport }),
        canCommit = state.canImport,
        onCommit = { viewModel.onEvent(ImportHoldingsEvent.Import) },
        key = { it.row.isin + it.row.shares },
        modifier = modifier
    ) { rowState ->
        ImportRowCard(
            rowState = rowState,
            catalogFunds = state.catalogFunds,
            onFundOverride = { fund -> viewModel.onEvent(ImportHoldingsEvent.FundOverride(rowState.row, fund)) },
            onIncludedChange = { included -> viewModel.onEvent(ImportHoldingsEvent.IncludedChange(rowState.row, included)) },
            onOccasionDateChange = { index, date -> viewModel.onEvent(ImportHoldingsEvent.OccasionDateChange(rowState.row, index, date)) },
            onOccasionSharesChange = { index, text -> viewModel.onEvent(ImportHoldingsEvent.OccasionSharesChange(rowState.row, index, text)) },
            onOccasionTypeChange = { index, type -> viewModel.onEvent(ImportHoldingsEvent.OccasionTypeChange(rowState.row, index, type)) },
            onAddOccasion = { viewModel.onEvent(ImportHoldingsEvent.AddOccasion(rowState.row)) },
            onRemoveOccasion = { index -> viewModel.onEvent(ImportHoldingsEvent.RemoveOccasion(rowState.row, index)) }
        )
    }

    // Fondmatchning + kursuppdatering (5 års historik per fond) tar en stund — en spärrande
    // dialog gör väntan tydlig i stället för att visa en tom, ointeraktiv lista (#266: importen behåller sin väntan).
    if (state.loading) {
        LoadingDialog(
            title = stringResource(R.string.import_loading_title),
            message = stringResource(R.string.import_loading_body)
        )
    }
}

@Composable
private fun ImportRowCard(
    rowState: ImportRowUiState,
    catalogFunds: List<Fund>,
    onFundOverride: (Fund?) -> Unit,
    onIncludedChange: (Boolean) -> Unit,
    onOccasionDateChange: (Int, LocalDate) -> Unit,
    onOccasionSharesChange: (Int, String) -> Unit,
    onOccasionTypeChange: (Int, TransactionType) -> Unit,
    onAddOccasion: () -> Unit,
    onRemoveOccasion: (Int) -> Unit
) {
    val row = rowState.row
    AppCard(modifier = Modifier.fillMaxWidth()) {
        ImportRowHeader(
            included = rowState.included,
            onIncludedChange = onIncludedChange,
            fundName = row.fundName,
            supportingText = stringResource(R.string.format_import_row_isin, row.isin),
            detail = stringResource(R.string.format_transaktion_rad, row.shares.toString(), MoneyFormat.kr(row.averageCostPerShare)),
            catalogFunds = catalogFunds,
            matchedFund = rowState.matchedFund,
            matchConfidence = rowState.matchConfidence,
            priceFetchFailed = rowState.priceFetchFailed,
            onFundOverride = onFundOverride
        )

        Text(
            stringResource(R.string.import_occasions_label),
            style = AppTheme.typography.itemTitle,
            modifier = Modifier.padding(top = Spacing.m)
        )
        rowState.occasions.forEachIndexed { index, occasion ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.s)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val types = TransactionType.entries
                    AppSegmentedChoice(
                        options = types.map { type -> stringResource(type.labelRes()) },
                        selectedIndex = types.indexOf(occasion.type),
                        onSelect = { onOccasionTypeChange(index, types[it]) }
                    )
                    DateField(
                        label = stringResource(R.string.import_date_label),
                        date = occasion.date.toKotlinLocalDate(),
                        onDateChange = { date -> onOccasionDateChange(index, date.toJavaLocalDate()) },
                        modifier = Modifier.padding(top = Spacing.s)
                    )
                    AppTextField(
                        value = occasion.sharesText,
                        onValueChange = { text -> onOccasionSharesChange(index, text) },
                        label = stringResource(R.string.import_occasion_shares_label),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                    if (!occasion.dateConfident) {
                        Text(
                            stringResource(R.string.import_date_uncertain),
                            style = AppTheme.typography.itemSubtitle,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (rowState.occasions.size > 1) {
                    AppIconButton(
                        icon = UiR.drawable.ic_close,
                        contentDescription = stringResource(R.string.import_remove_occasion),
                        onClick = { onRemoveOccasion(index) }
                    )
                }
            }
        }
        AppButton(
            text = stringResource(R.string.import_add_occasion),
            onClick = onAddOccasion,
            modifier = Modifier.padding(top = Spacing.xs),
            variant = ButtonVariant.Text
        )
        if (rowState.sharesMismatch) {
            Text(
                stringResource(
                    R.string.format_import_shares_mismatch,
                    rowState.occasionSharesTotal.toString(),
                    row.shares.toString()
                ),
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
