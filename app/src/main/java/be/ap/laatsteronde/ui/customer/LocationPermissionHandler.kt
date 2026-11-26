package be.ap.laatsteronde.ui.customer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

// Fine and coarse together: since Android 12 the customer then chooses "Precies" or "Bij benadering".
private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private fun isGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/** Opens the page of this app in the settings of the phone, where the customer can change the location permission. */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    context.startActivity(intent)
}

/**
 * Shows the explanation screen as long as we have not asked the location yet, and the [content]
 * after that. When the permission is already granted the explanation is skipped. The answer
 * goes to [viewModel]: precise or only approximate, denied, or denied for good.
 */
@Composable
fun LocationGate(
    viewModel: LocationViewModel,
    state: LocationUiState,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    // True after "Locatie toestaan", so the explanation does not flash back while the system dialog is open.
    var requested by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val precise = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        // Approximate OR precise is enough to show packages nearby.
        val granted = precise || results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        // Right after a "no", Android only wants an explanation when it will still ask again next time.
        // False here means "denied for good": the question is no longer shown.
        val canAskAgain = granted || activity == null ||
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        viewModel.onPermissionResult(granted = granted, precise = precise, canAskAgain = canAskAgain)
    }

    val alreadyGranted = LOCATION_PERMISSIONS.any { isGranted(context, it) }

    // The customer may have moved while the app was in the background, or changed the permission in the settings.
    LifecycleResumeEffect(viewModel) {
        viewModel.onPermissionChecked(
            granted = LOCATION_PERMISSIONS.any { isGranted(context, it) },
            precise = isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        )
        onPauseOrDispose { }
    }

    LaunchedEffect(state.status, alreadyGranted) {
        if (state.status == LocationStatus.UNKNOWN && alreadyGranted) {
            viewModel.onPermissionResult(
                granted = true,
                precise = isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
        // Back at "unknown" (after "Naar mijn locatie"): the explanation is shown again.
        if (state.status == LocationStatus.UNKNOWN) requested = false
    }

    if (state.status == LocationStatus.UNKNOWN && !alreadyGranted && !requested) {
        LocationExplanationScreen(
            blockedInSettings = state.blockedInSettings,
            onAllow = {
                if (state.blockedInSettings) {
                    // Android no longer shows the question: only the settings can give the permission.
                    openAppSettings(context)
                } else {
                    requested = true
                    launcher.launch(LOCATION_PERMISSIONS)
                }
            },
            onChooseOwnLocation = {
                viewModel.onPermissionResult(granted = false, canAskAgain = !state.blockedInSettings)
            }
        )
    } else {
        content()
    }
}
