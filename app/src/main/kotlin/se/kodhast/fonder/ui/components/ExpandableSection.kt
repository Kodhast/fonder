package se.kodhast.fonder.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import se.kodhast.ui.components.CollapsibleHeader

/**
 * Delad utfällningsmekanik (regel 4, issue #85) — en alltid synlig rubrikrad med en pil som
 * fäller ut godtyckligt innehåll under. Basen både för [ExpandableInfoRow] (rad + förklaringstext)
 * och för hela sektioner som ligger hopfällda som default, t.ex. Analys och ordlistan i
 * Fonddetalj (ANA-10).
 *
 * Rubrikraden är android-ui:s [CollapsibleHeader]: hela raden växlar med minst 48 dp träffyta,
 * läses som knapp med "Fäll ut"/"Fäll ihop" och läget ("Hopfälld"/"Utfälld") som
 * `stateDescription`, och chevronen fjädrar ett halvt varv (UI-3). Utfällt läge sparas med
 * `rememberSaveable` så det överlever en rotation (UI-9).
 *
 * @param onExpand körs när sektionen fälls **ut** (inte när den fälls ihop) — kroken för innehåll
 *   som är dyrt att hämta och därför inte ska laddas förrän någon vill se det, t.ex.
 *   jämförelsediagrammets kurshistorik för en föreslagen fond (ANA-11).
 */
@Composable
fun ExpandableSection(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    onExpand: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Column(modifier = modifier) {
        CollapsibleHeader(
            expanded = expanded,
            onToggle = {
                expanded = !expanded
                if (expanded) onExpand()
            }
        ) {
            Box(modifier = Modifier.weight(1f)) { header() }
        }
        AnimatedVisibility(visible = expanded) { content() }
    }
}

/** [ExpandableSection] med en ren textrubrik — det vanliga fallet för en hopfälld sektion. */
@Composable
fun ExpandableSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    onExpand: () -> Unit = {},
    content: @Composable () -> Unit
) {
    ExpandableSection(
        modifier = modifier,
        initiallyExpanded = initiallyExpanded,
        onExpand = onExpand,
        header = { Text(title, style = titleStyle) },
        content = content
    )
}
