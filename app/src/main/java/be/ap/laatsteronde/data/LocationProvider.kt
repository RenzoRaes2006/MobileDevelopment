package be.ap.laatsteronde.data

import android.annotation.SuppressLint
import android.content.Context
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
            val location = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                ?: client.lastLocation.await()
            location?.let { Coordinates(it.latitude, it.longitude) }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
