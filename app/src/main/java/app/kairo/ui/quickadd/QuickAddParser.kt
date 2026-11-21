package app.kairo.ui.quickadd

import app.kairo.data.model.Priority
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * Natural-language quick add: "Call Mom tomorrow 7pm !high #Family".
 * Recognised tokens are removed from the title and highlighted in the field.
 */
data class Parsed(
    val title: String,
    val due: LocalDate?,
    val time: LocalTime?,
    val priority: Priority?,
    val listName: String?,
    val tokens: List<Token>,
)

data class Token(val range: IntRange, val kind: Kind) {
    enum class Kind { Date, Priority, List }
}

object QuickAddParser {
    private val days = DayOfWeek.entries.associateBy { it.name.lowercase() } +
        DayOfWeek.entries.associateBy { it.name.lowercase().take(3) }

    // Everyday English and texting shorthand (owner, 2026-11-21): tmrw, 2moro, tdy, tonite,
    // day after tomorrow, this weekend, next month, in 3 days / 2 wks, eod…
    private val dateRx = Regex(
        """\b(day after (?:tomorrow|tmrw|tmr)|overmorrow|today|tdy|2day|tod|tomorrow|tomorow|tommorow|tommorrow|tmrw|tmr|tmw|tmro|tomo|2moro|2morrow|2mrw|tonight|tonite|eod|this weekend|weekend|next weekend|next week|next month|in (\d{1,2}) ?(d|day|days|w|wk|wks|week|weeks|mo|month|months)|(?:next |this |on )?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun))\b""",
        RegexOption.IGNORE_CASE,
    )
    private val timeRx = Regex(
        """\b(?:at |@ ?)?(\d{1,2})(?::|\.)?(\d{2})?\s?(am|pm)\b|\b(?:at |@ ?)?([01]?\d|2[0-3]):([0-5]\d)\b|\b(?:in the |this )?(morning|noon|midday|afternoon|evening|night|midnight)\b|\bat (\d{1,2})\b""",
        RegexOption.IGNORE_CASE,
    )
    private val prioRx = Regex("""(?<=^|\s)!(high|hi|h|medium|med|m|low|lo|l|1|2|3)\b""", RegexOption.IGNORE_CASE)
    private val listRx = Regex("""(?<=^|\s)#(\w+)""")

    private fun dayOf(w: String): DayOfWeek? = w.lowercase().let { x ->
        days[x] ?: days[x.take(3)]
    }

    fun parse(text: String, today: LocalDate = LocalDate.now()): Parsed {
        val tokens = mutableListOf<Token>()
        var due: LocalDate? = null
        var time: LocalTime? = null

        dateRx.find(text)?.let { m ->
            val w = m.value.lowercase()
            due = when {
                w.startsWith("day after") || w == "overmorrow" -> today.plusDays(2)
                w in setOf("today", "tdy", "2day", "tod", "tonight", "tonite", "eod") -> today
                w.startsWith("tom") || w.startsWith("tmr") || w.startsWith("tmw") || w.startsWith("2m") -> today.plusDays(1)
                w == "next week" -> today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                w == "next month" -> today.plusMonths(1).withDayOfMonth(1)
                w == "next weekend" -> today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).let { if (it.isBefore(today.plusDays(7))) it.plusWeeks(1) else it }
                w.endsWith("weekend") -> if (today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY) today
                    else today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
                w.startsWith("in ") -> {
                    val n = m.groupValues[2].toLong()
                    val unit = m.groupValues[3].lowercase()
                    when { unit.startsWith("w") -> today.plusWeeks(n); unit.startsWith("mo") -> today.plusMonths(n); else -> today.plusDays(n) }
                }
                else -> dayOf(m.groupValues[4])?.let { today.with(TemporalAdjusters.next(it)) }
            }
            if (w == "tonight" || w == "tonite") time = LocalTime.of(20, 0)
            if (w == "eod") time = LocalTime.of(18, 0)
            tokens += Token(m.range, Token.Kind.Date)
        }
        timeRx.find(text)?.let { m ->
            val g = m.groupValues
            time = when {
                g[1].isNotEmpty() -> {
                    val h = g[1].toInt() % 12
                    val pm = g[3].startsWith("p", true)
                    LocalTime.of(if (pm) h + 12 else h, g[2].ifEmpty { "0" }.toInt().coerceIn(0, 59))
                }
                g[4].isNotEmpty() -> LocalTime.of(g[4].toInt(), g[5].toInt())
                g[6].isNotEmpty() -> when (g[6].lowercase()) {
                    "morning" -> LocalTime.of(9, 0); "noon", "midday" -> LocalTime.of(12, 0); "afternoon" -> LocalTime.of(15, 0)
                    "evening" -> LocalTime.of(18, 0); "night" -> LocalTime.of(21, 0); else -> LocalTime.of(23, 59)
                }
                // "at 6": a bare hour reads as the next sensible one (1–7 → afternoon/evening).
                else -> g[7].toInt().let { h -> LocalTime.of(if (h in 1..7) h + 12 else h % 24, 0) }
            }
            if (due == null) due = today
            tokens += Token(m.range, Token.Kind.Date)
        }
        val priority = prioRx.find(text)?.let { m ->
            tokens += Token(m.range, Token.Kind.Priority)
            when (m.groupValues[1].lowercase()) {
                "high", "hi", "h", "1" -> Priority.High
                "medium", "med", "m", "2" -> Priority.Medium
                else -> Priority.Low
            }
        }
        val listName = listRx.find(text)?.let { m ->
            tokens += Token(m.range, Token.Kind.List)
            m.groupValues[1]
        }

        var title = text
        tokens.sortedByDescending { it.range.first }.forEach { title = title.removeRange(it.range) }
        title = title.replace(Regex("""\s+"""), " ").trim()
        return Parsed(title, due, time, priority, listName, tokens.sortedBy { it.range.first })
    }
}
