package be.ap.laatsteronde.data

import be.ap.laatsteronde.data.local.AddressDao
import be.ap.laatsteronde.data.local.CachedAddress
import be.ap.laatsteronde.data.remote.NominatimAddress
import be.ap.laatsteronde.data.remote.NominatimApi
import be.ap.laatsteronde.data.remote.NominatimResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Tests the cache logic with a fake database and a fake Nominatim, so no phone or internet is needed. */
class CachedAddressRepositoryTest {

    private class FakeDao : AddressDao {
        val rows = mutableMapOf<String, CachedAddress>()
        override suspend fun find(locationKey: String) = rows[locationKey]
        override suspend fun save(address: CachedAddress) {
            rows[address.locationKey] = address
        }
    }

    /** Answers with [municipality], or throws [error] when that is set. Counts the calls. */
    private class FakeApi(var municipality: String? = "Antwerpen", var error: Exception? = null) : NominatimApi {
        var calls = 0
        var lastZoom = 0
        var road: String? = null
        var houseNumber: String? = null
        var postcode: String? = null
        override suspend fun reverse(
            latitude: Double,
            longitude: Double,
            format: String,
            addressDetails: Int,
            zoom: Int
        ): NominatimResponse {
            calls++
            lastZoom = zoom
            error?.let { throw it }
            return NominatimResponse(
                NominatimAddress(
                    municipality = municipality, city = null, town = null, village = null,
                    road = road, houseNumber = houseNumber, postcode = postcode
                )
            )
        }
    }

    private val place = Coordinates(latitude = 51.230167, longitude = 4.416129)
    private var clock = 1_000_000L
    private lateinit var dao: FakeDao
    private lateinit var api: FakeApi
    private lateinit var repository: CachedAddressRepository

    @Before
    fun setUp() {
        dao = FakeDao()
        api = FakeApi()
        repository = CachedAddressRepository(api, dao, now = { clock })
    }

    @Test
    fun firstLookup_asksNominatimAndSavesTheAnswer() = runTest {
        val result = repository.municipalityAt(place)

        assertEquals("Antwerpen", result)
        assertEquals(1, api.calls)
        assertEquals("Antwerpen", dao.rows["51.230,4.416"]?.municipality)
    }

    @Test
    fun secondLookup_usesTheCacheAndDoesNotAskNominatimAgain() = runTest {
        repository.municipalityAt(place)
        api.error = IOException("offline") // would fail if it were asked again

        val result = repository.municipalityAt(place)

        assertEquals("Antwerpen", result)
        assertEquals(1, api.calls)
    }

    @Test
    fun nearbyPosition_sharesTheSavedAnswer() = runTest {
        repository.municipalityAt(place)

        repository.municipalityAt(Coordinates(latitude = 51.230201, longitude = 4.416180)) // a few meters away

        assertEquals(1, api.calls)
    }

    @Test
    fun farAwayPosition_isLookedUpSeparately() = runTest {
        repository.municipalityAt(place)
        api.municipality = "Willebroek"

        val result = repository.municipalityAt(Coordinates(latitude = 51.057, longitude = 4.373))

        assertEquals("Willebroek", result)
        assertEquals(2, api.calls)
    }

    @Test
    fun oldAnswer_isLookedUpAgain() = runTest {
        repository.municipalityAt(place)
        clock += TimeUnit.DAYS.toMillis(31)
        api.municipality = "Nieuwe naam"

        val result = repository.municipalityAt(place)

        assertEquals("Nieuwe naam", result)
        assertEquals(2, api.calls)
    }

    @Test
    fun noInternet_usesAnOldSavedAnswer() = runTest {
        repository.municipalityAt(place)
        clock += TimeUnit.DAYS.toMillis(31)
        api.error = IOException("offline")

        val result = repository.municipalityAt(place)

        assertEquals("Antwerpen", result)
    }

    @Test
    fun noInternetAndNothingSaved_givesNull() = runTest {
        api.error = IOException("offline")

        assertNull(repository.municipalityAt(place))
        assertEquals(emptyMap<String, CachedAddress>(), dao.rows)
    }

    @Test
    fun placeWithoutMunicipality_givesNullAndSavesNothing() = runTest {
        api.municipality = null

        assertNull(repository.municipalityAt(place))
        assertEquals(emptyMap<String, CachedAddress>(), dao.rows)
    }

    @Test
    fun addressAt_givesTheFullAddressAtBuildingLevel() = runTest {
        api.road = "Ellermanstraat"
        api.houseNumber = "12"
        api.postcode = "2060"

        val result = repository.addressAt(place)

        assertEquals(AddressParts("Ellermanstraat", "12", "2060", "Antwerpen"), result)
        assertEquals(18, api.lastZoom)
    }

    @Test
    fun addressAt_isNotSavedInTheMunicipalityCache() = runTest {
        repository.addressAt(place)

        assertEquals(emptyMap<String, CachedAddress>(), dao.rows)
    }

    @Test
    fun addressAt_withoutInternet_givesNull() = runTest {
        api.error = IOException("offline")

        assertNull(repository.addressAt(place))
    }

    @Test
    fun locationKey_roundsToThreeDecimals() {
        assertEquals("51.230,4.416", CachedAddressRepository.locationKey(place))
    }
}
