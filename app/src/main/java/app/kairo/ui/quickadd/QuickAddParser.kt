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

    private val dateRx = Regex(
        """\b(today|tod|tomorrow|tmr|tmrw|tonight|next week|in (\d{1,2}) (day|days|week|weeks)|(?:next |this )?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun))\b""",
        RegexOption.IGNORE_CASE,
    )
    private val timeRx = Regex("""\b(?:at )?(\d{1,2})(?::(\d{2}))?\s?(am|pm)\b|\b(?:at )?([01]?\d|2[0-3]):([0-5]\d)\b""", RegexOption.IGNORE_CASE)
    private val prioRx = Regex("""(?<=^|\s)!(high|hi|h|medium|med|m|low|lo|l|1|2|3)\b""", RegexOption.IGNORE_CASE)
    private val listRx = Regex("""(?<=^|\s)#(\w+)""")

    fun parse(text: String, today: LocalDate = LocalDate.now()): Parsed {
        val tokens = mutableListOf<Token>()
        var due: LocalDate? = null
        var time: LocalTime? = null

        dateRx.find(text)?.let { m ->
            val w = m.value.lowercase()
            due = when {
                w == "today" || w == "tod" || w == "tonight" -> today
                w.startsWith("tom") || w.startsWith("tmr") -> today.plusDays(1)
                w == "next week" -> today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                w.startsWith("in ") -> {
                    val n = m.groupValues[2].toLong()
                    if (m.groupValues[3].startsWith("week")) today.plusWeeks(n) else today.plusDays(n)
                }
                else -> days[m.groupValues[4].lowercase()]?.let { today.with(TemporalAdjusters.next(it)) }
            }
            if (w == "tonight") time = LocalTime.of(20, 0)
            tokens += Token(m.range, Token.Kind.Date)
        }
        timeRx.find(text)?.let { m ->
            time = if (m.groupValues[1].isNotEmpty()) {
                val h = m.groupValues[1].toInt() % 12
                val pm = m.groupValues[3].equals("pm", true)
                LocalTime.of(if (pm) h + 12 else h, m.groupValues[2].ifEmpty { "0" }.toInt())
            } else LocalTime.of(m.groupValues[4].toInt(), m.groupValues[5].toInt())
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
