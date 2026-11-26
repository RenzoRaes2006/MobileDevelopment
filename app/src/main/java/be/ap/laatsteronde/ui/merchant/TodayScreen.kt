package be.ap.laatsteronde.ui.merchant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.ap.laatsteronde.R
import be.ap.laatsteronde.ui.components.PackagePhoto
import be.ap.laatsteronde.data.FoodPackage
import be.ap.laatsteronde.data.PackageDetails
import be.ap.laatsteronde.ui.auth.FieldError
import be.ap.laatsteronde.ui.components.DashedDivider
import be.ap.laatsteronde.ui.customer.ProfileMenuButton
import be.ap.laatsteronde.ui.theme.CardBorder
import be.ap.laatsteronde.ui.theme.OSMTheme

/**
 * "Vandaag" of the merchant (Figma "Vandaag"): the shop and the closing time, and the list
 * "Mijn pakketten" of today with the switch "Zichtbaar voor klanten". The green card with the
 * number of picked-up packages comes with the reservations. All logic is in [TodayViewModel].
 */
@Composable
fun TodayScreen(
    shopName: String?,
    viewModel: TodayViewModel,
    onNewPackage: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Read the packages when the screen opens and again when the merchant comes back, for example
    // after "Nieuw pakket", so a new package shows up in the list.
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose { }
    }

    TodayContent(
        shopName = shopName,
        state = state,
        onNewPackage = onNewPackage,
        onVisibleChange = viewModel::onVisibleChange,
        onRetry = viewModel::load,
        onLogout = onLogout,
        modifier = modifier
    )
}

/** The stateless part of the screen. */
@Composable
private fun TodayContent(
    shopName: String?,
    state: TodayUiState,
    onNewPackage: () -> Unit,
    onVisibleChange: (String, Boolean) -> Unit,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 108.dp) // room for the floating bar
        ) {
            val closingTime = state.closingTime
            val shopLine = when {
                shopName == null -> null
                closingTime == null -> shopName
                else -> stringResource(R.string.today_shop_line, shopName, closingTime)
            }
            if (shopLine != null) {
                Text(
                    text = shopLine,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 60.dp) // the profile button sits at the right
                )
            }
            Text(
                text = stringResource(R.string.today_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    lineHeight = 48.sp
                ),
                modifier = Modifier.padding(end = 60.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.today_my_packages),
                    style = MaterialTheme.typography.titleLarge
                )
                OutlinedButton(
                    onClick = onNewPackage,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.today_new), style = MaterialTheme.typography.labelLarge)
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.packages.isEmpty() && state.errorMessage != null -> {
                    FieldError(state.errorMessage)
                    OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(R.string.today_retry))
                    }
                }
                state.packages.isEmpty() -> Text(
                    text = stringResource(R.string.today_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.packages.forEach { item ->
                            PackageCard(item = item, onVisibleChange = { onVisibleChange(item.id, it) })
                        }
                    }
                    // A message about a switch that could not be saved; the list stays visible.
                    state.errorMessage?.let { FieldError(it, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
        }

        ProfileMenuButton(
            onLogout = onLogout,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 26.dp, end = 16.dp)
        )
    }
}

/**
 * One package of today (Figma "Pakketkaart"): the photo (H4), the title with the
 * pick-up window and the price, the number of packages, and the switch "Zichtbaar voor klanten".
 */
@Composable
private fun PackageCard(item: FoodPackage, onVisibleChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val details = item.details
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, CardBorder, shape)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)
        ) {
            PackagePhoto(
                photoBase64 = details.photoBase64,
                contentDescription = stringResource(R.string.package_photo_of, details.title),
                cornerRadius = 12.dp,
                iconSize = 22.dp,
                modifier = Modifier.size(52.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    text = details.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.package_window_and_price,
                        details.pickupFrom,
                        details.pickupTo,
                        formatCents(details.priceCents)
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            // Nothing is reserved yet (reservations come later), so all packages are still there.
            Text(
                text = stringResource(R.string.package_quantity, details.quantity, details.quantity),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        DashedDivider(modifier = Modifier.padding(horizontal = 14.dp))

        val visibleText = stringResource(if (item.visible) R.string.package_visible else R.string.package_offline)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = item.visible, role = Role.Switch, onValueChange = onVisibleChange)
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Text(text = visibleText, style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp))
            // The row above handles the tap, so screen readers find one switch and not two.
            Switch(checked = item.visible, onCheckedChange = null)
        }
    }
}

private val previewPackages = listOf(
    FoodPackage("1", "uid", PackageDetails("Brood en koffiekoeken", "", 400, 1200, 10, "2026-11-25", "18:00", "19:30"), true),
    FoodPackage("2", "uid", PackageDetails("Taart van de dag", "", 600, 1500, 4, "2026-11-25", "18:30", "19:30"), false)
)

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun TodayContentPreview() {
    OSMTheme(darkTheme = false) {
        TodayContent(
            shopName = "Bakkerij Mertens",
            state = TodayUiState(isLoading = false, packages = previewPackages),
            onNewPackage = {}, onVisibleChange = { _, _ -> }, onRetry = {}, onLogout = {}
        )
    }
}
