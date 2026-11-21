package se.kodhast.fonder.ui.settings

import android.app.PendingIntent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import app.cash.turbine.test
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import se.kodhast.fonder.data.common.DataError
import se.kodhast.fonder.data.datastore.PreferencesRepository
import se.kodhast.fonder.data.repository.BackupFingerprint
import se.kodhast.fonder.data.repository.BackupFormatException
import se.kodhast.fonder.data.repository.BackupPayload
import se.kodhast.fonder.data.repository.BackupSerializer
import se.kodhast.fonder.data.repository.BackupStatusRepository
import se.kodhast.fonder.data.repository.DriveBackupRepository
import se.kodhast.fonder.data.repository.DriveResult
import se.kodhast.fonder.data.repository.FakeBackupRepository
import se.kodhast.fonder.data.repository.FakeCachedPriceRepository
import se.kodhast.fonder.data.repository.FakeFundMetadataRepository
import se.kodhast.fonder.data.repository.FakeTransactionRepository
import se.kodhast.fonder.data.repository.FundMetadataRepository
import se.kodhast.fonder.data.repository.FundPriceRepository
import se.kodhast.fonder.data.repository.RestoreSummary
import se.kodhast.fonder.data.repository.TransactionRepository
import se.kodhast.fonder.domain.model.AccountType
import se.kodhast.fonder.domain.model.BackupTarget
import se.kodhast.fonder.domain.model.Fund
import se.kodhast.fonder.domain.model.FundCatalog
import se.kodhast.fonder.domain.model.FundFilterVocabulary
import se.kodhast.fonder.domain.model.FundMetadata
import se.kodhast.fonder.domain.model.FundPrice
import se.kodhast.fonder.domain.model.FundScreenQuery
import se.kodhast.fonder.domain.model.Transaction
import se.kodhast.fonder.domain.model.TransactionType
import se.kodhast.fonder.domain.usecase.DriveBackupFile
import se.kodhast.fonder.domain.usecase.FeeComparisonCalc
import se.kodhast.fonder.domain.usecase.SwitchPlanCalc
import se.kodhast.fonder.domain.usecase.SwitchWatchCalc
import se.kodhast.fonder.worker.FakeFundPriceRefreshScheduler

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private var clearAllCalled = false
    private lateinit var dataStore: DataStore<Preferences>

    private val fakeTransactionRepo = object : TransactionRepository by FakeTransactionRepository() {
        override suspend fun clearAll() {
            clearAllCalled = true
        }
    }

    /** Räknar bl.a. manuella uppdateringar (SET-2) och referensfondsskanningar (HEM-10, issue #102). */
    private val fakeScheduler = FakeFundPriceRefreshScheduler()

    private val fakeBackupRepo = FakeBackupRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        // DataStore skriver annars på sin egen Dispatchers.IO-bundna scope, frikopplad från
        // testets StandardTestDispatcher — awaitItem() väntar då på en riktig (klock-)tid som
        // ibland hann överstiga Turbines timeout under CI-belastning (flaky, se
        // "lastPriceSyncEpochMillis speglar preferences efter en uppdatering"). Ger DataStore
        // samma dispatcher som testet så skrivningen blir en del av samma virtuella tid.
        dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(dispatcher + SupervisorJob()),
            produceFile = { tempFolder.newFile("settings_test.preferences_pb") }
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Slår upp ISIN för en katalogfond utan ett — null = fonden går inte att jämföra mot (issue #102). */
    private var isinByFundId: Map<String, String> = emptyMap()

    private val fakeFundPriceRepo = object : FundPriceRepository by FakeCachedPriceRepository() {
        override suspend fun lookupIsin(fundId: String): String? = isinByFundId[fundId]
    }

    private val fakeFundMetadataRepo = FakeFundMetadataRepository()

    /** Drive-transporten som testet styr (SET-7). Fake, inte mock — samma princip som övriga datalager. */
    private var driveUploadResult: DriveResult<String> = DriveResult.Success("file-id")
    private var driveDownloadResult: DriveResult<String> = DriveResult.Success("{}")
    private var uploadedJson: String? = null

    private val fakeDriveRepo = object : DriveBackupRepository {
        override suspend fun upload(json: String): DriveResult<String> {
            uploadedJson = json
            return driveUploadResult
        }
        override suspend fun downloadLatest(): DriveResult<String> = driveDownloadResult
        override suspend fun list(): DriveResult<List<DriveBackupFile>> = DriveResult.Success(emptyList())
    }

    /** Uppsamlade auktoriseringsintent — UI:t startar dem, testet räknar dem. */
    private val authorizationRequests = mutableListOf<PendingIntent>()

    private fun viewModel(preferences: PreferencesRepository = PreferencesRepository(dataStore)) = SettingsViewModel(preferences, fakeTransactionRepo, fakeScheduler, fakeBackupRepo, fakeFundPriceRepo, fakeFundMetadataRepo, fakeDriveRepo, BackupStatusRepository(fakeBackupRepo, preferences))

    @Test
    fun `clearDatabase anropar repository och satter databaseCleared i uiState`() = runTest(dispatcher) {
        val preferences = PreferencesRepository(dataStore)
        // Tom-markören pekar på en körning som töms med databasen — den får inte dölja nästa plan.
        preferences.setSwitchPlanEmptiedAt(5_000)
        val vm = viewModel(preferences)

        vm.uiState.test {
            assertFalse(awaitItem().databaseCleared)

            vm.onEvent(SettingsEvent.ClearDatabase)
            var state = awaitItem()
            while (!state.databaseCleared) state = awaitItem()

            assertTrue(clearAllCalled)
            assertNull(preferences.switchPlanEmptiedAtEpochMillis.first())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onClearedMessageDismissed kvitterar handelsen sa den inte spelas upp igen`() = runTest(dispatcher) {
        // Regression (issue #78): flaggan nollställdes aldrig, så meddelandet kom tillbaka vid
        // varje rotation och — eftersom uiState är WhileSubscribed — vid varje återbesök i
        // Inställningar. En engångshändelse får inte ligga kvar som bestående tillstånd.
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.ClearDatabase)
            var state = awaitItem()
            while (!state.databaseCleared) state = awaitItem()

            vm.onEvent(SettingsEvent.ClearedMessageDismissed)
            state = awaitItem()
            while (state.databaseCleared) state = awaitItem()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lastPriceSyncEpochMillis ar null innan nagon uppdatering skett (SET-2)`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.uiState.test {
            assertEquals(null, awaitItem().lastPriceSyncEpochMillis)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lastPriceSyncEpochMillis speglar preferences efter en uppdatering (SET-2)`() = runTest(dispatcher) {
        val preferences = PreferencesRepository(dataStore)
        val vm = viewModel(preferences)

        vm.uiState.test {
            awaitItem()
            preferences.setLastPriceSyncEpochMillis(1_700_000_000_000L)
            var state = awaitItem()
            while (state.lastPriceSyncEpochMillis == null) state = awaitItem()

            assertEquals(1_700_000_000_000L, state.lastPriceSyncEpochMillis)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Kontotyp (SET-4, issue #70) ---

    @Test
    fun `accountType ar null innan nagot val gjorts`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.uiState.test {
            assertEquals(null, awaitItem().accountType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setAccountType sparar valet och speglas i uiState`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.SetAccountType(AccountType.ISK_KF))
            var state = awaitItem()
            while (state.accountType == null) state = awaitItem()

            assertEquals(AccountType.ISK_KF, state.accountType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Säkerhetskopiering (SET-6, issue #82) ---

    @Test
    fun `export lamnar serialiserad json till skrivaren och kvitterar`() = runTest(dispatcher) {
        fakeBackupRepo.exportResult = Result.success("""{"formatVersion":1}""")
        var written: String? = null
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.Export { written = it })
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            assertEquals("""{"formatVersion":1}""", written)
            assertEquals(BackupMessage.Exported, state.backupMessage)
            assertFalse(state.backupInProgress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ett skrivfel raknas som misslyckad export, inte som tyst lyckad`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.Export { throw java.io.IOException("ingen skrivrätt") })
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            assertEquals(DataError.BackupExportFailed, state.backupMessage.failureCause())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restore skickar filens innehall till repositoryt och redovisar vad som skrevs`() = runTest(dispatcher) {
        fakeBackupRepo.restoreResult = Result.success(RestoreSummary(funds = 2, transactions = 5, suggestionRecords = 3))
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.Restore { """{"formatVersion":1}""" })
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            assertEquals("""{"formatVersion":1}""", fakeBackupRepo.restoredJson)
            assertEquals(BackupMessage.Restored(RestoreSummary(2, 5, 3)), state.backupMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `en olasbar fil nar aldrig repositoryt och redovisas som trasig`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.Restore { null })
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            assertEquals(null, fakeBackupRepo.restoredJson)
            assertEquals(DataError.BackupUnreadable, state.backupMessage.failureCause())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `en fil fran en nyare app redovisas som versionsfel, inte som trasig`() = runTest(dispatcher) {
        // Skillnaden är hela poängen: "trasig fil" och "nyare app" leder till olika åtgärd för
        // användaren, så felet får inte plattas till på vägen upp till UI:t.
        fakeBackupRepo.restoreResult = Result.failure(BackupFormatException(BackupFormatException.Reason.UNSUPPORTED_VERSION, fileVersion = 99))
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.Restore { """{"formatVersion":99}""" })
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            assertEquals(DataError.BackupUnsupportedVersion, state.backupMessage.failureCause())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onBackupMessageDismissed kvitterar handelsen sa den inte spelas upp igen`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.Export {})
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            vm.onEvent(SettingsEvent.BackupMessageDismissed)
            state = awaitItem()
            while (state.backupMessage != null) state = awaitItem()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dubbeltryck startar inte en andra aterstallning`() = runTest(dispatcher) {
        // Samma spärr som importflödena (issue #75): återställningen ger ingen synlig
        // återkoppling förrän den är klar, så ett andra tryck skulle skriva samma rader igen.
        var reads = 0
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(
                SettingsEvent.Restore {
                    reads++
                    """{"formatVersion":1}"""
                }
            )
            vm.onEvent(
                SettingsEvent.Restore {
                    reads++
                    """{"formatVersion":1}"""
                }
            )
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            assertEquals(1, reads)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Bevakningstid för pågående byten (SET-8, issue #244) ---

    @Test
    fun `vald bevakningstid sparas visas och ber om en omrakning`() = runTest(dispatcher) {
        // Bakgrundskörningen stänger då byten som passerat den nya tiden och räknar om likviden på väg.
        val preferencesRepository = PreferencesRepository(dataStore)
        val vm = viewModel(preferencesRepository)

        vm.uiState.test {
            assertEquals(SwitchWatchCalc.DEFAULT_WATCH_TTL_DAYS, awaitItem().switchWatchTtlDays)
            vm.onEvent(SettingsEvent.SetSwitchWatchTtl(60L))
            advanceUntilIdle()
            assertEquals(60L, expectMostRecentItem().switchWatchTtlDays)
        }
        assertEquals(60L, preferencesRepository.switchWatchTtlDays.first())
        assertEquals(1, fakeScheduler.switchPlanScans)
    }

    @Test
    fun `samma bevakningstid igen eller en otillaten tid kostar ingen skanning`() = runTest(dispatcher) {
        val preferencesRepository = PreferencesRepository(dataStore)
        val vm = viewModel(preferencesRepository)

        vm.onEvent(SettingsEvent.SetSwitchWatchTtl(SwitchWatchCalc.DEFAULT_WATCH_TTL_DAYS))
        vm.onEvent(SettingsEvent.SetSwitchWatchTtl(45L))
        advanceUntilIdle()

        assertEquals(0, fakeScheduler.switchPlanScans)
        assertNull(preferencesRepository.switchWatchTtlDaysSetting.first())
    }

    // --- Omräkning av bytesplanen vid kontotypsbyte (HEM-8, issue #88) ---

    @Test
    fun `byte till ISK eller KF ber om en omrakning av bytesplanen`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.onEvent(SettingsEvent.SetAccountType(AccountType.ISK_KF))
        advanceUntilIdle()

        assertEquals(1, fakeScheduler.switchPlanScans)
    }

    @Test
    fun `samma kontotyp igen kostar ingen ny skanning`() = runTest(dispatcher) {
        // Skanningen kostar en källfråga plus budgeterad köpbarhetsverifiering per underviktad
        // nivå — den ska följa faktiska byten, inte antalet tryck.
        val vm = viewModel()

        vm.onEvent(SettingsEvent.SetAccountType(AccountType.ISK_KF))
        advanceUntilIdle()
        vm.onEvent(SettingsEvent.SetAccountType(AccountType.ISK_KF))
        advanceUntilIdle()

        assertEquals(1, fakeScheduler.switchPlanScans)
    }

    @Test
    fun `byte till depa eller AF ber inte om nagon omrakning`() = runTest(dispatcher) {
        // SET-4-gaten ger ingen plan alls i depå/AF — det finns inget att räkna om.
        val vm = viewModel()

        vm.onEvent(SettingsEvent.SetAccountType(AccountType.DEPA_AF))
        advanceUntilIdle()

        assertEquals(0, fakeScheduler.switchPlanScans)
    }

    // --- Val av jämförelsefond (HEM-10, issue #102) ---

    @Test
    fun `valt referens-ISIN sparas och startar en skanning`() = runTest(dispatcher) {
        val preferences = PreferencesRepository(dataStore)
        fakeFundMetadataRepo.metadataByIsin = mapOf("SE0011527613" to benchmarkMetadata())
        val vm = viewModel(preferences)

        vm.onEvent(SettingsEvent.ChooseBenchmark(Fund(fundId = "SHB1", name = "Global Index", isin = "SE0011527613")))
        advanceUntilIdle()

        assertEquals("SE0011527613", preferences.chosenBenchmarkIsin.first())
        // Utan skanningen hade den valda fondens historik dröjt till nästa backstop (issue #88).
        assertEquals(1, fakeScheduler.benchmarkScans)
    }

    @Test
    fun `en katalogfond utan ISIN slas upp innan den sparas`() = runTest(dispatcher) {
        // Katalogens träffar saknar ISIN — hela jämförelsekedjan är ISIN-nycklad.
        val preferences = PreferencesRepository(dataStore)
        isinByFundId = mapOf("SHB1" to "SE0011527613")
        val vm = viewModel(preferences)

        vm.onEvent(SettingsEvent.ChooseBenchmark(Fund(fundId = "SHB1", name = "Global Index", isin = null)))
        advanceUntilIdle()

        assertEquals("SE0011527613", preferences.chosenBenchmarkIsin.first())
    }

    @Test
    fun `en fond vars ISIN inte gar att sla upp sparas inte alls`() = runTest(dispatcher) {
        // Ett sparat fond-id som inget annat lager kan använda hade gett ett val som såg gjort
        // ut men aldrig gav en kurva.
        val preferences = PreferencesRepository(dataStore)
        val vm = viewModel(preferences)

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.ChooseBenchmark(Fund(fundId = "SHB1", name = "Fond utan ISIN", isin = null)))
            advanceUntilIdle()
            var state = awaitItem()
            while (!state.benchmarkPickFailed) state = awaitItem()

            assertTrue(state.benchmarkPickFailed)
            assertNull(preferences.chosenBenchmarkIsin.first())
            assertEquals(0, fakeScheduler.benchmarkScans)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rensat val faller tillbaka pa appens referens`() = runTest(dispatcher) {
        val preferences = PreferencesRepository(dataStore)
        preferences.setChosenBenchmarkIsin("SE0011527613")
        val vm = viewModel(preferences)

        vm.onEvent(SettingsEvent.ClearBenchmark)
        advanceUntilIdle()

        assertNull(preferences.chosenBenchmarkIsin.first())
    }

    @Test
    fun `det egna valet visas med fondens namn ur cachen`() = runTest(dispatcher) {
        val preferences = PreferencesRepository(dataStore)
        fakeFundMetadataRepo.metadataByIsin = mapOf("SE0011527613" to benchmarkMetadata())
        preferences.setChosenBenchmarkIsin("SE0011527613")
        val vm = viewModel(preferences)

        vm.uiState.test {
            var state = awaitItem()
            while (state.chosenBenchmarkName == null) state = awaitItem()

            assertEquals("Global Index", state.chosenBenchmarkName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ett val vars namn inte finns i cachen visas som ISIN, aldrig tomt`() = runTest(dispatcher) {
        val preferences = PreferencesRepository(dataStore)
        preferences.setChosenBenchmarkIsin("SE0011527613")
        val vm = viewModel(preferences)

        vm.uiState.test {
            var state = awaitItem()
            while (state.chosenBenchmarkName == null) state = awaitItem()

            assertEquals("SE0011527613", state.chosenBenchmarkName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Molnbackup till Drive (SET-7) ---

    private fun collectAuth(): (PendingIntent) -> Unit = { authorizationRequests += it }

    @Test
    fun `backupToDrive laddar upp den exporterade strangen`() = runTest(dispatcher) {
        fakeBackupRepo.exportResult = Result.success("""{"formatVersion":1}""")
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.BackupToDrive(collectAuth()))
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            assertEquals("""{"formatVersion":1}""", uploadedJson)
            assertEquals(BackupMessage.DriveSaved, state.backupMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `en misslyckad export laddar aldrig upp nagot`() = runTest(dispatcher) {
        // Går kontraktet inte att serialisera är felet i formatet, inte i transporten — och då
        // ska ingenting skrivas till Drive.
        fakeBackupRepo.exportResult = Result.failure(IllegalStateException("trasigt"))
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.BackupToDrive(collectAuth()))
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            assertNull(uploadedJson)
            assertEquals(DataError.BackupExportFailed, state.backupMessage.failureCause())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `utloggad ger ett eget meddelande, inte ett allmant fel`() = runTest(dispatcher) {
        driveUploadResult = DriveResult.NoAccount
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.BackupToDrive(collectAuth()))
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            assertEquals(DataError.DriveSignedOut, state.backupMessage.failureCause())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saknad auktorisering startar Googles ruta och visar inget felmeddelande`() = runTest(dispatcher) {
        // Användaren möts av Googles egen dialog — en röd rad bakom den vore förvirrande.
        driveUploadResult = DriveResult.NeedsAuthorization(mockk(relaxed = true))
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.BackupToDrive(collectAuth()))
            advanceUntilIdle()

            assertEquals(1, authorizationRequests.size)
            assertNull(vm.uiState.value.backupMessage)
            assertTrue(vm.uiState.value.driveBackupNeedsAuth)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDriveAuthorizationResolved rensar flaggan`() = runTest(dispatcher) {
        driveUploadResult = DriveResult.NeedsAuthorization(mockk(relaxed = true))
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.BackupToDrive(collectAuth()))
            var state = awaitItem()
            while (!state.driveBackupNeedsAuth) state = awaitItem()

            vm.onEvent(SettingsEvent.DriveAuthorizationResolved)
            state = awaitItem()
            while (state.driveBackupNeedsAuth) state = awaitItem()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restoreFromDrive lamnar den hamtade strangen till backup-kontraktet`() = runTest(dispatcher) {
        driveDownloadResult = DriveResult.Success("""{"formatVersion":1,"funds":[]}""")
        fakeBackupRepo.restoreResult = Result.success(RestoreSummary(2, 3, 4))
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.RestoreFromDrive(collectAuth()))
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            assertEquals("""{"formatVersion":1,"funds":[]}""", fakeBackupRepo.restoredJson)
            assertEquals(BackupMessage.Restored(RestoreSummary(2, 3, 4)), state.backupMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `en tom Drive-mapp ger ett eget meddelande och ror inte databasen`() = runTest(dispatcher) {
        driveDownloadResult = DriveResult.NoBackupFound
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.RestoreFromDrive(collectAuth()))
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            assertNull(fakeBackupRepo.restoredJson)
            assertEquals(BackupMessage.DriveEmpty, state.backupMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `en trasig kopia i Drive ger samma fel som en trasig fil`() = runTest(dispatcher) {
        driveDownloadResult = DriveResult.Success("inte json")
        fakeBackupRepo.restoreResult = Result.failure(BackupFormatException(BackupFormatException.Reason.UNREADABLE))
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.RestoreFromDrive(collectAuth()))
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            assertEquals(DataError.BackupUnreadable, state.backupMessage.failureCause())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dubbeltryck startar inte tva samtidiga Drive-korningar`() = runTest(dispatcher) {
        // Samma spärr som export/restore (issue #75): ingen synlig återkoppling förrän den är
        // klar, och två parallella skulle skriva två kopior.
        var uploads = 0
        val countingDrive = object : DriveBackupRepository {
            override suspend fun upload(json: String): DriveResult<String> {
                uploads++
                return DriveResult.Success("id")
            }
            override suspend fun downloadLatest(): DriveResult<String> = DriveResult.NoBackupFound
            override suspend fun list(): DriveResult<List<DriveBackupFile>> = DriveResult.Success(emptyList())
        }
        val preferences = PreferencesRepository(dataStore)
        val vm = SettingsViewModel(
            preferences,
            fakeTransactionRepo,
            fakeScheduler,
            fakeBackupRepo,
            fakeFundPriceRepo,
            fakeFundMetadataRepo,
            countingDrive,
            BackupStatusRepository(fakeBackupRepo, preferences)
        )

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.BackupToDrive(collectAuth()))
            vm.onEvent(SettingsEvent.BackupToDrive(collectAuth()))
            advanceUntilIdle()

            assertEquals(1, uploads)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Senaste säkerhetskopia (SET-7, HEM-12) ---

    /** Väntar tills en säkerhetskopia är antecknad och knapparna släppts. */
    private suspend fun app.cash.turbine.ReceiveTurbine<SettingsUiState>.awaitLastBackup(): SettingsUiState {
        var state = awaitItem()
        while (state.lastBackup == null || state.backupInProgress) state = awaitItem()
        return state
    }

    @Test
    fun `ingen sakerhetskopia visas innan nagon gjorts`() = runTest(dispatcher) {
        viewModel().uiState.test {
            assertNull(awaitItem().lastBackup)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `en lyckad Drive-kopia antecknas med datum, mal och fingeravtryck`() = runTest(dispatcher) {
        fakeBackupRepo.payload.value = payloadWithTransactions(3)
        fakeBackupRepo.exportResult = Result.success(BackupSerializer.encode(payloadWithTransactions(3)))
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.BackupToDrive(collectAuth()))
            val record = awaitLastBackup().lastBackup!!

            assertEquals(BackupTarget.DRIVE, record.target)
            assertEquals(3, record.transactionCount)
            assertEquals(BackupFingerprint.of(fakeBackupRepo.payload.value), record.fingerprint)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `en lyckad filexport antecknas som fil`() = runTest(dispatcher) {
        fakeBackupRepo.exportResult = Result.success(BackupSerializer.encode(payloadWithTransactions(1)))
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.Export { })
            assertEquals(BackupTarget.FILE, awaitLastBackup().lastBackup?.target)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `kopian antecknas ur den uppladdade strangen, inte ur en senare lasning`() = runTest(dispatcher) {
        // Granskningsfynd: en ändring mellan exporten och anteckningen fanns inte i kopian.
        fakeBackupRepo.exportResult = Result.success(BackupSerializer.encode(payloadWithTransactions(2)))
        fakeBackupRepo.payload.value = payloadWithTransactions(3)
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.BackupToDrive(collectAuth()))
            val record = awaitLastBackup().lastBackup!!

            assertEquals(BackupFingerprint.of(payloadWithTransactions(2)), record.fingerprint)
            assertEquals(2, record.transactionCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `en misslyckad kopia lamnar senaste sakerhetskopia orord`() = runTest(dispatcher) {
        driveUploadResult = DriveResult.Error("Drive 503")
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.BackupToDrive(collectAuth()))
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            assertEquals(DataError.DriveFailed, state.backupMessage.failureCause())
            assertNull(state.lastBackup)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `en lyckad aterstallning fran fil antecknas, sa Hem inte paminner om den`() = runTest(dispatcher) {
        fakeBackupRepo.payload.value = payloadWithTransactions(2)
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.Restore { BackupSerializer.encode(payloadWithTransactions(2)) })
            val record = awaitLastBackup().lastBackup!!

            assertEquals(BackupTarget.FILE, record.target)
            assertEquals(BackupFingerprint.of(fakeBackupRepo.payload.value), record.fingerprint)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `en lyckad aterstallning fran Drive antecknas som Drive`() = runTest(dispatcher) {
        driveDownloadResult = DriveResult.Success(BackupSerializer.encode(payloadWithTransactions(1)))
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.RestoreFromDrive(collectAuth()))
            assertEquals(BackupTarget.DRIVE, awaitLastBackup().lastBackup?.target)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `en misslyckad aterstallning antecknas inte`() = runTest(dispatcher) {
        fakeBackupRepo.restoreResult = Result.failure(BackupFormatException(BackupFormatException.Reason.UNREADABLE))
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(SettingsEvent.Restore { "trasig" })
            var state = awaitItem()
            while (state.backupMessage == null) state = awaitItem()

            assertNull(state.lastBackup)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `den gamla molnbackupens tidsstampel visas som en Drive-kopia`() = runTest(dispatcher) {
        // Före HEM-12 sparades bara den dygnsvisa körningens tidsstämpel. Den ska inte tappas
        // vid uppgraderingen — men den har inget fingeravtryck, så Hem påminner ändå.
        dataStore.edit { it[longPreferencesKey("last_drive_backup_epoch_millis")] = 1_700_000_000_000L }
        val vm = viewModel()

        vm.uiState.test {
            val record = awaitLastBackup().lastBackup!!
            assertEquals(1_700_000_000_000L, record.epochMillis)
            assertEquals(BackupTarget.DRIVE, record.target)
            assertNull(record.fingerprint)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun payloadWithTransactions(count: Int) = BackupPayload(
        exportedAtEpochMillis = 0,
        funds = listOf(Fund(fundId = "SHB1", name = "Fond A")),
        transactions = (1..count).map { i ->
            Transaction(id = i.toLong(), fundId = "SHB1", type = TransactionType.KOP, epochDay = 19_000L + i, shares = 1.0, pricePerShare = 100.0)
        }
    )

    private fun benchmarkMetadata() = FundMetadata(
        isin = "SE0011527613", name = "Global Index", orderbookId = "1", totalFee = 0.2, managementFee = 0.2,
        category = null, fundType = "Aktiefond", companyName = null, risk = 5, indexFund = true,
        startDateEpochDay = null, minimumBuy = null, tags = emptyList()
    )

    /** Orsaken till ett misslyckat backup-meddelande (NFR-6), eller `null` för ett lyckat. */
    private fun BackupMessage?.failureCause(): Throwable? = (this as? BackupMessage.Failed)?.error?.cause
}
