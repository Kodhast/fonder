package se.kodhast.fonder.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import se.kodhast.ui.components.AppTopBar
import se.kodhast.ui.components.TopBarSize

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
