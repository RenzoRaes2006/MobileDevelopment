package be.ap.laatsteronde.ui.auth

import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.UserRepository
import be.ap.laatsteronde.data.UserRole
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Tests "Wachtwoord vergeten?" in [LoginViewModel] with a fake repository instead of Firebase. */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordTest {

    /** Records the reset requests and fails with [resetError] when that is set. */
    private class FakeRepository(var resetError: Exception? = null) : UserRepository {
        val resetRequests = mutableListOf<String>()

        override suspend fun sendPasswordReset(email: String) {
            resetRequests += email
            resetError?.let { throw it }
        }

        override suspend fun register(name: String, email: String, password: String, role: UserRole) = Unit
        override suspend fun login(email: String, password: String) = Unit
        override fun logout() = Unit
        override fun isLoggedIn() = false
        override suspend fun currentRole(): UserRole? = null
    }

    @Before
    fun setUp() {
        // viewModelScope runs on Dispatchers.Main, which does not exist in a plain JVM test.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun forgotPassword_invalidEmail_showsEmailErrorAndSendsNothing() {
        val repository = FakeRepository()
        val viewModel = LoginViewModel(repository)
        viewModel.onEmailChange("geen-email")

        viewModel.onForgotPassword()

        assertEquals(R.string.error_email_invalid, viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.infoMessage)
        assertEquals(emptyList<String>(), repository.resetRequests)
    }

    @Test
    fun forgotPassword_validEmail_sendsResetAndShowsInfo() {
        val repository = FakeRepository()
        val viewModel = LoginViewModel(repository)
        viewModel.onEmailChange("  naam@example.com ")

        viewModel.onForgotPassword()

        assertEquals(listOf("naam@example.com"), repository.resetRequests) // trimmed
        assertEquals(R.string.info_reset_sent, viewModel.uiState.value.infoMessage)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun forgotPassword_noInternet_showsNoInternetError() {
        val repository = FakeRepository(resetError = FirebaseNetworkException("offline"))
        val viewModel = LoginViewModel(repository)
        viewModel.onEmailChange("naam@example.com")

        viewModel.onForgotPassword()

        assertEquals(R.string.error_no_internet, viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.infoMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun forgotPassword_editingEmailClearsInfoMessage() {
        val viewModel = LoginViewModel(FakeRepository())
        viewModel.onEmailChange("naam@example.com")
        viewModel.onForgotPassword()

        viewModel.onEmailChange("andere@example.com")

        assertNull(viewModel.uiState.value.infoMessage)
    }
}
