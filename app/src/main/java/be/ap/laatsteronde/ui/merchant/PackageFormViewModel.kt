package be.ap.laatsteronde.ui.merchant

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.AndroidPhotoEncoder
import be.ap.laatsteronde.data.FirebasePackageRepository
import be.ap.laatsteronde.data.PHOTO_BASE64_MAX_LENGTH
import be.ap.laatsteronde.data.PackageDetails
import be.ap.laatsteronde.data.PackageRepository
import be.ap.laatsteronde.data.PhotoEncoder
import be.ap.laatsteronde.data.PickedPhoto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** The limits below are the same as in the Firestore rules. */
internal const val PACKAGE_TITLE_MAX_LENGTH = 60
internal const val PACKAGE_DESCRIPTION_MAX_LENGTH = 500
internal const val PACKAGE_QUANTITY_MAX = 999

/**
 * Everything the screen "Nieuw pakket" shows (Figma "Pakket aanmaken"). The number fields are
 * text, because the merchant types them; they are checked when [PackageFormViewModel.save] is called.
 * [saved] becomes true after a successful save: the screen then goes back to Vandaag.
 * [photoBase64] is the chosen photo, already small and in Base64 (H4); [isProcessingPhoto] is true while
 * it is being made smaller. The photo lives here, in the ViewModel, so it survives a rotation.
 */
data class PackageFormUiState(
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val originalValue: String = "",
    val quantity: String = "",
    val pickupFrom: String = "",
    val pickupTo: String = "",
    val photoBase64: String? = null,
    val isProcessingPhoto: Boolean = false,
    @StringRes val photoError: Int? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    @StringRes val titleError: Int? = null,
    @StringRes val descriptionError: Int? = null,
    @StringRes val priceError: Int? = null,
    @StringRes val originalValueError: Int? = null,
    @StringRes val quantityError: Int? = null,
    @StringRes val timeError: Int? = null,
    @StringRes val errorMessage: Int? = null
)

/**
 * Checks and saves a new package of the signed-in merchant. [today] gives the date of the
 * pick-up window ("yyyy-MM-dd") and [currentTime] the time now ("HH:mm"); they are parameters so a unit test
 * can use a fixed clock. [photoEncoder] makes a photo small and turns it into Base64.
 */
class PackageFormViewModel(
    private val packageRepository: PackageRepository,
    private val photoEncoder: PhotoEncoder,
    private val today: () -> String = ::todayAsText,
    private val currentTime: () -> String = ::timeNowAsText
) : ViewModel() {

    private val _uiState = MutableStateFlow(PackageFormUiState())
    val uiState: StateFlow<PackageFormUiState> = _uiState.asStateFlow()

    // Each change also clears the error of that field and the general message.
    fun onTitleChange(title: String) =
        _uiState.update { it.copy(title = title, titleError = null, errorMessage = null) }

    fun onDescriptionChange(description: String) =
        _uiState.update { it.copy(description = description, descriptionError = null, errorMessage = null) }

    // The price and the original value are compared with each other, so both errors go away.
    fun onPriceChange(price: String) =
        _uiState.update { it.copy(price = price, priceError = null, originalValueError = null, errorMessage = null) }

    fun onOriginalValueChange(originalValue: String) =
        _uiState.update {
            it.copy(originalValue = originalValue, priceError = null, originalValueError = null, errorMessage = null)
        }

    fun onQuantityChange(quantity: String) =
        _uiState.update { it.copy(quantity = quantity, quantityError = null, errorMessage = null) }

    fun onPickupFromChange(from: String) =
        _uiState.update { it.copy(pickupFrom = from, timeError = null, errorMessage = null) }

    fun onPickupToChange(to: String) =
        _uiState.update { it.copy(pickupTo = to, timeError = null, errorMessage = null) }

    /** The merchant took or chose a photo: make it small (on Dispatchers.Default, in the encoder) and keep it. */
    fun onPhotoPicked(photo: PickedPhoto) {
        _uiState.update { it.copy(isProcessingPhoto = true, photoError = null, errorMessage = null) }
        viewModelScope.launch {
            val base64 = photoEncoder.toBase64(photo)
            _uiState.update {
                when {
                    // The old photo (if any) stays when the new one cannot be used.
                    base64 == null -> it.copy(isProcessingPhoto = false, photoError = R.string.error_photo_unreadable)
                    base64.length > PHOTO_BASE64_MAX_LENGTH ->
                        it.copy(isProcessingPhoto = false, photoError = R.string.error_photo_too_large)
                    else -> it.copy(isProcessingPhoto = false, photoBase64 = base64)
                }
            }
        }
    }

    fun onPhotoRemoved() = _uiState.update { it.copy(photoBase64 = null, photoError = null) }

    /** The phone has no camera app to take the photo. */
    fun onCameraUnavailable() = _uiState.update { it.copy(photoError = R.string.error_camera_unavailable) }

    fun save() {
        val state = _uiState.value
        // While the photo is being made smaller, the package would be saved without it.
        if (state.isSaving || state.saved || state.isProcessingPhoto) return

        val title = state.title.trim()
        val description = state.description.trim()
        val priceCents = parseEuroToCents(state.price)
        val originalCents = parseEuroToCents(state.originalValue)
        val quantity = state.quantity.trim().toIntOrNull()?.takeIf { it in 1..PACKAGE_QUANTITY_MAX }
        val from = normalizeTime(state.pickupFrom)
        val to = normalizeTime(state.pickupTo)

        val titleError = when {
            title.isEmpty() -> R.string.error_package_title_empty
            title.length > PACKAGE_TITLE_MAX_LENGTH -> R.string.error_package_title_too_long
            else -> null
        }
        val descriptionError =
            if (description.length > PACKAGE_DESCRIPTION_MAX_LENGTH) R.string.error_package_description_too_long else null
        val originalError = if (originalCents == null) R.string.error_package_original_invalid else null
        // "Lower than the original value" is only checked when both amounts are valid.
        val priceError = when {
            priceCents == null -> R.string.error_package_price_invalid
            originalCents != null && priceCents >= originalCents -> R.string.error_package_price_not_lower
            else -> null
        }
        val quantityError = if (quantity == null) R.string.error_package_quantity_invalid else null
        val timeError = when {
            from == null || to == null -> R.string.error_package_time_invalid
            from >= to -> R.string.error_package_window_order // "HH:mm" sorts in the right order as text
            // A window that is already over can never be picked up (K3: customers only see windows that are not over).
            to <= currentTime() -> R.string.error_package_window_past
            else -> null
        }

        if (titleError != null || descriptionError != null || priceError != null || originalError != null ||
            quantityError != null || timeError != null ||
            priceCents == null || originalCents == null || quantity == null || from == null || to == null
        ) {
            _uiState.update {
                it.copy(
                    titleError = titleError,
                    descriptionError = descriptionError,
                    priceError = priceError,
                    originalValueError = originalError,
                    quantityError = quantityError,
                    timeError = timeError
                )
            }
            return
        }

        val details = PackageDetails(
            title = title,
            description = description,
            priceCents = priceCents,
            originalPriceCents = originalCents,
            quantity = quantity,
            pickupDate = today(),
            pickupFrom = from,
            pickupTo = to,
            photoBase64 = state.photoBase64
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                // Without internet Firestore keeps the write in a queue and waits for ever, so
                // after a while we tell the merchant instead of showing a spinner without end.
                withTimeout(SAVE_TIMEOUT_MILLIS) { packageRepository.createPackage(details) }
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: TimeoutCancellationException) {
                // Must come before CancellationException, which this one is a kind of.
                _uiState.update { it.copy(isSaving = false, errorMessage = R.string.error_no_internet) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = shopErrorMessageFor(e, R.string.error_package_save_failed))
                }
            }
        }
    }

    companion object {
        /** Builds the ViewModel with Firestore and the real photo encoder. */
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PackageFormViewModel(FirebasePackageRepository(), AndroidPhotoEncoder(context.applicationContext))
            }
        }
    }
}
