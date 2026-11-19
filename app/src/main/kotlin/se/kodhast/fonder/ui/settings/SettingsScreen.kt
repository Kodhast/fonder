package se.kodhast.fonder.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import se.kodhast.fonder.BuildConfig
import se.kodhast.fonder.R
import se.kodhast.fonder.data.datastore.ThemeMode
import se.kodhast.fonder.domain.model.AccountType
import se.kodhast.fonder.domain.model.BackupRecord
import se.kodhast.fonder.domain.model.BackupTarget
import se.kodhast.fonder.domain.usecase.BackupReminderCalc
import se.kodhast.fonder.domain.usecase.SwitchWatchCalc
import se.kodhast.fonder.ui.theme.Error
import se.kodhast.ui.components.AboutCard
import se.kodhast.ui.components.AccountRow
import se.kodhast.ui.components.AppButton
import se.kodhast.ui.components.AppCard
import se.kodhast.ui.components.AppDivider
import se.kodhast.ui.components.AppSegmentedChoice
import se.kodhast.ui.components.ButtonVariant
import se.kodhast.ui.components.ConfirmDialog
import se.kodhast.ui.components.FieldError
import se.kodhast.ui.components.GroupLabel
import se.kodhast.ui.components.ItemRow
import se.kodhast.ui.components.ItemRowText
import se.kodhast.ui.components.SettingsCard
import se.kodhast.ui.components.SettingsPage
import se.kodhast.ui.components.SignOutRow
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Spacing
import se.kodhast.ui.theme.Tone

/** Säkerhetskopieringskortet — målet för Hems påminnelse (HEM-12), adresserbart i tester. */
const val SETTINGS_BACKUP_CARD_TEST_TAG = "settings_backup_card"

private val lastSyncFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/**
 * Filväljarens filter vid återställning. Flera typer av samma skäl som importflödet: en
 * `.json`-fil rapporteras olika beroende på var den ligger (Drive, Downloads, en delad mapp),
 * och ett för snävt filter döljer användarens egen säkerhetskopia i väljaren.
 */
private val BACKUP_MIME_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onImportHoldings: () -> Unit = {},
    onImportOrders: () -> Unit = {},
    onOpenRiskProfile: () -> Unit = {},
    onOpenFacit: () -> Unit = {},
    onOpenBenchmarkPicker: () -> Unit = {},
    onOpenGallery: (() -> Unit)? = null,
    scrollToBackup: Boolean = false,
    onScrolledToBackup: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // SAF: appen får skriva/läsa precis den fil användaren pekar ut, utan lagringsbehörighet.
    // Själva I/O:n ligger här och inte i ViewModel:en — den känner bara till JSON-strängen,
    // precis som BackupRepository (SET-6, issue #82).
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.onEvent(
            SettingsEvent.Export { json ->
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("Kunde inte öppna $uri för skrivning")
                stream.use { it.write(json.toByteArray()) }
            }
        )
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.onEvent(
            SettingsEvent.Restore {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            }
        )
    }

    // Drive-scopet begärs skilt från inloggningen och kan kräva att användaren godkänner det i
    // Googles egen ruta. Den startas som en PendingIntent — därför måste den vägen gå via
    // aktiviteten och inte via tillståndet (samma mekanik som SAF-väljarna ovan).
    val driveAuthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        // Utfallet rapporteras inte tillförlitligt av Identity-API:t. Flaggan rensas och nästa
        // körning får avgöra — ett kvarliggande "behöver tillåtelse" efter ett beviljande är
        // värre än ett som dyker upp igen.
        viewModel.onEvent(SettingsEvent.DriveAuthorizationResolved)
    }
    val launchDriveAuth: (android.app.PendingIntent) -> Unit = { pendingIntent ->
        driveAuthLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
    }

    SettingsContent(
        state = state,
        onThemeSelect = { viewModel.onEvent(SettingsEvent.SetThemeMode(it)) },
        onImportHoldings = onImportHoldings,
        onImportOrders = onImportOrders,
        onOpenRiskProfile = onOpenRiskProfile,
        onOpenFacit = onOpenFacit,
        onOpenBenchmarkPicker = onOpenBenchmarkPicker,
        onOpenGallery = onOpenGallery,
        onClearBenchmark = { viewModel.onEvent(SettingsEvent.ClearBenchmark) },
        onAccountTypeSelect = { viewModel.onEvent(SettingsEvent.SetAccountType(it)) },
        onSwitchWatchTtlSelect = { viewModel.onEvent(SettingsEvent.SetSwitchWatchTtl(it)) },
        onExportBackup = { exportPicker.launch("fonder-backup-${LocalDate.now()}.json") },
        onRestoreBackup = { restorePicker.launch(BACKUP_MIME_TYPES) },
        onBackupMessageDismiss = { viewModel.onEvent(SettingsEvent.BackupMessageDismissed) },
        onBackupToDrive = { viewModel.onEvent(SettingsEvent.BackupToDrive(launchDriveAuth)) },
        onRestoreFromDrive = { viewModel.onEvent(SettingsEvent.RestoreFromDrive(launchDriveAuth)) },
        onSignIn = { viewModel.onEvent(SettingsEvent.SignIn(context)) },
        onSignOut = { viewModel.onEvent(SettingsEvent.SignOut) },
        onSignInErrorDismiss = { viewModel.onEvent(SettingsEvent.SignInErrorDismissed) },
        onClearDatabase = { viewModel.onEvent(SettingsEvent.ClearDatabase) },
        onClearedMessageDismiss = { viewModel.onEvent(SettingsEvent.ClearedMessageDismissed) },
        scrollToBackup = scrollToBackup,
        onScrolledToBackup = onScrolledToBackup,
        modifier = modifier,
        onBack = onBack
    )
}

/**
 * Tillståndsdriven, testbar del av [SettingsScreen] — inget ViewModel/Hilt-beroende (issue #27).
 * [onOpenGallery] öppnar komponentgalleriet; bara satt i debug-bygget, annars `null` och kortet syns inte.
 */
@OptIn(ExperimentalFoundationApi::class) // BringIntoViewRequester, för Hems påminnelse (HEM-12)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    modifier: Modifier = Modifier,
    onThemeSelect: (ThemeMode) -> Unit = {},
    onImportHoldings: () -> Unit = {},
    onImportOrders: () -> Unit = {},
    onOpenRiskProfile: () -> Unit = {},
    onOpenFacit: () -> Unit = {},
    onOpenBenchmarkPicker: () -> Unit = {},
    onOpenGallery: (() -> Unit)? = null,
    onClearBenchmark: () -> Unit = {},
    onAccountTypeSelect: (AccountType) -> Unit = {},
    onSwitchWatchTtlSelect: (Long) -> Unit = {},
    onExportBackup: () -> Unit = {},
    onRestoreBackup: () -> Unit = {},
    onBackupMessageDismiss: () -> Unit = {},
    onBackupToDrive: () -> Unit = {},
    onRestoreFromDrive: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onSignInErrorDismiss: () -> Unit = {},
    onClearDatabase: () -> Unit = {},
    onClearedMessageDismiss: () -> Unit = {},
    scrollToBackup: Boolean = false,
    onScrolledToBackup: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    versionName: String = BuildConfig.VERSION_NAME
) {
    // Hems påminnelse (HEM-12) landar här: säkerhetskopieringen ligger långt ned, så den
    // skrollas fram och begäran kvitteras — en gång, inte vid varje återbesök.
    val backupBringIntoView = remember { BringIntoViewRequester() }
    val scrolledToBackup by rememberUpdatedState(onScrolledToBackup)
    LaunchedEffect(scrollToBackup) {
        if (scrollToBackup) {
            backupBringIntoView.bringIntoView()
            scrolledToBackup()
        }
    }

    // rememberSaveable: en bekräftelsedialog ska inte försvinna tyst vid rotation (issue #78).
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    var showRestoreConfirm by rememberSaveable { mutableStateOf(false) }
    var showDriveRestoreConfirm by rememberSaveable { mutableStateOf(false) }

    // Inställningarnas ram från android-ui (UI-19): topprad, grupprubrik över varje kort, samma avstånd som
    // i ReseApoteket och Dagboken. Bakgrundsjobb syns som prickarna i toppraden, inte som en snurra i ett kort.
    SettingsPage(stringResource(R.string.nav_settings), modifier, onBack) {
        // Google-konto (TP-6). Inloggningen flyttar ingen data i sig — den är förutsättningen
        // för molnbackupen (TP-7 steg 2), vilket kortets text säger rakt ut så att "inloggad"
        // inte förväxlas med "säkerhetskopierad".
        GroupLabel(stringResource(R.string.settings_account_section))
        SettingsCard {
            val user = state.googleUser
            if (user == null) {
                ItemRow(
                    title = stringResource(R.string.settings_account_signed_out),
                    subtitle = stringResource(R.string.settings_account_body),
                    below = {
                        AppButton(
                            text = stringResource(R.string.settings_account_sign_in_button),
                            onClick = onSignIn,
                            enabled = !state.signInInProgress
                        )
                    }
                )
            } else {
                AccountRow(user.displayName, user.email, photoUrl = null)
                AppDivider()
                SignOutRow(stringResource(R.string.app_name), onSignOut = onSignOut)
                ItemRowText(title = null, subtitle = stringResource(R.string.settings_account_body), modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s))
            }
            // Ett avbrutet kontoval är inget fel och når aldrig hit — se
            // SettingsViewModel.signIn. Kvitteras bort som övriga engångshändelser (#78).
            state.signInError?.let { error ->
                FieldError(stringResource(error.message), modifier = Modifier.padding(horizontal = Spacing.m))
                AppButton(
                    text = stringResource(R.string.close),
                    onClick = onSignInErrorDismiss,
                    modifier = Modifier.padding(horizontal = Spacing.s),
                    variant = ButtonVariant.Text
                )
            }
        }

        GroupLabel(stringResource(R.string.settings_appearance_section))
        SettingsCard {
            ItemRow(
                title = stringResource(R.string.settings_theme_section),
                below = {
                    AppSegmentedChoice(
                        options = ThemeMode.entries.map { stringResource(themeModeLabelRes(it)) },
                        selectedIndex = ThemeMode.entries.indexOf(state.themeMode),
                        onSelect = { onThemeSelect(ThemeMode.entries[it]) }
                    )
                }
            )
        }

        GroupLabel(stringResource(R.string.settings_portfolio_section))
        SettingsCard {
            ItemRow(
                title = stringResource(R.string.settings_riskprofile_section),
                subtitle = stringResource(R.string.settings_riskprofile_body),
                onClick = onOpenRiskProfile,
                navigates = true
            )
            AppDivider()
            ItemRow(
                title = stringResource(R.string.settings_account_type_section),
                subtitle = stringResource(R.string.settings_account_type_body),
                below = {
                    AppSegmentedChoice(
                        options = AccountType.entries.map { stringResource(it.labelRes()) },
                        selectedIndex = AccountType.entries.indexOf(state.accountType),
                        onSelect = { onAccountTypeSelect(AccountType.entries[it]) }
                    )
                }
            )
            AppDivider()
            // Bevakningstiden för pågående byten (SET-8, issue #244) — samma val som kontotypen
            // (regel 4). Valet styr utgång, likvid på väg och "dag N av X" överallt.
            val ttlOptions = SwitchWatchCalc.ALLOWED_WATCH_TTL_DAYS
            ItemRow(
                title = stringResource(R.string.settings_switch_watch_ttl_section),
                subtitle = stringResource(R.string.settings_switch_watch_ttl_body),
                below = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        AppSegmentedChoice(
                            options = ttlOptions.map { stringResource(R.string.format_settings_switch_watch_ttl_option, it) },
                            selectedIndex = ttlOptions.indexOf(state.switchWatchTtlDays),
                            onSelect = { onSwitchWatchTtlSelect(ttlOptions[it]) }
                        )
                        ItemRowText(title = null, subtitle = stringResource(R.string.settings_switch_watch_ttl_note))
                    }
                }
            )
            AppDivider()
            // Jämförelsereferensen (HEM-10, issue #102) — appens val är ett förslag, användarens
            // val vinner. Raden säger alltid vilket av de två som gäller just nu.
            ItemRow(
                title = stringResource(R.string.settings_benchmark_section),
                subtitle = state.chosenBenchmarkName
                    ?.let { stringResource(R.string.format_settings_benchmark_chosen, it) }
                    ?: stringResource(R.string.settings_benchmark_automatic),
                below = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        ItemRowText(title = null, subtitle = stringResource(R.string.settings_benchmark_body))
                        if (state.benchmarkPickFailed) FieldError(stringResource(R.string.settings_benchmark_pick_failed))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            AppButton(text = stringResource(R.string.settings_benchmark_button), onClick = onOpenBenchmarkPicker)
                            if (state.chosenBenchmarkName != null) {
                                AppButton(
                                    text = stringResource(R.string.settings_benchmark_clear),
                                    onClick = onClearBenchmark,
                                    variant = ButtonVariant.Text
                                )
                            }
                        }
                    }
                }
            )
            AppDivider()
            ItemRow(
                title = stringResource(R.string.settings_facit_section),
                subtitle = stringResource(R.string.settings_facit_body),
                onClick = onOpenFacit,
                navigates = true
            )
        }

        GroupLabel(stringResource(R.string.settings_data_section))
        SettingsCard {
            // Ingen "Uppdatera nu"-knapp längre (SET-2 struken): uppdatering sker med en
            // dragning där datan faktiskt visas (UI-11), inte tre skärmar bort under en
            // inställningsrubrik. Raden svarar på "hur färsk är datan", vilket är en annan
            // fråga än "uppdatera den".
            ItemRow(
                title = stringResource(R.string.settings_price_update_section),
                subtitle = stringResource(R.string.settings_price_update_body),
                below = { ItemRowText(title = null, subtitle = lastPriceSyncText(state.lastPriceSyncEpochMillis)) }
            )
            AppDivider()
            ItemRow(
                title = stringResource(R.string.settings_import_section),
                subtitle = stringResource(R.string.settings_import_body),
                onClick = onImportHoldings,
                navigates = true
            )
            AppDivider()
            ItemRow(
                title = stringResource(R.string.settings_import_orders_section),
                subtitle = stringResource(R.string.settings_import_orders_body),
                onClick = onImportOrders,
                navigates = true
            )
        }

        GroupLabel(stringResource(R.string.settings_backup_section))
        SettingsCard(
            modifier = Modifier
                .bringIntoViewRequester(backupBringIntoView)
                .testTag(SETTINGS_BACKUP_CARD_TEST_TAG)
        ) {
            // En rad för båda transporterna: det är samma kopia, och det är den Hems
            // påminnelse (HEM-12) räknar från.
            ItemRowText(title = null, subtitle = stringResource(R.string.settings_backup_body), modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s))
            ItemRow(
                title = stringResource(R.string.settings_backup_file_title),
                below = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        ItemRowText(title = null, subtitle = lastBackupText(state.lastBackup))
                        // Meddelandet läses direkt ur tillståndet och kvitteras i ViewModel:en, av samma
                        // skäl som "databasen tömd" nedan: en engångshändelse lagrad som bestående
                        // tillstånd spelas upp igen vid varje rotation och återbesök (issue #78).
                        state.backupMessage?.let { message ->
                            Text(backupMessageText(message), style = AppTheme.typography.itemSubtitle)
                            AppButton(text = stringResource(R.string.close), onClick = onBackupMessageDismiss, variant = ButtonVariant.Text)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            AppButton(
                                text = stringResource(R.string.settings_backup_export_button),
                                onClick = onExportBackup,
                                enabled = !state.backupInProgress
                            )
                            AppButton(
                                text = stringResource(R.string.settings_backup_restore_button),
                                onClick = { showRestoreConfirm = true },
                                enabled = !state.backupInProgress,
                                variant = ButtonVariant.Text
                            )
                        }
                    }
                }
            )
            // Molnbackupen (SET-7) ligger i samma kort som filbackupen, inte i ett eget:
            // det är samma data och samma kontrakt, bara en annan transport. Två kort hade
            // antytt två sorters säkerhetskopia.
            AppDivider()
            ItemRow(
                title = stringResource(R.string.settings_drive_backup_title),
                subtitle = stringResource(R.string.settings_drive_backup_body),
                below = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        if (state.driveBackupNeedsAuth) FieldError(stringResource(R.string.settings_drive_backup_needs_auth))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            AppButton(
                                text = stringResource(R.string.settings_drive_backup_button),
                                onClick = onBackupToDrive,
                                enabled = !state.backupInProgress
                            )
                            AppButton(
                                text = stringResource(R.string.settings_drive_restore_button),
                                onClick = { showDriveRestoreConfirm = true },
                                enabled = !state.backupInProgress,
                                variant = ButtonVariant.Text
                            )
                        }
                    }
                }
            )
        }

        if (onOpenGallery != null) {
            GroupLabel(stringResource(R.string.settings_developer_section))
            SettingsCard {
                ItemRow(
                    title = stringResource(R.string.component_gallery_title),
                    subtitle = stringResource(R.string.settings_components_body),
                    onClick = onOpenGallery,
                    navigates = true
                )
            }
        }

        GroupLabel(stringResource(R.string.settings_danger_section))
        AppCard(tone = Tone.Error, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
            ItemRowText(title = null, subtitle = stringResource(R.string.settings_danger_body), modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s))
            Column(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                // Meddelandet läses direkt ur tillståndet och kvitteras i ViewModel:en — ingen
                // lokal spegling via LaunchedEffect, som fick engångshändelsen att spelas upp
                // igen vid varje rotation och återbesök (issue #78). Kvitteringen är också
                // meddelandets enda väg ut; tidigare fanns ingen.
                if (state.databaseCleared) {
                    Text(stringResource(R.string.settings_clear_database_success), style = AppTheme.typography.itemSubtitle)
                    AppButton(text = stringResource(R.string.close), onClick = onClearedMessageDismiss, variant = ButtonVariant.Text)
                }
                AppButton(
                    text = stringResource(R.string.settings_clear_database_button),
                    onClick = { showClearConfirm = true },
                    variant = ButtonVariant.Destructive
                )
            }
        }

        // Om-kortet med Kodhästs signatur sist i Inställningar (SET-9).
        GroupLabel(stringResource(R.string.settings_about_section))
        AboutCard(appName = stringResource(R.string.app_name), version = versionName)
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.clear_database_confirm_title),
            message = stringResource(R.string.clear_database_confirm_body),
            confirmLabel = stringResource(R.string.settings_clear_database_button),
            onConfirm = {
                onClearDatabase()
                showClearConfirm = false
            },
            onDismiss = { showClearConfirm = false },
            destructive = true
        )
    }

    // Samma bekräftelse som filåterställningen, och med flit samma text: det är samma
    // ersättning av samma data, bara från en annan plats.
    if (showDriveRestoreConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.backup_restore_confirm_title),
            message = stringResource(R.string.backup_restore_confirm_body),
            confirmLabel = stringResource(R.string.backup_restore_confirm_button),
            onConfirm = {
                showDriveRestoreConfirm = false
                onRestoreFromDrive()
            },
            onDismiss = { showDriveRestoreConfirm = false }
        )
    }

    // Bekräftelsen kommer före filväljaren, inte efter: en återställning ersätter all data, och
    // frågan ska ställas innan användaren är mitt i ett val (samma ordning som farozonen).
    if (showRestoreConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.backup_restore_confirm_title),
            message = stringResource(R.string.backup_restore_confirm_body),
            confirmLabel = stringResource(R.string.backup_restore_confirm_button),
            onConfirm = {
                showRestoreConfirm = false
                onRestoreBackup()
            },
            onDismiss = { showRestoreConfirm = false }
        )
    }
}

@Composable
private fun lastBackupText(record: BackupRecord?): String {
    if (record == null) return stringResource(R.string.settings_backup_never)
    val date = BackupReminderCalc.formatDate(record.epochMillis)
    return when (record.target) {
        BackupTarget.DRIVE -> stringResource(R.string.format_settings_backup_last, date, stringResource(R.string.backup_target_drive))
        BackupTarget.FILE -> stringResource(R.string.format_settings_backup_last, date, stringResource(R.string.backup_target_file))
        null -> stringResource(R.string.format_settings_backup_last_date_only, date)
    }
}

@Composable
private fun backupMessageText(message: BackupMessage): String = when (message) {
    BackupMessage.Exported -> stringResource(R.string.backup_export_success)

    is BackupMessage.Restored -> stringResource(
        R.string.format_backup_restore_success,
        message.summary.funds,
        message.summary.transactions,
        message.summary.suggestionRecords,
        message.summary.switchWatches
    )

    BackupMessage.DriveSaved -> stringResource(R.string.drive_backup_success)

    BackupMessage.DriveEmpty -> stringResource(R.string.drive_backup_empty)

    // Texten ur den enda felmappningen (DataError.toMessage(), NFR-6).
    is BackupMessage.Failed -> stringResource(message.error.message)
}

@Composable
private fun lastPriceSyncText(epochMillis: Long?): String {
    if (epochMillis == null) return stringResource(R.string.settings_last_price_sync_never)
    val formatted = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(lastSyncFormatter)
    return stringResource(R.string.format_settings_last_price_sync, formatted)
}

private fun themeModeLabelRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.AUTO -> R.string.theme_auto
}
