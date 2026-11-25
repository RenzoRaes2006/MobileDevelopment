package be.ap.laatsteronde.ui.customer

import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.data.FoodPackage
import be.ap.laatsteronde.data.PackageDetails
import be.ap.laatsteronde.data.Shop
import be.ap.laatsteronde.data.ShopCategory
import be.ap.laatsteronde.data.ShopDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests the distance, the discount and the joining of packages with their shops. */
class PackageOffersTest {

    private val campus = Coordinates(latitude = 51.230167, longitude = 4.416129) // AP Hogeschool, Antwerp
    private val willebroek = Coordinates(latitude = 51.057, longitude = 4.373)

    @Test
    fun distanceBetween_samePlaceIsZero() {
        assertEquals(0.0, distanceBetween(campus, campus), 0.001)
    }

    @Test
    fun distanceBetween_oneHundredthOfADegreeOfLatitudeIsAboutOneAndAHalfKilometers() {
        // 0.01 degree of latitude is about 1112 meters everywhere on earth.
        val north = Coordinates(campus.latitude + 0.01, campus.longitude)
        assertEquals(1112.0, distanceBetween(campus, north), 5.0)
    }

    @Test
    fun distanceBetween_antwerpAndWillebroekIsAboutNineteenAndAHalfKilometers() {
        val distance = distanceBetween(campus, willebroek)
        assertTrue("distance was $distance", distance in 19_000.0..20_000.0)
    }

    @Test
    fun distanceBetween_isTheSameInBothDirections() {
        assertEquals(distanceBetween(campus, willebroek), distanceBetween(willebroek, campus), 0.001)
    }

    @Test
    fun formatDistance_underAKilometerIsInMetersRoundedToTen() {
        assertEquals("10 m", formatDistance(0.0)) // never "0 m": there is always some distance to walk
        assertEquals("10 m", formatDistance(4.0))
        assertEquals("350 m", formatDistance(347.0))
        assertEquals("990 m", formatDistance(994.0))
    }

    @Test
    fun formatDistance_aKilometerOrMoreIsInKilometersWithAComma() {
        assertEquals("1,0 km", formatDistance(995.0)) // not "1000 m"
        assertEquals("1,2 km", formatDistance(1_240.0))
        assertEquals("9,9 km", formatDistance(9_940.0))
    }

    @Test
    fun formatDistance_tenKilometersOrMoreIsInWholeKilometers() {
        assertEquals("10 km", formatDistance(9_960.0))
        assertEquals("19 km", formatDistance(19_480.0))
    }

    @Test
    fun formatPriceShort_dropsTheCentsOfWholeEuros() {
        assertEquals("€4", formatPriceShort(400))
        assertEquals("€10", formatPriceShort(1000))
        assertEquals("€5,50", formatPriceShort(550))
        assertEquals("€0,05", formatPriceShort(5))
        assertEquals("€12,01", formatPriceShort(1201))
    }

    @Test
    fun discountPercent_isTheCheaperPartInWholePercent() {
        assertEquals(67, discountPercent(priceCents = 400, originalPriceCents = 1200))
        assertEquals(17, discountPercent(priceCents = 1000, originalPriceCents = 1200))
        assertEquals(50, discountPercent(priceCents = 500, originalPriceCents = 1000))
    }

    @Test
    fun discountPercent_neverGoesBelowZeroOrAboveOneHundred() {
        assertEquals(0, discountPercent(priceCents = 1500, originalPriceCents = 1500))
        assertEquals(0, discountPercent(priceCents = 2000, originalPriceCents = 1500))
        assertEquals(0, discountPercent(priceCents = 400, originalPriceCents = 0))
        assertEquals(100, discountPercent(priceCents = 0, originalPriceCents = 1500))
    }

    private fun shop(owner: String, name: String, at: Coordinates) = Shop(
        ownerId = owner,
        details = ShopDetails(name, ShopCategory.BAKER, "Straat", "1", "2000", "Antwerpen", at)
    )

    private fun pack(
        id: String,
        owner: String,
        title: String = "Pakket",
        priceCents: Int = 400,
        quantity: Int = 3,
        from: String = "18:00",
        to: String = "19:30"
    ) = FoodPackage(
        id = id,
        ownerId = owner,
        details = PackageDetails(title, "", priceCents, 1200, quantity, "2026-11-25", from, to),
        visible = true
    )

    private val nearShop = shop("near", "Dichtbij", Coordinates(campus.latitude + 0.002, campus.longitude)) // about 220 m
    private val farShop = shop("far", "Veraf", willebroek)

    @Test
    fun buildOffers_joinsEachPackageWithItsShop_nearestFirst() {
        val packages = listOf(pack("1", "far"), pack("2", "near"))

        val offers = buildOffers(packages, listOf(farShop, nearShop), from = campus)

        assertEquals(listOf("2", "1"), offers.map { it.pack.id })
        assertEquals(listOf("Dichtbij", "Veraf"), offers.map { it.shop.details.name })
        assertEquals(222.0, offers[0].distanceMeters, 5.0)
    }

    @Test
    fun buildOffers_leavesOutAPackageOfAnUnknownShop() {
        val packages = listOf(pack("1", "near"), pack("2", "nobody"))

        val offers = buildOffers(packages, listOf(nearShop), from = campus)

        assertEquals(listOf("1"), offers.map { it.pack.id })
    }

    @Test
    fun buildOffers_withoutPackagesIsEmpty() {
        assertTrue(buildOffers(emptyList(), listOf(nearShop), campus).isEmpty())
    }

    @Test
    fun buildOffers_sameShopSortsByTheEndOfThePickUpWindow() {
        val packages = listOf(pack("late", "near", to = "20:00"), pack("early", "near", to = "18:30"))

        val offers = buildOffers(packages, listOf(nearShop), from = campus)

        assertEquals(listOf("early", "late"), offers.map { it.pack.id })
    }

    @Test
    fun buildOffers_theDistanceFollowsTheLocationOfTheCustomer() {
        val packages = listOf(pack("1", "near"), pack("2", "far"))

        // From Willebroek the far shop is the near one.
        val offers = buildOffers(packages, listOf(farShop, nearShop), from = willebroek)

        assertEquals(listOf("2", "1"), offers.map { it.pack.id })
        assertEquals(0.0, offers[0].distanceMeters, 0.001)
    }

    @Test
    fun groupByShop_makesOneEntryPerShop_nearestFirst() {
        val packages = listOf(
            pack("1", "far", priceCents = 300),
            pack("2", "near", priceCents = 600, quantity = 2, from = "18:00", to = "19:00"),
            pack("3", "near", priceCents = 400, quantity = 5, from = "17:30", to = "19:30")
        )

        val groups = groupByShop(buildOffers(packages, listOf(farShop, nearShop), from = campus))

        assertEquals(listOf("Dichtbij", "Veraf"), groups.map { it.shop.details.name })
        val near = groups[0]
        assertEquals(2, near.offers.size)
        assertEquals(400, near.fromPriceCents) // "Vanaf €4"
        assertEquals(7, near.remaining)
        assertEquals("17:30", near.pickupFrom)
        assertEquals("19:30", near.pickupTo)
        assertEquals(300, groups[1].fromPriceCents)
    }

    @Test
    fun groupByShop_withoutOffersIsEmpty() {
        assertTrue(groupByShop(emptyList()).isEmpty())
    }
}
