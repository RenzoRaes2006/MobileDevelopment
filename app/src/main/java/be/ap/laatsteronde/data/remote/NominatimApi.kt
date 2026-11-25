package be.ap.laatsteronde.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/** The part of a Nominatim answer that we use. */
data class NominatimResponse(
    @SerializedName("address") val address: NominatimAddress?
)

data class NominatimAddress(
    @SerializedName("municipality") val municipality: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("town") val town: String?,
    @SerializedName("village") val village: String?
) {
    /**
     * The municipality ("gemeente"). Nominatim puts it in a different field depending on the
     * place. "municipality" comes first: for Blaasveld it gives Willebroek, where "village"
     * would give only the part of the town. Returns null when none of the fields is filled.
     */
    fun municipalityName(): String? =
        listOf(municipality, city, town, village).firstOrNull { !it.isNullOrBlank() }?.trim()
}

/** Reverse geocoding: from a position to an address. https://nominatim.org/release-docs/latest/api/Reverse/ */
interface NominatimApi {
    @GET("reverse")
    suspend fun reverse(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressDetails: Int = 1,
        // zoom 10 is the level of a city or municipality, so no street details come back.
        @Query("zoom") zoom: Int = 10
    ): NominatimResponse

    companion object {
        private const val BASE_URL = "https://nominatim.openstreetmap.org/"

        /**
         * Nominatim's usage rules ask for a User-Agent that names the app (no default
         * library name), and we ask for Dutch place names.
         */
        fun create(userAgent: String): NominatimApi {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", userAgent)
                            .header("Accept-Language", "nl")
                            .build()
                    )
                }
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NominatimApi::class.java)
        }
    }
}
