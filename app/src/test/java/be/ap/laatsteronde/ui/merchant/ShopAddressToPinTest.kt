package be.ap.laatsteronde.ui.merchant

import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.AddressParts
import be.ap.laatsteronde.data.AddressRepository
import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.data.Shop
import be.ap.laatsteronde.data.ShopCategory
import be.ap.laatsteronde.data.ShopDetails
import be.ap.laatsteronde.data.ShopRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
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

/**
 * Tests that [ShopViewModel] moves the pin to an address that the merchant types: after a short wait, only for a
 * complete address, only for the last address, and without changing what was typed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShopAddressToPinTest {

    private class FakeShopRepository(var shop: Shop? = null) : ShopRepository {
        val saved = mutableListOf<ShopDetails>()

        override suspend fun myShop(): Shop? = shop

        override suspend fun allShops(): List<Shop> = listOfNotNull(shop)

        override suspend fun saveMyShop(details: ShopDetails) {
            saved += details
            shop = Shop("uid", details)
        }
    }

    /** Knows the position of some addresses and remembers every question. */
    private class FakeAddressRepository : AddressRepository {
        val locations = mutableMapOf<AddressParts, Coordinates>()
        val locationLookups = mutableListOf<AddressParts>()
        val addressLookups = mutableListOf<Coordinates>()

        override suspend fun municipalityAt(coordinates: Coordinates): String? = null

        override suspend fun addressAt(coordinates: Coordinates): AddressParts? {
            addressLookups += coordinates
            return null
        }

        override suspend fun locationOf(address: AddressParts): Coordinates? {
            locationLookups += address
            return locations[address]
        }
    }

    private val knokke = Coordinates(latitude = 51.35, longitude = 3.29)
    private val antwerp = Coordinates(latitude = 51.23, longitude = 4.41)
    private val zeedijk = AddressParts(street = "Zeedijk", houseNumber = "12", postcode = "8300", municipality = "Knokke")

    private lateinit var shops: FakeShopRepository
    private lateinit var addresses: FakeAddressRepository
    private val scheduler = TestCoroutineScheduler() // lets a test move the clock forward

    @Before
    fun setUp() {
        // viewModelScope runs on Dispatchers.Main, which does not exist in a plain JVM test.
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        shops = FakeShopRepository()
        addresses = FakeAddressRepository().apply { locations[zeedijk] = knokke }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ShopViewModel(shops, addresses)

    /** Lets the wait after the last typed letter pass, so the lookup of the pin runs. */
    private fun waitForTheLookup() {
        scheduler.advanceTimeBy(PIN_LOOKUP_DELAY_MILLIS + 1)
        scheduler.runCurrent()
    }

    private fun ShopViewModel.typeAddress(address: AddressParts) {
        onStreetChange(address.street)
        onHouseNumberChange(address.houseNumber)
        onPostcodeChange(address.postcode)
        onMunicipalityChange(address.municipality)
    }

    @Test
    fun aCompleteTypedAddress_movesThePinToIt() {
        val viewModel = viewModel()

        viewModel.typeAddress(zeedijk)
        waitForTheLookup()

        val state = viewModel.uiState.value
        assertEquals(knokke, state.pin)
        assertFalse(state.isLookingUpPin)
        assertNull(state.errorMessage)
    }

    @Test
    fun theTypedAddressStaysAsTyped_andThePinIsNotUsedToLookItUpAgain() {
        val viewModel = viewModel()

        viewModel.typeAddress(zeedijk)
        waitForTheLookup()

        val state = viewModel.uiState.value
        assertEquals("Zeedijk", state.street)
        assertEquals("12", state.houseNumber)
        assertEquals("8300", state.postcode)
        assertEquals("Knokke", state.municipality)
        assertFalse(state.addressAutoFilled)
        assertTrue(addresses.addressLookups.isEmpty()) // no answer from the pin that overwrites the typed address
    }

    @Test
    fun thePinDoesNotMoveBeforeTheWaitIsOver_andTheScreenKnowsItIsLooking() {
        val viewModel = viewModel()

        viewModel.typeAddress(zeedijk)
        scheduler.advanceTimeBy(PIN_LOOKUP_DELAY_MILLIS - 100)
        scheduler.runCurrent()

        assertNull(viewModel.uiState.value.pin)
        assertTrue(viewModel.uiState.value.isLookingUpPin)
        assertTrue(addresses.locationLookups.isEmpty())
    }

    @Test
    fun theMapIsToldEachTimeThePinMovesToAnAddress() {
        val viewModel = viewModel()
        assertEquals(0, viewModel.uiState.value.addressPinCount)

        viewModel.typeAddress(zeedijk)
        waitForTheLookup()
        assertEquals(1, viewModel.uiState.value.addressPinCount)

        addresses.locations[zeedijk.copy(houseNumber = "14")] = antwerp
        viewModel.onHouseNumberChange("14")
        waitForTheLookup()

        assertEquals(2, viewModel.uiState.value.addressPinCount)
        assertEquals(antwerp, viewModel.uiState.value.pin)
    }

    @Test
    fun aPinPlacedByHand_doesNotCountAsAMoveToAnAddress() {
        val viewModel = viewModel()

        viewModel.onPinPlaced(antwerp)

        assertEquals(0, viewModel.uiState.value.addressPinCount)
    }

    @Test
    fun quickTyping_looksUpOnlyTheLastAddress() {
        val viewModel = viewModel()

        // Each letter comes before the wait is over, so only the last address counts.
        for (street in listOf("Z", "Ze", "Zee", "Zeed", "Zeedi", "Zeedij", "Zeedijk")) {
            viewModel.onPostcodeChange("8300")
            viewModel.onStreetChange(street)
            scheduler.advanceTimeBy(300)
            scheduler.runCurrent()
        }
        viewModel.onHouseNumberChange("12")
        viewModel.onMunicipalityChange("Knokke")
        waitForTheLookup()

        assertEquals(listOf(zeedijk), addresses.locationLookups)
        assertEquals(knokke, viewModel.uiState.value.pin)
    }

    @Test
    fun theAddressIsLookedUpWithoutSpaces() {
        val viewModel = viewModel()

        viewModel.typeAddress(zeedijk.copy(street = "  Zeedijk ", municipality = " Knokke "))
        waitForTheLookup()

        assertEquals(listOf(zeedijk), addresses.locationLookups)
    }

    @Test
    fun withoutAStreet_nothingIsLookedUp() {
        val viewModel = viewModel()

        viewModel.onPostcodeChange("8300")
        viewModel.onMunicipalityChange("Knokke")
        waitForTheLookup()

        assertTrue(addresses.locationLookups.isEmpty())
        assertNull(viewModel.uiState.value.pin)
        assertFalse(viewModel.uiState.value.isLookingUpPin)
    }

    @Test
    fun withAStreetOnly_nothingIsLookedUp() {
        val viewModel = viewModel()

        viewModel.onStreetChange("Zeedijk")
        waitForTheLookup()

        assertTrue(addresses.locationLookups.isEmpty())
        assertFalse(viewModel.uiState.value.isLookingUpPin)
    }

    @Test
    fun aStreetAndAPostcode_isEnough() {
        val viewModel = viewModel()
        val postcodeOnly = AddressParts("Zeedijk", "", "8300", "")
        addresses.locations[postcodeOnly] = knokke

        viewModel.onStreetChange("Zeedijk")
        viewModel.onPostcodeChange("8300")
        waitForTheLookup()

        assertEquals(knokke, viewModel.uiState.value.pin)
    }

    @Test
    fun emptyingTheStreetAgain_cancelsTheLookup() {
        val viewModel = viewModel()
        viewModel.typeAddress(zeedijk)

        viewModel.onStreetChange("")
        waitForTheLookup()

        assertTrue(addresses.locationLookups.isEmpty())
        assertNull(viewModel.uiState.value.pin)
        assertFalse(viewModel.uiState.value.isLookingUpPin)
    }

    @Test
    fun anAddressThatIsNotFound_leavesThePinAndSaysSo() {
        val viewModel = viewModel()
        viewModel.onPinPlaced(antwerp)

        viewModel.typeAddress(zeedijk.copy(street = "Straat die niet bestaat"))
        waitForTheLookup()

        val state = viewModel.uiState.value
        assertEquals(antwerp, state.pin) // the old pin is not thrown away
        assertEquals(R.string.error_pin_not_found, state.errorMessage)
        assertFalse(state.isLookingUpPin)
        assertEquals("Straat die niet bestaat", state.street) // what was typed stays
    }

    @Test
    fun afterANotFound_aNewAddressThatIsFound_removesTheMessage() {
        val viewModel = viewModel()
        viewModel.typeAddress(zeedijk.copy(street = "Zeedyk"))
        waitForTheLookup()
        assertEquals(R.string.error_pin_not_found, viewModel.uiState.value.errorMessage)

        viewModel.onStreetChange("Zeedijk")
        waitForTheLookup()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(knokke, viewModel.uiState.value.pin)
    }

    @Test
    fun aFoundPin_removesTheErrorThatThePinWasMissing() {
        val viewModel = viewModel()
        viewModel.onNameChange("Bakkerij Jan")
        viewModel.onCategoryChange(ShopCategory.BAKER)
        viewModel.save() // no pin yet
        assertEquals(R.string.error_shop_pin_missing, viewModel.uiState.value.pinError)

        viewModel.typeAddress(zeedijk)
        waitForTheLookup()

        assertNull(viewModel.uiState.value.pinError)
    }

    @Test
    fun aPinPlacedByHandWhileLooking_winsOverTheTypedAddress() {
        val viewModel = viewModel()
        viewModel.typeAddress(zeedijk)

        viewModel.onPinPlaced(antwerp)
        waitForTheLookup()

        assertEquals(antwerp, viewModel.uiState.value.pin) // the lookup was cancelled
        assertTrue(addresses.locationLookups.isEmpty())
        assertFalse(viewModel.uiState.value.isLookingUpPin)
    }

    @Test
    fun saveWhileTheLookupIsBusy_waits_andDoesNotSaveTheOldPin() {
        val viewModel = viewModel()
        viewModel.onNameChange("Bakkerij Jan")
        viewModel.onCategoryChange(ShopCategory.BAKER)
        viewModel.onPinPlaced(antwerp)
        viewModel.typeAddress(zeedijk)

        viewModel.save() // the pin is still the old one: this tap does nothing

        assertTrue(shops.saved.isEmpty())
    }

    @Test
    fun saveAfterTheLookup_savesThePinOfTheTypedAddress() {
        val viewModel = viewModel()
        viewModel.onNameChange("Bakkerij Jan")
        viewModel.onCategoryChange(ShopCategory.BAKER)
        viewModel.onPinPlaced(antwerp)
        viewModel.typeAddress(zeedijk)
        waitForTheLookup()

        viewModel.save()

        assertEquals(
            listOf(ShopDetails("Bakkerij Jan", ShopCategory.BAKER, "Zeedijk", "12", "8300", "Knokke", knokke)),
            shops.saved
        )
    }

    @Test
    fun loadingTheSavedShop_doesNotLookUpTheAddress() {
        shops.shop = Shop("uid", ShopDetails("Bakkerij Jan", ShopCategory.BAKER, "Zeedijk", "12", "8300", "Knokke", antwerp))

        val viewModel = viewModel()
        waitForTheLookup()

        assertTrue(addresses.locationLookups.isEmpty())
        assertEquals(antwerp, viewModel.uiState.value.pin) // the saved pin stays where it was saved
    }

    @Test
    fun theAddressOfAPinPlacedByHand_doesNotMoveThePinAgain() {
        val viewModel = viewModel()

        viewModel.onPinPlaced(antwerp)
        waitForTheLookup()

        assertTrue(addresses.locationLookups.isEmpty())
        assertEquals(antwerp, viewModel.uiState.value.pin)
    }
}
