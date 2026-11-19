package se.kodhast.fonder.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import se.kodhast.ui.components.AppHeader
import se.kodhast.ui.components.AppTopBar
import se.kodhast.ui.components.LocalBottomClearance
import se.kodhast.ui.components.TopBarSize

/**
 * En flik som ännu inte står på någon av android-ui:s ramar (Översikt, Innehav, Beslut): Kodhäst-sidhuvudet
 * ([AppHeader], NAV-7) med [title] och flikens [actions], som fälls ihop när [content] rullas, och plats för
 * flikraden under innehållet. Försvinner när flikarna flyttar till ramarna (#266 F3), som ritar samma sidhuvud.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabPage(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppHeader(title, scroll, actions = actions) }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(bottom = LocalBottomClearance.current)
        ) { content() }
    }
}

/**
 * En underskärm utan egen ram (Inställningar, importerna, Facit, Pågående byte): bibliotekets lilla topprad
 * ([AppTopBar]) med [title] och tillbakapil ([onBack], UI-8), och innehållet ovanför tangentbordet –
 * appen kör edge-to-edge, där tangentbordet är en inset appen själv måste ta hand om (issue #78).
 */
@Composable
fun SubPage(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title, size = TopBarSize.Small, onBack = onBack) }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
        ) { content() }
    }
}
