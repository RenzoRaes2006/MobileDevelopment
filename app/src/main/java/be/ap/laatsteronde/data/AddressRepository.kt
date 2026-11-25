package be.ap.laatsteronde.data

import android.content.Context
import be.ap.laatsteronde.data.local.AddressDao
import be.ap.laatsteronde.data.local.AppDatabase
import be.ap.laatsteronde.data.local.CachedAddress
import be.ap.laatsteronde.data.remote.NominatimApi
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/** Turns a position into the name of the municipality. An interface, so a unit test can use a fake. */
interface AddressRepository {
    /** The municipality at this position, or null when it is not known and cannot be looked up now. */
    suspend fun municipalityAt(coordinates: Coordinates): String?
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

    companion object {
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
