package be.ap.laatsteronde.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** The local database of the app. For now it only holds the address cache. */
@Database(entities = [CachedAddress::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun addressDao(): AddressDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** One database for the whole app. */
        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "laatste_ronde.db"
                ).build().also { instance = it }
            }
    }
}
