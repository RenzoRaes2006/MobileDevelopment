package be.ap.laatsteronde.ui.customer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import be.ap.laatsteronde.R

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

/**
 * Asks for the location permission, with an explanation first. It shows nothing when the
 * permission is already granted. The answer goes to [viewModel].
 */
@Composable
fun LocationPermissionHandler(viewModel: LocationViewModel, status: LocationStatus) {
    val context = LocalContext.current
    // True after "Toestaan", so the explanation does not come back while the system dialog is open.
    var requested by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Approximate OR exact is enough for us.
        viewModel.onPermissionResult(granted = results.values.any { it })
    }

    val alreadyGranted = LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(status, alreadyGranted) {
        if (status == LocationStatus.UNKNOWN && alreadyGranted) {
            viewModel.onPermissionResult(granted = true)
        }
    }

    if (status == LocationStatus.UNKNOWN && !alreadyGranted && !requested) {
        AlertDialog(
            onDismissRequest = { viewModel.onPermissionResult(granted = false) },
            title = { Text(stringResource(R.string.location_dialog_title)) },
            text = { Text(stringResource(R.string.location_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    requested = true
                    launcher.launch(LOCATION_PERMISSIONS)
                }) {
                    Text(stringResource(R.string.location_dialog_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onPermissionResult(granted = false) }) {
                    Text(stringResource(R.string.location_dialog_not_now))
                }
            }
        )
    }
}
