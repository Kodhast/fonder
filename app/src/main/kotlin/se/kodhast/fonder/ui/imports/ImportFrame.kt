package se.kodhast.fonder.ui.imports

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import se.kodhast.ui.common.ListUiState
import se.kodhast.ui.common.UiError
import se.kodhast.ui.components.AppButton
import se.kodhast.ui.components.EmptyAction
import se.kodhast.ui.components.EmptyContent
import se.kodhast.ui.components.EntityListScreen
import se.kodhast.ui.components.ListGroup
import se.kodhast.ui.components.TopBarSize
import se.kodhast.ui.theme.Spacing

/** Vad importen visar innan en fil valts: ikon, rubrik, en mening och knappen som öppnar filväljaren. */
internal data class ImportPick(@param:DrawableRes val icon: Int, val title: String, val message: String, val button: String)

/**
 * Importernas gemensamma skärm (IMP-4, IMP-6), på android-ui:s listram (#266 F6): liten topprad med [title] och
 * tillbakapil ([onBack]); innan en fil valts ramens tomma läge med filväljaren ([pick], [onPick]); ett läsfel med
 * "Försök igen", som väljer en ny fil (NFR-6); annars raderna som egna kort i gruppen [group], med [header] överst,
 * och importknappen ([commit], [canCommit], [onCommit]) fast under listan, ovanför tangentbordet.
 *
 * Läsningen och fondmatchningen visas av importens spärrande `LoadingDialog` (#266: importen behåller sin väntan),
 * inte av den här skärmen.
 */
@Composable
internal fun <T> ImportFrame(
    title: String,
    state: ListUiState<T>,
    pick: ImportPick,
    onPick: () -> Unit,
    onBack: () -> Unit,
    group: String,
    commit: String,
    canCommit: Boolean,
    onCommit: () -> Unit,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    row: @Composable (T) -> Unit
) {
    val committing = state is ListUiState.Content
    Column(modifier.fillMaxSize().imePadding()) {
        EntityListScreen(
            title = title,
            state = state,
            empty = EmptyContent(pick.icon, pick.title, pick.message, actions = listOf(EmptyAction(pick.button, onPick))),
            add = null,
            key = key,
            // Knappen under listan bär navigationsradens luft; listan ska inte lägga den en gång till.
            modifier = Modifier.weight(1f).then(if (committing) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier),
            onRetry = onPick,
            onBack = onBack,
            topBarSize = TopBarSize.Small,
            errorTitle = title,
            group = { ListGroup(group, cards = true) },
            header = header,
            row = row
        )
        if (committing) {
            AppButton(
                text = commit,
                onClick = onCommit,
                enabled = canCommit,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.l)
            )
        }
    }
}

/**
 * Listtillståndet för en import: ingen fil vald → tomt (filväljaren), läsfel → fel, läsningen pågår utan rader →
 * laddning (bakom importens dialog), annars raderna.
 */
internal fun <T> importListState(selected: Boolean, loading: Boolean, error: UiError?, rows: ImmutableList<T>): ListUiState<T> = when {
    !selected -> ListUiState.Empty
    error != null -> ListUiState.Error(error)
    loading && rows.isEmpty() -> ListUiState.Loading
    else -> ListUiState.Content(rows)
}
