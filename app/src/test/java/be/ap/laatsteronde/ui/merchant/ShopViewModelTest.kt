package be.ap.laatsteronde.ui.merchant

import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.AddressRepository
import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.data.Shop
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Tests [ShopViewModel] with fakes instead of Firebase and Nominatim. */
@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {

    private class FakeShopRepository(var shop: Shop? = null) : ShopRepository {
        var loadError: Exception? = null
        var saveError: Exception? = null
        val saved = mutableListOf<Triple<String, String, Coordinates>>()

        override suspend fun myShop(): Shop? {
            loadError?.let { throw it }
            return shop
        }

        override suspend fun saveMyShop(name: String, address: String, location: Coordinates) {
            saveError?.let { throw it }
            saved += Triple(name, address, location)
            shop = Shop("uid", name, address, location.latitude, location.longitude)
        }
    }

    /** Gives the address from [answers] for a position, or null when the position is not in the map. */
    private class FakeAddressRepository(val answers: MutableMap<Coordinates, String> = mutableMapOf()) : AddressRepository {
        override suspend fun municipalityAt(coordinates: Coordinates): String? = null
        override suspend fun addressAt(coordinates: Coordinates): String? = answers[coordinates]
    }

    private val antwerp = Coordinates(latitude = 51.23, longitude = 4.41)
    private val willebroek = Coordinates(latitude = 51.06, longitude = 4.36)
    private val savedShop = Shop("uid", "Bakkerij Jan", "Dorpsstraat 1, 2830 Willebroek", willebroek.latitude, willebroek.longitude)

    private lateinit var shops: FakeShopRepository
    private lateinit var addresses: FakeAddressRepository

    @Before
    fun setUp() {
        // viewModelScope runs on Dispatchers.Main, which does not exist in a plain JVM test.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        shops = FakeShopRepository()
        addresses = FakeAddressRepository(mutableMapOf(antwerp to "Ellermanstraat 12, 2060 Antwerpen", willebroek to "Dorpsstraat 1, 2830 Willebroek"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ShopViewModel(shops, addresses)

    // --- loading the saved shop

    @Test
    fun merchantWithoutShop_isNotLoadingAndHasNoShop() {
        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertNull(state.shop)
        assertFalse(state.isEditing)
    }

    @Test
    fun merchantWithShop_seesTheSavedShop() {
        shops.shop = savedShop

        assertEquals(savedShop, viewModel().uiState.value.shop)
    }

    @Test
    fun loadFailsWithoutInternet_showsNoInternetAndCanTryAgain() {
        shops.loadError = FirebaseNetworkException("offline")
        val viewModel = viewModel()
        assertEquals(R.string.error_no_internet, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)

        shops.loadError = null
        shops.shop = savedShop
        viewModel.load()

        assertEquals(savedShop, viewModel.uiState.value.shop)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun loadFailsForAnotherReason_showsTheLoadError() {
        shops.loadError = IllegalStateException("boom")

        assertEquals(R.string.error_shop_load_failed, viewModel().uiState.value.errorMessage)
    }

    // --- the form

    @Test
    fun startEditingWithoutShop_givesAnEmptyForm() {
        val viewModel = viewModel()

        viewModel.onStartEditing()

        val state = viewModel.uiState.value
        assertTrue(state.isEditing)
        assertEquals("", state.name)
        assertNull(state.pin)
        assertEquals("", state.address)
    }

    @Test
    fun startEditingWithShop_fillsTheFormWithTheSavedValues() {
        shops.shop = savedShop
        val viewModel = viewModel()

        viewModel.onStartEditing()

        val state = viewModel.uiState.value
        assertEquals("Bakkerij Jan", state.name)
        assertEquals(willebroek, state.pin)
        assertEquals("Dorpsstraat 1, 2830 Willebroek", state.address)
    }

    @Test
    fun cancelEditing_closesTheFormAndKeepsTheSavedShop() {
        shops.shop = savedShop
        val viewModel = viewModel()
        viewModel.onStartEditing()
        viewModel.onNameChange("Andere naam")

        viewModel.onCancelEditing()

        assertFalse(viewModel.uiState.value.isEditing)
        assertEquals(savedShop, viewModel.uiState.value.shop)
    }

    // --- the pin and the automatic address

    @Test
    fun placingThePin_fillsInTheAddress() {
        val viewModel = viewModel()
        viewModel.onStartEditing()

        viewModel.onPinPlaced(antwerp)

        val state = viewModel.uiState.value
        assertEquals(antwerp, state.pin)
        assertEquals("Ellermanstraat 12, 2060 Antwerpen", state.address)
        assertFalse(state.isLookingUpAddress)
        assertNull(state.errorMessage)
    }

    @Test
    fun placingThePinOnAnotherPlace_replacesTheAddress() {
        val viewModel = viewModel()
        viewModel.onStartEditing()
        viewModel.onPinPlaced(antwerp)

        viewModel.onPinPlaced(willebroek)

        assertEquals("Dorpsstraat 1, 2830 Willebroek", viewModel.uiState.value.address)
    }

    @Test
    fun noAddressFound_leavesTheAddressEmptyAndShowsAHint() {
        val viewModel = viewModel()
        viewModel.onStartEditing()

        viewModel.onPinPlaced(Coordinates(0.0, 0.0)) // in the sea: Nominatim knows nothing

        val state = viewModel.uiState.value
        assertEquals("", state.address)
        assertEquals(R.string.error_address_not_found, state.errorMessage)
        assertEquals(Coordinates(0.0, 0.0), state.pin)
    }

    // --- saving

    @Test
    fun saveWithEmptyName_showsNameErrorAndSavesNothing() {
        val viewModel = viewModel()
        viewModel.onStartEditing()
        viewModel.onPinPlaced(antwerp)
        viewModel.onNameChange("   ")

        viewModel.save()

        assertEquals(R.string.error_shop_name_empty, viewModel.uiState.value.nameError)
        assertTrue(shops.saved.isEmpty())
    }

    @Test
    fun saveWithTooLongName_showsNameError() {
        val viewModel = viewModel()
        viewModel.onStartEditing()
        viewModel.onPinPlaced(antwerp)
        viewModel.onNameChange("a".repeat(SHOP_NAME_MAX_LENGTH + 1))

        viewModel.save()

        assertEquals(R.string.error_shop_name_too_long, viewModel.uiState.value.nameError)
        assertTrue(shops.saved.isEmpty())
    }

    @Test
    fun saveWithoutPin_showsPinErrorAndSavesNothing() {
        val viewModel = viewModel()
        viewModel.onStartEditing()
        viewModel.onNameChange("Bakkerij Jan")

        viewModel.save()

        assertEquals(R.string.error_shop_pin_missing, viewModel.uiState.value.pinError)
        assertTrue(shops.saved.isEmpty())
    }

    @Test
    fun saveWithEmptyAddress_showsAddressError() {
        val viewModel = viewModel()
        viewModel.onStartEditing()
        viewModel.onPinPlaced(Coordinates(0.0, 0.0)) // no address found
        viewModel.onNameChange("Bakkerij Jan")

        viewModel.save()

        assertEquals(R.string.error_shop_address_empty, viewModel.uiState.value.addressError)
        assertTrue(shops.saved.isEmpty())
    }

    @Test
    fun saveWithAnAddressTypedByHand_works() {
        val viewModel = viewModel()
        viewModel.onStartEditing()
        viewModel.onPinPlaced(Coordinates(0.0, 0.0))
        viewModel.onNameChange("Bakkerij Jan")
        viewModel.onAddressChange("  Zeedijk 1, 8300 Knokke  ")

        viewModel.save()

        assertEquals(listOf(Triple("Bakkerij Jan", "Zeedijk 1, 8300 Knokke", Coordinates(0.0, 0.0))), shops.saved)
    }

    @Test
    fun saveSucceeds_closesTheFormAndShowsTheShop() {
        val viewModel = viewModel()
        viewModel.onStartEditing()
        viewModel.onNameChange("  Bakkerij Jan ")
        viewModel.onPinPlaced(willebroek)

        viewModel.save()

        val state = viewModel.uiState.value
        assertEquals(listOf(Triple("Bakkerij Jan", "Dorpsstraat 1, 2830 Willebroek", willebroek)), shops.saved) // name trimmed
        assertFalse(state.isEditing)
        assertFalse(state.isSaving)
        assertNotNull(state.shop)
        assertEquals("Bakkerij Jan", state.shop?.name)
    }

    @Test
    fun saveWithoutInternet_showsNoInternetAndKeepsTheForm() {
        shops.saveError = FirebaseNetworkException("offline")
        val viewModel = viewModel()
        viewModel.onStartEditing()
        viewModel.onNameChange("Bakkerij Jan")
        viewModel.onPinPlaced(willebroek)

        viewModel.save()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_no_internet, state.errorMessage)
        assertTrue(state.isEditing)
        assertFalse(state.isSaving)
        assertEquals("Bakkerij Jan", state.name) // nothing the merchant typed is lost
    }

    @Test
    fun saveFailsForAnotherReason_showsTheSaveError() {
        shops.saveError = IllegalStateException("permission denied")
        val viewModel = viewModel()
        viewModel.onStartEditing()
        viewModel.onNameChange("Bakkerij Jan")
        viewModel.onPinPlaced(willebroek)

        viewModel.save()

        assertEquals(R.string.error_shop_save_failed, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun typingClearsAnOldError() {
        val viewModel = viewModel()
        viewModel.onStartEditing()
        viewModel.save()
        assertNotNull(viewModel.uiState.value.nameError)

        viewModel.onNameChange("B")

        assertNull(viewModel.uiState.value.nameError)
    }
}
