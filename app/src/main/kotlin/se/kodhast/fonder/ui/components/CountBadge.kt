package se.kodhast.fonder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Spacing

/** Storleken på [CountBadge]: liten bredvid en text eller ikon, stor som ledande element i en rad. */
enum class CountBadgeSize { Small, Large }

/**
 * Kopparmärket med ett antal (UI-14) i Översiktens "Att besluta"-rad ([ReminderRow] med `count`);
 * Beslut-flikens märke ritas av android-ui:s `AppFloatingToolbar` (`ToolbarItem.badgeCount`). Rund, i sekundärfärgen (koppar) med text i
 * `onSecondary`; växer på bredden för tvåsiffriga tal.
 *
 * Dekorativt för skärmläsaren: antalet ska alltid också stå i omgivande text eller beskrivning
 * (flikens "Beslut, 3 nya", radens undertext), så märket läses inte en gång till.
 */
@Composable
fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    size: CountBadgeSize = CountBadgeSize.Small
) {
    val minSize = if (size == CountBadgeSize.Large) 28.dp else 18.dp
    Box(
        modifier = modifier
            .clearAndSetSemantics {}
            .heightIn(min = minSize)
            .widthIn(min = minSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = if (size == CountBadgeSize.Large) Spacing.s else 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            count.toString(),
            style = if (size == CountBadgeSize.Large) {
                AppTheme.typography.button.copy(fontWeight = FontWeight.Bold)
            } else {
                AppTheme.typography.caption
            },
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}
