package se.kodhast.fonder.ui.imports

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import se.kodhast.fonder.ui.components.CenteredState
import se.kodhast.ui.common.UiError
import se.kodhast.ui.components.LoadErrorState

/**
 * Importens läsfel (NFR-6), gemensamt för innehavs- och orderimporten: android-ui:s `LoadErrorState`
 * mitt på ytan under importens [title]. "Försök igen" anropar [onPickAgain], som öppnar filväljaren igen.
 */
@Composable
internal fun ImportError(title: String, error: UiError, onPickAgain: () -> Unit, modifier: Modifier = Modifier) {
    CenteredState(modifier) { LoadErrorState(title, error, onRetry = onPickAgain) }
}
