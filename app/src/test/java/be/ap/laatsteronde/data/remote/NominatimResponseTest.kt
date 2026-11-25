package be.ap.laatsteronde.data.remote

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests how we read a Nominatim answer, with answers in the shape the real server sends. */
class NominatimResponseTest {

    private fun parse(json: String): NominatimResponse = Gson().fromJson(json, NominatimResponse::class.java)

    @Test
    fun city_isTheMunicipality() {
        val response = parse(
            """{"display_name":"Antwerpen, Vlaanderen","address":{"city":"Antwerpen","municipality":"Antwerpen","country":"België"}}"""
        )

        assertEquals("Antwerpen", response.address?.municipalityName())
    }

    @Test
    fun municipalityComesBeforeVillage() {
        // Blaasveld is a village that belongs to the municipality Willebroek.
        val response = parse(
            """{"address":{"village":"Blaasveld","municipality":"Willebroek","county":"Antwerpen"}}"""
        )

        assertEquals("Willebroek", response.address?.municipalityName())
    }

    @Test
    fun townOrVillage_isUsedWhenThereIsNoMunicipality() {
        assertEquals("Mechelen", parse("""{"address":{"town":"Mechelen"}}""").address?.municipalityName())
        assertEquals("Kapelle-op-den-Bos", parse("""{"address":{"village":"Kapelle-op-den-Bos"}}""").address?.municipalityName())
    }

    @Test
    fun blankFieldsAreSkipped() {
        val response = parse("""{"address":{"municipality":"  ","city":"Gent"}}""")

        assertEquals("Gent", response.address?.municipalityName())
    }

    @Test
    fun noPlaceFields_givesNull() {
        assertNull(parse("""{"address":{"country":"België"}}""").address?.municipalityName())
    }

    @Test
    fun answerWithoutAddress_hasNoMunicipality() {
        // Nominatim answers {"error":"Unable to geocode"} for a place in the sea.
        val response = parse("""{"error":"Unable to geocode"}""")

        assertNull(response.address?.municipalityName())
    }
}
