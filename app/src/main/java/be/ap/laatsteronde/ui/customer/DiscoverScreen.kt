package be.ap.laatsteronde.ui.customer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ap.laatsteronde.R

/**
 * The Discover tab. For now only the title "Pakketten in de buurt van {gemeente}"; the list of
 * packages comes in a later week.
 */
@Composable
fun DiscoverScreen(
    location: LocationUiState,
    onLogout: () -> Unit,
    viewModel: DiscoverViewModel = viewModel(factory = DiscoverViewModel.factory(LocalContext.current))
) {
    val municipality by viewModel.municipality.collectAsStateWithLifecycle()

    // Wait until we know where the customer is (or know that we will not get a position).
    // With a denied permission the default location is used, just like on the map.
    LaunchedEffect(location.status, location.location) {
        if (location.status != LocationStatus.UNKNOWN && location.status != LocationStatus.LOADING) {
            viewModel.onLocationChanged(location.location)
        }
    }

    val title = when (val state = municipality) {
        is MunicipalityState.Found -> stringResource(R.string.customer_discover_title_near, state.name)
        MunicipalityState.Loading, MunicipalityState.Unknown -> stringResource(R.string.customer_discover_title)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .align(Alignment.TopStart)
                // The end padding keeps a long name away from the profile button.
                .padding(start = 16.dp, top = 24.dp, end = 80.dp)
        )
        ProfileMenuButton(
            onLogout = onLogout,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        )
        Text(
            text = stringResource(R.string.customer_discover_placeholder),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
