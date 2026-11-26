package be.ap.laatsteronde.ui.customer

import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.data.LocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Tests [LocationViewModel] with a fake location provider instead of Google Play services. */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationViewModelTest {

    private class FakeLocationProvider(var result: Coordinates?) : LocationProvider {
        var calls = 0
        override suspend fun currentLocation(): Coordinates? {
            calls++
            return result
        }
    }

    private val customerPosition = Coordinates(latitude = 51.2, longitude = 4.4)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsUnknownWithDefaultLocation() {
        val viewModel = LocationViewModel(FakeLocationProvider(customerPosition))

        assertEquals(LocationStatus.UNKNOWN, viewModel.uiState.value.status)
        assertEquals(LocationViewModel.DEFAULT_LOCATION, viewModel.uiState.value.location)
    }

    @Test
    fun permissionGranted_showsRealPosition() {
        val viewModel = LocationViewModel(FakeLocationProvider(customerPosition))

        viewModel.onPermissionResult(granted = true)

        assertEquals(LocationStatus.FOUND, viewModel.uiState.value.status)
        assertEquals(customerPosition, viewModel.uiState.value.location)
    }

    @Test
    fun permissionDenied_keepsDefaultLocationAndDoesNotAskTheProvider() {
        val provider = FakeLocationProvider(customerPosition)
        val viewModel = LocationViewModel(provider)

        viewModel.onPermissionResult(granted = false)

        assertEquals(LocationStatus.DENIED, viewModel.uiState.value.status)
        assertEquals(LocationViewModel.DEFAULT_LOCATION, viewModel.uiState.value.location)
        assertEquals(0, provider.calls)
    }

    @Test
    fun refresh_afterCustomerMoved_showsNewPosition() {
        val provider = FakeLocationProvider(customerPosition)
        val viewModel = LocationViewModel(provider)
        viewModel.onPermissionResult(granted = true)
        val newPosition = Coordinates(latitude = 51.06, longitude = 4.36)
        provider.result = newPosition

        viewModel.refresh()

        assertEquals(LocationStatus.FOUND, viewModel.uiState.value.status)
        assertEquals(newPosition, viewModel.uiState.value.location)
    }

    @Test
    fun refresh_afterNoPosition_findsPositionWhenItBecomesAvailable() {
        val provider = FakeLocationProvider(result = null)
        val viewModel = LocationViewModel(provider)
        viewModel.onPermissionResult(granted = true)
        provider.result = customerPosition

        viewModel.refresh()

        assertEquals(LocationStatus.FOUND, viewModel.uiState.value.status)
        assertEquals(customerPosition, viewModel.uiState.value.location)
    }

    @Test
    fun refresh_withoutNewPosition_keepsTheOldPosition() {
        val provider = FakeLocationProvider(customerPosition)
        val viewModel = LocationViewModel(provider)
        viewModel.onPermissionResult(granted = true)
        provider.result = null

        viewModel.refresh()

        assertEquals(LocationStatus.FOUND, viewModel.uiState.value.status)
        assertEquals(customerPosition, viewModel.uiState.value.location)
    }

    @Test
    fun refresh_beforeAnswerOrWhenDenied_doesNotAskTheProvider() {
        val provider = FakeLocationProvider(customerPosition)
        val viewModel = LocationViewModel(provider)

        viewModel.refresh() // permission not answered yet
        viewModel.onPermissionResult(granted = false)
        viewModel.refresh() // denied

        assertEquals(0, provider.calls)
        assertEquals(LocationStatus.DENIED, viewModel.uiState.value.status)
    }

    @Test
    fun customerChoosesAPlace_thatPlaceIsUsed() {
        val viewModel = LocationViewModel(FakeLocationProvider(customerPosition))
        viewModel.onPermissionResult(granted = false)
        val chosen = Coordinates(latitude = 51.0, longitude = 4.0)

        viewModel.onLocationChosen(chosen)

        assertEquals(LocationStatus.CHOSEN, viewModel.uiState.value.status)
        assertEquals(chosen, viewModel.uiState.value.location)
    }

    @Test
    fun refresh_doesNotReplaceAChosenPlace() {
        val provider = FakeLocationProvider(customerPosition)
        val viewModel = LocationViewModel(provider)
        val chosen = Coordinates(latitude = 51.0, longitude = 4.0)
        viewModel.onLocationChosen(chosen)

        viewModel.refresh()

        assertEquals(chosen, viewModel.uiState.value.location)
        assertEquals(0, provider.calls)
    }

    @Test
    fun changeLocation_keepsThePlaceButLetsTheCustomerChooseAnother() {
        val provider = FakeLocationProvider(customerPosition)
        val viewModel = LocationViewModel(provider)
        viewModel.onPermissionResult(granted = true)

        viewModel.onChangeLocation()

        assertEquals(LocationStatus.CHOSEN, viewModel.uiState.value.status)
        assertEquals(customerPosition, viewModel.uiState.value.location) // still where the customer is
        viewModel.refresh()
        assertEquals(1, provider.calls) // a refresh does not replace the chosen mode
    }

    @Test
    fun askAgain_startsOverSoTheExplanationComesBack() {
        val viewModel = LocationViewModel(FakeLocationProvider(customerPosition))
        viewModel.onLocationChosen(Coordinates(latitude = 51.0, longitude = 4.0))

        viewModel.onAskAgain()

        assertEquals(LocationStatus.UNKNOWN, viewModel.uiState.value.status)
        assertEquals(LocationViewModel.DEFAULT_LOCATION, viewModel.uiState.value.location)
    }

    @Test
    fun permissionGrantedButNoPosition_fallsBackToDefaultLocation() {
        val viewModel = LocationViewModel(FakeLocationProvider(result = null))

        viewModel.onPermissionResult(granted = true)

        assertEquals(LocationStatus.UNAVAILABLE, viewModel.uiState.value.status)
        assertEquals(LocationViewModel.DEFAULT_LOCATION, viewModel.uiState.value.location)
    }

    // The four cases of the permission that the assignment asks for: precise, only approximate,
    // denied, and denied for good (only the app settings can still give it).

    @Test
    fun preciseLocationGranted_isMarkedPrecise() {
        val viewModel = LocationViewModel(FakeLocationProvider(customerPosition))

        viewModel.onPermissionResult(granted = true, precise = true)

        assertEquals(LocationStatus.FOUND, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.precise)
    }

    @Test
    fun onlyApproximateLocationGranted_findsThePositionButIsMarkedApproximate() {
        val viewModel = LocationViewModel(FakeLocationProvider(customerPosition))

        viewModel.onPermissionResult(granted = true, precise = false)

        assertEquals(LocationStatus.FOUND, viewModel.uiState.value.status)
        assertEquals(customerPosition, viewModel.uiState.value.location)
        assertFalse(viewModel.uiState.value.precise)
    }

    @Test
    fun approximate_staysApproximateAfterARefresh() {
        val provider = FakeLocationProvider(customerPosition)
        val viewModel = LocationViewModel(provider)
        viewModel.onPermissionResult(granted = true, precise = false)

        viewModel.refresh()

        assertEquals(2, provider.calls)
        assertFalse(viewModel.uiState.value.precise)
    }

    @Test
    fun deniedOnce_canStillBeAskedAgain() {
        val viewModel = LocationViewModel(FakeLocationProvider(customerPosition))

        viewModel.onPermissionResult(granted = false, canAskAgain = true)

        assertEquals(LocationStatus.DENIED, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.blockedInSettings)
    }

    @Test
    fun deniedForGood_isBlockedInSettings_alsoAfterAskingAgainOrChoosingAPlace() {
        val viewModel = LocationViewModel(FakeLocationProvider(customerPosition))

        viewModel.onPermissionResult(granted = false, canAskAgain = false)
        assertTrue(viewModel.uiState.value.blockedInSettings)

        viewModel.onLocationChosen(Coordinates(latitude = 51.0, longitude = 4.0))
        assertTrue(viewModel.uiState.value.blockedInSettings)

        // "Naar mijn locatie": the explanation comes back, but it must send the customer to the settings.
        viewModel.onAskAgain()
        assertEquals(LocationStatus.UNKNOWN, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.blockedInSettings)
    }

    @Test
    fun backFromSettingsWithPermission_looksUpThePosition() {
        val provider = FakeLocationProvider(customerPosition)
        val viewModel = LocationViewModel(provider)
        viewModel.onPermissionResult(granted = false, canAskAgain = false)

        viewModel.onPermissionChecked(granted = true, precise = true)

        assertEquals(LocationStatus.FOUND, viewModel.uiState.value.status)
        assertEquals(customerPosition, viewModel.uiState.value.location)
        assertTrue(viewModel.uiState.value.precise)
        assertFalse(viewModel.uiState.value.blockedInSettings)
    }

    @Test
    fun backFromSettingsWithPreciseLocation_isNoLongerApproximate() {
        val viewModel = LocationViewModel(FakeLocationProvider(customerPosition))
        viewModel.onPermissionResult(granted = true, precise = false)

        viewModel.onPermissionChecked(granted = true, precise = true)

        assertTrue(viewModel.uiState.value.precise)
    }

    @Test
    fun permissionTakenAwayInSettings_isDenied() {
        val viewModel = LocationViewModel(FakeLocationProvider(customerPosition))
        viewModel.onPermissionResult(granted = true)

        viewModel.onPermissionChecked(granted = false, precise = false)

        assertEquals(LocationStatus.DENIED, viewModel.uiState.value.status)
        assertEquals(LocationViewModel.DEFAULT_LOCATION, viewModel.uiState.value.location)
    }

    @Test
    fun permissionChecked_beforeTheQuestionOrWithAChosenPlace_changesNothing() {
        val provider = FakeLocationProvider(customerPosition)
        val viewModel = LocationViewModel(provider)

        viewModel.onPermissionChecked(granted = false, precise = false) // explanation still open
        assertEquals(LocationStatus.UNKNOWN, viewModel.uiState.value.status)

        val chosen = Coordinates(latitude = 51.0, longitude = 4.0)
        viewModel.onLocationChosen(chosen)
        viewModel.onPermissionChecked(granted = true, precise = true)
        assertEquals(LocationStatus.CHOSEN, viewModel.uiState.value.status)
        assertEquals(chosen, viewModel.uiState.value.location)
        assertEquals(0, provider.calls)
    }
}
