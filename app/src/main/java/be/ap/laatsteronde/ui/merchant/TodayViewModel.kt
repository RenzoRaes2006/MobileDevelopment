package be.ap.laatsteronde.ui.merchant

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.FirebasePackageRepository
import be.ap.laatsteronde.data.FoodPackage
import be.ap.laatsteronde.data.PackageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Everything "Vandaag" shows (Figma "Vandaag"): the packages of today of the merchant.
 * [isLoading] is only true until the first answer; a refresh later keeps showing the list.
 */
data class TodayUiState(
    val isLoading: Boolean = true,
    val packages: List<FoodPackage> = emptyList(),
    @StringRes val errorMessage: Int? = null
) {
    /** The latest end of a pick-up window of today ("sluit om 19:30"), or null without packages. */
    val closingTime: String? get() = packages.maxOfOrNull { it.details.pickupTo }
}

/**
 * Shows the packages of today and switches them visible or offline for customers.
 * [today] gives the date to look for ("yyyy-MM-dd"); it is a parameter so a unit test can use a fixed date.
 */
class TodayViewModel(
    private val packageRepository: PackageRepository,
    private val today: () -> String = ::todayAsText
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    /**
     * Reads the packages of today. Called when the screen opens, when the merchant comes back
     * (for example from "Nieuw pakket") and for "opnieuw proberen".
     */
    fun load() {
        viewModelScope.launch {
            try {
                val packages = packageRepository.myPackagesOn(today())
                _uiState.update { it.copy(isLoading = false, packages = packages, errorMessage = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // An old list stays visible; only the first load fails "empty".
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = shopErrorMessageFor(e, R.string.error_package_load_failed))
                }
            }
        }
    }

    /**
     * The switch "Zichtbaar voor klanten" was tapped. The switch changes at once; when saving fails
     * (no internet, no rights) it goes back and the merchant sees why.
     */
    fun onVisibleChange(packageId: String, visible: Boolean) {
        val before = _uiState.value.packages.firstOrNull { it.id == packageId } ?: return
        if (before.visible == visible) return
        setVisibleInList(packageId, visible)
        viewModelScope.launch {
            try {
                // Without internet Firestore keeps the write in a queue and waits for ever.
                withTimeout(SAVE_TIMEOUT_MILLIS) { packageRepository.setVisible(packageId, visible) }
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: TimeoutCancellationException) {
                // Must come before CancellationException, which this one is a kind of.
                setVisibleInList(packageId, before.visible)
                _uiState.update { it.copy(errorMessage = R.string.error_no_internet) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setVisibleInList(packageId, before.visible)
                _uiState.update {
                    it.copy(errorMessage = shopErrorMessageFor(e, R.string.error_package_visibility_failed))
                }
            }
        }
    }

    private fun setVisibleInList(packageId: String, visible: Boolean) = _uiState.update { state ->
        state.copy(packages = state.packages.map { if (it.id == packageId) it.copy(visible = visible) else it })
    }

    companion object {
        /** Builds the ViewModel with Firestore. */
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer { TodayViewModel(FirebasePackageRepository()) }
        }
    }
}
