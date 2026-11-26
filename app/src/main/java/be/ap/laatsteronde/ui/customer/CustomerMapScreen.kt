package be.ap.laatsteronde.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.ui.components.DashedDivider
import be.ap.laatsteronde.ui.map.MapPin
import be.ap.laatsteronde.ui.map.MarkerMap
import be.ap.laatsteronde.ui.theme.CardBorder
import be.ap.laatsteronde.ui.theme.Kraft
import be.ap.laatsteronde.ui.theme.TextBody
import be.ap.laatsteronde.ui.theme.isDarkTheme

/**
 * The map tab (Figma "5 - Kaart"): the map with the position of the customer, with above it a bar with
 * the municipality and the round "Naar mijn locatie" button. Every shop with packages is a label
 * with the name of the shop on the map (K4); a tap on it shows a card with the shop at the bottom. The customer can
 * only look at it: the button "Bekijk" of the Figma comes with reserving.
 *
 * Without a position (no permission, or not found) a tap on the map chooses a place.
 */
@Composable
fun CustomerMapScreen(
    location: LocationUiState,
    municipality: MunicipalityState,
    shops: List<ShopOffers>,
    onLocationChosen: (Coordinates) -> Unit,
    onLocateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = location.status
    // Each tap on the locate button moves the map back to the position, also after looking around.
    var recenterKey by rememberSaveable { mutableIntStateOf(0) }
    // The shop whose card is open. A shop that is no longer in the list (for example after a refresh) closes its card.
    var selectedShopId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedShop = shops.firstOrNull { it.shop.ownerId == selectedShopId }
    val canChoose = status == LocationStatus.DENIED ||
        status == LocationStatus.UNAVAILABLE ||
        status == LocationStatus.CHOSEN
    val showsPosition = status == LocationStatus.FOUND || status == LocationStatus.CHOSEN

    // The bar and the button stand above the map and the map stops above the floating navigation bar,
    // so no button covers a part of the map (feedback of the lector).
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            MunicipalityBar(
                title = (municipality as? MunicipalityState.Found)?.name
                    ?: stringResource(R.string.customer_map_title_unknown),
                subtitle = if (shops.isEmpty()) {
                    stringResource(
                        when (status) {
                            LocationStatus.FOUND -> R.string.customer_map_sub_found
                            LocationStatus.CHOSEN -> R.string.customer_map_sub_chosen
                            LocationStatus.DENIED, LocationStatus.UNAVAILABLE -> R.string.customer_map_sub_default
                            LocationStatus.UNKNOWN, LocationStatus.LOADING -> R.string.customer_map_sub_searching
                        }
                    )
                } else {
                    // As in the Figma: "4 winkels met pakketten".
                    pluralStringResource(R.plurals.customer_map_shops, shops.size, shops.size)
                },
                modifier = Modifier.weight(1f)
            )
            LocateButton(
                onClick = {
                    recenterKey++
                    onLocateClick()
                }
            )
        }

        if (canChoose) {
            HintChip(
                text = stringResource(R.string.location_choose_hint),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
        }

        val mapShape = RoundedCornerShape(20.dp)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(mapShape)
                .border(1.dp, CardBorder, mapShape)
        ) {
            MarkerMap(
                position = if (showsPosition) location.location else null,
                centerOn = location.location,
                markerIcon = R.drawable.marker_my_location,
                recenterKey = recenterKey,
                pins = shops.map { MapPin(it.shop.ownerId, it.shop.details.location, it.shop.details.name) },
                selectedPinId = selectedShop?.shop?.ownerId,
                onPinClick = { selectedShopId = it },
                onMapTap = { selectedShopId = null },
                onPositionPicked = if (canChoose) onLocationChosen else null,
                modifier = Modifier.fillMaxSize()
            )

            selectedShop?.let { shop ->
                ShopCard(
                    item = shop,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            }
        }

        // Room for the floating navigation bar, which stands on top of the tabs.
        Spacer(Modifier.height(NAV_BAR_SPACE))
    }
}

/** Height of the floating navigation bar (68 dp) with its 16 dp margin and 16 dp between the map and the bar. */
private val NAV_BAR_SPACE = 100.dp

/**
 * The card of a shop on the map (Figma "Winkelkaart"): the shop with the distance and the number of offers,
 * and under it every offer of that shop as a row: the title, the pick-up window, how many are left and
 * the price. A shop with several packages thus shows all of them to choose from. Looking only for now:
 * choosing an offer (the button "Bekijk" of the Figma) comes with reserving. Many offers scroll inside the card.
 */
@Composable
private fun ShopCard(item: ShopOffers, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, CardBorder, shape)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(Kraft, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_image),
                    contentDescription = stringResource(R.string.customer_package_photo),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    text = item.shop.details.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.customer_map_card_header,
                        formatDistance(item.distanceMeters),
                        pluralStringResource(R.plurals.customer_map_offers, item.offers.size, item.offers.size)
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        // The offers of this shop, each under a dashed line. With many offers this part scrolls.
        Column(
            modifier = Modifier
                .heightIn(max = 232.dp)
                .verticalScroll(rememberScrollState())
        ) {
            item.offers.forEach { offer ->
                DashedDivider(modifier = Modifier.padding(horizontal = 14.dp))
                OfferRow(offer)
            }
        }
    }
}

/** One offer in the card of a shop: title and pick-up window with how many are left on the left, the price on the right. */
@Composable
private fun OfferRow(offer: PackageOffer) {
    val details = offer.pack.details
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = details.title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.customer_map_offer_info, details.pickupFrom, details.pickupTo, details.quantity),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        PriceWithDiscount(details.priceCents, details.originalPriceCents, compact = true)
    }
}

/** White pill with a pin, the municipality and a small line below it (Figma "Zoekbalk"). */
@Composable
private fun MunicipalityBar(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = modifier.height(52.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pin_small),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Round white button "Naar mijn locatie" (Figma), 52 dp. */
@Composable
private fun LocateButton(onClick: () -> Unit) {
    val label = stringResource(R.string.customer_map_locate)
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = Modifier
            .size(52.dp)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_locate),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Small white label on the map, in the style of the hint "Tik of versleep de pin" of the Figma. */
@Composable
fun HintChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isDarkTheme()) MaterialTheme.colorScheme.onSurface else TextBody,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
