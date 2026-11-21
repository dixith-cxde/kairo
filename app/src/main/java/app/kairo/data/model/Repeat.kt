package app.kairo.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * How a task repeats: every [interval] days / weeks / months / years. Weekly rules can pick weekdays
 * ("every Thursday", "Mon–Fri"). Stored in Room as short text: "" (none), "D1", "W1:1,4", "M1", "Y1".
 * Completing a repeating task keeps that one as done (so Stats counts it) and adds the next one.
 */
data class Repeat(val unit: Unit, val interval: Int = 1, val days: Set<DayOfWeek> = emptySet()) {
    enum class Unit(val code: Char) { Day('D'), Week('W'), Month('M'), Year('Y') }

    fun encode(): String = "${unit.code}$interval" +
        if (unit == Unit.Week && days.isNotEmpty()) ":" + days.sorted().joinToString(",") { it.value.toString() } else ""

    /** First date after [from] that the rule hits, and not before today. */
    fun next(from: LocalDate?, today: LocalDate = LocalDate.now()): LocalDate {
        var d = step(from ?: today.minusDays(1))
        while (d.isBefore(today)) d = step(d)
        return d
    }

    private fun step(from: LocalDate): LocalDate = when (unit) {
        Unit.Day -> from.plusDays(interval.toLong())
        Unit.Month -> from.plusMonths(interval.toLong())
        Unit.Year -> from.plusYears(interval.toLong())
        Unit.Week -> if (days.isEmpty()) from.plusWeeks(interval.toLong()) else {
            val week0 = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            var d = from.plusDays(1)
            while (true) {
                val weeks = ChronoUnit.WEEKS.between(week0, d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                if (d.dayOfWeek in days && weeks % interval == 0L) break
                d = d.plusDays(1)
            }
            d
        }
    }

    /** "Every day", "Every Thursday", "Every 2 weeks on Mon, Fri", "Weekdays". */
    fun label(): String {
        val n = interval
        fun every(one: String, many: String) = if (n == 1) "Every $one" else "Every $n $many"
        return when (unit) {
            Unit.Day -> every("day", "days")
            Unit.Month -> every("month", "months")
            Unit.Year -> every("year", "years")
            Unit.Week -> when {
                days.isEmpty() -> every("week", "weeks")
                n == 1 && days == WEEKDAYS -> "Weekdays"
                n == 1 && days.size == 7 -> "Every day"
                n == 1 -> "Every " + days.sorted().joinToString(", ") { if (days.size == 1) it.long() else it.short() }
                else -> "Every $n weeks on " + days.sorted().joinToString(", ") { it.short() }
            }
        }
    }

    companion object {
        val WEEKDAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

        fun decode(s: String?): Repeat? {
            if (s.isNullOrBlank()) return null
            val unit = Unit.entries.firstOrNull { it.code == s[0] } ?: return null
            val parts = s.substring(1).split(":")
            val n = parts[0].toIntOrNull()?.coerceAtLeast(1) ?: 1
            val days = parts.getOrNull(1)?.split(",")?.mapNotNull { it.toIntOrNull()?.takeIf { v -> v in 1..7 }?.let(DayOfWeek::of) }?.toSet().orEmpty()
            return Repeat(unit, n, days)
        }
    }
}

fun DayOfWeek.short() = name.take(3).lowercase().replaceFirstChar { it.uppercase() }
fun DayOfWeek.long() = name.lowercase().replaceFirstChar { it.uppercase() }
