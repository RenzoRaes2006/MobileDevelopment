package be.ap.laatsteronde.ui.merchant

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests the small helpers for amounts, times and the date. */
class PackageFormatTest {

    @Test
    fun parseEuroToCents_acceptsCommaAndDot() {
        assertEquals(1400, parseEuroToCents("14,00"))
        assertEquals(1400, parseEuroToCents("14.00"))
        assertEquals(400, parseEuroToCents("4"))
        assertEquals(450, parseEuroToCents("4,5"))
        assertEquals(5, parseEuroToCents("0,05"))
    }

    @Test
    fun parseEuroToCents_ignoresSpacesAndTheEuroSign() {
        assertEquals(1200, parseEuroToCents("  12,00 "))
        assertEquals(1200, parseEuroToCents("€ 12,00"))
    }

    @Test
    fun parseEuroToCents_hasNoRoundingError() {
        // 0.1 + 0.2 style problems do not exist because we never use a Double.
        assertEquals(1999, parseEuroToCents("19,99"))
        assertEquals(1010, parseEuroToCents("10,10"))
    }

    @Test
    fun parseEuroToCents_refusesWhatIsNotAnAmount() {
        assertNull(parseEuroToCents(""))
        assertNull(parseEuroToCents("abc"))
        assertNull(parseEuroToCents("4,005")) // three decimals
        assertNull(parseEuroToCents("-4"))
        assertNull(parseEuroToCents("4,"))
        assertNull(parseEuroToCents("1.000,00"))
    }

    @Test
    fun parseEuroToCents_refusesZeroAndTooHigh() {
        assertNull(parseEuroToCents("0"))
        assertNull(parseEuroToCents("0,00"))
        assertEquals(PRICE_MAX_CENTS, parseEuroToCents("1000"))
        assertNull(parseEuroToCents("1000,01"))
    }

    @Test
    fun formatCents_showsEuroWithTwoDecimals() {
        assertEquals("€4,00", formatCents(400))
        assertEquals("€14,05", formatCents(1405))
        assertEquals("€0,05", formatCents(5))
    }

    @Test
    fun centsToInput_isWhatTheMerchantTypesAgain() {
        assertEquals("14,00", centsToInput(1400))
        assertEquals(1400, parseEuroToCents(centsToInput(1400)))
    }

    @Test
    fun normalizeTime_givesHoursAndMinutesWithTwoDigits() {
        assertEquals("18:00", normalizeTime("18:00"))
        assertEquals("09:05", normalizeTime("9:05"))
        assertEquals("18:30", normalizeTime("18.30"))
        assertEquals("19:30", normalizeTime("  19:30 "))
        assertEquals("00:00", normalizeTime("0:00"))
        assertEquals("23:59", normalizeTime("23:59"))
    }

    @Test
    fun normalizeTime_refusesWhatIsNotATime() {
        assertNull(normalizeTime(""))
        assertNull(normalizeTime("24:00"))
        assertNull(normalizeTime("18:60"))
        assertNull(normalizeTime("18"))
        assertNull(normalizeTime("18:5"))
        assertNull(normalizeTime("abc"))
    }

    @Test
    fun todayAsText_isYearMonthDay() {
        val date = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 5, 12, 0, 0) }.time
        assertEquals("2026-11-05", todayAsText(date))
    }

    @Test
    fun timeNowAsText_isHoursAndMinutesWithTwoDigits() {
        val morning = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 5, 9, 5, 0) }.time
        val evening = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 5, 21, 30, 0) }.time
        assertEquals("09:05", timeNowAsText(morning)) // "09:05", so it compares in the right order with "18:00"
        assertEquals("21:30", timeNowAsText(evening))
    }
}
