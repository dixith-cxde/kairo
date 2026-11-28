package app.kairo.habits

import app.kairo.data.model.ListShape
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class Goal { Yes, Count, Time }

/** What one day holds: state 1 = logged (value = count or minutes; 0 on a plain check), 0 = not done. */
data class DayLog(val state: Int, val value: Double)

/**
 * A habit and its logged days, with the v2 rules from the canvas (Habits block in Main.dc.html):
 * a day is pending, done or not done; rest days (not scheduled) and paused days are never due and
 * never break a streak; an unchecked today doesn't break it either; "N times a week" habits count
 * weeks instead of days.
 */
data class Habit(val e: HabitEntity, val logs: Map<LocalDate, DayLog>) {
    val id get() = e.id
    val name get() = e.name
    val color get() = e.color
    val icon get() = e.icon
    val reminderTime get() = e.reminderTime
    val createdAt get() = e.createdAt
    val archived get() = e.archived
    val perWeek get() = e.perWeek
    val target get() = if (goal == Goal.Yes) 1.0 else e.target
    val goal: Goal get() = when (e.goal) { "count" -> Goal.Count; "time" -> Goal.Time; else -> Goal.Yes }

    /** v2 habit shape (MaterialShapes name). 1.3.1 habits get the closest allowed one. */
    @Suppress("DEPRECATION")
    val shapeName: String get() = e.hshape.ifEmpty {
        when (e.shape) {
            ListShape.Hexagon, ListShape.Arch -> "ClamShell"
            ListShape.Triangle, ListShape.Sunny -> "Gem"
            ListShape.Square, ListShape.Cookie4 -> "Square"
            ListShape.Pentagon -> "Pentagon"
            ListShape.Gem -> "Gem"
            ListShape.Cookie6, ListShape.Cookie9, ListShape.Clover -> "Circle" // no cookies or clover for habits
        }
    }

    val weekdays: Set<Int>? get() = e.days?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.toSet()?.takeIf { it.size in 1..6 }

    fun paused(d: LocalDate): Boolean {
        val from = e.pauseFrom ?: return false
        return !d.isBefore(from) && (e.pauseTo == null || !d.isAfter(e.pauseTo))
    }

    /** Due on [d]: not paused, and one of its weekdays (every day when none are set). */
    fun scheduled(d: LocalDate): Boolean = !paused(d) && (weekdays?.contains(d.dayOfWeek.value - 1) ?: true)

    fun dropped(d: LocalDate) = logs[d]?.state == 0

    /** The day's count or minutes (a plain check counts as the full goal). */
    fun count(d: LocalDate): Double {
        val l = logs[d] ?: return 0.0
        if (l.state == 0) return 0.0
        return if (l.value <= 0.0) target else l.value
    }

    fun done(d: LocalDate): Boolean {
        val l = logs[d] ?: return false
        return l.state == 1 && (goal == Goal.Yes || l.value <= 0.0 || l.value >= target)
    }

    fun weekDone(anyDay: LocalDate, today: LocalDate = LocalDate.now()): Int {
        val mon = anyDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0..6).count { val d = mon.plusDays(it.toLong()); !d.isAfter(today) && done(d) }
    }

    /** Due on the week ring: scheduled, and for times-a-week habits only the days it was done. */
    fun dueForRing(d: LocalDate) = scheduled(d) && !d.isBefore(createdAt) && (perWeek == null || done(d))

    fun streak(today: LocalDate = LocalDate.now()): Int {
        perWeek?.let { n -> return weekStreak(n, today) }
        if (dropped(today)) return 0
        var d = if (done(today)) today else today.minusDays(1)
        var s = 0
        repeat(800) {
            if (d.isBefore(createdAt.minusDays(400))) return s
            if (scheduled(d)) { if (done(d)) s++ else return s }
            d = d.minusDays(1)
        }
        return s
    }

    fun best(today: LocalDate = LocalDate.now()): Int {
        perWeek?.let { n ->
            var b = 0; var c = 0
            var mon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(60)
            while (!mon.isAfter(today)) { if (weekDone(mon, today) >= n) { c++; b = maxOf(b, c) } else if (mon.plusDays(6).isBefore(today)) c = 0; mon = mon.plusWeeks(1) }
            return b
        }
        val first = logs.keys.minOrNull() ?: return 0
        var b = 0; var c = 0; var d = first
        while (!d.isAfter(today)) {
            if (scheduled(d)) { if (done(d)) { c++; b = maxOf(b, c) } else if (d.isBefore(today)) c = 0 }
            d = d.plusDays(1)
        }
        return b
    }

    fun total() = logs.keys.count { done(it) }

    private fun weekStreak(n: Int, today: LocalDate): Int {
        var mon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var s = if (weekDone(mon, today) >= n) 1 else 0
        repeat(60) { mon = mon.minusWeeks(1); if (weekDone(mon, today) >= n) s++ else return s }
        return s
    }

    // 1.3.1 helpers still used by the reminder code.
    fun doneOn(d: LocalDate) = done(d)
}
