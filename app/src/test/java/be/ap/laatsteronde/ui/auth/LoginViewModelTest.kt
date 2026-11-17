package be.ap.laatsteronde.ui.auth

import be.ap.laatsteronde.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginViewModelTest {

    @Test
    fun validateLogin_emptyEmail_returnsEmailError() {
        assertEquals(R.string.error_email_invalid, validateLogin("", "secret1"))
    }

    @Test
    fun validateLogin_emailWithoutDomain_returnsEmailError() {
        assertEquals(R.string.error_email_invalid, validateLogin("test@", "secret1"))
    }

    @Test
    fun validateLogin_emptyPassword_returnsPasswordError() {
        assertEquals(R.string.error_password_empty, validateLogin("test@example.com", ""))
    }

    @Test
    fun validateLogin_validInput_returnsNull() {
        assertNull(validateLogin("test@example.com", "secret1"))
    }

    @Test
    fun loginErrorMessageFor_invalidCredentials_returnsWrongCredentials() {
        val error = FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "wrong")
        assertEquals(R.string.error_wrong_credentials, loginErrorMessageFor(error))
    }

    @Test
    fun loginErrorMessageFor_networkError_returnsNoInternet() {
        assertEquals(R.string.error_no_internet, loginErrorMessageFor(FirebaseNetworkException("offline")))
    }

    @Test
    fun loginErrorMessageFor_otherError_returnsGenericMessage() {
        assertEquals(R.string.error_login_failed, loginErrorMessageFor(IllegalStateException("boom")))
    }
}
