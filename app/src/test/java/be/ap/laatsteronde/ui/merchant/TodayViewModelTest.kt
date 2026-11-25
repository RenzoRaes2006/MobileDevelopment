package be.ap.laatsteronde.ui.merchant

import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.FoodPackage
import be.ap.laatsteronde.data.PackageDetails
import be.ap.laatsteronde.data.PackageRepository
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

/** Tests [TodayViewModel] with a fake instead of Firebase. */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private class FakePackageRepository(var packages: List<FoodPackage> = emptyList()) : PackageRepository {
        var loadError: Exception? = null
        var visibleError: Exception? = null

        /** Like Firestore without internet: the change never finishes. */
        var visibleHangs = false
        val askedDates = mutableListOf<String>()
        val visibleCalls = mutableListOf<Pair<String, Boolean>>()

        override suspend fun myPackagesOn(date: String): List<FoodPackage> {
            askedDates += date
            loadError?.let { throw it }
            return packages
        }

        override suspend fun createPackage(details: PackageDetails) = Unit

        override suspend fun setVisible(packageId: String, visible: Boolean) {
            if (visibleHangs) awaitCancellation()
            visibleError?.let { throw it }
            visibleCalls += packageId to visible
        }
    }

    private fun pack(id: String, title: String, to: String, visible: Boolean = true) = FoodPackage(
        id = id,
        ownerId = "uid",
        details = PackageDetails(title, "", 400, 1200, 10, "2026-11-25", "18:00", to),
        visible = visible
    )

    private val bread = pack("1", "Brood en koffiekoeken", to = "19:30")
    private val cake = pack("2", "Taart van de dag", to = "19:00", visible = false)

    private lateinit var packages: FakePackageRepository
    private val scheduler = TestCoroutineScheduler() // lets a test move the clock forward

    @Before
    fun setUp() {
        // viewModelScope runs on Dispatchers.Main, which does not exist in a plain JVM test.
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        packages = FakePackageRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = TodayViewModel(packages, today = { "2026-11-25" })

    @Test
    fun beforeTheFirstLoad_itIsLoading() {
        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.packages.isEmpty())
    }

    @Test
    fun load_showsThePackagesOfToday() {
        packages.packages = listOf(bread, cake)
        val viewModel = viewModel()

        viewModel.load()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(bread, cake), state.packages)
        assertNull(state.errorMessage)
        assertEquals(listOf("2026-11-25"), packages.askedDates) // only today
    }

    @Test
    fun load_withoutPackages_givesAnEmptyList() {
        val viewModel = viewModel()

        viewModel.load()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.packages.isEmpty())
        assertNull(viewModel.uiState.value.closingTime)
    }

    @Test
    fun closingTime_isTheLatestEndOfAPickUpWindow() {
        packages.packages = listOf(cake, bread) // 19:00 and 19:30
        val viewModel = viewModel()

        viewModel.load()

        assertEquals("19:30", viewModel.uiState.value.closingTime)
    }

    @Test
    fun loadFailsWithoutInternet_showsNoInternet_andTryingAgainWorks() {
        packages.loadError = FirebaseNetworkException("offline")
        val viewModel = viewModel()

        viewModel.load()
        assertEquals(R.string.error_no_internet, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)

        packages.loadError = null
        packages.packages = listOf(bread)
        viewModel.load()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf(bread), viewModel.uiState.value.packages)
    }

    @Test
    fun loadFailsForAnotherReason_showsTheLoadError() {
        packages.loadError = IllegalStateException("boom")
        val viewModel = viewModel()

        viewModel.load()

        assertEquals(R.string.error_package_load_failed, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun loadingAgain_keepsTheListWhenItFails() {
        packages.packages = listOf(bread)
        val viewModel = viewModel()
        viewModel.load()

        packages.loadError = FirebaseNetworkException("offline")
        viewModel.load()

        assertEquals(listOf(bread), viewModel.uiState.value.packages) // the old list is not thrown away
        assertEquals(R.string.error_no_internet, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun loadingAgain_showsANewPackage() {
        packages.packages = listOf(bread)
        val viewModel = viewModel()
        viewModel.load()

        packages.packages = listOf(bread, cake) // made on "Nieuw pakket"
        viewModel.load()

        assertEquals(listOf(bread, cake), viewModel.uiState.value.packages)
    }

    @Test
    fun switchingOff_savesAndShowsTheNewState() {
        packages.packages = listOf(bread, cake)
        val viewModel = viewModel()
        viewModel.load()

        viewModel.onVisibleChange("1", false)

        assertEquals(listOf("1" to false), packages.visibleCalls)
        assertFalse(viewModel.uiState.value.packages.first { it.id == "1" }.visible)
        assertFalse(viewModel.uiState.value.packages.first { it.id == "2" }.visible) // the other stays
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun switchingOn_savesAndShowsTheNewState() {
        packages.packages = listOf(bread, cake)
        val viewModel = viewModel()
        viewModel.load()

        viewModel.onVisibleChange("2", true)

        assertEquals(listOf("2" to true), packages.visibleCalls)
        assertTrue(viewModel.uiState.value.packages.first { it.id == "2" }.visible)
    }

    @Test
    fun switchingToTheSameState_doesNothing() {
        packages.packages = listOf(bread)
        val viewModel = viewModel()
        viewModel.load()

        viewModel.onVisibleChange("1", true) // is visible already

        assertTrue(packages.visibleCalls.isEmpty())
    }

    @Test
    fun switchingAnUnknownPackage_doesNothing() {
        packages.packages = listOf(bread)
        val viewModel = viewModel()
        viewModel.load()

        viewModel.onVisibleChange("nope", false)

        assertTrue(packages.visibleCalls.isEmpty())
        assertEquals(listOf(bread), viewModel.uiState.value.packages)
    }

    @Test
    fun switchWithoutInternet_goesBack_andShowsNoInternet() {
        packages.packages = listOf(bread)
        val viewModel = viewModel()
        viewModel.load()
        packages.visibleError = FirebaseNetworkException("offline")

        viewModel.onVisibleChange("1", false)

        assertTrue(viewModel.uiState.value.packages.single().visible) // back to how it was
        assertEquals(R.string.error_no_internet, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun switchThatNeverFinishes_goesBackAfterTheTimeLimit() {
        packages.packages = listOf(bread)
        val viewModel = viewModel()
        viewModel.load()
        packages.visibleHangs = true

        viewModel.onVisibleChange("1", false)
        assertFalse(viewModel.uiState.value.packages.single().visible) // changes at once

        scheduler.advanceTimeBy(SAVE_TIMEOUT_MILLIS + 1)
        scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.packages.single().visible)
        assertEquals(R.string.error_no_internet, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun switchFailsForAnotherReason_goesBack_andShowsTheVisibilityError() {
        packages.packages = listOf(cake)
        val viewModel = viewModel()
        viewModel.load()
        packages.visibleError = IllegalStateException("permission denied")

        viewModel.onVisibleChange("2", true)

        assertFalse(viewModel.uiState.value.packages.single().visible)
        assertEquals(R.string.error_package_visibility_failed, viewModel.uiState.value.errorMessage)
    }
}
