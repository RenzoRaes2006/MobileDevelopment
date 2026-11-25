package be.ap.laatsteronde.ui.merchant

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ap.laatsteronde.R
import be.ap.laatsteronde.ui.components.FloatingNavBar
import be.ap.laatsteronde.ui.components.NavBarItem
import be.ap.laatsteronde.ui.customer.ProfileMenuButton

/** The two tabs of the merchant. The order here is the order in the bar. */
private enum class MerchantTab(@StringRes val label: Int, @DrawableRes val icon: Int) {
    TODAY(R.string.merchant_tab_today, R.drawable.ic_nav_orders),
    MY_SHOP(R.string.merchant_tab_my_shop, R.drawable.ic_nav_shop)
}

/**
 * Home of a merchant (Figma "FloatingNavBar · handelaar"). A merchant without a shop starts on
 * "Mijn winkel" to fill it in first, as in the flow of the Figma; otherwise on "Vandaag".
 */
@Composable
fun MerchantHomeScreen(
    onLogout: () -> Unit,
    shopViewModel: ShopViewModel = viewModel(factory = ShopViewModel.factory(LocalContext.current))
) {
    val state by shopViewModel.uiState.collectAsStateWithLifecycle()
    // null until the shop is loaded; fixed after that, so saving the first shop does not switch the tab.
    var selectedTab by rememberSaveable { mutableStateOf<MerchantTab?>(null) }

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && selectedTab == null) {
            selectedTab = if (state.shop == null) MerchantTab.MY_SHOP else MerchantTab.TODAY
        }
    }

    val tab = selectedTab
    if (tab == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (tab) {
            MerchantTab.TODAY -> TodayPlaceholder(onLogout = onLogout)
            MerchantTab.MY_SHOP -> MyShopScreen(viewModel = shopViewModel, onLogout = onLogout)
        }

        FloatingNavBar(
            items = MerchantTab.entries.map { NavBarItem(it.label, it.icon) },
            selectedIndex = tab.ordinal,
            onSelect = { selectedTab = MerchantTab.entries[it] },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(270.dp) // width in the Figma: two tabs
                .padding(bottom = 16.dp)
        )
    }
}

/** "Vandaag" is built in the next step (H3, H4); until then a title and a text. */
@Composable
private fun TodayPlaceholder(onLogout: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.merchant_tab_today),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 24.dp)
        )
        ProfileMenuButton(
            onLogout = onLogout,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 26.dp, end = 16.dp)
        )
        Text(
            text = stringResource(R.string.merchant_today_placeholder),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
