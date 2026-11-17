package be.ap.laatsteronde.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.UserRepository
import be.ap.laatsteronde.data.UserRole
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Shared by the login and register validation. */
internal val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/** Checks the input before we contact Firebase. Returns null when everything is fine. */
@StringRes
internal fun validateLogin(email: String, password: String): Int? = when {
    !EMAIL_REGEX.matches(email.trim()) -> R.string.error_email_invalid
    password.isEmpty() -> R.string.error_password_empty
    else -> null
}

/** Turns a Firebase exception into a message the user understands. */
@StringRes
internal fun loginErrorMessageFor(e: Exception): Int = when (e) {
    // Firebase does not tell whether the e-mail or the password was wrong (on purpose).
    is FirebaseAuthInvalidCredentialsException,
    is FirebaseAuthInvalidUserException -> R.string.error_wrong_credentials
    is FirebaseNetworkException -> R.string.error_no_internet
    else -> R.string.error_login_failed
}

/** Everything the login screen shows. [loggedInRole] is set once, after a successful login. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    @StringRes val errorMessage: Int? = null,
    val loggedInRole: UserRole? = null
)

class LoginViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Each change also clears an old error message.
    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email, errorMessage = null) }
    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password, errorMessage = null) }

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return

        val inputError = validateLogin(state.email, state.password)
        if (inputError != null) {
            _uiState.update { it.copy(errorMessage = inputError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.login(state.email.trim(), state.password)
                val role = repository.currentRole()
                if (role == null) {
                    // An account without a profile has no role, so it cannot use the app.
                    repository.logout()
                    _uiState.update { it.copy(isLoading = false, errorMessage = R.string.error_login_failed) }
                } else {
                    _uiState.update { it.copy(isLoading = false, loggedInRole = role) }
                }
            } catch (e: CancellationException) {
                throw e // the ViewModel is being cleared; do not show an error
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = loginErrorMessageFor(e)) }
            }
        }
    }
}
