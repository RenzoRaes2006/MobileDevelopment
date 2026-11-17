package be.ap.laatsteronde.ui.auth

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.UserRole
import be.ap.laatsteronde.ui.theme.OSMTheme

/**
 * Register screen (A1), following the Figma frame "2 - Registreren".
 * Contains only UI; all logic is in [RegisterViewModel].
 */
@Composable
fun RegisterScreen(
    onRegistered: (UserRole) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate away once, as soon as registration succeeded. The chosen role decides where we go.
    LaunchedEffect(state.isRegistered) {
        if (state.isRegistered) state.role?.let(onRegistered)
    }

    RegisterContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onRoleChange = viewModel::onRoleChange,
        onRegisterClick = viewModel::register,
        onBack = onBack,
        modifier = modifier
    )
}

/** The stateless part of the screen, so it can be previewed without Firebase. */
@Composable
private fun RegisterContent(
    state: RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onRegisterClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Each error message is shown under the field it is about; the rest is shown above the button.
    val error = state.errorMessage
    val nameError = error == R.string.error_name_empty
    val emailError = error == R.string.error_email_invalid || error == R.string.error_email_in_use
    val passwordError = error == R.string.error_password_too_short
    val roleError = error == R.string.error_role_missing
    val generalError = error.takeIf { !nameError && !emailError && !passwordError && !roleError }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        // The design puts the arrow 12 dp from the screen edge; the page padding is 24 dp.
        IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.register_back),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        // The form scrolls (landscape, big fonts); the button below stays in view.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.register_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = stringResource(R.string.register_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.label_role),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.selectableGroup()
                ) {
                    RoleCard(
                        role = UserRole.CUSTOMER,
                        title = R.string.role_customer,
                        description = R.string.role_customer_description,
                        icon = R.drawable.ic_role_customer,
                        selected = state.role == UserRole.CUSTOMER,
                        enabled = !state.isLoading,
                        onClick = onRoleChange,
                        modifier = Modifier.weight(1f)
                    )
                    RoleCard(
                        role = UserRole.MERCHANT,
                        title = R.string.role_merchant,
                        description = R.string.role_merchant_description,
                        icon = R.drawable.ic_role_merchant,
                        selected = state.role == UserRole.MERCHANT,
                        enabled = !state.isLoading,
                        onClick = onRoleChange,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (roleError) FieldError(R.string.error_role_missing)
            }

            Column {
                LabeledTextField(
                    label = R.string.label_name,
                    value = state.name,
                    onValueChange = onNameChange,
                    enabled = !state.isLoading,
                    isError = nameError,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
                if (nameError) FieldError(R.string.error_name_empty)
            }

            Column {
                LabeledTextField(
                    label = R.string.label_email,
                    value = state.email,
                    onValueChange = onEmailChange,
                    enabled = !state.isLoading,
                    isError = emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )
                if (emailError) error?.let { FieldError(it) }
            }

            Column {
                LabeledTextField(
                    label = R.string.label_password,
                    value = state.password,
                    onValueChange = onPasswordChange,
                    enabled = !state.isLoading,
                    isError = passwordError,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )
                if (passwordError) {
                    FieldError(R.string.error_password_too_short)
                } else {
                    Text(
                        text = stringResource(R.string.register_password_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        generalError?.let { FieldError(it, modifier = Modifier.padding(bottom = 8.dp)) }

        Button(
            onClick = onRegisterClick,
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .heightIn(min = 52.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.button_register), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** One of the two role cards. Works as a radio button for screen readers. */
@Composable
private fun RoleCard(
    role: UserRole,
    @StringRes title: Int,
    @StringRes description: Int,
    @DrawableRes icon: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val borderColor = if (selected) colors.primary else colors.outline
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .heightIn(min = 160.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = { onClick(role) }
            )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Chosen card: green box with white icon. Other card: neutral box.
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) colors.primary else colors.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = if (selected) colors.onPrimary else colors.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = stringResource(description),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            if (selected) {
                Text(
                    text = stringResource(R.string.role_selected),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterContentPreview() {
    OSMTheme(darkTheme = false) {
        RegisterContent(
            state = RegisterUiState(name = "Marwan Bouchdig", role = UserRole.CUSTOMER),
            onNameChange = {}, onEmailChange = {}, onPasswordChange = {},
            onRoleChange = {}, onRegisterClick = {}, onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterContentErrorPreview() {
    OSMTheme(darkTheme = false) {
        RegisterContent(
            state = RegisterUiState(
                name = "Marwan Bouchdig",
                email = "renzo@student.ap.be",
                role = UserRole.MERCHANT,
                errorMessage = R.string.error_email_in_use
            ),
            onNameChange = {}, onEmailChange = {}, onPasswordChange = {},
            onRoleChange = {}, onRegisterClick = {}, onBack = {}
        )
    }
}
