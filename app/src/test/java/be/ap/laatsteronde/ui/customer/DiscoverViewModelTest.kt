package be.ap.laatsteronde.ui.customer

import be.ap.laatsteronde.data.AddressRepository
import be.ap.laatsteronde.data.Coordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Tests [DiscoverViewModel] with a fake [AddressRepository] instead of Nominatim and Room. */
@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {

    private class FakeAddressRepository(var answer: String? = "Antwerpen") : AddressRepository {
        val asked = mutableListOf<Coordinates>()
        override suspend fun municipalityAt(coordinates: Coordinates): String? {
            asked += coordinates
            return answer
        }

        override suspend fun addressAt(coordinates: Coordinates): String? = null
    }

    private val antwerp = Coordinates(latitude = 51.23, longitude = 4.41)
    private val blaasveld = Coordinates(latitude = 51.057, longitude = 4.373)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsLoading() {
        val viewModel = DiscoverViewModel(FakeAddressRepository())

        assertEquals(MunicipalityState.Loading, viewModel.municipality.value)
    }

    @Test
    fun knownPlace_showsTheMunicipality() {
        val viewModel = DiscoverViewModel(FakeAddressRepository(answer = "Antwerpen"))

        viewModel.onLocationChanged(antwerp)

        assertEquals(MunicipalityState.Found("Antwerpen"), viewModel.municipality.value)
    }

    @Test
    fun unknownPlace_givesUnknown() {
        val viewModel = DiscoverViewModel(FakeAddressRepository(answer = null))

        viewModel.onLocationChanged(antwerp)

        assertEquals(MunicipalityState.Unknown, viewModel.municipality.value)
    }

    @Test
    fun samePositionTwice_isLookedUpOnce() {
        val repository = FakeAddressRepository()
        val viewModel = DiscoverViewModel(repository)

        viewModel.onLocationChanged(antwerp)
        viewModel.onLocationChanged(antwerp)

        assertEquals(listOf(antwerp), repository.asked)
    }

    @Test
    fun afterAFailedLookup_samePositionIsTriedAgain() {
        val repository = FakeAddressRepository(answer = null)
        val viewModel = DiscoverViewModel(repository)
        viewModel.onLocationChanged(antwerp)
        repository.answer = "Antwerpen" // the internet is back

        viewModel.onLocationChanged(antwerp)

        assertEquals(MunicipalityState.Found("Antwerpen"), viewModel.municipality.value)
        assertEquals(2, repository.asked.size)
    }

    @Test
    fun customerMoves_showsTheNewMunicipality() {
        val repository = FakeAddressRepository(answer = "Antwerpen")
        val viewModel = DiscoverViewModel(repository)
        viewModel.onLocationChanged(antwerp)
        repository.answer = "Willebroek"

        viewModel.onLocationChanged(blaasveld)

        assertEquals(MunicipalityState.Found("Willebroek"), viewModel.municipality.value)
    }
}
