package se.kodhast.fonder.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import se.kodhast.ui.components.Foldout
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Spacing

/**
 * Delad, återanvändbar rad som kan fällas ut med en klartextförklaring (issue #22, regel 4).
 * Radens synliga innehåll ([content]) — t.ex. en [PeriodRow] eller en signalrad — ligger kvar,
 * och en pil till höger fäller ut/ihop en pedagogisk [explanation] under.
 *
 * Ett tunt skal ovanpå android-ui:s [Foldout] med eget läge (UI-3): hela raden växlar med minst 48 dp träffyta,
 * läses som knapp med läget "Hopfälld"/"Utfälld", chevronen fjädrar, läget sparas så det överlever en rotation
 * (UI-9) och [onExpand] körs vid utfällning – allt bibliotekets (#280 E). Det enda Fonder lägger till är
 * förklaringstextens stil och att radens innehåll tar bredden före chevronen; skalet stannar för att de ~15
 * anropen inte ska upprepa det. Hela sektioner (rubrik + innehåll) är [Foldout] direkt.
 *
 * @param extraContent valfritt innehåll som visas under [explanation] när raden är utfälld, t.ex.
 *   ett jämförelsediagram för ett bytesförslag (ANA-11).
 * @param onExpand körs när raden fälls **ut** (inte ihop) — kroken för innehåll som är dyrt att hämta
 *   och därför inte ska laddas förrän någon vill se det, t.ex. jämförelsediagrammets kurshistorik (ANA-11).
 */
@Composable
fun ExpandableInfoRow(
    explanation: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    onExpand: () -> Unit = {},
    extraContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Foldout(
        title = "",
        modifier = modifier,
        initiallyExpanded = initiallyExpanded,
        onExpand = onExpand,
        header = infoRowHeader(content),
        contentSpacing = Spacing.s
    ) {
        Text(
            text = explanation,
            style = AppTheme.typography.itemSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        extraContent?.invoke()
    }
}

/** Radens innehåll tar bredden före chevronen. */
private fun infoRowHeader(content: @Composable () -> Unit): @Composable RowScope.() -> Unit = { Box(Modifier.weight(1f)) { content() } }
