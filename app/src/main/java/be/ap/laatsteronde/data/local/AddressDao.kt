package be.ap.laatsteronde.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Reads and writes [CachedAddress] rows. An interface, so a unit test can use a fake. */
@Dao
interface AddressDao {
    /** The saved address for this key, or null when we never saved one. */
    @Query("SELECT * FROM cached_address WHERE locationKey = :locationKey")
    suspend fun find(locationKey: String): CachedAddress?

    /** Saves the address; an older row with the same key is replaced. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(address: CachedAddress)
}
