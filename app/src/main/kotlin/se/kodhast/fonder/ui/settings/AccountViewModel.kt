package se.kodhast.fonder.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.kodhast.fonder.data.auth.AuthRepository
import se.kodhast.fonder.data.auth.AuthUser
import se.kodhast.fonder.data.auth.SignInException
import se.kodhast.fonder.data.common.AppErrors
import se.kodhast.fonder.data.common.DataError
import se.kodhast.ui.common.UiError

/**
 * Kontot i avatarmenyn (TP-6, NAV-7): vem som är inloggad, inloggning med Google och utloggning. Inloggningen flyttar
 * ingen data i sig – den är förutsättningen för Drive-backupen (SET-7). Ett fel står i [error] tills det visats
 * ([onErrorShown]); ett avbrutet kontoval är inget fel och hamnar aldrig där.
 */
@HiltViewModel
class AccountViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {

    /** Inloggad Google-användare, null utloggad – läst ur Firebase, aldrig en egen kopia. */
    val user: StateFlow<AuthUser?> = auth.currentUser.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val failure = MutableStateFlow<UiError?>(null)
    val error: StateFlow<UiError?> = failure

    private var signInJob: Job? = null

    /** Loggar in med Google; [activityContext] måste vara aktivitetens. Ett andra tryck medan rutan är uppe gör inget. */
    fun signIn(activityContext: Context) {
        if (signInJob?.isActive == true) return
        failure.update { null }
        signInJob = viewModelScope.launch {
            auth.signInWithGoogle(activityContext).onFailure { error ->
                val cancelled = (error as? SignInException)?.reason == SignInException.Reason.CANCELLED
                if (!cancelled) failure.update { AppErrors.map(error as? SignInException ?: DataError.SignInFailed) }
            }
        }
    }

    fun signOut() {
        failure.update { null }
        viewModelScope.launch { auth.signOut() }
    }

    fun onErrorShown() = failure.update { null }
}
