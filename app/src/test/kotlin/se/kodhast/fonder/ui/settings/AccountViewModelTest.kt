package se.kodhast.fonder.ui.settings

import android.content.Context
import app.cash.turbine.test
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.kodhast.fonder.data.auth.AuthRepository
import se.kodhast.fonder.data.auth.AuthUser
import se.kodhast.fonder.data.auth.SignInException
import se.kodhast.fonder.data.common.DataError

/**
 * Kontot i avatarmenyn (TP-6, NAV-7, #280 C): inloggning, utloggning och felet som visas en gång. Fake i stället för
 * mock: `currentUser` är ett hett flöde som ska kunna emittera mitt i ett test – den vägen den riktiga
 * `FirebaseAuthRepository` rapporterar en session som gått ut.
 */
class AccountViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /** Credential Manager kräver ett aktivitets-context; fakerepot ignorerar det, så en relaxad mock räcker. */
    private val activityContext: Context = mockk(relaxed = true)

    private val authUser = MutableStateFlow<AuthUser?>(null)
    private var signInResult: Result<AuthUser> = Result.success(AuthUser("uid-1", "Test Testsson", "test@example.com"))
    private var signInCalls = 0
    private var signOutCalled = false

    private val fakeAuth = object : AuthRepository {
        override val currentUser: Flow<AuthUser?> = authUser
        override suspend fun signInWithGoogle(activityContext: Context): Result<AuthUser> {
            signInCalls++
            return signInResult.onSuccess { authUser.value = it }
        }
        override suspend fun signOut() {
            signOutCalled = true
            authUser.value = null
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `en lyckad inloggning syns som användaren`() = runTest(dispatcher) {
        val vm = AccountViewModel(fakeAuth)
        vm.user.test {
            assertNull(awaitItem())
            vm.signIn(activityContext)
            assertEquals("test@example.com", awaitItem()?.email)
            assertNull(vm.error.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ett avbrutet kontoval ger inget fel`() = runTest(dispatcher) {
        // Att stänga kontoväljaren är ett val, inte ett fel.
        signInResult = Result.failure(SignInException(SignInException.Reason.CANCELLED))
        val vm = AccountViewModel(fakeAuth)
        vm.signIn(activityContext)
        advanceUntilIdle()
        assertNull(vm.error.value)
    }

    @Test
    fun `ett riktigt fel visas med sin orsak och kvitteras bort`() = runTest(dispatcher) {
        signInResult = Result.failure(SignInException(SignInException.Reason.NO_CREDENTIAL))
        val vm = AccountViewModel(fakeAuth)
        vm.signIn(activityContext)
        advanceUntilIdle()
        assertEquals(DataError.SignInNoCredential, vm.error.value?.cause)

        vm.onErrorShown()
        assertNull(vm.error.value)
    }

    @Test
    fun `ett fel utan orsak faller tillbaka på SignInFailed`() = runTest(dispatcher) {
        // Ett undantag som inte är en SignInException får inte ge ett tyst misslyckande.
        signInResult = Result.failure(IllegalStateException("oväntat"))
        val vm = AccountViewModel(fakeAuth)
        vm.signIn(activityContext)
        advanceUntilIdle()
        assertEquals(DataError.SignInFailed, vm.error.value?.cause)
    }

    @Test
    fun `dubbeltryck på logga in startar en inloggning`() = runTest(dispatcher) {
        // Kontoväljaren ger ingen synlig återkoppling förrän den öppnats, och två anrop ger två väljare.
        val vm = AccountViewModel(fakeAuth)
        vm.signIn(activityContext)
        vm.signIn(activityContext)
        advanceUntilIdle()
        assertEquals(1, signInCalls)
    }

    @Test
    fun `logga ut tömmer användaren`() = runTest(dispatcher) {
        authUser.value = AuthUser("uid-1", "Test Testsson", "test@example.com")
        val vm = AccountViewModel(fakeAuth)
        vm.user.test {
            // stateIn börjar på null innan Firebase-flödet hunnit svara.
            val first = awaitItem() ?: awaitItem()
            assertEquals("uid-1", first?.id)
            vm.signOut()
            assertNull(awaitItem())
            assertTrue(signOutCalled)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
