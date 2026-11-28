package app.kairo.ui.calendar

import androidx.compose.ui.graphics.Color
import app.kairo.data.calendar.CalEvent
import app.kairo.data.model.Task
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

/** The three views (canvas: Month / Week / Day connected group). Day is the default view of the hours. */
enum class CalView { Month, Week, Day }

/** An event occurrence touches day [d] (an all-day event ends the day before its end date). */
internal fun occursOn(e: CalEvent, d: LocalDate): Boolean {
    val s = e.start.toLocalDate()
    val en = e.end.toLocalDate()
    if (d.isBefore(s)) return false
    if (e.allDay) return d.isBefore(en) || (s == en && d == s)
    return d.isBefore(en) || (d == en && (e.end.toLocalTime() > LocalTime.MIDNIGHT || e.start == e.end))
}

/** Events per day, computed once per load. */
internal fun eventsByDay(events: List<CalEvent>): Map<LocalDate, List<CalEvent>> {
    val m = HashMap<LocalDate, MutableList<CalEvent>>()
    events.forEach { e ->
        generateSequence(e.start.toLocalDate()) { it.plusDays(1) }
            .takeWhile { !it.isAfter(e.end.toLocalDate()) }.filter { occursOn(e, it) }
            .forEach { m.getOrPut(it) { mutableListOf() } += e }
    }
    return m
}

/** The part of a timed event on day [d], in minutes; null = shows as all day (all-day, or a middle day). */
internal fun span(e: CalEvent, d: LocalDate): Pair<Int, Int>? {
    if (e.allDay) return null
    val a = if (e.start.toLocalDate() == d) e.start.toLocalTime().toSecondOfDay() / 60 else 0
    val b = if (e.end.toLocalDate() == d) e.end.toLocalTime().toSecondOfDay() / 60 else 24 * 60
    return if (a == 0 && b == 24 * 60) null else a to max(a, b)
}

/** How full a day is (canvas "load"): timed events plus 30 min per open timed task. */
internal fun load(events: List<CalEvent>, tasks: List<Task>, d: LocalDate): Int =
    events.sumOf { e -> span(e, d)?.let { it.second - it.first } ?: 0 } + tasks.count { it.time != null && !it.done } * 30

/** "6 h 30 min" (canvas hm2). */
internal fun hoursMinutes(m: Int): String = listOfNotNull(
    (m / 60).takeIf { it > 0 }?.let { "$it h" }, (m % 60).takeIf { it > 0 }?.let { "$it min" },
).joinToString(" ")

/** One block on the hour timeline: an event or a task. */
internal data class Block(
    val key: String, val title: String, val sub: String, val start: Int, val end: Int,
    val bg: Color, val fg: Color, val task: Task?, val event: CalEvent?, val done: Boolean,
)

internal class Placed(val block: Block, val lane: Int, val lanes: Int)

/** Shortest a block draws (minutes), so a 15-min item still reads as a block. Canvas: 22. */
internal const val MinBlock = 22

/** Greedy lanes per cluster of overlapping blocks; each cluster splits its width evenly. */
internal fun lanes(blocks: List<Block>): List<Placed> {
    val out = mutableListOf<Placed>()
    var cluster = mutableListOf<Pair<Block, Int>>()
    var ends = mutableListOf<Int>()
    var clusterEnd = -1
    fun flush() { val n = ends.size; cluster.forEach { (b, l) -> out += Placed(b, l, n) }; cluster = mutableListOf(); ends = mutableListOf(); clusterEnd = -1 }
    for (b in blocks.sortedWith(compareBy<Block>({ it.start }, { -it.end }))) {
        val end = max(b.end, b.start + MinBlock)
        if (cluster.isNotEmpty() && b.start >= clusterEnd) flush()
        val free = ends.indexOfFirst { it <= b.start }
        val lane = if (free == -1) { ends += end; ends.lastIndex } else { ends[free] = end; free }
        cluster += b to lane
        clusterEnd = max(clusterEnd, end)
    }
    flush()
    return out
}

internal val Clock: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
internal val ShortDay: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
