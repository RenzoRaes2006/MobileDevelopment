package be.ap.laatsteronde.ui.customer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ap.laatsteronde.R
import be.ap.laatsteronde.ui.components.FloatingNavBar
import be.ap.laatsteronde.ui.components.NavBarItem
import be.ap.laatsteronde.ui.theme.OSMTheme
import be.ap.laatsteronde.ui.theme.ProfileButtonBorder

/** The four tabs of the customer. The order here is the order in the bar. */
private enum class CustomerTab(@StringRes val label: Int, @DrawableRes val icon: Int) {
    DISCOVER(R.string.customer_tab_discover, R.drawable.ic_nav_discover),
    MAP(R.string.customer_tab_map, R.drawable.ic_nav_map),
    ORDERS(R.string.customer_tab_orders, R.drawable.ic_nav_orders),
    FAVORITES(R.string.customer_tab_favorites, R.drawable.ic_nav_favorites)
}

/**
 * Home of a customer (Figma "FloatingNavBar"): the screen of the chosen tab, with a dark
 * navigation bar floating above it. The selected tab is remembered across rotation.
 */
@Composable
fun CustomerHomeScreen(
    onLogout: () -> Unit,
    locationViewModel: LocationViewModel = viewModel(
        factory = LocationViewModel.factory(LocalContext.current)
    ),
    discoverViewModel: DiscoverViewModel = viewModel(
        factory = DiscoverViewModel.factory(LocalContext.current)
    )
) {
    var selectedTab by rememberSaveable { mutableStateOf(CustomerTab.DISCOVER) }
    val locationState by locationViewModel.uiState.collectAsStateWithLifecycle()
    val municipality by discoverViewModel.municipality.collectAsStateWithLifecycle()

    // The municipality is shared by the Discover tab and the map tab, so it is looked up here.
    // Wait until we know where the customer is (or know that we will not get a position).
    LaunchedEffect(locationState.status, locationState.location) {
        if (locationState.status != LocationStatus.UNKNOWN && locationState.status != LocationStatus.LOADING) {
            discoverViewModel.onLocationChanged(locationState.location)
        }
    }

    // Opening the map looks up the position again, so the dot is where the customer is now.
    LaunchedEffect(selectedTab) {
        if (selectedTab == CustomerTab.MAP) locationViewModel.refresh()
    }

    // First the explanation of the location (Figma "3 - Locatie-uitleg"), then the tabs.
    LocationGate(viewModel = locationViewModel, status = locationState.status) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                CustomerTab.DISCOVER -> DiscoverScreen(
                    municipality = municipality,
                    // The municipality was tapped: go to the map, where a tap chooses another place.
                    onChangeLocation = {
                        locationViewModel.onChangeLocation()
                        selectedTab = CustomerTab.MAP
                    },
                    onLogout = onLogout
                )
                CustomerTab.MAP -> CustomerMapScreen(
                    location = locationState,
                    municipality = municipality,
                    onLocationChosen = locationViewModel::onLocationChosen,
                    onLocateClick = {
                        when (locationState.status) {
                            LocationStatus.FOUND, LocationStatus.UNAVAILABLE -> locationViewModel.refresh()
                            // No position shared yet: ask again, starting with the explanation.
                            LocationStatus.DENIED, LocationStatus.CHOSEN -> locationViewModel.onAskAgain()
                            LocationStatus.UNKNOWN, LocationStatus.LOADING -> Unit
                        }
                    }
                )
                CustomerTab.ORDERS -> PlaceholderScreen(
                    title = R.string.customer_tab_orders,
                    text = R.string.customer_orders_placeholder,
                    onLogout = onLogout
                )
                CustomerTab.FAVORITES -> PlaceholderScreen(
                    title = R.string.customer_tab_favorites,
                    text = R.string.customer_favorites_placeholder,
                    onLogout = onLogout
                )
            }

            FloatingNavBar(
                items = CustomerTab.entries.map { NavBarItem(it.label, it.icon) },
                selectedIndex = selectedTab.ordinal,
                onSelect = { selectedTab = CustomerTab.entries[it] },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    }
}

/** White round profile button (Figma "Profiel en uitloggen"); it opens a menu with Uitloggen. */
@Composable
internal fun ProfileMenuButton(onLogout: () -> Unit, modifier: Modifier = Modifier) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ProfileButtonBorder),
            modifier = Modifier
                .size(44.dp) // Figma "Profiel en uitloggen"
                .selectable(selected = false, role = Role.Button, onClick = { menuOpen = true })
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_profile),
                    contentDescription = stringResource(R.string.customer_profile_menu),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.customer_logout)) },
                onClick = {
                    menuOpen = false
                    onLogout()
                }
            )
        }
    }
}

/** Empty screen with a title and a text, until the real screen is built. */
@Composable
private fun PlaceholderScreen(@StringRes title: Int, @StringRes text: Int, onLogout: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 24.dp)
        )
        ProfileMenuButton(
            onLogout = onLogout,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        )
        Text(text = stringResource(text), modifier = Modifier.align(Alignment.Center))
    }
}

// The preview starts on the Discover tab, so the MapView is not created in the preview.
@Preview(showBackground = true)
@Composable
private fun CustomerHomeScreenPreview() {
    OSMTheme(darkTheme = false) {
        CustomerHomeScreen(onLogout = {})
    }
}
