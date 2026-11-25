package be.ap.laatsteronde.ui.merchant

import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.AddressParts
import be.ap.laatsteronde.data.AddressRepository
import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.data.Shop
import be.ap.laatsteronde.data.ShopCategory
import be.ap.laatsteronde.data.ShopDetails
import be.ap.laatsteronde.data.ShopRepository
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestCoroutineScheduler
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

        /** Like Firestore without internet: the save never finishes. */
        var saveHangs = false
        val saved = mutableListOf<ShopDetails>()

        override suspend fun myShop(): Shop? {
            loadError?.let { throw it }
            return shop
        }

        override suspend fun allShops(): List<Shop> = listOfNotNull(shop)

        override suspend fun saveMyShop(details: ShopDetails) {
            if (saveHangs) awaitCancellation()
            saveError?.let { throw it }
            saved += details
            shop = Shop("uid", details)
        }
    }

    /** Gives the address from [answers] for a position, or null when the position is not in the map. */
    private class FakeAddressRepository(val answers: MutableMap<Coordinates, AddressParts> = mutableMapOf()) : AddressRepository {
        override suspend fun municipalityAt(coordinates: Coordinates): String? = null
        override suspend fun addressAt(coordinates: Coordinates): AddressParts? = answers[coordinates]

        override suspend fun locationOf(address: AddressParts): Coordinates? = null
    }

    private val antwerp = Coordinates(latitude = 51.23, longitude = 4.41)
    private val willebroek = Coordinates(latitude = 51.06, longitude = 4.36)
    private val nowhere = Coordinates(0.0, 0.0) // in the sea: Nominatim knows nothing
    private val antwerpAddress = AddressParts("Kammenstraat", "12", "2000", "Antwerpen")
    private val willebroekAddress = AddressParts("Dorpsstraat", "1", "2830", "Willebroek")
    private val savedShop = Shop(
        "uid",
        ShopDetails("Bakkerij Jan", ShopCategory.BAKER, "Dorpsstraat", "1", "2830", "Willebroek", willebroek)
    )

    private lateinit var shops: FakeShopRepository
    private lateinit var addresses: FakeAddressRepository
    private val scheduler = TestCoroutineScheduler() // lets a test move the clock forward

    @Before
    fun setUp() {
        // viewModelScope runs on Dispatchers.Main, which does not exist in a plain JVM test.
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        shops = FakeShopRepository()
        addresses = FakeAddressRepository(mutableMapOf(antwerp to antwerpAddress, willebroek to willebroekAddress))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ShopViewModel(shops, addresses)

    /** A viewModel with a valid form: name, category and a pin in Antwerp with its address. */
    private fun viewModelWithValidForm(): ShopViewModel {
        val viewModel = viewModel()
        viewModel.onNameChange("Bakkerij Jan")
        viewModel.onCategoryChange(ShopCategory.BAKER)
        viewModel.onPinPlaced(antwerp)
        return viewModel
    }

    // --- loading the saved shop

    @Test
    fun merchantWithoutShop_hasAnEmptyForm() {
        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertNull(state.shop)
        assertEquals("", state.name)
        assertNull(state.category)
        assertNull(state.pin)
    }

    @Test
    fun merchantWithShop_seesTheFormFilledWithTheSavedShop() {
        shops.shop = savedShop

        val state = viewModel().uiState.value

        assertEquals(savedShop, state.shop)
        assertEquals("Bakkerij Jan", state.name)
        assertEquals(ShopCategory.BAKER, state.category)
        assertEquals(willebroek, state.pin)
        assertEquals("Dorpsstraat", state.street)
        assertEquals("1", state.houseNumber)
        assertEquals("2830", state.postcode)
        assertEquals("Willebroek", state.municipality)
        assertFalse(state.addressAutoFilled)
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

    // --- the pin and the automatic address

    @Test
    fun placingThePin_fillsInTheFourAddressFields() {
        val viewModel = viewModel()

        viewModel.onPinPlaced(antwerp)

        val state = viewModel.uiState.value
        assertEquals(antwerp, state.pin)
        assertEquals("Kammenstraat", state.street)
        assertEquals("12", state.houseNumber)
        assertEquals("2000", state.postcode)
        assertEquals("Antwerpen", state.municipality)
        assertTrue(state.addressAutoFilled)
        assertFalse(state.isLookingUpAddress)
        assertNull(state.errorMessage)
    }

    @Test
    fun placingThePinOnAnotherPlace_replacesTheAddress() {
        val viewModel = viewModel()
        viewModel.onPinPlaced(antwerp)

        viewModel.onPinPlaced(willebroek)

        assertEquals("Dorpsstraat", viewModel.uiState.value.street)
        assertEquals("Willebroek", viewModel.uiState.value.municipality)
    }

    @Test
    fun noAddressFound_leavesTheFieldsEmptyAndShowsAHint() {
        val viewModel = viewModel()

        viewModel.onPinPlaced(nowhere)

        val state = viewModel.uiState.value
        assertEquals("", state.street)
        assertEquals("", state.municipality)
        assertFalse(state.addressAutoFilled)
        assertEquals(R.string.error_address_not_found, state.errorMessage)
        assertEquals(nowhere, state.pin)
    }

    @Test
    fun editingTheAddress_isNoLongerAutomaticallyFilledIn() {
        val viewModel = viewModel()
        viewModel.onPinPlaced(antwerp)

        viewModel.onStreetChange("Andere straat")

        assertFalse(viewModel.uiState.value.addressAutoFilled)
        assertEquals("Andere straat", viewModel.uiState.value.street)
        assertEquals("Antwerpen", viewModel.uiState.value.municipality) // the other fields stay
    }

    // --- saving

    @Test
    fun saveWithEmptyName_showsNameErrorAndSavesNothing() {
        val viewModel = viewModelWithValidForm()
        viewModel.onNameChange("   ")

        viewModel.save()

        assertEquals(R.string.error_shop_name_empty, viewModel.uiState.value.nameError)
        assertTrue(shops.saved.isEmpty())
    }

    @Test
    fun saveWithTooLongName_showsNameError() {
        val viewModel = viewModelWithValidForm()
        viewModel.onNameChange("a".repeat(SHOP_NAME_MAX_LENGTH + 1))

        viewModel.save()

        assertEquals(R.string.error_shop_name_too_long, viewModel.uiState.value.nameError)
        assertTrue(shops.saved.isEmpty())
    }

    @Test
    fun saveWithoutCategory_showsCategoryError() {
        val viewModel = viewModel()
        viewModel.onNameChange("Bakkerij Jan")
        viewModel.onPinPlaced(antwerp)

        viewModel.save()

        assertEquals(R.string.error_shop_category_missing, viewModel.uiState.value.categoryError)
        assertTrue(shops.saved.isEmpty())
    }

    @Test
    fun saveWithoutPin_showsPinErrorAndSavesNothing() {
        val viewModel = viewModel()
        viewModel.onNameChange("Bakkerij Jan")
        viewModel.onCategoryChange(ShopCategory.BAKER)

        viewModel.save()

        assertEquals(R.string.error_shop_pin_missing, viewModel.uiState.value.pinError)
        assertTrue(shops.saved.isEmpty())
    }

    @Test
    fun saveWithIncompleteAddress_showsAddressError() {
        val viewModel = viewModel()
        viewModel.onNameChange("Bakkerij Jan")
        viewModel.onCategoryChange(ShopCategory.BAKER)
        viewModel.onPinPlaced(nowhere) // no address found

        viewModel.save()

        assertEquals(R.string.error_shop_address_incomplete, viewModel.uiState.value.addressError)
        assertTrue(shops.saved.isEmpty())
    }

    @Test
    fun saveWithAnAddressTypedByHand_andNoHouseNumber_works() {
        val viewModel = viewModel()
        viewModel.onNameChange("Bakkerij Jan")
        viewModel.onCategoryChange(ShopCategory.SUPERMARKET)
        viewModel.onPinPlaced(nowhere)
        viewModel.onStreetChange("  Zeedijk ")
        viewModel.onPostcodeChange("8300")
        viewModel.onMunicipalityChange("Knokke")
        viewModel.save()

        assertEquals(
            listOf(ShopDetails("Bakkerij Jan", ShopCategory.SUPERMARKET, "Zeedijk", "", "8300", "Knokke", nowhere)),
            shops.saved
        )
    }

    @Test
    fun saveSucceeds_savesTrimmedValuesAndShowsSavedMessage() {
        val viewModel = viewModel()
        viewModel.onNameChange("  Bakkerij Jan ")
        viewModel.onCategoryChange(ShopCategory.BAKER)
        viewModel.onPinPlaced(willebroek)

        viewModel.save()

        val state = viewModel.uiState.value
        assertEquals(
            listOf(ShopDetails("Bakkerij Jan", ShopCategory.BAKER, "Dorpsstraat", "1", "2830", "Willebroek", willebroek)),
            shops.saved
        ) // name trimmed
        assertFalse(state.isSaving)
        assertTrue(state.justSaved)
        assertNotNull(state.shop)
        assertEquals("Bakkerij Jan", state.shop?.details?.name)
    }

    @Test
    fun changingSomethingAfterSaving_hidesTheSavedMessage() {
        val viewModel = viewModelWithValidForm()
        viewModel.save()
        assertTrue(viewModel.uiState.value.justSaved)

        viewModel.onNameChange("Andere naam")

        assertFalse(viewModel.uiState.value.justSaved)
    }

    @Test
    fun saveWithoutInternet_showsNoInternetAndKeepsWhatWasTyped() {
        shops.saveError = FirebaseNetworkException("offline")
        val viewModel = viewModelWithValidForm()

        viewModel.save()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_no_internet, state.errorMessage)
        assertFalse(state.isSaving)
        assertFalse(state.justSaved)
        assertEquals("Bakkerij Jan", state.name) // nothing the merchant typed is lost
    }

    @Test
    fun saveThatNeverFinishes_waitsAndThenShowsNoInternet() {
        shops.saveHangs = true
        val viewModel = viewModelWithValidForm()

        viewModel.save()
        assertTrue(viewModel.uiState.value.isSaving) // still waiting for Firestore

        scheduler.advanceTimeBy(SAVE_TIMEOUT_MILLIS + 1)
        scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_no_internet, state.errorMessage)
        assertFalse(state.isSaving) // the merchant can try again
        assertFalse(state.justSaved)
        assertEquals("Bakkerij Jan", state.name)
    }

    @Test
    fun saveFailsAfterAnEarlierSave_nolongerShowsSaved() {
        val viewModel = viewModelWithValidForm()
        viewModel.save()
        assertTrue(viewModel.uiState.value.justSaved)

        shops.saveError = FirebaseNetworkException("offline")
        viewModel.save()

        // The old "Winkel opgeslagen" must not stand next to the error.
        assertFalse(viewModel.uiState.value.justSaved)
        assertEquals(R.string.error_no_internet, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun saveFailsForAnotherReason_showsTheSaveError() {
        shops.saveError = IllegalStateException("permission denied")
        val viewModel = viewModelWithValidForm()

        viewModel.save()

        assertEquals(R.string.error_shop_save_failed, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun typingClearsAnOldError() {
        val viewModel = viewModel()
        viewModel.save()
        assertNotNull(viewModel.uiState.value.nameError)

        viewModel.onNameChange("B")

        assertNull(viewModel.uiState.value.nameError)
    }
}
