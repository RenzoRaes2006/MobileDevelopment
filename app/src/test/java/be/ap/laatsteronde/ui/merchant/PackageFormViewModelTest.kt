package be.ap.laatsteronde.ui.merchant

import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.FoodPackage
import be.ap.laatsteronde.data.PackageDetails
import be.ap.laatsteronde.data.PHOTO_BASE64_MAX_LENGTH
import be.ap.laatsteronde.data.PackageRepository
import be.ap.laatsteronde.data.PhotoEncoder
import be.ap.laatsteronde.data.PickedPhoto
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Tests [PackageFormViewModel] with a fake instead of Firebase. */
@OptIn(ExperimentalCoroutinesApi::class)
class PackageFormViewModelTest {

    private class FakePackageRepository : PackageRepository {
        var saveError: Exception? = null

        /** Like Firestore without internet: the save never finishes. */
        var saveHangs = false
        val created = mutableListOf<PackageDetails>()

        override suspend fun myPackagesOn(date: String): List<FoodPackage> = emptyList()

        override suspend fun visiblePackagesOn(date: String): List<FoodPackage> = emptyList()

        override suspend fun createPackage(details: PackageDetails) {
            if (saveHangs) awaitCancellation()
            saveError?.let { throw it }
            created += details
        }

        override suspend fun setVisible(packageId: String, visible: Boolean) = Unit
    }

    /** Gives a fixed text instead of scaling a real photo; [result] null means "cannot be read". */
    private class FakePhotoEncoder : PhotoEncoder {
        var result: String? = "BASE64PHOTO"

        /** Like a very large photo: the work never finishes. */
        var hangs = false
        override suspend fun toBase64(photo: PickedPhoto): String? {
            if (hangs) awaitCancellation()
            return result
        }
    }

    /** A Bitmap or Uri cannot be made in a JVM test; the fake encoder does not look at the photo anyway. */
    private object TestPhoto : PickedPhoto

    private lateinit var packages: FakePackageRepository
    private lateinit var photos: FakePhotoEncoder
    private val scheduler = TestCoroutineScheduler() // lets a test move the clock forward

    @Before
    fun setUp() {
        // viewModelScope runs on Dispatchers.Main, which does not exist in a plain JVM test.
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        packages = FakePackageRepository()
        photos = FakePhotoEncoder()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** The clock of the test: it is always 14:00, so the windows of the valid form (18:00 - 19:30) are still to come. */
    private var timeNow = "14:00"

    private fun viewModel() = PackageFormViewModel(packages, photos, today = { "2026-11-25" }, currentTime = { timeNow })

    /** A form as in the Figma, with the price lower than the original value. */
    private fun viewModelWithValidForm(): PackageFormViewModel = viewModel().apply {
        onTitleChange("Brood en koffiekoeken")
        onDescriptionChange("Een mix van brood en koffiekoeken van vandaag.")
        onPriceChange("4,00")
        onOriginalValueChange("12,00")
        onQuantityChange("10")
        onPickupFromChange("18:00")
        onPickupToChange("19:30")
    }

    @Test
    fun validForm_savesThePackageForToday() {
        val viewModel = viewModelWithValidForm()

        viewModel.save()

        assertEquals(
            listOf(
                PackageDetails(
                    title = "Brood en koffiekoeken",
                    description = "Een mix van brood en koffiekoeken van vandaag.",
                    priceCents = 400,
                    originalPriceCents = 1200,
                    quantity = 10,
                    pickupDate = "2026-11-25",
                    pickupFrom = "18:00",
                    pickupTo = "19:30"
                )
            ),
            packages.created
        )
        assertTrue(viewModel.uiState.value.saved)
        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun spacesAreTrimmed_andTheTimeIsNormalized() {
        val viewModel = viewModelWithValidForm()
        viewModel.onTitleChange("  Taart  ")
        viewModel.onPriceChange("3.5")
        viewModel.onPickupFromChange("9:05")

        viewModel.save()

        val details = packages.created.single()
        assertEquals("Taart", details.title)
        assertEquals(350, details.priceCents)
        assertEquals("09:05", details.pickupFrom)
    }

    @Test
    fun emptyDescription_isAllowed() {
        val viewModel = viewModelWithValidForm()
        viewModel.onDescriptionChange("")

        viewModel.save()

        assertEquals("", packages.created.single().description)
    }

    @Test
    fun emptyForm_showsAnErrorForEveryRequiredField_andSavesNothing() {
        val viewModel = viewModel()

        viewModel.save()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_package_title_empty, state.titleError)
        assertEquals(R.string.error_package_price_invalid, state.priceError)
        assertEquals(R.string.error_package_original_invalid, state.originalValueError)
        assertEquals(R.string.error_package_quantity_invalid, state.quantityError)
        assertEquals(R.string.error_package_time_invalid, state.timeError)
        assertTrue(packages.created.isEmpty())
        assertFalse(state.saved)
    }

    @Test
    fun priceHigherThanTheOriginalValue_isRefused() {
        val viewModel = viewModelWithValidForm()
        viewModel.onPriceChange("14,00")
        viewModel.onOriginalValueChange("12,00")

        viewModel.save()

        assertEquals(R.string.error_package_price_not_lower, viewModel.uiState.value.priceError)
        assertTrue(packages.created.isEmpty())
    }

    @Test
    fun priceEqualToTheOriginalValue_isRefused() {
        val viewModel = viewModelWithValidForm()
        viewModel.onPriceChange("12,00")

        viewModel.save()

        assertEquals(R.string.error_package_price_not_lower, viewModel.uiState.value.priceError)
    }

    @Test
    fun priceWithoutAValidOriginalValue_asksForTheOriginalValue() {
        val viewModel = viewModelWithValidForm()
        viewModel.onOriginalValueChange("abc")

        viewModel.save()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_package_original_invalid, state.originalValueError)
        assertNull(state.priceError) // the price itself is fine; we cannot compare yet
    }

    @Test
    fun invalidAmounts_areRefused() {
        val viewModel = viewModelWithValidForm()
        viewModel.onPriceChange("0")

        viewModel.save()

        assertEquals(R.string.error_package_price_invalid, viewModel.uiState.value.priceError)
    }

    @Test
    fun quantity_mustBeBetweenOneAndNineHundredNinetyNine() {
        val viewModel = viewModelWithValidForm()
        for (bad in listOf("0", "1000", "-3", "tien", "")) {
            viewModel.onQuantityChange(bad)
            viewModel.save()
            assertEquals("quantity '$bad'", R.string.error_package_quantity_invalid, viewModel.uiState.value.quantityError)
        }
        assertTrue(packages.created.isEmpty())

        viewModel.onQuantityChange("999")
        viewModel.save()
        assertEquals(999, packages.created.single().quantity)
    }

    @Test
    fun title_mustBeFilledInAndNotTooLong() {
        val viewModel = viewModelWithValidForm()
        viewModel.onTitleChange("   ")
        viewModel.save()
        assertEquals(R.string.error_package_title_empty, viewModel.uiState.value.titleError)

        viewModel.onTitleChange("x".repeat(PACKAGE_TITLE_MAX_LENGTH + 1))
        viewModel.save()
        assertEquals(R.string.error_package_title_too_long, viewModel.uiState.value.titleError)

        viewModel.onTitleChange("x".repeat(PACKAGE_TITLE_MAX_LENGTH))
        viewModel.save()
        assertEquals(1, packages.created.size)
    }

    @Test
    fun description_mayNotBeTooLong() {
        val viewModel = viewModelWithValidForm()
        viewModel.onDescriptionChange("x".repeat(PACKAGE_DESCRIPTION_MAX_LENGTH + 1))

        viewModel.save()

        assertEquals(R.string.error_package_description_too_long, viewModel.uiState.value.descriptionError)
        assertTrue(packages.created.isEmpty())
    }

    @Test
    fun timeThatIsNotATime_isRefused() {
        val viewModel = viewModelWithValidForm()
        viewModel.onPickupToChange("25:00")

        viewModel.save()

        assertEquals(R.string.error_package_time_invalid, viewModel.uiState.value.timeError)
    }

    @Test
    fun windowThatEndsBeforeOrAtTheStart_isRefused() {
        val viewModel = viewModelWithValidForm()
        viewModel.onPickupFromChange("19:30")
        viewModel.onPickupToChange("18:00")
        viewModel.save()
        assertEquals(R.string.error_package_window_order, viewModel.uiState.value.timeError)

        viewModel.onPickupToChange("19:30") // the same time: no window at all
        viewModel.save()
        assertEquals(R.string.error_package_window_order, viewModel.uiState.value.timeError)
        assertTrue(packages.created.isEmpty())
    }

    @Test
    fun windowThatIsAlreadyOver_isRefused() {
        val viewModel = viewModelWithValidForm()
        viewModel.onPickupFromChange("06:00")
        viewModel.onPickupToChange("07:00")

        viewModel.save()

        assertEquals(R.string.error_package_window_past, viewModel.uiState.value.timeError)
        assertTrue(packages.created.isEmpty())
    }

    @Test
    fun windowThatEndsRightNow_isRefused() {
        val viewModel = viewModelWithValidForm()
        viewModel.onPickupFromChange("12:00")
        viewModel.onPickupToChange("14:00") // it is 14:00: nobody can still pick it up

        viewModel.save()

        assertEquals(R.string.error_package_window_past, viewModel.uiState.value.timeError)
    }

    @Test
    fun windowThatAlreadyStartedButIsNotOver_isAllowed() {
        val viewModel = viewModelWithValidForm()
        viewModel.onPickupFromChange("13:00")
        viewModel.onPickupToChange("15:00")

        viewModel.save()

        assertNull(viewModel.uiState.value.timeError)
        assertEquals("15:00", packages.created.single().pickupTo)
    }

    @Test
    fun theClockIsReadWhenSaving_notWhenTheFormOpens() {
        val viewModel = viewModelWithValidForm() // 18:00 - 19:30, made at 14:00
        timeNow = "19:45" // the merchant waited too long before saving

        viewModel.save()

        assertEquals(R.string.error_package_window_past, viewModel.uiState.value.timeError)
    }

    @Test
    fun changingAField_clearsItsError() {
        val viewModel = viewModel()
        viewModel.save()
        assertEquals(R.string.error_package_title_empty, viewModel.uiState.value.titleError)

        viewModel.onTitleChange("Taart")

        assertNull(viewModel.uiState.value.titleError)
        assertEquals(R.string.error_package_price_invalid, viewModel.uiState.value.priceError) // others stay
    }

    @Test
    fun changingThePrice_clearsTheErrorOfTheOriginalValueToo() {
        val viewModel = viewModelWithValidForm()
        viewModel.onOriginalValueChange("abc")
        viewModel.save()
        assertEquals(R.string.error_package_original_invalid, viewModel.uiState.value.originalValueError)

        viewModel.onPriceChange("3,00")

        assertNull(viewModel.uiState.value.originalValueError)
    }

    @Test
    fun saveWithoutInternet_showsNoInternet_andKeepsTheForm() {
        packages.saveError = FirebaseNetworkException("offline")
        val viewModel = viewModelWithValidForm()

        viewModel.save()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_no_internet, state.errorMessage)
        assertFalse(state.isSaving)
        assertFalse(state.saved)
        assertEquals("Brood en koffiekoeken", state.title)
    }

    @Test
    fun saveThatNeverFinishes_waitsAndThenShowsNoInternet() {
        packages.saveHangs = true
        val viewModel = viewModelWithValidForm()

        viewModel.save()
        assertTrue(viewModel.uiState.value.isSaving) // still waiting for Firestore

        scheduler.advanceTimeBy(SAVE_TIMEOUT_MILLIS + 1)
        scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_no_internet, state.errorMessage)
        assertFalse(state.isSaving) // the merchant can try again
        assertFalse(state.saved)
    }

    @Test
    fun saveFailsForAnotherReason_showsTheSaveError() {
        packages.saveError = IllegalStateException("permission denied")
        val viewModel = viewModelWithValidForm()

        viewModel.save()

        assertEquals(R.string.error_package_save_failed, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun afterAFailure_tryingAgainSaves() {
        packages.saveError = IllegalStateException("temporary")
        val viewModel = viewModelWithValidForm()
        viewModel.save()

        packages.saveError = null
        viewModel.save()

        assertEquals(1, packages.created.size)
        assertTrue(viewModel.uiState.value.saved)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun aSavedForm_isNotSavedTwice() {
        val viewModel = viewModelWithValidForm()
        viewModel.save()

        viewModel.save()

        assertEquals(1, packages.created.size)
    }

    // The photo (H4)

    @Test
    fun withoutPhoto_savesThePackageWithoutPhoto() {
        val viewModel = viewModelWithValidForm()

        viewModel.save()

        assertNull(packages.created.single().photoBase64)
    }

    @Test
    fun photoPicked_isShownAndSavedWithThePackage() {
        val viewModel = viewModelWithValidForm()

        viewModel.onPhotoPicked(TestPhoto)
        assertEquals("BASE64PHOTO", viewModel.uiState.value.photoBase64)
        assertFalse(viewModel.uiState.value.isProcessingPhoto)

        viewModel.save()
        assertEquals("BASE64PHOTO", packages.created.single().photoBase64)
    }

    @Test
    fun photoThatCannotBeRead_givesAMessageAndKeepsTheOldPhoto() {
        val viewModel = viewModelWithValidForm()
        viewModel.onPhotoPicked(TestPhoto)
        photos.result = null

        viewModel.onPhotoPicked(TestPhoto)

        assertEquals(R.string.error_photo_unreadable, viewModel.uiState.value.photoError)
        assertEquals("BASE64PHOTO", viewModel.uiState.value.photoBase64)
    }

    @Test
    fun photoTooLarge_givesAMessageAndIsNotKept() {
        val viewModel = viewModelWithValidForm()
        photos.result = "A".repeat(PHOTO_BASE64_MAX_LENGTH + 1)

        viewModel.onPhotoPicked(TestPhoto)

        assertEquals(R.string.error_photo_too_large, viewModel.uiState.value.photoError)
        assertNull(viewModel.uiState.value.photoBase64)
    }

    @Test
    fun removePhoto_savesThePackageWithoutPhoto() {
        val viewModel = viewModelWithValidForm()
        viewModel.onPhotoPicked(TestPhoto)

        viewModel.onPhotoRemoved()
        viewModel.save()

        assertNull(viewModel.uiState.value.photoBase64)
        assertNull(packages.created.single().photoBase64)
    }

    @Test
    fun noCameraApp_givesAMessage() {
        val viewModel = viewModel()

        viewModel.onCameraUnavailable()

        assertEquals(R.string.error_camera_unavailable, viewModel.uiState.value.photoError)
    }

    @Test
    fun aNewPhoto_clearsTheOldPhotoMessage() {
        val viewModel = viewModel()
        viewModel.onCameraUnavailable()

        viewModel.onPhotoPicked(TestPhoto)

        assertNull(viewModel.uiState.value.photoError)
    }

    @Test
    fun whileThePhotoIsBeingMadeSmaller_saveWaits() {
        val viewModel = viewModelWithValidForm()
        photos.hangs = true

        viewModel.onPhotoPicked(TestPhoto)
        viewModel.save()

        assertTrue(viewModel.uiState.value.isProcessingPhoto)
        assertTrue(packages.created.isEmpty())
    }
}
