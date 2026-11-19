package se.kodhast.fonder.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import se.kodhast.fonder.R
import se.kodhast.ui.components.copyOnLongPress

/**
 * Gör ett fondnamn kopierbart (UI-16, issue #245): android-ui:s [copyOnLongPress] med Fonders texter –
 * ett **långtryck** lägger [name] i urklipp med haptik, och TalkBack får åtgärden "Kopiera fondnamn".
 * Bara namnet kopieras, aldrig radens övriga text. Sätts på namnets text eller på en rad/ett kort som
 * bara visar en fond; inte på en rad där långtryck redan betyder något annat (Transaktioner: ta bort).
 * Urklipp rörs bara i biblioteket (`ui-forbidden.txt`).
 */
@Composable
fun Modifier.copyableFundName(name: String): Modifier = copyOnLongPress(name, label = stringResource(R.string.copy_fund_name), clipLabel = stringResource(R.string.fund_name_clip_label))
