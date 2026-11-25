package be.ap.laatsteronde.data

import android.content.Context
import be.ap.laatsteronde.data.local.AddressDao
import be.ap.laatsteronde.data.local.AppDatabase
import be.ap.laatsteronde.data.local.CachedAddress
import be.ap.laatsteronde.data.remote.NominatimApi
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/** The address in the four parts of the shop form. A part Nominatim does not know is empty. */
data class AddressParts(
    val street: String,
    val houseNumber: String,
    val postcode: String,
    val municipality: String
) {
    fun isEmpty(): Boolean = street.isEmpty() && houseNumber.isEmpty() && postcode.isEmpty() && municipality.isEmpty()
}

/** Turns a position into the name of the municipality. An interface, so a unit test can use a fake. */
interface AddressRepository {
    /** The municipality at this position, or null when it is not known and cannot be looked up now. */
    suspend fun municipalityAt(coordinates: Coordinates): String?

    /** The address at this position in four parts, or null when it cannot be looked up now. */
    suspend fun addressAt(coordinates: Coordinates): AddressParts?

    /**
     * The position of an address, for the pin of the shop. Null when the address is not complete
     * enough (a street and a postcode or a municipality are needed), is not found, or cannot be looked up now.
     */
    suspend fun locationOf(address: AddressParts): Coordinates?
}

/**
 * Looks in the Room cache first and asks Nominatim only when the place is not saved yet (or the
 * saved answer is old). Without internet it uses an old saved answer instead of nothing.
 */
class CachedAddressRepository(
    private val api: NominatimApi,
    private val dao: AddressDao,
    private val now: () -> Long = System::currentTimeMillis
) : AddressRepository {

    override suspend fun municipalityAt(coordinates: Coordinates): String? {
        val key = locationKey(coordinates)
        val saved = dao.find(key)
        if (saved != null && now() - saved.savedAtMillis < MAX_AGE_MILLIS) {
            return saved.municipality
        }

        return try {
            val municipality = api.reverse(coordinates.latitude, coordinates.longitude)
                .address?.municipalityName()
            if (municipality != null) {
                dao.save(CachedAddress(key, municipality, savedAtMillis = now()))
            }
            // Nominatim knows nothing about this place (for example in the sea): use an old answer if there is one.
            municipality ?: saved?.municipality
        } catch (e: CancellationException) {
            throw e // the screen was closed: do not treat this as a failed lookup
        } catch (e: Exception) {
            // No internet or a server problem: an old saved answer is better than nothing.
            saved?.municipality
        }
    }

    /**
     * Not cached: a merchant looks up one address when placing the pin of the shop, and the
     * saved shop keeps the address, so there is nothing to look up twice.
     */
    override suspend fun addressAt(coordinates: Coordinates): AddressParts? {
        return try {
            api.reverse(coordinates.latitude, coordinates.longitude, zoom = ADDRESS_ZOOM)
                .address?.addressParts()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Not cached either. When Nominatim does not know the house number, the street alone is tried:
     * the pin then stands in the street and the merchant can move it to the door.
     */
    override suspend fun locationOf(address: AddressParts): Coordinates? {
        val street = address.street.trim()
        if (street.isEmpty() || (address.postcode.isBlank() && address.municipality.isBlank())) return null
        return try {
            val number = address.houseNumber.trim()
            val withNumber = if (number.isEmpty()) street else "$number $street"
            search(withNumber, address) ?: if (number.isNotEmpty()) search(street, address) else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun search(street: String, address: AddressParts): Coordinates? =
        api.search(
            street = street,
            postcode = address.postcode.trim().ifEmpty { null },
            city = address.municipality.trim().ifEmpty { null }
        ).firstNotNullOfOrNull { it.coordinates() }

    companion object {
        /** Nominatim zoom 18 is the level of a building, so the street and house number come back. */
        private const val ADDRESS_ZOOM = 18

        /** Municipalities hardly change, so a saved answer is good for 30 days. */
        private val MAX_AGE_MILLIS = TimeUnit.DAYS.toMillis(30)

        /** Coordinates rounded to 3 decimals (about 100 meters), so nearby positions share one saved answer. */
        fun locationKey(coordinates: Coordinates): String =
            String.format(Locale.US, "%.3f,%.3f", coordinates.latitude, coordinates.longitude)

        /** The real repository: Nominatim over the internet and the Room database of the app. */
        fun create(context: Context): CachedAddressRepository = CachedAddressRepository(
            api = NominatimApi.create(userAgent = context.packageName),
            dao = AppDatabase.get(context).addressDao()
        )
    }
}
