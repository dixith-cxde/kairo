package app.kairo.habits

import app.kairo.KairoApplication
import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import app.kairo.data.share
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.round

class HabitRepository(private val app: KairoApplication, scope: CoroutineScope) {
    private val dao get() = app.database.habits()

    /** Every habit, archived ones included (the Habits tab shows those in a fold). */
    val all: Flow<List<Habit>> = combine(dao.habits(), dao.checks()) { hs, cs ->
        val byHabit = cs.groupBy { it.habitId }
        hs.map { h -> Habit(h, byHabit[h.id].orEmpty().associate { it.date to DayLog(it.state, it.value) }) }
    }.share(scope)

    /** Active habits (reminders, widgets). */
    val habits: Flow<List<Habit>> = all.map { l -> l.filter { !it.archived } }.share(scope)

    suspend fun add(name: String, shape: ListShape, color: ListColor, reminder: LocalTime?): Long =
        dao.insert(HabitEntity(name = name.trim(), shape = shape, color = color, reminderTime = reminder))

    suspend fun add(e: HabitEntity): Long = dao.insert(e)
    suspend fun update(e: HabitEntity) = dao.update(e)
    suspend fun reorder(ids: List<Long>, all: List<Habit>) =
        dao.updateAll(ids.mapIndexedNotNull { i, id -> all.firstOrNull { it.id == id }?.e?.copy(position = i) })
    suspend fun delete(id: Long) { dao.delete(id); HabitReminders(app).cancel(id) }
    /** Puts a deleted habit back with all its days (Undo after Delete). */
    suspend fun restore(h: Habit) {
        dao.insert(h.e)
        h.logs.forEach { (d, l) -> dao.put(HabitCheckEntity(h.id, d, l.state, l.value)) }
    }
    suspend fun setPause(h: Habit, days: Int?) = dao.update(h.e.copy(pauseFrom = LocalDate.now(), pauseTo = days?.let { LocalDate.now().plusDays(it - 1L) }))
    suspend fun resume(h: Habit) = dao.update(h.e.copy(pauseFrom = null, pauseTo = null))
    suspend fun setArchived(h: Habit, archived: Boolean) = dao.update(h.e.copy(archived = archived))

    /** 1.3.1 API: done or cleared. */
    suspend fun set(id: Long, date: LocalDate, done: Boolean) {
        if (done) dao.put(HabitCheckEntity(id, date)) else dao.clear(id, date)
    }

    suspend fun clear(id: Long, date: LocalDate) = dao.clear(id, date)
    suspend fun log(id: Long, date: LocalDate, value: Double) =
        if (value <= 0.0) dao.clear(id, date) else dao.put(HabitCheckEntity(id, date, 1, round1(value)))
    suspend fun notDone(id: Long, date: LocalDate) = dao.put(HabitCheckEntity(id, date, 0, 0.0))

    /**
     * The shape check (tap): yes/no toggles; a count adds one step and resets once it's at the goal;
     * a time habit fills to the goal or resets (the live timer comes with the habit page).
     */
    suspend fun tap(h: Habit, d: LocalDate) = when (h.goal) {
        Goal.Yes -> if (h.done(d)) clear(h.id, d) else dao.put(HabitCheckEntity(h.id, d))
        Goal.Count -> if (h.done(d)) clear(h.id, d) else log(h.id, d, minOf(h.target, h.count(d) + h.e.step))
        Goal.Time -> if (h.done(d)) clear(h.id, d) else log(h.id, d, h.target)
    }

    /** Swipe right = done (a count adds one step, from 0 after "not done"); swipe left = not done. */
    suspend fun swipe(h: Habit, d: LocalDate, done: Boolean) {
        if (!done) { notDone(h.id, d); return }
        when (h.goal) {
            Goal.Yes -> dao.put(HabitCheckEntity(h.id, d))
            Goal.Count -> log(h.id, d, minOf(h.target, (if (h.dropped(d)) 0.0 else h.count(d)) + h.e.step))
            Goal.Time -> log(h.id, d, h.target)
        }
    }

    /** Puts a day back exactly as it was (Undo). */
    suspend fun restore(id: Long, d: LocalDate, before: DayLog?) =
        if (before == null) dao.clear(id, d) else dao.put(HabitCheckEntity(id, d, before.state, before.value))

    suspend fun toggle(h: Habit, date: LocalDate = LocalDate.now()) = tap(h, date)

    private fun round1(v: Double) = round(v * 10) / 10
}
