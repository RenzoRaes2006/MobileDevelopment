package be.ap.laatsteronde.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.data.FoodPackage
import be.ap.laatsteronde.data.PackageDetails
import be.ap.laatsteronde.data.Shop
import be.ap.laatsteronde.data.ShopCategory
import be.ap.laatsteronde.data.ShopDetails
import be.ap.laatsteronde.ui.components.DashedDivider
import be.ap.laatsteronde.ui.merchant.formatCents
import be.ap.laatsteronde.ui.theme.CardBorder
import be.ap.laatsteronde.ui.theme.Kraft
import be.ap.laatsteronde.ui.theme.OSMTheme

/**
 * One package in the list "Dichtbij jou" (Figma "Pakketkaart"): the shop with the distance, the
 * title, the pick-up window, how many are left and the price with the discount. It is only to look
 * at: there is no heart and no tap yet (reserving comes later).
 */
@Composable
fun PackageOfferCard(offer: PackageOffer, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    val details = offer.pack.details
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, CardBorder, shape)
    ) {
        Row(modifier = Modifier.padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .background(Kraft, RoundedCornerShape(14.dp))
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_image),
                    contentDescription = stringResource(R.string.customer_package_photo),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = stringResource(
                        R.string.customer_package_shop_line,
                        offer.shop.details.name,
                        offer.shop.details.municipality,
                        formatDistance(offer.distanceMeters)
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = details.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = stringResource(R.string.customer_package_window, details.pickupFrom, details.pickupTo),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        DashedDivider(modifier = Modifier.padding(horizontal = 12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.customer_package_left, details.quantity),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val percent = discountPercent(details.priceCents, details.originalPriceCents)
                if (percent > 0) {
                    Text(
                        text = stringResource(R.string.customer_package_discount, percent),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = formatCents(details.originalPriceCents),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Normal,
                        textDecoration = TextDecoration.LineThrough
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatCents(details.priceCents),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun PackageOfferCardPreview() {
    val shop = Shop(
        "uid",
        ShopDetails("Bakkerij Mertens", ShopCategory.BAKER, "Straat", "1", "2000", "Antwerpen", Coordinates(51.23, 4.41))
    )
    val pack = FoodPackage(
        "1", "uid",
        PackageDetails("Brood en koffiekoeken", "", 400, 1200, 3, "2026-11-25", "18:00", "19:30"),
        true
    )
    OSMTheme(darkTheme = false) {
        PackageOfferCard(PackageOffer(pack, shop, 420.0), modifier = Modifier.padding(16.dp))
    }
}
