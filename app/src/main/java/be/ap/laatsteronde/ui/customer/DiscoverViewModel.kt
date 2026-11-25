package be.ap.laatsteronde.ui.customer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import be.ap.laatsteronde.data.AddressRepository
import be.ap.laatsteronde.data.CachedAddressRepository
import be.ap.laatsteronde.data.Coordinates
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The municipality shown in the title of the Discover screen. */
sealed interface MunicipalityState {
    /** Still looking it up. */
    data object Loading : MunicipalityState

    data class Found(val name: String) : MunicipalityState

    /** Not found, for example without internet and nothing saved yet. The title then has no name. */
    data object Unknown : MunicipalityState
}

/** Looks up the municipality for "Pakketten in de buurt van {gemeente}". */
class DiscoverViewModel(private val addressRepository: AddressRepository) : ViewModel() {

    private val _municipality = MutableStateFlow<MunicipalityState>(MunicipalityState.Loading)
    val municipality: StateFlow<MunicipalityState> = _municipality.asStateFlow()

    private var lastCoordinates: Coordinates? = null
    private var lookupJob: Job? = null

    /**
     * Called with the position of the customer. The same position twice does nothing, except
     * after a failed lookup: then it tries again, for example when the internet is back.
     */
    fun onLocationChanged(coordinates: Coordinates) {
        if (coordinates == lastCoordinates && _municipality.value !is MunicipalityState.Unknown) return
        lastCoordinates = coordinates
        lookupJob?.cancel()
        lookupJob = viewModelScope.launch {
            val name = addressRepository.municipalityAt(coordinates)
            _municipality.value = if (name != null) MunicipalityState.Found(name) else MunicipalityState.Unknown
        }
    }

    companion object {
        /** Builds the ViewModel with the real repository (Nominatim and Room). */
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { DiscoverViewModel(CachedAddressRepository.create(context.applicationContext)) }
        }
    }
}
