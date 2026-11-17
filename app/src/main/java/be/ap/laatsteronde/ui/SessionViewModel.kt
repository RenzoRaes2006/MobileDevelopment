package be.ap.laatsteronde.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.ap.laatsteronde.data.UserRepository
import be.ap.laatsteronde.data.UserRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Knows who is signed in. At startup it decides which screen to show first,
 * and it signs the user out, so composables never touch Firebase directly.
 */
class SessionViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    // null = still deciding; otherwise the route to open first.
    private val _startRoute = MutableStateFlow<String?>(null)
    val startRoute: StateFlow<String?> = _startRoute.asStateFlow()

    init {
        viewModelScope.launch { _startRoute.value = findStartRoute() }
    }

    fun logout() = repository.logout()

    private suspend fun findStartRoute(): String {
        if (!repository.isLoggedIn()) return Routes.LOGIN
        return try {
            repository.currentRole()?.let(::homeRouteFor) ?: signOutToLogin()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // No profile or no internet: the user can sign in again.
            signOutToLogin()
        }
    }

    // An account without a readable profile cannot be used, so start clean on the login screen.
    private fun signOutToLogin(): String {
        repository.logout()
        return Routes.LOGIN
    }
}

fun homeRouteFor(role: UserRole): String = when (role) {
    UserRole.CUSTOMER -> Routes.CUSTOMER_HOME
    UserRole.MERCHANT -> Routes.MERCHANT_HOME
}
