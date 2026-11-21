package se.kodhast.fonder.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import se.kodhast.fonder.BuildConfig
import se.kodhast.fonder.R
import se.kodhast.ui.components.AboutCard
import se.kodhast.ui.components.SettingsPage

/**
 * Om appen (SET-9): en underskärm som avatarmenyns "Om Fonder" öppnar (NAV-7) – bibliotekets [AboutCard] med
 * versionen och Kodhästs signatur, som i ReseApoteket och Dagboken.
 */
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier, versionName: String = BuildConfig.VERSION_NAME) {
    SettingsPage(stringResource(R.string.settings_about_section), modifier, onBack) {
        AboutCard(appName = stringResource(R.string.app_name), version = versionName)
    }
}
