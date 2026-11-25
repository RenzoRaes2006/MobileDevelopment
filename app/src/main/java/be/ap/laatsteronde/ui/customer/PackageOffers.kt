package be.ap.laatsteronde.ui.customer

import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.data.FoodPackage
import be.ap.laatsteronde.data.Shop
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** A package with the shop it comes from and how far that shop is from the customer. */
data class PackageOffer(
    val pack: FoodPackage,
    val shop: Shop,
    val distanceMeters: Double
)

/**
 * The packages of one shop: what the map shows as one marker. [fromPriceCents] is the lowest price
 * ("Vanaf €4"), [remaining] the number of packages left in all, and the window runs from the
 * earliest start to the latest end.
 */
data class ShopOffers(
    val shop: Shop,
    val offers: List<PackageOffer>,
    val distanceMeters: Double
) {
    val fromPriceCents: Int get() = offers.minOf { it.pack.details.priceCents }
    val remaining: Int get() = offers.sumOf { it.pack.details.quantity }
    val pickupFrom: String get() = offers.minOf { it.pack.details.pickupFrom }
    val pickupTo: String get() = offers.maxOf { it.pack.details.pickupTo }
}

private const val EARTH_RADIUS_METERS = 6_371_000.0

/** The distance over the earth between two places in meters (haversine formula). */
fun distanceBetween(from: Coordinates, to: Coordinates): Double {
    val latitudeDelta = Math.toRadians(to.latitude - from.latitude)
    val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
    val a = sin(latitudeDelta / 2).pow(2) +
        cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) * sin(longitudeDelta / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
}

/** A distance as the customer reads it: "350 m" under a kilometer, "1,2 km" above (as in the Figma, with a comma). */
fun formatDistance(meters: Double): String {
    val roundedMeters = ((meters / 10).roundToInt() * 10).coerceAtLeast(10)
    if (roundedMeters < 1000) return "%d m".format(Locale.ROOT, roundedMeters)
    val kilometers = (meters / 100).roundToInt() / 10.0
    return if (kilometers < 10.0) {
        "%.1f km".format(Locale.ROOT, kilometers).replace('.', ',')
    } else {
        "%d km".format(Locale.ROOT, (meters / 1000).roundToInt())
    }
}

/** The discount in whole percent: 400 of 1200 is 67 (the "-67%" of the Figma). */
fun discountPercent(priceCents: Int, originalPriceCents: Int): Int {
    if (originalPriceCents <= 0) return 0
    return ((1.0 - priceCents.toDouble() / originalPriceCents) * 100).roundToInt().coerceIn(0, 100)
}

/**
 * Joins each package with its shop and sorts them by distance from [from], nearest first. A package
 * of a shop we do not know (for example a shop that is not complete) is left out, because the
 * customer could not see where it comes from. Packages at the same distance are sorted by the end of the pick-up window.
 */
fun buildOffers(packages: List<FoodPackage>, shops: List<Shop>, from: Coordinates): List<PackageOffer> {
    val shopsByOwner = shops.associateBy { it.ownerId }
    return packages
        .mapNotNull { pack ->
            val shop = shopsByOwner[pack.ownerId] ?: return@mapNotNull null
            PackageOffer(pack, shop, distanceBetween(from, shop.details.location))
        }
        .sortedWith(compareBy<PackageOffer> { it.distanceMeters }.thenBy { it.pack.details.pickupTo })
}

/** Groups the offers per shop, the nearest shop first. */
fun groupByShop(offers: List<PackageOffer>): List<ShopOffers> =
    offers.groupBy { it.shop.ownerId }
        .values
        .map { ShopOffers(shop = it.first().shop, offers = it, distanceMeters = it.first().distanceMeters) }
        .sortedBy { it.distanceMeters }
