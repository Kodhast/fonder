package se.kodhast.fonder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * android-ui:s tomma läge eller läsfel (`EmptyState`, `LoadErrorState`) mitt på en yta som inte är en
 * skärmram – flikarna Översikt, Innehav och Beslut, Facit och Fonddetalj (NFR-6). Ytan skrollar när
 * budskapet och knapparna inte ryms (stor text, liten skärm, UI-5). En ramskärm (`EntityListScreen`)
 * placerar sina lägen själv.
 */
@Composable
fun CenteredState(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}
