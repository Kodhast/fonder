package se.kodhast.fonder.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Storleken på kortens indikator — ska läsa som en del av rubrikraden, inte som ett eget element. */
private val CARD_INDICATOR_SIZE = 18.dp

/**
 * Snurran som säger att datan bakom just den här ytan håller på att bearbetas (NAV-6). Appens
 * bakgrundsjobb syns i stället i sidhuvudets prickar (NAV-6); de här snurrorna ersätts av dem i
 * flikarnas flytt till ramarna (#266).
 *
 * Renderar ingenting när [working] är falskt: en snurra som ligger kvar som tom yta hade fått
 * layouten att hoppa varje gång ett jobb startar eller slutar, och en ständigt närvarande
 * platshållare hade lärt användaren att inte se den.
 *
 * [contentDescription] är obligatorisk och skärmspecifik — "Uppdaterar" utan att säga *vad* är
 * värdelöst för en skärmläsare när flera kort snurrar samtidigt. Använd [CardTitleRow], som
 * formulerar den ur kortets egen rubrik.
 *
 * Ritas som M3 Expressives formskiftande [LoadingIndicator] (UI-12) i stället för en snurrande ring.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkingIndicator(
    working: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = CARD_INDICATOR_SIZE
) {
    if (!working) return
    // Lokal kopia: inne i `semantics`-lambdan skuggar mottagarens (write-only)
    // `contentDescription` parameternamnet, och en läsning av den kompilerar inte.
    val description = contentDescription
    LoadingIndicator(
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = description },
        color = MaterialTheme.colorScheme.primary
    )
}
