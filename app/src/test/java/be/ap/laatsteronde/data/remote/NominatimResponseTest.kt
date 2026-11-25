package be.ap.laatsteronde.data.remote

import be.ap.laatsteronde.data.AddressParts
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
    fun addressParts_hasStreetNumberPostcodeAndMunicipality() {
        val response = parse(
            """{"address":{"house_number":"12","road":"Ellermanstraat","postcode":"2060","city":"Antwerpen","municipality":"Antwerpen","country":"België"}}"""
        )

        assertEquals(AddressParts("Ellermanstraat", "12", "2060", "Antwerpen"), response.address?.addressParts())
    }

    @Test
    fun addressParts_withoutHouseNumber_hasAnEmptyNumber() {
        val response = parse("""{"address":{"road":"Dorpsstraat","postcode":"2830","municipality":"Willebroek"}}""")

        assertEquals(AddressParts("Dorpsstraat", "", "2830", "Willebroek"), response.address?.addressParts())
    }

    @Test
    fun addressParts_withoutStreet_hasOnlyPostcodeAndMunicipality() {
        val response = parse("""{"address":{"postcode":"2830","municipality":"Willebroek"}}""")

        assertEquals(AddressParts("", "", "2830", "Willebroek"), response.address?.addressParts())
    }

    @Test
    fun addressParts_withSeveralHouseNumbers_takesTheFirst() {
        // Seen on the emulator for the AP campus: OpenStreetMap lists all numbers of the building.
        val response = parse(
            """{"address":{"road":"Ellermanstraat","house_number":"51;53;55;57;59;61;63","postcode":"2060","city":"Antwerpen"}}"""
        )

        assertEquals(AddressParts("Ellermanstraat", "51", "2060", "Antwerpen"), response.address?.addressParts())
    }

    @Test
    fun addressParts_trimsSpaces() {
        val response = parse("""{"address":{"road":" Dorpsstraat ","house_number":" 1 ","postcode":" 2830 ","town":" Mechelen "}}""")

        assertEquals(AddressParts("Dorpsstraat", "1", "2830", "Mechelen"), response.address?.addressParts())
    }

    @Test
    fun addressParts_withoutAnyPlaceInformation_givesNull() {
        assertNull(parse("""{"address":{"country":"België"}}""").address?.addressParts())
    }

    @Test
    fun answerWithoutAddress_hasNoMunicipality() {
        // Nominatim answers {"error":"Unable to geocode"} for a place in the sea.
        val response = parse("""{"error":"Unable to geocode"}""")

        assertNull(response.address?.municipalityName())
    }
}
