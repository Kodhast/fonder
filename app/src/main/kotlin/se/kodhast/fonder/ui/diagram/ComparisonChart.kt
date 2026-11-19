package se.kodhast.fonder.ui.diagram

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import se.kodhast.fonder.R
import se.kodhast.ui.components.ItemRowText
import se.kodhast.ui.theme.Spacing

/**
 * Jämförelsediagrammet – en fond mot ett eller flera alternativ i samma [FundLineChart], indexerade till 100 vid
 * periodens första gemensamma dag (ANA-11, ANA-12). Det delade mönstret för Fonddetaljs utfällda bytesförslag och
 * Pågående byte (regel 4): tre lägen och aldrig ett tomt diagram.
 *
 * - [available] = `false` och [loading]: "Hämtar kurshistorik för jämförelsen …" som text – ingen snurra (#266:
 *   en gemensam laddningsindikator, kort som saknar data säger det i text).
 * - [available] = `false`: [unavailableText], som säger att ingen jämförelse visas och varför.
 * - annars diagrammet; pågår hämtningen ändå ([loading], t.ex. ett alternativ vars kurva inte landat) står texten
 *   under, så att en ofullständig jämförelse inte ser färdig ut.
 */
@Composable
fun ComparisonChart(
    points: List<Pair<Long, Double>>,
    comparisonSeries: List<ChartSeries>,
    available: Boolean,
    unavailableText: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    primaryLabel: String? = null
) {
    val loadingText = stringResource(R.string.switch_comparison_loading)
    Column(modifier) {
        when {
            !available && loading -> ItemRowText(title = null, subtitle = loadingText)

            !available -> ItemRowText(title = null, subtitle = unavailableText)

            else -> {
                FundLineChart(points = points, primaryLabel = primaryLabel, comparisonSeries = comparisonSeries)
                if (loading) ItemRowText(title = null, subtitle = loadingText, modifier = Modifier.padding(top = Spacing.xs))
            }
        }
    }
}
