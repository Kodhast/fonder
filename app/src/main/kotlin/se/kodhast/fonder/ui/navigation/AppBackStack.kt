package se.kodhast.fonder.ui.navigation

import androidx.compose.runtime.Composable
import se.kodhast.ui.navigation.TabBackStack
import se.kodhast.ui.navigation.rememberTabBackStack

/**
 * Appens back stack: android-ui:s [TabBackStack] med en stack per flik (NAV-7) över flikarna i [TOP_LEVEL] –
 * appen startar på Översikt, varje flik minns sin undersida när man byter flik, att välja fliken man står
 * på går till flikens rot, tillbaka i en annan fliks rot går till Översikt och tillbaka i Översikts rot
 * lämnar appen. Ett sparat läge som inte går att läsa (från en äldre version) ger startläget.
 */
typealias AppBackStack = TabBackStack<AppKey>

/** Flikarnas rötter i flikradens ordning ([Screen]). */
val TOP_LEVEL: List<AppKey> = Screen.entries.map { it.key }

/** En ny back stack i startläget. */
fun appBackStack(): AppBackStack = TabBackStack(TOP_LEVEL, AppKey.serializer())

/** Back stacken, sparad över rotation och processdöd. */
@Composable
fun rememberAppBackStack(): AppBackStack = rememberTabBackStack(TOP_LEVEL, AppKey.serializer())
