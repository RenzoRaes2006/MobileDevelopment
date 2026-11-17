package be.ap.laatsteronde.ui.customer

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import be.ap.laatsteronde.MapScreen
import be.ap.laatsteronde.R

/** The four tabs of the customer. The order here is the order in the bar. */
private enum class CustomerTab(@StringRes val label: Int, val icon: ImageVector) {
    DISCOVER(R.string.customer_tab_discover, Icons.Filled.Search),
    MAP(R.string.customer_tab_map, Icons.Filled.Place),
    ORDERS(R.string.customer_tab_orders, Icons.Filled.ShoppingCart),
    FAVORITES(R.string.customer_tab_favorites, Icons.Filled.Favorite)
}

/**
 * Home of a customer: a top bar with a logout button and a bottom bar with four tabs.
 * The selected tab is remembered across rotation with rememberSaveable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(onLogout: () -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(CustomerTab.DISCOVER) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.customer_logout)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                CustomerTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedTab) {
                CustomerTab.DISCOVER -> PlaceholderScreen(R.string.customer_discover_placeholder)
                CustomerTab.MAP -> MapScreen()
                CustomerTab.ORDERS -> PlaceholderScreen(R.string.customer_orders_placeholder)
                CustomerTab.FAVORITES -> PlaceholderScreen(R.string.customer_favorites_placeholder)
            }
        }
    }
}

/** Empty screen with only a text, until the real screen is built. */
@Composable
private fun PlaceholderScreen(@StringRes text: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(text))
    }
}

// The preview starts on the Discover tab, so the MapView is not created in the preview.
@Preview(showBackground = true)
@Composable
private fun CustomerHomeScreenPreview() {
    CustomerHomeScreen(onLogout = {})
}
