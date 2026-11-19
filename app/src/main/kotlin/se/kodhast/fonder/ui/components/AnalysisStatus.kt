package se.kodhast.fonder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import se.kodhast.fonder.R
import se.kodhast.fonder.domain.usecase.AnalysisGuidance
import se.kodhast.fonder.domain.usecase.FundAnalysisCalc
import se.kodhast.fonder.domain.usecase.MoneyFormat
import se.kodhast.fonder.ui.theme.StatusColors
import se.kodhast.ui.components.AppCard
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Spacing

/**
 * Delade byggstenar för att visa [FundAnalysisCalc]-status (issue #16, regel 4) — återanvänds
 * mellan Fonddetaljs statusbanner och Hems summeringskort (ANA-3, HEM-4) så de alltid visar
 * samma titel/triggertexter för samma status. Domänlagret bär bara rådata (nivå + numeriska
 * detaljer) — Swedish text komponeras här, samma princip som POR-3/SLD-2/HEM-2.
 */

@Composable
fun statusColor(level: FundAnalysisCalc.SignalLevel) = when (level) {
    FundAnalysisCalc.SignalLevel.GRON -> StatusColors.gron
    FundAnalysisCalc.SignalLevel.GUL -> StatusColors.gul
    FundAnalysisCalc.SignalLevel.ROD -> StatusColors.rod
}

@Composable
fun statusTitle(level: FundAnalysisCalc.SignalLevel) = when (level) {
    FundAnalysisCalc.SignalLevel.GRON -> stringResource(R.string.analys_status_gron)
    FundAnalysisCalc.SignalLevel.GUL -> stringResource(R.string.analys_status_gul)
    FundAnalysisCalc.SignalLevel.ROD -> stringResource(R.string.analys_status_rod)
}

/**
 * Svensk kontexttext för varje härledd [AnalysisGuidance.GuidanceKey] (issue #22, ANA-6) — sätter
 * signalerna i sammanhang för en nybörjare. Domänlagret bär bara nycklarna, texten komponeras här
 * (samma princip som [statusTriggerMessages]). Alltid förklarande, aldrig ett köp-/säljråd (ANA-3).
 */
@Composable
fun guidanceMessages(analysis: FundAnalysisCalc.Analysis): List<String> = AnalysisGuidance.guidanceFor(analysis).map { key ->
    when (key) {
        AnalysisGuidance.GuidanceKey.NEDGANG_MEN_PLUS_MOT_GAV -> stringResource(R.string.analys_guidance_plus_mot_gav)
        AnalysisGuidance.GuidanceKey.NEDGANG_OCH_MINUS_MOT_GAV -> stringResource(R.string.analys_guidance_minus_mot_gav)
        AnalysisGuidance.GuidanceKey.DJUP_NEDGANG_TIDSHORISONT -> stringResource(R.string.analys_guidance_djup_nedgang)
        AnalysisGuidance.GuidanceKey.UNDER_TREND_INTE_SALJBUD -> stringResource(R.string.analys_guidance_under_trend)
        AnalysisGuidance.GuidanceKey.SVAG_MOT_PORTFOLJ -> stringResource(R.string.analys_guidance_svag_portfolj)
        AnalysisGuidance.GuidanceKey.LUGNT_LAGE -> stringResource(R.string.analys_guidance_lugnt)
    }
}

/** Svensk text för varje triggad (icke-grön) signal i [analysis], i fast ordning (avstånd, trend, momentum). */
@Composable
fun statusTriggerMessages(analysis: FundAnalysisCalc.Analysis): List<String> {
    val messages = mutableListOf<String>()
    analysis.distanceFromHigh?.let { signal ->
        if (signal.level != FundAnalysisCalc.SignalLevel.GRON) {
            messages += stringResource(R.string.format_analys_signal_distance, MoneyFormat.percentSigned(signal.distanceFraction))
        }
    }
    analysis.trend?.let { signal ->
        if (signal.level != FundAnalysisCalc.SignalLevel.GRON) messages += stringResource(R.string.analys_signal_trend)
    }
    analysis.momentum?.let { signal ->
        if (signal.level != FundAnalysisCalc.SignalLevel.GRON) messages += stringResource(R.string.analys_signal_momentum)
    }
    return messages
}

/**
 * Säljsignalens nivå som en färgad prick (ANA-3). Färgen är aldrig ensam bärare (UI-3): skärmläsaren läser
 * [contentDescription], som standard den summerade statusen ("Säljsignal: Bör ses över"). En enskild
 * indikator skickar bara nivåns namn; `null` bara där nivåns namn redan står i text bredvid.
 */
@Composable
fun StatusDot(
    level: FundAnalysisCalc.SignalLevel,
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(R.string.format_status_dot_description, statusTitle(level))
) {
    Box(
        modifier = modifier
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .size(10.dp)
            .clip(CircleShape)
            .background(statusColor(level))
    )
}

/** Statusbanner (prick + rubrik + triggertexter) ovanför kurshistoriken i Fonddetalj (ANA-3), på android-ui:s [AppCard]. */
@Composable
fun AnalysisStatusBanner(analysis: FundAnalysisCalc.Analysis?, modifier: Modifier = Modifier) {
    val level = analysis?.status
    if (analysis == null || level == null) {
        AppCard(modifier = modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.analys_status_insufficient),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    // Bibliotekets kort (22 dp, djupet), som alla andra kort; nivåns färg bärs av pricken och rubriken av texten.
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            StatusDot(level, modifier = Modifier.padding(top = 6.dp, end = Spacing.m), contentDescription = null)
            Column {
                Text(statusTitle(level), style = MaterialTheme.typography.titleMedium)
                val messages = statusTriggerMessages(analysis)
                if (messages.isNotEmpty()) {
                    Text(
                        messages.joinToString(" · "),
                        style = AppTheme.typography.itemSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Neutralt kontextkort under statusbannern i Fonddetalj (ANA-6, issue #22) — förklarar för en
 * nybörjare vad signalerna innebär i sammanhang (t.ex. att kursen ligger under toppen men
 * fortfarande över GAV, eller att en nedgång inte i sig är ett säljläge). Visas inget om analysen
 * saknar vägledning (otillräcklig data, ANA-4) — då renderas kortet inte alls. Aldrig rådgivande.
 */
@Composable
fun AnalysisGuidanceCard(analysis: FundAnalysisCalc.Analysis, modifier: Modifier = Modifier) {
    val messages = guidanceMessages(analysis)
    if (messages.isEmpty()) return
    AppCard(modifier = modifier.fillMaxWidth()) {
        messages.forEachIndexed { index, message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (index == 0) Modifier else Modifier.padding(top = Spacing.s)
            )
        }
    }
}
