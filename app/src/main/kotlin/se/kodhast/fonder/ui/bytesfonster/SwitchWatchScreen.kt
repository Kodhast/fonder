package se.kodhast.fonder.ui.bytesfonster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import se.kodhast.fonder.R
import se.kodhast.fonder.data.common.DataError
import se.kodhast.fonder.domain.model.Fund
import se.kodhast.fonder.domain.usecase.MoneyFormat
import se.kodhast.fonder.domain.usecase.SwitchWatchCalc
import se.kodhast.fonder.ui.components.PeriodRow
import se.kodhast.fonder.ui.components.RISK_LEVELS
import se.kodhast.fonder.ui.components.RiskBadge
import se.kodhast.fonder.ui.components.copyableFundName
import se.kodhast.fonder.ui.diagram.ChartSeries
import se.kodhast.fonder.ui.diagram.ComparisonChart
import se.kodhast.fonder.ui.navigation.PickedFundEffect
import se.kodhast.ui.common.DetailUiState
import se.kodhast.ui.components.AppButton
import se.kodhast.ui.components.AppCard
import se.kodhast.ui.components.ButtonVariant
import se.kodhast.ui.components.ChoiceChips
import se.kodhast.ui.components.DeleteAction
import se.kodhast.ui.components.DeleteConfirmDialog
import se.kodhast.ui.components.EntityDetailScreen
import se.kodhast.ui.components.GroupLabel
import se.kodhast.ui.components.ItemRowText
import se.kodhast.ui.components.R as UiR
import se.kodhast.ui.components.SectionCard
import se.kodhast.ui.components.SwipeToDelete
import se.kodhast.ui.components.SwitchRow
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Spacing

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * Skärmen **Pågående byte** (ANA-12, issue #114) — den yta som saknades mellan "sålt" och
 * "köpt". Leder med jämförelsen: ett diagram med säljfonden och samtliga bevakade alternativ,
 * följt av en rad per alternativ med det tal beslutet faktiskt hänger på — utvecklingen sedan
 * säljdagen, inte fondens historik i stort.
 *
 * Ingen åtgärdsknapp utför något byte (samma princip som HEM-8/ANA-10): "Köpte den här"
 * registrerar vad användaren gjort och stänger bevakningen.
 */
@Composable
fun SwitchWatchScreen(
    watchId: Long,
    onAddCandidate: () -> Unit,
    onBuy: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    pickedCandidate: Fund? = null,
    onPickedCandidateAcknowledge: () -> Unit = {},
    viewModel: SwitchWatchViewModel = hiltViewModel<SwitchWatchViewModel, SwitchWatchViewModel.Factory> { it.create(watchId) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Alternativet som valts i Fondsöks valläge (ANA-13) läggs till av den här skärmens ViewModel, som
    // ligger kvar och visar meddelandet om ISIN saknas eller taket är nått.
    PickedFundEffect(
        fund = pickedCandidate,
        onPick = { viewModel.onEvent(SwitchWatchEvent.AddCandidate(it)) },
        onAcknowledge = onPickedCandidateAcknowledge
    )

    // En stängd bevakning har ingen egen skärm att fylla — kvitteringen är avslutet, och att
    // ligga kvar på en död vy hade sett ut som att något mer skulle hända.
    val close by rememberUpdatedState(onClose)
    val closed = state.content.closed
    LaunchedEffect(closed) {
        if (closed) close()
    }

    SwitchWatchContent(
        state = state,
        onRefresh = { viewModel.onEvent(SwitchWatchEvent.Refresh) },
        onBuy = onBuy,
        onRemoveCandidate = { viewModel.onEvent(SwitchWatchEvent.RemoveCandidate(it)) },
        onAddCandidate = onAddCandidate,
        onCancel = { viewModel.onEvent(SwitchWatchEvent.Cancel) },
        onMessageShow = { viewModel.onEvent(SwitchWatchEvent.MessageShown) },
        onRetryAutoFill = { viewModel.onEvent(SwitchWatchEvent.RetryAutoFill) },
        onRetry = { viewModel.onEvent(SwitchWatchEvent.Retry) },
        onBack = onClose,
        onSelectTargetLevel = { viewModel.onEvent(SwitchWatchEvent.SelectTargetLevel(it)) },
        onIndexOnlyChange = { viewModel.onEvent(SwitchWatchEvent.IndexOnlyChanged(it)) },
        modifier = modifier
    )
}

/**
 * Tillståndsdriven, testbar del av [SwitchWatchScreen] — inget ViewModel/Hilt-beroende. Står på android-ui:s
 * detaljram (#266 F6): liten topprad med tillbakapil ([onBack]), laddning, läsfel med "Försök igen" (NFR-6) och
 * dra ned för att uppdatera. En bevakning som inte finns (raderad, eller en gammal länk) säger det med sin egen text
 * och utan "Försök igen" – den blir inte bättre av ett nytt försök.
 */
@Composable
fun SwitchWatchContent(
    state: SwitchWatchUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onBuy: (String) -> Unit = {},
    onRemoveCandidate: (Long) -> Unit = {},
    onAddCandidate: () -> Unit = {},
    onCancel: () -> Unit = {},
    onMessageShow: () -> Unit = {},
    onRetryAutoFill: () -> Unit = {},
    onRetry: () -> Unit = {},
    onSelectTargetLevel: (Int) -> Unit = {},
    onIndexOnlyChange: (Boolean) -> Unit = {}
) {
    val missing = state.watch.let { it is DetailUiState.Error && it.error.cause == DataError.NotFound }
    val watch = state.watch.let { watch ->
        if (missing && watch is DetailUiState.Error) {
            DetailUiState.Error(watch.error.withMessage(R.string.switch_watch_missing_body))
        } else {
            watch
        }
    }
    EntityDetailScreen(
        state = watch,
        header = null,
        onBack = onBack,
        title = stringResource(R.string.switch_watch_title),
        errorTitle = if (missing) stringResource(R.string.switch_watch_missing_title) else null,
        onRetry = onRetry,
        // Dra ned hämtar om kandidaternas kurvor och säljfondens historik (UI-11). Kandidaterna
        // ligger inte i kurscachen (ANA-11), så det är den enda vägen till färskare tal än de som
        // hämtades när skärmen öppnades. Bara på en läst bevakning: ett läsfel eller en borttagen
        // bevakning har inget att hämta om, och första läsningen visar ramens egen laddning.
        refreshing = state.chartWorking && !state.loading,
        onRefresh = onRefresh.takeIf { watch is DetailUiState.Content },
        modifier = modifier
    ) {
        SwitchWatchSections(
            state = state,
            onBuy = onBuy,
            onRemoveCandidate = onRemoveCandidate,
            onAddCandidate = onAddCandidate,
            onCancel = onCancel,
            onMessageShow = onMessageShow,
            onRetryAutoFill = onRetryAutoFill,
            onSelectTargetLevel = onSelectTargetLevel,
            onIndexOnlyChange = onIndexOnlyChange
        )
    }
}

/** Bevakningens sektioner: huvud, väljare, diagram och alternativ. */
@Composable
private fun ColumnScope.SwitchWatchSections(
    state: SwitchWatchUiState,
    onBuy: (String) -> Unit,
    onRemoveCandidate: (Long) -> Unit,
    onAddCandidate: () -> Unit,
    onCancel: () -> Unit,
    onMessageShow: () -> Unit,
    onRetryAutoFill: () -> Unit,
    onSelectTargetLevel: (Int) -> Unit,
    onIndexOnlyChange: (Boolean) -> Unit
) {
    val content = state.content
    HeaderCard(state)
    // Nivå och filter går bara att ändra medan bytet pågår (ANA-13) — en stängd
    // bevakning bär vad som hände och visar ingen väljare.
    if (state.canChangeSelection) {
        TargetLevelCard(
            state = state,
            onSelectTargetLevel = onSelectTargetLevel,
            onIndexOnlyChange = onIndexOnlyChange
        )
    }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        ComparisonChart(
            points = content.sellSeries,
            primaryLabel = content.sellFundName,
            comparisonSeries = content.candidateSeries.map { (label, points) -> ChartSeries(label = label, points = points) },
            available = content.hasChart,
            loading = state.chartWorking,
            unavailableText = stringResource(R.string.switch_watch_chart_unavailable)
        )
    }

    if (content.rows.isEmpty()) {
        // Ingen `EmptyState` här: den fyller skärmen och hade tryckt bort kortet med
        // säljfond, belopp och dag — den informationen gäller även utan alternativ.
        EmptyCandidates(state)
        // Bara när kontrollen misslyckades: "inga köpbara" och "okänd nivå" blir inte
        // annorlunda av ett nytt försök, ett misslyckat uppslag kan bli det.
        if (content.emptyReason == SwitchWatchEmptyReason.LOOKUP_FAILED) {
            AppButton(
                text = stringResource(R.string.switch_watch_retry),
                onClick = onRetryAutoFill,
                variant = ButtonVariant.Text
            )
        }
    }

    // Rubriken över alternativen: vad talen mäter och vilken nivå (och filter) förslagen gäller.
    if (content.rows.isNotEmpty() && content.targetLevel != null) {
        GroupLabel(
            stringResource(
                if (content.indexOnly) {
                    R.string.format_switch_watch_alternatives_heading_index
                } else {
                    R.string.format_switch_watch_alternatives_heading
                },
                content.targetLevel
            )
        )
    }

    content.rows.forEach { row ->
        key(row.candidateId) {
            CandidateCard(
                row = row,
                closed = content.closed,
                onBuy = onBuy,
                onRemoveCandidate = onRemoveCandidate
            )
        }
    }

    Column {
        if (state.canAddCandidate) {
            AppButton(
                text = stringResource(R.string.switch_watch_add_candidate),
                onClick = onAddCandidate,
                variant = ButtonVariant.Text
            )
        }
        state.message?.let { message ->
            Text(
                text = when (message) {
                    SwitchWatchMessage.IsinUnavailable -> stringResource(R.string.switch_watch_isin_unavailable)

                    SwitchWatchMessage.CandidateLimitReached ->
                        stringResource(R.string.format_switch_watch_limit, SwitchWatchCalc.MAX_CANDIDATES)
                },
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.error
            )
            // Meddelandet är en kvittens på en åtgärd, inte ett tillstånd — det kvitteras
            // när det visats så nästa tillägg inte möts av föregående försöks felmeddelande.
            val messageShown by rememberUpdatedState(onMessageShow)
            LaunchedEffect(message) { messageShown() }
        }
        if (!content.closed) {
            AppButton(
                text = stringResource(R.string.switch_watch_cancel),
                onClick = onCancel,
                variant = ButtonVariant.Text
            )
        }
        if (state.canChangeSelection) {
            Text(
                stringResource(R.string.switch_watch_level_footer),
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.s)
            )
        }
        Text(
            stringResource(R.string.switch_watch_disclaimer),
            style = AppTheme.typography.itemSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.s)
        )
    }
}

/**
 * Varför det inte finns några alternativ. Medan appens förslag hämtas säger texten det ("Hämtar förslag på
 * alternativ…") – ingen snurra (#266: en gemensam laddningsindikator); "inga alternativ ännu" är ett färdigt svar.
 */
@Composable
private fun EmptyCandidates(state: SwitchWatchUiState) {
    val content = state.content
    val text = when {
        state.fillingCandidates -> stringResource(R.string.switch_watch_filling)

        // Med filtret är "inga köpbara indexfonder" ett annat besked än inga
        // köpbara fonder alls — och det säger vad som går att ändra (issue #150).
        content.emptyBecauseOfIndexFilter && content.targetLevel != null ->
            stringResource(R.string.format_switch_watch_empty_index_body, content.targetLevel)

        // Tre orsaker som förr delade samma text (issue #149) — var och en
        // säger vad användaren kan göra åt den.
        else -> when (content.emptyReason) {
            SwitchWatchEmptyReason.NO_TARGET_LEVEL ->
                stringResource(R.string.format_switch_watch_empty_no_level, content.sellFundName)

            SwitchWatchEmptyReason.NONE_AVAILABLE ->
                content.targetLevel
                    ?.let { stringResource(R.string.format_switch_watch_empty_none_available, it) }
                    ?: stringResource(R.string.switch_watch_empty_body)

            SwitchWatchEmptyReason.LOOKUP_FAILED -> stringResource(R.string.switch_watch_empty_lookup_failed)

            null -> stringResource(R.string.switch_watch_empty_body)
        }
    }
    val title = if (!state.fillingCandidates && content.emptyBecauseOfIndexFilter && content.targetLevel != null) {
        stringResource(R.string.format_switch_watch_empty_index_title, content.targetLevel)
    } else {
        null
    }
    ItemRowText(title = title, subtitle = text)
}

/** Vad som såldes, för hur mycket och hur länge sedan — sammanhanget varje rad under läses i. */
@Composable
private fun HeaderCard(state: SwitchWatchUiState) {
    val content = state.content
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(
                R.string.format_switch_watch_sold,
                content.sellFundName,
                LocalDate.ofEpochDay(content.soldAtEpochDay).format(dateFormatter)
            ),
            style = MaterialTheme.typography.titleSmall
        )
        content.proceedsKr?.let { amount ->
            Text(
                stringResource(R.string.format_switch_watch_amount, MoneyFormat.kr(amount)),
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val status = when {
            content.closed && content.boughtFundName != null ->
                stringResource(R.string.format_switch_watch_bought, content.boughtFundName)

            content.closed -> stringResource(R.string.switch_watch_closed)

            content.expired -> stringResource(R.string.format_switch_watch_expired, content.ttlDays)

            else -> stringResource(R.string.format_switch_watch_day, content.daysWaiting, content.ttlDays)
        }
        Text(
            status,
            style = AppTheme.typography.itemSubtitle,
            color = if (content.expired && !content.closed) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}

/**
 * Risknivån appen föreslår fonder på och filtret "Bara indexfonder" (ANA-13, issue #150). Nivån
 * väljs bland bibliotekets [ChoiceChips] (#280 C); säljfondens egen nivå står i texten under, så
 * användaren ser vad "något likvärdigt" hade varit; filtret är en delad [SwitchRow]. Båda byter appens förslag —
 * ViewModeln tar bort de gamla och hämtar nya, de handplockade ligger kvar.
 */
@Composable
private fun TargetLevelCard(
    state: SwitchWatchUiState,
    onSelectTargetLevel: (Int) -> Unit,
    onIndexOnlyChange: (Boolean) -> Unit
) {
    val content = state.content
    SectionCard(title = stringResource(R.string.switch_watch_level_title), modifier = Modifier.fillMaxWidth()) {
        ChoiceChips(
            options = RISK_LEVELS.toList(),
            selected = content.targetLevel,
            onSelect = { level -> level?.let(onSelectTargetLevel) },
            label = { it.toString() }
        )
        Text(
            levelHint(state),
            style = AppTheme.typography.itemSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.s)
        )
        SwitchRow(
            title = stringResource(R.string.switch_watch_index_only),
            checked = content.indexOnly,
            onCheckedChange = onIndexOnlyChange,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}

/**
 * Förklaringsraden under nivåväljaren (issue #150). Med riskprofil säger den vad profilen saknar
 * på den valda nivån ("9 procentenheter för lite där") eller, när en annan nivå är vald, var
 * profilen saknar mest; utan profil vilken nivå appen föreslår på och var säljfondens egen nivå
 * ligger. Texten säger aldrig att nivån *förvaldes*: bevakningen bär inte vem som valde den (en
 * planrad, profilen eller användaren), och ett sådant fält finns inte i backupkontraktet.
 */
@Composable
private fun levelHint(state: SwitchWatchUiState): String {
    val content = state.content
    val target = content.targetLevel ?: return stringResource(R.string.switch_watch_level_hint_none)
    val sell = content.sellLevel
    val profileLevel = content.profileSuggestedLevel
    val gap = content.profileGapPp
    return when {
        profileLevel != null && gap != null && profileLevel == target -> {
            val reason = stringResource(R.string.format_switch_watch_level_hint_profile, target, gap)
            when (sell) {
                null -> reason
                target -> "$reason ${stringResource(R.string.switch_watch_level_hint_sell_same)}"
                else -> "$reason ${stringResource(R.string.format_switch_watch_level_hint_sell, sell)}"
            }
        }

        profileLevel != null -> stringResource(R.string.format_switch_watch_level_hint_profile_other, target, profileLevel)

        sell == null -> stringResource(R.string.format_switch_watch_level_hint_unknown_sell, target)

        sell == target -> stringResource(R.string.format_switch_watch_level_hint_same, target)

        else -> stringResource(R.string.format_switch_watch_level_hint, target, sell)
    }
}

/**
 * Ett bevakat alternativ: namn, risknivå (UI-10), avgift och — talet beslutet hänger på —
 * utvecklingen sedan säljdagen via den delade [PeriodRow] (regel 4), som färgar efter procenten
 * och markerar en ej utvärderad rad som otillräcklig data i stället för som 0.
 */
@Composable
private fun CandidateCard(
    row: CandidateRow,
    closed: Boolean,
    onBuy: (String) -> Unit,
    onRemoveCandidate: (Long) -> Unit
) {
    // Bara handplockade alternativ går att radera: de automatiska är appens förslag för nivån, och att
    // plocka bort ett av dem hade sett ut som ett val men bara dolt vad planen faktiskt föreslår.
    // Raderingen frågar först, och svep åt vänster begär samma sak (android-ui #118).
    val deletable = !closed && row.manual
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (deletable) {
        SwipeToDelete({ confirmDelete = true }) {
            CandidateCardBody(row, closed, onBuy, onDelete = { confirmDelete = true })
        }
    } else {
        CandidateCardBody(row, closed, onBuy, onDelete = null)
    }
    if (confirmDelete) {
        DeleteConfirmDialog(
            DeleteAction(
                title = stringResource(R.string.format_switch_watch_remove_title, row.name),
                message = stringResource(R.string.switch_watch_remove_message),
                onConfirm = { onRemoveCandidate(row.candidateId) }
            ),
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun CandidateCardBody(row: CandidateRow, closed: Boolean, onBuy: (String) -> Unit, onDelete: (() -> Unit)?) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = Spacing.s).copyableFundName(row.name)
            )
            RiskBadge(level = row.riskLevel)
        }
        // Utvecklingen sedan säljdagen är talet beslutet hänger på; medan kandidatens historik
        // hämtas säger raden det i text, aldrig som 0 och aldrig med en snurra (#266).
        PeriodRow(
            label = stringResource(R.string.switch_watch_change_label),
            amount = row.changeKr,
            fraction = row.changeFraction,
            partial = row.partial,
            stackValue = true,
            insufficientText = if (row.historyLoading) stringResource(R.string.holdings_fetching) else null,
            modifier = Modifier.padding(top = Spacing.s)
        )
        row.feePercent?.let { fee ->
            Text(
                stringResource(R.string.format_switch_watch_fee, MoneyFormat.feePercent(fee)),
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (row.historyUnavailable) {
            Text(
                stringResource(R.string.switch_watch_history_unavailable),
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!closed) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                AppButton(
                    text = stringResource(R.string.switch_watch_bought_action),
                    onClick = { onBuy(row.isin) },
                    variant = ButtonVariant.Text
                )
                onDelete?.let {
                    AppButton(
                        text = stringResource(UiR.string.delete),
                        onClick = it,
                        variant = ButtonVariant.DestructiveText
                    )
                }
            }
        }
    }
}
