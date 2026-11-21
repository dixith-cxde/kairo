package app.kairo.data.money

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * ₹ with Indian digit grouping (rule 11): ₹1,23,456 — paise shown only when not zero.
 * Uses the platform's en-IN formatter, which does lakh/crore grouping.
 */
object Money {
    private val inr: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

    fun format(paise: Long, sign: Boolean = false): String {
        val f = inr.clone() as NumberFormat
        val whole = paise % 100 == 0L
        f.minimumFractionDigits = if (whole) 0 else 2
        f.maximumFractionDigits = if (whole) 0 else 2
        val s = f.format(abs(paise) / 100.0)
        return when {
            paise < 0 -> "−$s"
            sign && paise > 0 -> "+$s"
            else -> s
        }
    }

    /** Parse keypad text like "1234.5" into paise without floating point. */
    fun parsePaise(text: String): Long {
        if (text.isBlank()) return 0
        val parts = text.split(".")
        val rupees = parts[0].ifEmpty { "0" }.toLong()
        val p = parts.getOrNull(1).orEmpty().padEnd(2, '0').take(2).toLong()
        return rupees * 100 + p
    }
}
