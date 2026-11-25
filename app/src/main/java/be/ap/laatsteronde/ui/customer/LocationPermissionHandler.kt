package be.ap.laatsteronde.ui.customer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

/**
 * Shows the explanation screen as long as we have not asked the location yet, and the [content]
 * after that. When the permission is already granted the explanation is skipped. The answer
 * goes to [viewModel].
 */
@Composable
fun LocationGate(
    viewModel: LocationViewModel,
    status: LocationStatus,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // True after "Locatie toestaan", so the explanation does not flash back while the system dialog is open.
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

    // The customer may have moved while the app was in the background.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(status, alreadyGranted) {
        if (status == LocationStatus.UNKNOWN && alreadyGranted) {
            viewModel.onPermissionResult(granted = true)
        }
        // Back at "unknown" (after "Naar mijn locatie"): the explanation is shown again.
        if (status == LocationStatus.UNKNOWN) requested = false
    }

    if (status == LocationStatus.UNKNOWN && !alreadyGranted && !requested) {
        LocationExplanationScreen(
            onAllow = {
                requested = true
                launcher.launch(LOCATION_PERMISSIONS)
            },
            onChooseOwnLocation = { viewModel.onPermissionResult(granted = false) }
        )
    } else {
        content()
    }
}
