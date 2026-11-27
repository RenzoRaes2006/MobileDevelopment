package be.ap.laatsteronde.ui.merchant

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The highest price we accept, in cents (1000 euro). The Firestore rules check the same limit. */
internal const val PRICE_MAX_CENTS = 100_000

/**
 * Turns what the merchant typed ("4", "4,5", "14,00" or "14.00") into whole cents, or null when
 * it is not an amount with at most two decimals, is zero, or is above [PRICE_MAX_CENTS].
 */
internal fun parseEuroToCents(text: String): Int? {
    val cleaned = text.trim().removePrefix("€").trim().replace(',', '.')
    if (!Regex("""\d{1,4}(\.\d{1,2})?""").matches(cleaned)) return null
    val cents = BigDecimal(cleaned).movePointRight(2).toInt()
    return if (cents in 1..PRICE_MAX_CENTS) cents else null
}

/** Cents as the user sees them: 400 becomes "€4,00". */
internal fun formatCents(cents: Int): String =
    "€%d,%02d".format(Locale.ROOT, cents / 100, cents % 100)

/** A cents amount as it is typed in the form again: 1400 becomes "14,00". */
internal fun centsToInput(cents: Int): String =
    "%d,%02d".format(Locale.ROOT, cents / 100, cents % 100)

/**
 * Turns a typed time ("18:00", "9:05" or "18.30") into "HH:mm", or null when it is not a time of the day.
 * This is the form the Firestore rules expect, and the form that sorts in the right order as text.
 */
internal fun normalizeTime(text: String): String? {
    val match = Regex("""(\d{1,2})[:.](\d{2})""").matchEntire(text.trim()) ?: return null
    val hours = match.groupValues[1].toInt()
    val minutes = match.groupValues[2].toInt()
    if (hours !in 0..23 || minutes !in 0..59) return null
    return "%02d:%02d".format(Locale.ROOT, hours, minutes)
}

/** The time now as "HH:mm", in the time zone of the phone: the same form as the pick-up times. */
internal fun timeNowAsText(now: Date = Date()): String =
    SimpleDateFormat("HH:mm", Locale.ROOT).format(now)

/** The date of today as "yyyy-MM-dd", in the time zone of the phone. */
internal fun todayAsText(now: Date = Date()): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(now)
