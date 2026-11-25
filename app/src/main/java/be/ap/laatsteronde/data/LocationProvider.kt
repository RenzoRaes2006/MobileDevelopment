package be.ap.laatsteronde.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

/** A position on the map. Our own class, so ViewModels and tests do not need osmdroid or Android. */
data class Coordinates(val latitude: Double, val longitude: Double)

/**
 * Gives the position of the device. It is an interface so a unit test can use a fake.
 * Only call it after the user granted the location permission.
 */
interface LocationProvider {
    /** The current position, or null when it cannot be found (for example GPS off). */
    suspend fun currentLocation(): Coordinates?
}

/** Uses Google's fused location provider (GPS, wifi and cell towers together). */
class FusedLocationProvider(context: Context) : LocationProvider {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context.applicationContext) }

    // The permission is checked by the screen before this class is used.
    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): Coordinates? {
        return try {
            // Without a time limit this search can last forever (no GPS, no network), and the
            // screen would wait for ever. After 10 seconds we use the last known position instead.
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(SEARCH_TIMEOUT_MILLIS)
                .setMaxUpdateAgeMillis(MAX_AGE_MILLIS)
                .build()
            val location = client.getCurrentLocation(request, null).await()
                ?: client.lastLocation.await()
            location?.let { Coordinates(it.latitude, it.longitude) }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val SEARCH_TIMEOUT_MILLIS = 10_000L

        /** A stored position older than one minute is not good enough: we want where the customer is now. */
        const val MAX_AGE_MILLIS = 60_000L
    }
}
