package be.ap.laatsteronde.data.remote

import be.ap.laatsteronde.data.AddressParts
import be.ap.laatsteronde.data.Coordinates
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
    @SerializedName("village") val village: String?,
    @SerializedName("road") val road: String? = null,
    @SerializedName("house_number") val houseNumber: String? = null,
    @SerializedName("postcode") val postcode: String? = null
) {
    /**
     * The address in the four parts of the shop form: street, number, postcode and municipality.
     * Parts that Nominatim does not know are empty, so a pin in a field gives only a postcode
     * and a municipality. Returns null when nothing is known.
     */
    fun addressParts(): AddressParts? {
        val parts = AddressParts(
            street = road.orEmpty().trim(),
            // A big building has several numbers in OpenStreetMap ("51;53;55"): a shop needs one, the first.
            houseNumber = houseNumber.orEmpty().substringBefore(';').trim(),
            postcode = postcode.orEmpty().trim(),
            municipality = municipalityName().orEmpty()
        )
        return parts.takeUnless { it.isEmpty() }
    }

    /**
     * The municipality ("gemeente"). Nominatim puts it in a different field depending on the
     * place. "municipality" comes first: for Blaasveld it gives Willebroek, where "village"
     * would give only the part of the town. Returns null when none of the fields is filled.
     */
    fun municipalityName(): String? =
        listOf(municipality, city, town, village).firstOrNull { !it.isNullOrBlank() }?.trim()
}

/** One place in the answer of a search. Nominatim gives the position as text, for example "51.2301". */
data class NominatimPlace(
    @SerializedName("lat") val latitude: String?,
    @SerializedName("lon") val longitude: String?
) {
    /** The position, or null when Nominatim gave no usable numbers. */
    fun coordinates(): Coordinates? {
        val lat = latitude?.toDoubleOrNull() ?: return null
        val lon = longitude?.toDoubleOrNull() ?: return null
        return Coordinates(lat, lon)
    }
}

/**
 * Reverse geocoding (from a position to an address, https://nominatim.org/release-docs/latest/api/Reverse/)
 * and searching (from an address to a position, https://nominatim.org/release-docs/latest/api/Search/).
 */
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

    /**
     * Searches an address in parts. [street] is the house number and the street ("58 Ellermanstraat").
     * Parts that are null are left out. The shops are in Belgium, so the search is limited to it.
     */
    @GET("search")
    suspend fun search(
        @Query("street") street: String,
        @Query("postalcode") postcode: String? = null,
        @Query("city") city: String? = null,
        @Query("countrycodes") countryCodes: String = "be",
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 1
    ): List<NominatimPlace>

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
