package se.kodhast.fonder.ui.facit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import se.kodhast.fonder.R
import se.kodhast.fonder.domain.model.SuggestionKind
import se.kodhast.fonder.domain.usecase.FacitRad
import se.kodhast.fonder.domain.usecase.MoneyFormat
import se.kodhast.fonder.domain.usecase.SwitchOutcomeCalc
import se.kodhast.fonder.ui.components.FollowedToggleRow
import se.kodhast.fonder.ui.components.PeriodRow
import se.kodhast.ui.common.ListUiState
import se.kodhast.ui.components.EmptyContent
import se.kodhast.ui.components.EntityListScreen
import se.kodhast.ui.components.Foldout
import se.kodhast.ui.components.ListGroup
import se.kodhast.ui.components.SectionCard
import se.kodhast.ui.components.TopBarSize
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Spacing

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * Facit för bytesplanen (SET-5, issue #80) — en skärm i stacken med bakåtpil ([onBack]), från Beslut
 * ("Hur gick det?", NAV-9) och från Inställningar.
 */
@Composable
fun FacitScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FacitViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FacitContent(
        state = state,
        onBack = onBack,
        onFollowedChange = { recordId, followed -> viewModel.onEvent(FacitEvent.SetFollowed(recordId, followed)) },
        onRefresh = { viewModel.onEvent(FacitEvent.Refresh) },
        onRetry = { viewModel.onEvent(FacitEvent.Retry) },
        modifier = modifier
    )
}

/**
 * Tillståndsdriven, testbar del av [FacitScreen] — inget ViewModel/Hilt-beroende (issue #21-mönstret). Står på
 * android-ui:s listram (#266 F6): laddning, tomt och läsfel med "Försök igen" (NFR-6) är ramens; summeringen står
 * överst i listan och förslagen i en grupp, ett hopfällbart [Foldout] per förslag.
 */
@Composable
fun FacitContent(
    state: FacitUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onFollowedChange: (Long, Boolean) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val content = state.list is ListUiState.Content
    val suggestions = stringResource(R.string.facit_suggestions_group)
    EntityListScreen(
        title = stringResource(R.string.facit_title),
        state = state.list,
        empty = EmptyContent(
            icon = R.drawable.ic_chart,
            title = stringResource(R.string.facit_empty_title),
            message = stringResource(R.string.facit_empty_body)
        ),
        add = null,
        key = { it.recordId },
        modifier = modifier,
        onRetry = onRetry,
        onBack = onBack,
        topBarSize = TopBarSize.Small,
        errorTitle = stringResource(R.string.error_load_title),
        group = { ListGroup(suggestions) },
        // Summeringen ligger överst i samma lista som raderna, inte fast ovanför — annars äter den
        // permanent skärmhöjd och kan klippa bort förslag på en liten skärm (UI-5).
        header = if (content) ({ SummaryCard(state) }) else null,
        footer = if (content) ({ Disclaimer() }) else null,
        // Dra ned startar bytesplansskanningen (UI-11) — den körning som spelar in nya rader
        // och fyller på utfallens NAV. Utan inspelade förslag finns ingenting att mäta, och då
        // erbjuds ingen gest.
        refreshing = state.switchPlanWorking,
        onRefresh = if (content) onRefresh else null
    ) { row ->
        FacitRadRow(row = row, onFollowedChange = onFollowedChange)
    }
}

@Composable
private fun Disclaimer() {
    Text(
        stringResource(R.string.facit_disclaimer),
        style = AppTheme.typography.itemSubtitle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.s)
    )
}

/**
 * De två måtten per sort, utfallen per horisont och per plats i planen. **Alla förslag** och **enbart
 * genomförda** står som skilda rader och slås aldrig ihop: ett oföljt förslag är ett
 * hypotetiskt utfall, ett följt ett verkligt, och en gemensam siffra hade mätt ingetdera
 * (SET-5).
 *
 * Bytesplanens byten (HEM-8) och avgiftsbytena (ANA-9) redovisas i sin tur var för sig (issue
 * #91) — de besvarar olika frågor, och ett gemensamt snitt hade dolt att avgiftsbytets vinst
 * är känd på förhand medan riskplansbytets inte är det.
 */
@Composable
private fun SummaryCard(state: FacitUiState) {
    var details by rememberSaveable { mutableStateOf(false) }
    SectionCard(title = stringResource(R.string.facit_summary_title), modifier = Modifier.fillMaxWidth()) {
        // Varje sektion visas bara när dess sort faktiskt har inspelade rader — en rubrik
        // med tomma streck hade sett ut som ett mätfel i stället för som "inget att mäta
        // än". Kortet i sig ritas bara när listan är icke-tom, så minst en sektion syns.
        if (state.planAllSummary.totalCount > 0) {
            SubHeading(stringResource(R.string.facit_summary_plan_title))
            SummaryRow(label = stringResource(R.string.facit_summary_all_label), summary = state.planAllSummary)
            SummaryRow(label = stringResource(R.string.facit_summary_followed_label), summary = state.planFollowedSummary)
        }

        if (state.feeAllSummary.totalCount > 0) {
            SubHeading(stringResource(R.string.facit_summary_fee_title), Modifier.padding(top = Spacing.s))
            SummaryRow(label = stringResource(R.string.facit_summary_all_label), summary = state.feeAllSummary)
            SummaryRow(label = stringResource(R.string.facit_summary_followed_label), summary = state.feeFollowedSummary)
            Caption(stringResource(R.string.facit_summary_fee_explain))
        }

        // Fördelningen per horisont och per plats i planen är underlag man slår upp, inte huvudmåttet —
        // den ligger hopfälld (som i mockupen till #266), läget sparat (UI-9).
        if (state.planByHorizon.isNotEmpty() || state.byPlanIndex.isNotEmpty()) {
            Foldout(
                title = stringResource(R.string.facit_details_title),
                expanded = details,
                onToggle = { details = !details },
                contentSpacing = Spacing.xs
            ) {
                // Procent bara inom ett horisontband (issue #197): ett par dagars utfall och ett års i samma
                // snitt säger ingenting. Band utan utvärderade förslag visas inte.
                if (state.planByHorizon.isNotEmpty()) {
                    SubHeading(stringResource(R.string.facit_by_horizon_title))
                    state.planByHorizon.forEach { entry -> HorizonRow(entry = entry) }
                    Caption(stringResource(R.string.facit_by_horizon_explain))
                }
                if (state.byPlanIndex.isNotEmpty()) {
                    SubHeading(stringResource(R.string.facit_by_plan_index_title), Modifier.padding(top = Spacing.s))
                    state.byPlanIndex.forEach { entry ->
                        PeriodRow(
                            label = stringResource(R.string.format_facit_plan_index_label, entry.planIndex + 1),
                            amount = null,
                            fraction = null,
                            valueText = hitRateText(entry.summary),
                            valueTone = hitRateTone(entry.summary),
                            supportingValue = entry.summary.medianExcessReturn?.let {
                                stringResource(R.string.format_facit_median, MoneyFormat.percentSigned(it))
                            }
                        )
                    }
                    Caption(stringResource(R.string.facit_by_plan_index_explain))
                }
            }
        }
    }
}

/** En mellanrubrik inne i summeringskortet (en grupprubrik över kort är `GroupLabel`). */
@Composable
private fun SubHeading(text: String, modifier: Modifier = Modifier) {
    Text(text, style = AppTheme.typography.itemTitle, modifier = modifier)
}

@Composable
private fun Caption(text: String) {
    Text(text, style = AppTheme.typography.itemSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * En summeringsrad via den delade [PeriodRow] (regel 4): **träffandelen** ("3 av 5 bättre") är
 * huvudmåttet, färgad efter om majoriteten var bättre, med kronsumman under — aldrig ett snitt i
 * procent, som blandade förslag med olika ålder (issue #197). Under etiketten hur många som gått
 * att utvärdera. Utan utvärderade förslag faller raden tillbaka på komponentens "otillräcklig data".
 */
@Composable
private fun SummaryRow(label: String, summary: SwitchOutcomeCalc.Summary, modifier: Modifier = Modifier) {
    PeriodRow(
        label = label,
        amount = null,
        fraction = null,
        valueText = hitRateText(summary),
        valueTone = hitRateTone(summary),
        supportingValue = summary.totalExcessKr?.let { MoneyFormat.krSigned(it) },
        supportingLabel = if (summary.totalCount > 0) {
            pluralStringResource(R.plurals.format_facit_evaluated_of, summary.evaluatedCount, summary.evaluatedCount, summary.totalCount)
        } else {
            null
        },
        modifier = modifier
    )
}

/** "x av n bättre" — null utan utvärderade förslag, så raden visar "otillräcklig data" i stället för "0 av 0". */
@Composable
private fun hitRateText(summary: SwitchOutcomeCalc.Summary): String? = if (summary.evaluatedCount > 0) {
    stringResource(R.string.format_facit_better_of, summary.betterCount, summary.evaluatedCount)
} else {
    null
}

/** Vinstfärg när majoriteten var bättre än att ha behållit, förlustfärg när majoriteten var sämre, dämpad vid exakt hälften. */
private fun hitRateTone(summary: SwitchOutcomeCalc.Summary): Double = summary.hitRateSign.toDouble()

/**
 * Ett horisontband (issue #197): under ett halvår medianen i procent, över ett halvår medianen i
 * årstakt — så ett halvårs och ett treårs utfall går att jämföra. Träffandelen står under.
 *
 * Båda varianterna går genom `valueText` + `valueTone`, så en median på exakt noll färgas lika —
 * dämpat — i alla band, och ett tecken ger samma vinst-/förlustfärg som i resten av appen.
 */
@Composable
private fun HorizonRow(entry: SwitchOutcomeCalc.HorizonSummary, modifier: Modifier = Modifier) {
    val label = stringResource(
        when (entry.horizon) {
            SwitchOutcomeCalc.Horizon.UNDER_ONE_MONTH -> R.string.format_facit_horizon_under_one_month
            SwitchOutcomeCalc.Horizon.ONE_TO_SIX_MONTHS -> R.string.format_facit_horizon_one_to_six_months
            SwitchOutcomeCalc.Horizon.OVER_SIX_MONTHS -> R.string.format_facit_horizon_over_six_months
        },
        entry.count
    )
    val better = stringResource(
        if (entry.isAnnualized) R.string.format_facit_horizon_annualized_better else R.string.format_facit_horizon_median_better,
        entry.betterCount,
        entry.count
    )
    PeriodRow(
        label = label,
        amount = null,
        fraction = null,
        valueText = if (entry.isAnnualized) {
            stringResource(R.string.format_facit_per_year, MoneyFormat.percentSigned(entry.medianExcessReturn))
        } else {
            MoneyFormat.percentSigned(entry.medianExcessReturn)
        },
        valueTone = entry.medianExcessReturn,
        supportingValue = better,
        modifier = modifier
    )
}

/**
 * Ett inspelat förslag. Hopfällbart av samma skäl som sälj-korten i Sålda fonder (SLD-4) —
 * stängt visas fonderna, sorten och datumet och utfallet, utfällt detaljerna. Byggt på den delade
 * android-ui:s [Foldout] (regel 4): rubrik, undertext och meravkastning i rubrikraden (knapp med
 * "Hopfälld"/"Utfälld", UI-3) – meravkastningen alltid synlig – och detaljerna i det som fälls ut.
 * Läget sparas med `rememberSaveable` (UI-9).
 *
 * "Genomförd" ligger här *och* på Beslut (HEM-8): planen på Beslut försvinner efter
 * `SwitchPlanCalc.PLAN_TTL_DAYS`, så utan kryssrutan här gick ett äldre förslag aldrig att
 * markera i efterhand — och då hade det följda måttet aldrig kunnat bli komplett.
 */
@Composable
private fun FacitRadRow(row: FacitRad, onFollowedChange: (Long, Boolean) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Foldout(
        title = "",
        expanded = expanded,
        onToggle = { expanded = !expanded },
        header = {
            Column(modifier = Modifier.weight(1f).padding(vertical = Spacing.s)) {
                Text(
                    stringResource(
                        R.string.format_facit_row_funds,
                        row.sellFundName,
                        row.buyFundName
                    ),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    stringResource(
                        R.string.format_facit_row_kind_and_date,
                        stringResource(
                            if (row.kind == SuggestionKind.FEE) R.string.facit_kind_fee else R.string.facit_kind_plan
                        ),
                        LocalDate.ofEpochDay(row.suggestedAtEpochDay).format(dateFormatter)
                    ),
                    style = AppTheme.typography.itemSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Utfallet via den delade PeriodRow (regel 4): procent + kronor, färgat efter
                // procenten. Är raden inte utvärderad ännu är båda talen null och komponenten
                // markerar den som otillräcklig data — aldrig som 0, som lästs som "gav ingenting".
                // Mätdagen står under etiketten (issue #197): båda fonderna mäts samma dag, och utan en
                // sådan dag efter förslaget är raden ej utvärderad. Saknar förslaget ett utgångsläge
                // (NAV ≤ 0 vid förslaget) blir det aldrig utvärderat — då "Otillräcklig data", inte "väntar".
                val measuredAt = row.outcome.measuredAtEpochDay
                val days = row.outcome.days
                val hasBaseNav = row.outcome.hasBaseNav
                PeriodRow(
                    label = stringResource(R.string.facit_row_excess_label),
                    amount = row.outcome.excessKr,
                    fraction = row.outcome.excessReturn,
                    stackValue = true,
                    insufficientText = if (hasBaseNav) stringResource(R.string.facit_row_not_evaluated) else null,
                    supportingLabel = when {
                        measuredAt != null && days != null -> pluralStringResource(
                            R.plurals.format_facit_measured_at,
                            days.toInt(),
                            LocalDate.ofEpochDay(measuredAt).format(dateFormatter),
                            days.toInt()
                        )

                        hasBaseNav -> stringResource(R.string.facit_row_waiting_same_day)

                        else -> null
                    }
                )
            }
        }
    ) {
        Column(modifier = Modifier.padding(top = Spacing.s)) {
            // Plats i planen bara för planens rader: ett avgiftsbyte ingår inte i någon
            // rangordning, så dess planIndex är alltid 0 och "1:a bytet i planen" hade
            // varit en påhittad ordning (issue #91).
            if (row.kind == SuggestionKind.RISK_PLAN) {
                Text(
                    stringResource(R.string.format_facit_plan_index_detail, row.planIndex + 1),
                    style = AppTheme.typography.itemSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            row.switchValueKr?.let { valueKr ->
                Text(
                    stringResource(R.string.format_facit_amount, MoneyFormat.kr(valueKr)),
                    style = AppTheme.typography.itemSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (row.outcome.isEvaluated) {
                Text(
                    stringResource(
                        R.string.format_facit_legs,
                        MoneyFormat.percentSigned(row.outcome.sellReturn!!),
                        MoneyFormat.percentSigned(row.outcome.buyReturn!!)
                    ),
                    style = AppTheme.typography.itemSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    stringResource(if (row.outcome.hasBaseNav) R.string.facit_not_evaluated_explain else R.string.facit_not_evaluable_explain),
                    style = AppTheme.typography.itemSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FollowedToggleRow(
                followed = row.followed,
                onFollowedChange = { checked -> onFollowedChange(row.recordId, checked) },
                modifier = Modifier.padding(top = Spacing.xs)
            )
        }
    }
}
