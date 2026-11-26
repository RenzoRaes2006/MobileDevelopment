package be.ap.laatsteronde.ui.customer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.data.FusedLocationProvider
import be.ap.laatsteronde.data.LocationProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where we are with the location of the customer. */
enum class LocationStatus {
    /** We did not ask the permission yet (or the answer is still coming). */
    UNKNOWN,

    /** Permission granted, busy looking for the position. */
    LOADING,

    /** The real position of the customer is known. */
    FOUND,

    /** The customer said no; the map shows the default location. */
    DENIED,

    /** Permission granted, but the position could not be found; the map shows the default location. */
    UNAVAILABLE,

    /** The customer chose a place on the map ("Zelf een locatie kiezen") instead of sharing the position. */
    CHOSEN
}

/**
 * [location] is always filled: the real position when [status] is FOUND,
 * otherwise [DEFAULT_LOCATION], so the map always has something to show.
 *
 * [precise] is false when the customer only allowed the approximate location ("Bij benadering"):
 * the position can then be a few kilometres off. [blockedInSettings] is true when Android no longer
 * shows the question (denied for good): only the settings of the app can still give the permission.
 */
data class LocationUiState(
    val status: LocationStatus = LocationStatus.UNKNOWN,
    val location: Coordinates = LocationViewModel.DEFAULT_LOCATION,
    val precise: Boolean = true,
    val blockedInSettings: Boolean = false
)

/** Remembers the answer to the location question and the position of the customer. */
class LocationViewModel(private val locationProvider: LocationProvider) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    /**
     * Called with the answer of the permission dialog (or true when the permission was already there).
     * [precise] is false when only the approximate location was allowed. [canAskAgain] is false when
     * the customer denied it for good: Android will not show the question again.
     */
    fun onPermissionResult(granted: Boolean, precise: Boolean = granted, canAskAgain: Boolean = true) {
        if (!granted) {
            fetchJob?.cancel()
            _uiState.value = LocationUiState(status = LocationStatus.DENIED, blockedInSettings = !canAskAgain)
            return
        }
        _uiState.update { it.copy(status = LocationStatus.LOADING, precise = precise, blockedInSettings = false) }
        fetchLocation()
    }

    /**
     * Called every time the app comes back to the front with the permission as it is now: the customer may have
     * changed it in the settings of the app (allowed it, taken it away, or switched on the precise location).
     */
    fun onPermissionChecked(granted: Boolean, precise: Boolean) {
        when (_uiState.value.status) {
            // Back from the settings with the permission: look up the position.
            LocationStatus.DENIED -> if (granted) onPermissionResult(granted = true, precise = precise)
            LocationStatus.FOUND, LocationStatus.UNAVAILABLE, LocationStatus.LOADING ->
                if (granted) {
                    _uiState.update { it.copy(precise = precise) }
                    refresh() // the customer may have moved
                } else {
                    // The permission was taken away in the settings.
                    onPermissionResult(granted = false)
                }
            // The question is not answered yet, or the customer chose a place: that stays.
            LocationStatus.UNKNOWN, LocationStatus.CHOSEN -> Unit
        }
    }

    /** The customer tapped a place on the map: that place is used instead of the real position. */
    fun onLocationChosen(position: Coordinates) {
        fetchJob?.cancel()
        _uiState.update { it.copy(status = LocationStatus.CHOSEN, location = position) }
    }

    /**
     * The customer wants another place (the municipality on Discover): from now on a tap on the map
     * chooses the place. The current place stays until the customer taps.
     */
    fun onChangeLocation() {
        fetchJob?.cancel()
        _uiState.update { it.copy(status = LocationStatus.CHOSEN) }
    }

    /**
     * "Naar mijn locatie" after a "no" or a chosen place: start again, so the explanation and the question come back.
     * A permission that was denied for good stays so: the explanation then sends the customer to the settings.
     */
    fun onAskAgain() {
        fetchJob?.cancel()
        _uiState.update { LocationUiState(blockedInSettings = it.blockedInSettings) }
    }

    /**
     * Looks up the position again, because the customer may have moved. Does nothing as long as
     * the permission was not answered yet or was denied. The old position stays on the map
     * while we look, and when the new lookup finds nothing.
     */
    fun refresh() {
        val status = _uiState.value.status
        if (status == LocationStatus.FOUND || status == LocationStatus.UNAVAILABLE) {
            fetchLocation()
        }
    }

    private fun fetchLocation() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            val found = locationProvider.currentLocation()
            _uiState.update { current ->
                when {
                    found != null -> current.copy(status = LocationStatus.FOUND, location = found)
                    current.status == LocationStatus.FOUND -> current
                    else -> current.copy(status = LocationStatus.UNAVAILABLE, location = DEFAULT_LOCATION)
                }
            }
        }
    }

    companion object {
        /** Builds the ViewModel with the real (Google) location provider. */
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { LocationViewModel(FusedLocationProvider(context.applicationContext)) }
        }

        /** The AP Hogeschool in Antwerp, shown when we have no position of the customer. */
        val DEFAULT_LOCATION = Coordinates(latitude = 51.230167, longitude = 4.416129)
    }
}
