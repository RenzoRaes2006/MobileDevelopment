package be.ap.laatsteronde.ui.customer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.ap.laatsteronde.MapScreen
import be.ap.laatsteronde.R
import be.ap.laatsteronde.ui.theme.CarrotOrange
import be.ap.laatsteronde.ui.theme.DarkNavBarBackground
import be.ap.laatsteronde.ui.theme.NavBarBackground
import be.ap.laatsteronde.ui.theme.NavBarInactive
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
fun CustomerHomeScreen(onLogout: () -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(CustomerTab.DISCOVER) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {
            CustomerTab.DISCOVER -> PlaceholderScreen(
                title = R.string.customer_tab_discover,
                text = R.string.customer_discover_placeholder,
                onLogout = onLogout
            )
            CustomerTab.MAP -> {
                MapScreen()
                ProfileMenuButton(
                    onLogout = onLogout,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                )
            }
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
            selected = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

/** The dark pill with the four tabs. The chosen tab is an orange pill. */
@Composable
private fun FloatingNavBar(
    selected: CustomerTab,
    onSelect: (CustomerTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = if (isSystemInDarkTheme()) DarkNavBarBackground else NavBarBackground
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(34.dp))
            .background(barColor, RoundedCornerShape(34.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .selectableGroup()
    ) {
        CustomerTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val contentColor = if (isSelected) NavBarBackground else NavBarInactive
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(
                        color = if (isSelected) CarrotOrange else Color.Transparent,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(tab.icon),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(tab.label),
                        color = contentColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** White round profile button (Figma "Profiel en uitloggen"); it opens a menu with Uitloggen. */
@Composable
private fun ProfileMenuButton(onLogout: () -> Unit, modifier: Modifier = Modifier) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ProfileButtonBorder),
            modifier = Modifier
                .size(48.dp)
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
