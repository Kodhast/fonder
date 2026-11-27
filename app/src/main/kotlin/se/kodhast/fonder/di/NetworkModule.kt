package se.kodhast.fonder.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import se.kodhast.ui.account.ConnectivityMonitor
import se.kodhast.ui.account.NetworkStatus

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** Om appen har internet – pluppen på avataren (NAV-7). En per app, som lever lika länge som appen. */
    @Provides
    @Singleton
    fun provideNetworkStatus(@ApplicationContext context: Context): NetworkStatus = ConnectivityMonitor(context, CoroutineScope(SupervisorJob() + Dispatchers.Default))
}
