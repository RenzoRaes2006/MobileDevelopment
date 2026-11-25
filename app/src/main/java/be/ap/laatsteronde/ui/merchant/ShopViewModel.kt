package be.ap.laatsteronde.ui.merchant

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.AddressRepository
import be.ap.laatsteronde.data.CachedAddressRepository
import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.data.FirebaseShopRepository
import be.ap.laatsteronde.data.Shop
import be.ap.laatsteronde.data.ShopRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Longest shop name; the Firestore rules check the same limit. */
internal const val SHOP_NAME_MAX_LENGTH = 60

/** Turns a Firebase exception into a message the user understands. [fallback] is used for all other problems. */
@StringRes
internal fun shopErrorMessageFor(e: Exception, @StringRes fallback: Int): Int = when {
    e is FirebaseNetworkException -> R.string.error_no_internet
    e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_no_internet
    else -> fallback
}

/**
 * Everything the shop screens show.
 * [shop] is the saved shop (null when the merchant has none yet). While [isEditing] is true the
 * form with [name], [pin] and [address] is shown; the three can be filled from the saved shop.
 */
data class ShopUiState(
    val isLoading: Boolean = true,
    val shop: Shop? = null,
    val isEditing: Boolean = false,
    val name: String = "",
    val pin: Coordinates? = null,
    val address: String = "",
    val isLookingUpAddress: Boolean = false,
    val isSaving: Boolean = false,
    @StringRes val nameError: Int? = null,
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

    init {
        load()
    }

    /** Reads the saved shop. Also used for "try again" after a failure. */
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val shop = shopRepository.myShop()
                _uiState.update { it.copy(isLoading = false, shop = shop) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = shopErrorMessageFor(e, R.string.error_shop_load_failed))
                }
            }
        }
    }

    /** Opens the form: empty for a new shop, filled with the saved values to change a shop. */
    fun onStartEditing() {
        val shop = _uiState.value.shop
        _uiState.update {
            it.copy(
                isEditing = true,
                name = shop?.name.orEmpty(),
                pin = shop?.location,
                address = shop?.address.orEmpty(),
                nameError = null,
                pinError = null,
                addressError = null,
                errorMessage = null
            )
        }
    }

    /** Closes the form without saving. */
    fun onCancelEditing() {
        addressJob?.cancel()
        _uiState.update {
            it.copy(isEditing = false, isLookingUpAddress = false, nameError = null, pinError = null, addressError = null, errorMessage = null)
        }
    }

    fun onNameChange(name: String) =
        _uiState.update { it.copy(name = name, nameError = null, errorMessage = null) }

    fun onAddressChange(address: String) =
        _uiState.update { it.copy(address = address, addressError = null, errorMessage = null) }

    /**
     * The merchant put the pin on a place. We look up the address of that place; the old address
     * belongs to the old place, so it is cleared. Only the answer for the last pin counts.
     */
    fun onPinPlaced(position: Coordinates) {
        addressJob?.cancel()
        _uiState.update {
            it.copy(
                pin = position,
                address = "",
                isLookingUpAddress = true,
                pinError = null,
                addressError = null,
                errorMessage = null
            )
        }
        addressJob = viewModelScope.launch {
            val found = addressRepository.addressAt(position)
            _uiState.update {
                it.copy(
                    address = found.orEmpty(),
                    isLookingUpAddress = false,
                    // Without an address the merchant can type it, so this is a hint and not a dead end.
                    errorMessage = if (found == null) R.string.error_address_not_found else null
                )
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLookingUpAddress) return

        val name = state.name.trim()
        val address = state.address.trim()
        val pin = state.pin
        val nameError = when {
            name.isEmpty() -> R.string.error_shop_name_empty
            name.length > SHOP_NAME_MAX_LENGTH -> R.string.error_shop_name_too_long
            else -> null
        }
        val pinError = if (pin == null) R.string.error_shop_pin_missing else null
        val addressError = if (pin != null && address.isEmpty()) R.string.error_shop_address_empty else null
        if (nameError != null || pinError != null || addressError != null || pin == null) {
            _uiState.update { it.copy(nameError = nameError, pinError = pinError, addressError = addressError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                shopRepository.saveMyShop(name, address, pin)
                val saved = shopRepository.myShop()
                _uiState.update { it.copy(isSaving = false, isEditing = false, shop = saved) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = shopErrorMessageFor(e, R.string.error_shop_save_failed))
                }
            }
        }
    }

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
