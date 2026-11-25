package be.ap.laatsteronde.ui.customer

import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.data.FoodPackage
import be.ap.laatsteronde.data.PackageDetails
import be.ap.laatsteronde.data.PackageRepository
import be.ap.laatsteronde.data.Shop
import be.ap.laatsteronde.data.ShopCategory
import be.ap.laatsteronde.data.ShopDetails
import be.ap.laatsteronde.data.ShopRepository
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Tests [CustomerPackagesViewModel] with fakes instead of Firebase. */
@OptIn(ExperimentalCoroutinesApi::class)
class CustomerPackagesViewModelTest {

    private class FakePackageRepository(var packages: List<FoodPackage> = emptyList()) : PackageRepository {
        var loadError: Exception? = null
        val askedDates = mutableListOf<String>()

        override suspend fun myPackagesOn(date: String): List<FoodPackage> = emptyList()

        override suspend fun visiblePackagesOn(date: String): List<FoodPackage> {
            askedDates += date
            loadError?.let { throw it }
            return packages
        }

        override suspend fun createPackage(details: PackageDetails) = Unit

        override suspend fun setVisible(packageId: String, visible: Boolean) = Unit
    }

    private class FakeShopRepository(var shops: List<Shop> = emptyList()) : ShopRepository {
        var loadError: Exception? = null

        override suspend fun myShop(): Shop? = null

        override suspend fun allShops(): List<Shop> {
            loadError?.let { throw it }
            return shops
        }

        override suspend fun saveMyShop(details: ShopDetails) = Unit
    }

    private val campus = LocationViewModel.DEFAULT_LOCATION // where the customer is until we know better
    private val willebroek = Coordinates(latitude = 51.057, longitude = 4.373)

    private fun shop(owner: String, name: String, at: Coordinates) = Shop(
        ownerId = owner,
        details = ShopDetails(name, ShopCategory.BAKER, "Straat", "1", "2000", "Antwerpen", at)
    )

    private fun pack(id: String, owner: String, priceCents: Int = 400) = FoodPackage(
        id = id,
        ownerId = owner,
        details = PackageDetails("Pakket $id", "", priceCents, 1200, 3, "2026-11-25", "18:00", "19:30"),
        visible = true
    )

    private val nearShop = shop("near", "Dichtbij", Coordinates(campus.latitude + 0.002, campus.longitude))
    private val farShop = shop("far", "Veraf", willebroek)

    private lateinit var packages: FakePackageRepository
    private lateinit var shops: FakeShopRepository

    @Before
    fun setUp() {
        // viewModelScope runs on Dispatchers.Main, which does not exist in a plain JVM test.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        packages = FakePackageRepository(listOf(pack("1", "far"), pack("2", "near")))
        shops = FakeShopRepository(listOf(farShop, nearShop))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = CustomerPackagesViewModel(packages, shops, today = { "2026-11-25" })

    @Test
    fun beforeTheFirstLoad_itIsLoading() {
        val state = viewModel().uiState.value

        assertTrue(state.isLoading)
        assertTrue(state.offers.isEmpty())
    }

    @Test
    fun load_showsThePackagesWithTheirShop_nearestFirst() {
        val viewModel = viewModel()

        viewModel.load()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(listOf("2", "1"), state.offers.map { it.pack.id })
        assertEquals(listOf("Dichtbij", "Veraf"), state.offers.map { it.shop.details.name })
        assertEquals(listOf("Dichtbij", "Veraf"), state.shops.map { it.shop.details.name })
    }

    @Test
    fun load_asksOnlyForTheDateOfToday() {
        val viewModel = viewModel()

        viewModel.load()

        assertEquals(listOf("2026-11-25"), packages.askedDates)
    }

    @Test
    fun load_withoutPackages_givesAnEmptyList() {
        packages.packages = emptyList()
        val viewModel = viewModel()

        viewModel.load()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.offers.isEmpty())
        assertTrue(viewModel.uiState.value.shops.isEmpty())
    }

    @Test
    fun aPackageOfAShopWeDoNotKnow_isNotShown() {
        packages.packages = listOf(pack("1", "near"), pack("2", "nobody"))
        val viewModel = viewModel()

        viewModel.load()

        assertEquals(listOf("1"), viewModel.uiState.value.offers.map { it.pack.id })
    }

    @Test
    fun movingToAnotherPlace_sortsAgainWithoutReading() {
        val viewModel = viewModel()
        viewModel.load()
        assertEquals("Dichtbij", viewModel.uiState.value.offers.first().shop.details.name)

        viewModel.onLocationChanged(willebroek)

        assertEquals("Veraf", viewModel.uiState.value.offers.first().shop.details.name)
        assertEquals(0.0, viewModel.uiState.value.offers.first().distanceMeters, 0.001)
        assertEquals(1, packages.askedDates.size) // no second read
    }

    @Test
    fun aLocationBeforeTheFirstLoad_isUsedForTheFirstList() {
        val viewModel = viewModel()
        viewModel.onLocationChanged(willebroek)

        viewModel.load()

        assertEquals("Veraf", viewModel.uiState.value.offers.first().shop.details.name)
    }

    @Test
    fun sameLocationAgain_changesNothing() {
        val viewModel = viewModel()
        viewModel.load()
        val before = viewModel.uiState.value

        viewModel.onLocationChanged(campus)

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun loadFailsWithoutInternet_showsNoInternet_andTryingAgainWorks() {
        packages.loadError = FirebaseNetworkException("offline")
        val viewModel = viewModel()

        viewModel.load()
        assertEquals(R.string.error_no_internet, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)

        packages.loadError = null
        viewModel.load()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(2, viewModel.uiState.value.offers.size)
    }

    @Test
    fun loadFailsInTheShops_showsTheLoadError() {
        shops.loadError = IllegalStateException("boom")
        val viewModel = viewModel()

        viewModel.load()

        assertEquals(R.string.error_customer_packages_load_failed, viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.offers.isEmpty())
    }

    @Test
    fun loadingAgain_keepsTheListWhenItFails() {
        val viewModel = viewModel()
        viewModel.load()

        packages.loadError = FirebaseNetworkException("offline")
        viewModel.load()

        assertEquals(2, viewModel.uiState.value.offers.size) // the old list is not thrown away
        assertEquals(R.string.error_no_internet, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun loadingAgain_showsANewPackage() {
        val viewModel = viewModel()
        viewModel.load()

        packages.packages = packages.packages + pack("3", "near", priceCents = 300)
        viewModel.load()

        assertEquals(3, viewModel.uiState.value.offers.size)
        assertEquals(300, viewModel.uiState.value.shops.first().fromPriceCents) // "Vanaf €3"
    }
}
