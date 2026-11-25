package be.ap.laatsteronde.ui.merchant

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.AddressParts
import be.ap.laatsteronde.data.AddressRepository
import be.ap.laatsteronde.data.CachedAddressRepository
import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.data.FirebaseShopRepository
import be.ap.laatsteronde.data.Shop
import be.ap.laatsteronde.data.ShopCategory
import be.ap.laatsteronde.data.ShopDetails
import be.ap.laatsteronde.data.ShopRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Longest shop name; the Firestore rules check the same limit. */
internal const val SHOP_NAME_MAX_LENGTH = 60

/** How long we wait for Firestore to confirm a save before we say there is no connection. */
internal const val SAVE_TIMEOUT_MILLIS = 10_000L

/**
 * How long we wait after the last typed letter of the address before we look for the pin. Nominatim
 * asks for at most one question a second, and a merchant who is still typing does not need an answer yet.
 */
internal const val PIN_LOOKUP_DELAY_MILLIS = 1_000L

/** Turns a Firebase exception into a message the user understands. [fallback] is used for all other problems. */
@StringRes
internal fun shopErrorMessageFor(e: Exception, @StringRes fallback: Int): Int = when {
    e is FirebaseNetworkException -> R.string.error_no_internet
    e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_no_internet
    else -> fallback
}

/**
 * Everything the "Mijn winkel" screen shows (Figma "10 - Mijn winkel"): the form with
 * the name, category, pin and address. It is filled from the saved shop ([shop]) once that is loaded.
 * [addressAutoFilled] is true as long as the address comes from the pin and was not changed by hand.
 * [justSaved] is true after a successful save, until the merchant changes something again.
 * [isLookingUpPin] is true while the pin is being looked up for a typed address (including the short wait after
 * the last letter); [addressPinCount] goes up each time the pin was moved to a typed address, so the map knows to look at it.
 */
data class ShopUiState(
    val isLoading: Boolean = true,
    val shop: Shop? = null,
    val name: String = "",
    val category: ShopCategory? = null,
    val pin: Coordinates? = null,
    val street: String = "",
    val houseNumber: String = "",
    val postcode: String = "",
    val municipality: String = "",
    val addressAutoFilled: Boolean = false,
    val isLookingUpAddress: Boolean = false,
    val isLookingUpPin: Boolean = false,
    val addressPinCount: Int = 0,
    val isSaving: Boolean = false,
    val justSaved: Boolean = false,
    @StringRes val nameError: Int? = null,
    @StringRes val categoryError: Int? = null,
    @StringRes val pinError: Int? = null,
    @StringRes val addressError: Int? = null,
    @StringRes val errorMessage: Int? = null
)

/** Shows, creates and changes the shop of the signed-in merchant. */
class ShopViewModel(
    private val shopRepository: ShopRepository,
    private val addressRepository: AddressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    private var addressJob: Job? = null
    private var pinJob: Job? = null

    init {
        load()
    }

    /** Reads the saved shop and fills the form with it. Also used for "try again" after a failure. */
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val shop = shopRepository.myShop()
                _uiState.update { state ->
                    if (shop == null) state.copy(isLoading = false, shop = null) else state.withShop(shop)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = shopErrorMessageFor(e, R.string.error_shop_load_failed))
                }
            }
        }
    }

    // Each change also clears the error of that field and the "saved" message.
    fun onNameChange(name: String) =
        _uiState.update { it.copy(name = name, nameError = null, errorMessage = null, justSaved = false) }

    fun onCategoryChange(category: ShopCategory) =
        _uiState.update { it.copy(category = category, categoryError = null, errorMessage = null, justSaved = false) }

    // A typed change of the address also moves the pin to it, after a short wait.
    fun onStreetChange(street: String) = onAddressTyped { it.copy(street = street) }

    fun onHouseNumberChange(houseNumber: String) = onAddressTyped { it.copy(houseNumber = houseNumber) }

    fun onPostcodeChange(postcode: String) = onAddressTyped { it.copy(postcode = postcode) }

    fun onMunicipalityChange(municipality: String) = onAddressTyped { it.copy(municipality = municipality) }

    private fun onAddressTyped(change: (ShopUiState) -> ShopUiState) {
        _uiState.update { change(it.addressEdited()) }
        lookUpPin()
    }

    /**
     * Moves the pin to the typed address, as soon as the merchant stopped typing for a moment. Needs a
     * street and a postcode or a municipality; with less there is nothing to look for. Every new letter
     * starts the wait again, so only the last address is looked up. The address stays as it was typed:
     * only the pin moves, and the address of the pin is not looked up again (that would change what was typed).
     */
    private fun lookUpPin() {
        pinJob?.cancel()
        val state = _uiState.value
        val address = AddressParts(
            street = state.street.trim(),
            houseNumber = state.houseNumber.trim(),
            postcode = state.postcode.trim(),
            municipality = state.municipality.trim()
        )
        val complete = address.street.isNotEmpty() && (address.postcode.isNotEmpty() || address.municipality.isNotEmpty())
        if (!complete) {
            _uiState.update { it.copy(isLookingUpPin = false) }
            return
        }
        // Also true during the wait, so "Winkel opslaan" does not save the old pin of the old address.
        _uiState.update { it.copy(isLookingUpPin = true) }
        pinJob = viewModelScope.launch {
            delay(PIN_LOOKUP_DELAY_MILLIS)
            val found = addressRepository.locationOf(address)
            _uiState.update {
                if (found == null) {
                    // The pin stays where it was; the merchant can fix the address or place the pin by hand.
                    it.copy(isLookingUpPin = false, errorMessage = R.string.error_pin_not_found)
                } else {
                    it.copy(
                        pin = found,
                        pinError = null,
                        isLookingUpPin = false,
                        addressPinCount = it.addressPinCount + 1,
                        errorMessage = null
                    )
                }
            }
        }
    }

    /**
     * The merchant put the pin on a place (a tap or a drag). We look up the address of that place;
     * the old address belongs to the old place, so it is cleared. Only the answer for the last pin counts.
     */
    fun onPinPlaced(position: Coordinates) {
        addressJob?.cancel()
        pinJob?.cancel() // a pin placed by hand beats a typed address that is still being looked up
        _uiState.update {
            it.copy(
                pin = position,
                isLookingUpPin = false,
                street = "",
                houseNumber = "",
                postcode = "",
                municipality = "",
                addressAutoFilled = false,
                isLookingUpAddress = true,
                pinError = null,
                addressError = null,
                errorMessage = null,
                justSaved = false
            )
        }
        addressJob = viewModelScope.launch {
            val found = addressRepository.addressAt(position)
            _uiState.update {
                if (found == null) {
                    // The merchant can type the address, so this is a hint and not a dead end.
                    it.copy(isLookingUpAddress = false, errorMessage = R.string.error_address_not_found)
                } else {
                    it.copy(
                        street = found.street,
                        houseNumber = found.houseNumber,
                        postcode = found.postcode,
                        municipality = found.municipality,
                        addressAutoFilled = true,
                        isLookingUpAddress = false
                    )
                }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLookingUpAddress || state.isLookingUpPin) return

        val name = state.name.trim()
        val category = state.category
        val pin = state.pin
        val street = state.street.trim()
        val postcode = state.postcode.trim()
        val municipality = state.municipality.trim()

        val nameError = when {
            name.isEmpty() -> R.string.error_shop_name_empty
            name.length > SHOP_NAME_MAX_LENGTH -> R.string.error_shop_name_too_long
            else -> null
        }
        val categoryError = if (category == null) R.string.error_shop_category_missing else null
        val pinError = if (pin == null) R.string.error_shop_pin_missing else null
        // The house number is allowed to be empty: not every place has one.
        val addressError =
            if (street.isEmpty() || postcode.isEmpty() || municipality.isEmpty()) R.string.error_shop_address_incomplete else null
        if (nameError != null || categoryError != null || pinError != null || addressError != null ||
            category == null || pin == null
        ) {
            _uiState.update {
                it.copy(nameError = nameError, categoryError = categoryError, pinError = pinError, addressError = addressError)
            }
            return
        }

        val details = ShopDetails(
            name = name,
            category = category,
            street = street,
            houseNumber = state.houseNumber.trim(),
            postcode = postcode,
            municipality = municipality,
            location = pin
        )
        viewModelScope.launch {
            // The "saved" message of an earlier save goes away, so it does not stand next to an error.
            _uiState.update { it.copy(isSaving = true, errorMessage = null, justSaved = false) }
            try {
                // Without internet Firestore waits for ever, so after a while we tell the
                // merchant instead of showing a spinner without end.
                withTimeout(SAVE_TIMEOUT_MILLIS) { shopRepository.saveMyShop(details) }
                val saved = shopRepository.myShop()
                _uiState.update { current ->
                    (if (saved != null) current.withShop(saved) else current).copy(isSaving = false, justSaved = true)
                }
            } catch (e: TimeoutCancellationException) {
                // Must come before CancellationException, which this one is a kind of.
                _uiState.update { it.copy(isSaving = false, errorMessage = R.string.error_no_internet) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = shopErrorMessageFor(e, R.string.error_shop_save_failed))
                }
            }
        }
    }

    /** The form after the merchant typed in the address: no longer "automatisch ingevuld", old messages gone. */
    private fun ShopUiState.addressEdited() =
        copy(addressAutoFilled = false, addressError = null, errorMessage = null, justSaved = false)

    /** The form filled with the values of a saved shop. */
    private fun ShopUiState.withShop(shop: Shop) = copy(
        isLoading = false,
        shop = shop,
        name = shop.details.name,
        category = shop.details.category,
        pin = shop.details.location,
        street = shop.details.street,
        houseNumber = shop.details.houseNumber,
        postcode = shop.details.postcode,
        municipality = shop.details.municipality,
        addressAutoFilled = false,
        nameError = null,
        categoryError = null,
        pinError = null,
        addressError = null,
        errorMessage = null
    )

    companion object {
        /** Builds the ViewModel with Firestore for the shop and Nominatim for the address. */
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ShopViewModel(
                    shopRepository = FirebaseShopRepository(),
                    addressRepository = CachedAddressRepository.create(context.applicationContext)
                )
            }
        }
    }
}
