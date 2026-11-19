package se.kodhast.fonder.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import se.kodhast.fonder.R
import se.kodhast.fonder.domain.usecase.MoneyFormat
import se.kodhast.fonder.domain.usecase.PortfolioTotalCalc
import se.kodhast.fonder.ui.theme.MonoAmountStyle
import se.kodhast.ui.components.SectionCard
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Spacing

/**
 * Portföljens totalkort (HEM-1/POR-3) — totalt värde, **totalresultat** (realiserat + orealiserat
 * − avgifter, kr + %), **årsavkastning** (pengaviktad, XIRR) och "Varav orealiserat" på de andelar
 * som finns kvar, "Värde per <datum>" (POR-7) och en utfällbar förklaring av vad som räknas in
 * (issue #191, mockup i issuet). **En** komponent för Hem och Portfölj (regel 4, skill
 * `shared-ui-components`), en [SectionCard] från android-ui; tar primitiver och [PortfolioTotalCalc.Result], inte en skärms tillstånd.
 *
 * Årsavkastningen visas först när portföljen är minst ett år gammal ([PortfolioTotalCalc.ANNUALIZE_MIN_DAYS])
 * — dessförinnan texten "Visas efter ett år", aldrig en årstakt extrapolerad ur några månader.
 *
 * Saknar **alla** innehav kurs visas nettoinvesterat och "Kurs saknas ännu" i stället för ett
 * felaktigt värde (POR-3) — [gainLossFraction] är då null. Saknar bara **något** innehav kurs
 * räknas orealiserat på de kända (som förut), men totalresultat och årsavkastning gäller hela
 * portföljen och visar då "Kurs saknas ännu" i stället för tal ([total] är partiell).
 *
 * @param gainLoss orealiserat resultat på kvarvarande andelar ([se.kodhast.fonder.domain.usecase.PortfolioCalc.totalGainLoss]).
 * @param total totalresultat och pengaviktad avkastning över alla transaktioner, även helt sålda fonder.
 * @param afterTotalResult rader direkt under totalresultatet — Hems schablonskatt i ISK/KF
 *   (HEM-16, issue #199, en [ApproxTaxRow]). Null = kortet som förut (Portfölj).
 * @param footer rader längst ned i kortet — förklaringen till schablonskatten (HEM-16). Null = som förut.
 */
@Composable
fun TotalCard(
    totalValue: Double,
    totalInvested: Double,
    gainLoss: Double,
    gainLossFraction: Double?,
    total: PortfolioTotalCalc.Result,
    navEpochDay: Long?,
    modifier: Modifier = Modifier,
    afterTotalResult: (@Composable ColumnScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null
) {
    SectionCard(title = stringResource(R.string.portfolj_total_value), modifier = modifier.fillMaxWidth()) {
        if (gainLossFraction != null) {
            val priceMissing = if (total.partial) stringResource(R.string.portfolj_price_missing) else null
            Text(MoneyFormat.kr(totalValue), style = MonoAmountStyle.merge(AppTheme.typography.bigNumber))
            PeriodRow(
                label = stringResource(R.string.total_result),
                amount = total.totalGainKr,
                fraction = total.totalGainFraction,
                modifier = Modifier.padding(top = Spacing.xs),
                insufficientText = priceMissing
            )
            afterTotalResult?.invoke(this)
            PeriodRow(
                label = stringResource(R.string.total_money_weighted_return),
                amount = null,
                fraction = total.moneyWeightedAnnualReturn,
                insufficientText = priceMissing ?: if (total.youngerThanYear) stringResource(R.string.total_shown_after_a_year) else null
            )
            PeriodRow(
                label = stringResource(R.string.total_unrealized),
                amount = gainLoss,
                fraction = gainLossFraction,
                muted = true
            )
            ValueAsOfRow(navEpochDay = navEpochDay, modifier = Modifier.padding(top = 2.dp))
            ExpandableInfoRow(explanation = stringResource(R.string.total_explain)) {
                Text(
                    stringResource(R.string.total_what_counts),
                    style = AppTheme.typography.itemSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(MoneyFormat.kr(totalInvested), style = MonoAmountStyle.merge(AppTheme.typography.bigNumber))
            Text(
                stringResource(R.string.portfolj_price_missing),
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        footer?.invoke(this)
    }
}
