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
    fun permissionGrantedButNoPosition_fallsBackToDefaultLocation() {
        val viewModel = LocationViewModel(FakeLocationProvider(result = null))

        viewModel.onPermissionResult(granted = true)

        assertEquals(LocationStatus.UNAVAILABLE, viewModel.uiState.value.status)
        assertEquals(LocationViewModel.DEFAULT_LOCATION, viewModel.uiState.value.location)
    }
}
