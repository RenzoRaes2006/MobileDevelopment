package be.ap.laatsteronde.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.UserRepository
import be.ap.laatsteronde.data.UserRole
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the register screen shows. The ViewModel survives rotation, so nothing is lost. */
data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val role: UserRole? = null,
    val isLoading: Boolean = false,
    @StringRes val errorMessage: Int? = null,
    val isRegistered: Boolean = false
)

class RegisterViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    // Each change also clears an old error message.
    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, errorMessage = null) }
    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email, errorMessage = null) }
    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password, errorMessage = null) }
    fun onRoleChange(role: UserRole) = _uiState.update { it.copy(role = role, errorMessage = null) }

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return

        val inputError = validate(state)
        if (inputError != null) {
            _uiState.update { it.copy(errorMessage = inputError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.register(
                    name = state.name.trim(),
                    email = state.email.trim(),
                    password = state.password,
                    role = state.role!! // validate() checked it is not null
                )
                _uiState.update { it.copy(isLoading = false, isRegistered = true) }
            } catch (e: CancellationException) {
                throw e // the ViewModel is being cleared; do not show an error
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMessageFor(e)) }
            }
        }
    }

    /** Checks the input before we contact Firebase. Returns null when everything is fine. */
    @StringRes
    private fun validate(state: RegisterUiState): Int? = when {
        state.name.isBlank() -> R.string.error_name_empty
        !EMAIL_REGEX.matches(state.email.trim()) -> R.string.error_email_invalid
        state.password.length < MIN_PASSWORD_LENGTH -> R.string.error_password_too_short
        state.role == null -> R.string.error_role_missing
        else -> null
    }

    /** Turns a Firebase exception into a message the user understands. */
    @StringRes
    private fun errorMessageFor(e: Exception): Int = when (e) {
        is FirebaseAuthUserCollisionException -> R.string.error_email_in_use
        is FirebaseAuthWeakPasswordException -> R.string.error_password_too_short
        is FirebaseAuthInvalidCredentialsException -> R.string.error_email_invalid
        is FirebaseNetworkException -> R.string.error_no_internet
        else -> R.string.error_register_failed
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6 // Firebase's own minimum
        val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
