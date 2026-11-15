package se.kodhast.fonder.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import se.kodhast.fonder.data.auth.AuthRepository
import se.kodhast.fonder.data.auth.FirebaseAuthRepository
import se.kodhast.fonder.data.imports.ContentResolverImportFileReader
import se.kodhast.fonder.data.imports.ImportFileReader
import se.kodhast.fonder.data.imports.PdfBoxTextExtractor
import se.kodhast.fonder.data.imports.PdfTextExtractor
import se.kodhast.fonder.data.network.AvanzaClient
import se.kodhast.fonder.data.network.AvanzaPriceSource
import se.kodhast.fonder.data.network.AvanzaSource
import se.kodhast.fonder.data.network.FondlistaFundPageSource
import se.kodhast.fonder.data.network.FondlistaHtmlSource
import se.kodhast.fonder.data.network.FondlistaWafTokenSource
import se.kodhast.fonder.data.network.FxRateSource
import se.kodhast.fonder.data.network.HandelsbankenFondlistaClient
import se.kodhast.fonder.data.network.IsinPriceHistorySource
import se.kodhast.fonder.data.network.RiksbankFxClient
import se.kodhast.fonder.data.network.WebViewFondlistaWafTokenSource
import se.kodhast.fonder.data.repository.AvanzaFundMetadataRepository
import se.kodhast.fonder.data.repository.BackupRepository
import se.kodhast.fonder.data.repository.DriveBackupRepository
import se.kodhast.fonder.data.repository.FundMetadataRepository
import se.kodhast.fonder.data.repository.FundPriceRepository
import se.kodhast.fonder.data.repository.GoogleDriveBackupRepository
import se.kodhast.fonder.data.repository.HandelsbankenFundPriceRepository
import se.kodhast.fonder.data.repository.LocalBackupRepository
import se.kodhast.fonder.data.repository.RoomSuggestionDismissalRepository
import se.kodhast.fonder.data.repository.RoomSuggestionRecordRepository
import se.kodhast.fonder.data.repository.RoomSwitchWatchRepository
import se.kodhast.fonder.data.repository.RoomTransactionRepository
import se.kodhast.fonder.data.repository.SuggestionDismissalRepository
import se.kodhast.fonder.data.repository.SuggestionRecordRepository
import se.kodhast.fonder.data.repository.SwitchWatchRepository
import se.kodhast.fonder.data.repository.TransactionRepository
import se.kodhast.fonder.worker.FundPriceRefreshScheduler
import se.kodhast.fonder.worker.WorkManagerFundPriceRefreshScheduler

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindTransactionRepository(impl: RoomTransactionRepository): TransactionRepository

    @Binds
    abstract fun bindFundPriceRepository(impl: HandelsbankenFundPriceRepository): FundPriceRepository

    @Binds
    abstract fun bindFundMetadataRepository(impl: AvanzaFundMetadataRepository): FundMetadataRepository

    @Binds
    abstract fun bindSuggestionRecordRepository(impl: RoomSuggestionRecordRepository): SuggestionRecordRepository

    @Binds
    abstract fun bindSwitchWatchRepository(impl: RoomSwitchWatchRepository): SwitchWatchRepository

    @Binds
    abstract fun bindSuggestionDismissalRepository(impl: RoomSuggestionDismissalRepository): SuggestionDismissalRepository

    @Binds
    abstract fun bindFondlistaHtmlSource(impl: HandelsbankenFondlistaClient): FondlistaHtmlSource

    @Binds
    abstract fun bindFondlistaWafTokenSource(impl: WebViewFondlistaWafTokenSource): FondlistaWafTokenSource

    @Binds
    abstract fun bindFondlistaFundPageSource(impl: HandelsbankenFondlistaClient): FondlistaFundPageSource

    @Binds
    abstract fun bindFxRateSource(impl: RiksbankFxClient): FxRateSource

    @Binds
    abstract fun bindAvanzaSource(impl: AvanzaClient): AvanzaSource

    @Binds
    abstract fun bindPdfTextExtractor(impl: PdfBoxTextExtractor): PdfTextExtractor

    @Binds
    abstract fun bindImportFileReader(impl: ContentResolverImportFileReader): ImportFileReader

    @Binds
    abstract fun bindBackupRepository(impl: LocalBackupRepository): BackupRepository

    @Binds
    abstract fun bindDriveBackupRepository(impl: GoogleDriveBackupRepository): DriveBackupRepository

    @Binds
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    abstract fun bindFundPriceRefreshScheduler(impl: WorkManagerFundPriceRefreshScheduler): FundPriceRefreshScheduler

    companion object {
        /**
         * Prioritetsordning för ISIN-baserad kurshistorik (se KRAVLISTA TP-14) — idag bara
         * Avanza; Nordnet/Morningstar undersöktes men saknade en bekräftat inloggningsfri
         * sökväg från ISIN. Lista (inte Set-multibinding) för att ordningen ska vara
         * deterministisk.
         */
        @Provides
        @Singleton
        fun provideIsinPriceHistorySources(avanza: AvanzaPriceSource): List<IsinPriceHistorySource> = listOf(avanza)
    }
}
