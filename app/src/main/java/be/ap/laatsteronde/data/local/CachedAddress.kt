package be.ap.laatsteronde.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The municipality of a place, saved on the phone so we do not ask Nominatim again for the
 * same place (Nominatim asks apps to cache their answers).
 *
 * [locationKey] is the coordinates rounded to about 100 meters, for example "51.230,4.416".
 */
@Entity(tableName = "cached_address")
data class CachedAddress(
    @PrimaryKey val locationKey: String,
    val municipality: String,
    /** When we saved it, in milliseconds since 1970. Used to throw away old answers. */
    val savedAtMillis: Long
)
