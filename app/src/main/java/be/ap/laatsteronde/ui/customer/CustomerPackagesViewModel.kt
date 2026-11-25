package be.ap.laatsteronde.ui.customer

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.data.FirebasePackageRepository
import be.ap.laatsteronde.data.FirebaseShopRepository
import be.ap.laatsteronde.data.FoodPackage
import be.ap.laatsteronde.data.PackageRepository
import be.ap.laatsteronde.data.Shop
import be.ap.laatsteronde.data.ShopRepository
import be.ap.laatsteronde.ui.merchant.shopErrorMessageFor
import be.ap.laatsteronde.ui.merchant.todayAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The packages a customer can see, to look at only. [offers] are all packages of today with their
 * shop, nearest first (the list on Ontdekken); [shops] are the same offers grouped per shop (the
 * markers on the map). [isLoading] is only true until the first answer; a refresh later keeps the list.
 */
data class CustomerPackagesUiState(
    val isLoading: Boolean = true,
    val offers: List<PackageOffer> = emptyList(),
    val shops: List<ShopOffers> = emptyList(),
    @StringRes val errorMessage: Int? = null
)

/**
 * Reads the visible packages of today and all shops, and sorts them by the distance from the
 * customer. [today] gives the date to look for ("yyyy-MM-dd"); it is a parameter so a unit test can use a fixed date.
 */
class CustomerPackagesViewModel(
    private val packageRepository: PackageRepository,
    private val shopRepository: ShopRepository,
    private val today: () -> String = ::todayAsText
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerPackagesUiState())
    val uiState: StateFlow<CustomerPackagesUiState> = _uiState.asStateFlow()

    private var packages: List<FoodPackage> = emptyList()
    private var shops: List<Shop> = emptyList()
    private var location: Coordinates = LocationViewModel.DEFAULT_LOCATION

    /**
     * Reads the packages and the shops. Called when the customer opens the app or comes back to it,
     * so a package that was just put online shows up, and for "try again".
     */
    fun load() {
        viewModelScope.launch {
            try {
                coroutineScope {
                    val newPackages = async { packageRepository.visiblePackagesOn(today()) }
                    val newShops = async { shopRepository.allShops() }
                    packages = newPackages.await()
                    shops = newShops.await()
                }
                publish { it.copy(isLoading = false, errorMessage = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // An old list stays visible; only the first load fails "empty".
                publish { it.copy(isLoading = false, errorMessage = shopErrorMessageFor(e, R.string.error_customer_packages_load_failed)) }
            }
        }
    }

    /** The customer moved or chose another place: sort again by the new distance, without reading again. */
    fun onLocationChanged(coordinates: Coordinates) {
        if (coordinates == location) return
        location = coordinates
        publish { it }
    }

    /** Builds the offers for the current location and puts them in the state, after [change] changed the rest. */
    private fun publish(change: (CustomerPackagesUiState) -> CustomerPackagesUiState) {
        val offers = buildOffers(packages, shops, location)
        _uiState.update { change(it).copy(offers = offers, shops = groupByShop(offers)) }
    }

    companion object {
        /** Builds the ViewModel with Firestore. */
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer { CustomerPackagesViewModel(FirebasePackageRepository(), FirebaseShopRepository()) }
        }
    }
}
