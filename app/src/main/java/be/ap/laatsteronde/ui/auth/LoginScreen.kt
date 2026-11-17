package be.ap.laatsteronde.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.UserRole
import be.ap.laatsteronde.ui.theme.DarkCarrot
import be.ap.laatsteronde.ui.theme.DarkPrimary
import be.ap.laatsteronde.ui.theme.OSMTheme
import be.ap.laatsteronde.ui.theme.isDarkTheme

private val MAX_LOGO_SIZE = 220.dp // size in the Figma
private val MIN_LOGO_SIZE = 72.dp
private const val LOGO_HEIGHT_SHARE = 0.28f // of the screen height
private const val DARK_SYMBOL_SHARE = 0.7f // of the logo size; the name below takes the rest

/**
 * Login screen (A2), following the Figma frame "1 - Login".
 * Contains only UI; all logic is in [LoginViewModel].
 */
@Composable
fun LoginScreen(
    onLoggedIn: (UserRole) -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate away once, as soon as the login succeeded.
    LaunchedEffect(state.loggedInRole) {
        state.loggedInRole?.let(onLoggedIn)
    }

    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::login,
        onForgotPasswordClick = viewModel::onForgotPassword,
        onRegisterClick = onRegisterClick,
        modifier = modifier
    )
}

/** The stateless part of the screen, so it can be previewed without Firebase. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Show the message under the field it is about; every other error belongs to the password field.
    val emailHasError = state.errorMessage == R.string.error_email_invalid

    // On a low screen (landscape) a big logo would push the fields out of view, so it shrinks.
    val logoSize = (LocalConfiguration.current.screenHeightDp * LOGO_HEIGHT_SHARE).dp
        .coerceIn(MIN_LOGO_SIZE, MAX_LOGO_SIZE)
    val keyboardOpen = WindowInsets.isImeVisible

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The form scrolls (landscape, big fonts); the register link stays at the bottom like in the design.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // With the keyboard open the logo makes room, so the fields and the button stay in view.
            AnimatedVisibility(visible = !keyboardOpen) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoginLogo(size = logoSize)
                    Text(
                        text = stringResource(R.string.login_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )
                }
            }

            LabeledTextField(
                label = R.string.label_email,
                value = state.email,
                onValueChange = onEmailChange,
                enabled = !state.isLoading,
                isError = emailHasError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )
            if (emailHasError) FieldError(R.string.error_email_invalid)

            Spacer(Modifier.size(16.dp))
            LabeledTextField(
                label = R.string.label_password,
                value = state.password,
                onValueChange = onPasswordChange,
                enabled = !state.isLoading,
                isError = state.errorMessage != null && !emailHasError,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onLoginClick() })
            )
            if (!emailHasError) {
                state.errorMessage?.let { FieldError(it) }
            }
            state.infoMessage?.let { message ->
                Text(
                    text = stringResource(message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            UnderlinedLink(
                text = R.string.login_forgot_password,
                onClick = onForgotPasswordClick,
                enabled = !state.isLoading,
                modifier = Modifier.align(Alignment.End)
            )

            Button(
                onClick = onLoginClick,
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(min = 52.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.button_login), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        AnimatedVisibility(visible = !keyboardOpen) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // With a big font this text wraps; the link keeps its own line instead of breaking mid-word.
                Text(
                    text = stringResource(R.string.login_no_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false)
                )
                UnderlinedLink(
                    text = R.string.login_register_link,
                    onClick = onRegisterClick,
                    enabled = !state.isLoading
                )
            }
        }
    }
}

/**
 * The Figma logo. Its name is dark green and unreadable on a dark page, so there the
 * symbol and the name are drawn separately, the name in the house colors.
 */
@Composable
private fun LoginLogo(size: Dp) {
    val description = stringResource(R.string.login_logo_description)
    if (!isDarkTheme()) {
        Image(
            painter = painterResource(R.drawable.logo_full),
            contentDescription = description,
            modifier = Modifier.size(size)
        )
        return
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description }
    ) {
        Image(
            painter = painterResource(R.drawable.logo_symbol),
            contentDescription = null,
            modifier = Modifier.size(size * DARK_SYMBOL_SHARE)
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = DarkPrimary)) { append(stringResource(R.string.logo_word_first)) }
                append(" ")
                withStyle(SpanStyle(color = DarkCarrot)) { append(stringResource(R.string.logo_word_second)) }
            },
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

/** A green, underlined link as in the Figma, with a touch target of at least 48 dp. */
@Composable
private fun UnderlinedLink(
    text: Int,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp)) {
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.titleSmall,
            textDecoration = TextDecoration.Underline,
            softWrap = false
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentPreview() {
    OSMTheme(darkTheme = false) {
        LoginContent(LoginUiState(email = "naam@example.com"), {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentErrorPreview() {
    OSMTheme(darkTheme = false) {
        LoginContent(
            LoginUiState(
                email = "naam@example.com",
                password = "123456",
                errorMessage = R.string.error_wrong_credentials
            ),
            {}, {}, {}, {}, {}
        )
    }
}
