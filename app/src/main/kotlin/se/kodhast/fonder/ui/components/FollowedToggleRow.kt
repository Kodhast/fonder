package se.kodhast.fonder.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import se.kodhast.fonder.R
import se.kodhast.ui.components.CheckRow

/**
 * "Genomförd"-markeringen för ett inspelat bytesförslag (SET-5, issue #80) — delad mellan Hems
 * bytesplan (HEM-8), Facit och Fonddetaljs bytesavsnitt (ANA-10), som alla visar **samma**
 * `SuggestionRecord`. Låg tidigare som en egen kopia per skärm; en tredje kopia i Fonddetalj
 * hade varit tre ställen att glömma när träffytan eller skärmläsartexten ändras (regel 4,
 * issue #90).
 *
 * android-ui:s [CheckRow] med kvitteringens etikett, utan överstruken eller tonad rad (kryss betyder
 * "gjort", inte "klar att stryka"): hela raden är klickbar, en nod med rollen kryssruta
 * (UI-3).
 *
 * Markeringen utför aldrig ett byte — den **registrerar** att användaren gjorde det, så facit
 * kan mäta följda råd separat från alla givna råd. Utan den skillnaden vore ett hypotetiskt och
 * ett verkligt utfall samma siffra.
 */
@Composable
fun FollowedToggleRow(
    followed: Boolean,
    onFollowedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    CheckRow(
        title = stringResource(R.string.facit_followed_label),
        checked = followed,
        onCheckedChange = onFollowedChange,
        modifier = modifier,
        done = false,
        tinted = false
    )
}
