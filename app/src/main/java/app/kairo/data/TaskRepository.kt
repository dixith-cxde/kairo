package app.kairo.data

import app.kairo.data.db.KairoDatabase
import app.kairo.data.db.ListEntity
import app.kairo.data.db.TaskEntity
import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import app.kairo.data.model.Priority
import app.kairo.data.model.Repeat
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/** The one place screens get data from. Screens see domain models, never Room entities. */
/** Share one upstream between all collectors; skip re-emitting an identical value. */
internal fun <T> Flow<T>.share(scope: CoroutineScope): Flow<T> =
    distinctUntilChanged().shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

class TaskRepository(private val db: KairoDatabase, scope: CoroutineScope) {

    // Shared: one Room query per change, however many screens, widgets and the reminder sync are
    // listening (each collector used to run its own). Stays hot 5 s after the last one leaves.
    private val all: Flow<List<Task>> = db.tasks().observeAll().map { it.map(TaskEntity::toModel) }.share(scope)
    /** Everything the app shows; archived tasks are hidden except in the Archived smart list. */
    val tasks: Flow<List<Task>> = all.map { l -> l.filter { !it.archived } }.share(scope)
    val archived: Flow<List<Task>> = all.map { l -> l.filter { it.archived } }
    /** Trash (canvas): deleted tasks, newest first, with when each was deleted. */
    val trash: Flow<List<Pair<Task, Long>>> = db.tasks().observeTrash().map { l -> l.map { it.toModel() to (it.deletedAt ?: 0L) } }
    val lists: Flow<List<TaskList>> = db.lists().observeAll().map { it.map(ListEntity::toModel) }.share(scope)

    fun task(id: Long): Flow<Task?> = db.tasks().observe(id).map { it?.toModel() }

    suspend fun setDone(task: Task, done: Boolean) {
        db.tasks().setDone(task.id, done, if (done) System.currentTimeMillis() else null)
        repeatAfter(listOf(task.toEntity()), done)
    }

    /**
     * Repeating tasks: completing one adds the next occurrence (same title, list, time, notes and
     * rule; new due date). Un-completing it removes that next one again if it's still untouched,
     * so Undo and a mis-tap leave no duplicate.
     */
    private suspend fun repeatAfter(rows: List<TaskEntity>, done: Boolean) {
        for (e in rows) {
            val rule = Repeat.decode(e.repeat) ?: continue
            val next = e.copy(id = 0, due = rule.next(e.due), done = false, completedAt = null, subtasksDone = 0,
                archived = false, createdAt = System.currentTimeMillis())
            val twin = db.tasks().findOpen(e.title, e.listId, e.repeat, next.due)
            if (done && twin == null) db.tasks().insert(next)
            if (!done && twin != null) db.tasks().delete(twin)
        }
    }

    suspend fun add(task: Task): Long = db.tasks().insert(task.toEntity())
    suspend fun update(task: Task) = db.tasks().update(task.toEntity())
    suspend fun delete(task: Task) = db.tasks().delete(task.id)

    // Bulk actions for multi-select.
    suspend fun setDone(ids: List<Long>, done: Boolean) {
        db.tasks().setDoneAll(ids, done, if (done) System.currentTimeMillis() else null)
        repeatAfter(db.tasks().get(ids), done)
    }
    suspend fun setArchived(ids: List<Long>, archived: Boolean) = db.tasks().setArchived(ids, archived)
    suspend fun move(ids: List<Long>, listId: Long?) = db.tasks().move(ids, listId)
    suspend fun reschedule(ids: List<Long>, due: LocalDate?, time: LocalTime?, reminder: Boolean) =
        db.tasks().reschedule(ids, due, time, reminder)
    suspend fun duplicate(task: Task): Long = db.tasks().insert(task.toEntity().copy(id = 0, done = false, completedAt = null))
    /** Moves to Trash and returns the rows as they were, so Undo can put them back exactly. */
    suspend fun deleteAll(ids: List<Long>): List<TaskEntity> = db.tasks().get(ids).also { db.tasks().setDeleted(ids, System.currentTimeMillis()) }
    suspend fun restore(rows: List<TaskEntity>) = db.tasks().insertAll(rows)
    suspend fun restoreFromTrash(ids: List<Long>) = db.tasks().setDeleted(ids, null)
    /** Delete forever (Trash only). */
    suspend fun purge(ids: List<Long>) = db.tasks().deleteAll(ids)
    /** Trash keeps tasks 30 days; runs on app start. */
    suspend fun purgeOldTrash() = db.tasks().purgeBefore(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)

    suspend fun addList(list: TaskList): Long = db.lists().insert(ListEntity(name = list.name, shape = list.shape, color = list.color))
    suspend fun updateList(list: TaskList) = db.lists().update(ListEntity(list.id, list.name, list.shape, list.color, list.position))
    suspend fun reorderLists(lists: List<TaskList>) =
        db.lists().updateAll(lists.mapIndexed { i, l -> ListEntity(l.id, l.name, l.shape, l.color, i) })
    suspend fun deleteList(list: TaskList) = db.lists().delete(list.id)
    /** Deletes a list but keeps its tasks: they move to Inbox first (canvas: a deleted list's tasks go to Inbox). */
    suspend fun deleteListKeepTasks(list: TaskList) {
        db.tasks().move(db.tasks().idsInList(list.id), null)
        db.lists().delete(list.id)
    }
    suspend fun listsNow(): List<TaskList> = lists.first()

    /**
     * No default lists (owner, 2026-11-21): tasks start in the Inbox. Debug builds still get the
     * canvas sample lists and tasks. Older installs lose the untouched default lists once.
     */
    suspend fun seedIfEmpty(sampleTasks: Boolean, cleanDefaults: Boolean) {
        db.lists().migrateShapes()
        if (cleanDefaults) defaultLists.forEach { (name, _, _) -> db.lists().deleteIfEmpty(name) }
        if (!sampleTasks || db.lists().count() > 0) return
        val ids = defaultLists.map { (name, shape, color) -> db.lists().insert(ListEntity(name = name, shape = shape, color = color)) }
        val today = LocalDate.now()
        fun t(title: String, list: Int, due: LocalDate? = today, time: LocalTime? = null, p: Priority = Priority.None,
              done: Boolean = false, sub: Pair<Int, Int> = 0 to 0, reminder: Boolean = false) =
            TaskEntity(title = title, listId = ids[list], due = due, time = time, priority = p, done = done,
                completedAt = if (done) System.currentTimeMillis() else null,
                subtasksDone = sub.first, subtasksTotal = sub.second, reminder = reminder)
        db.tasks().insertAll(listOf(
            t("Renew passport photos", 5, today.minusDays(1), p = Priority.High),
            t("Standup notes for design review", 0, time = LocalTime.of(9, 30), p = Priority.Medium),
            t("Pay electricity bill", 1, time = LocalTime.of(12, 0), p = Priority.High, reminder = true),
            t("Buy groceries", 2, sub = 2 to 5),
            t("30-min run", 3, time = LocalTime.of(18, 0), p = Priority.Low),
            t("Call Mom", 4, time = LocalTime.of(19, 30), reminder = true),
            t("Read 20 pages", 6, time = LocalTime.of(21, 0), p = Priority.Low),
            t("Reply to landlord", 1, done = true),
            t("Book dentist", 5, done = true),
            t("Water plants", 1, done = true),
            t("Plan weekend trip", 5, today.plusDays(2)),
        ))
        // Debug only: a month of past completions so Stats has something to show.
        val zone = java.time.ZoneId.systemDefault()
        val rnd = java.util.Random(7)
        db.tasks().insertAll((1..40).map { n ->
            val day = today.minusDays(rnd.nextInt(45).toLong() + 1)
            val late = rnd.nextInt(5) == 0
            TaskEntity(
                title = "Past task $n", listId = ids[rnd.nextInt(ids.size)], due = if (late) day.minusDays(1) else day,
                done = true, completedAt = day.atTime(9 + rnd.nextInt(10), 0).atZone(zone).toInstant().toEpochMilli(),
            )
        })
    }

    private val defaultLists = listOf(
        Triple("Work", ListShape.Cookie9, ListColor.Indigo),
        Triple("Home", ListShape.Hexagon, ListColor.Amber),
        Triple("Groceries", ListShape.Square, ListColor.Green),
        Triple("Health", ListShape.Triangle, ListColor.Teal),
        Triple("Family", ListShape.Pentagon, ListColor.Pink),
        Triple("Personal", ListShape.Gem, ListColor.Coral),
        Triple("Reading", ListShape.Cookie6, ListColor.Violet),
    )
}

fun TaskEntity.toModel() = Task(id, title, listId, due, time, priority, done, subtasksDone, subtasksTotal, reminder, notes, completedAt, archived, Repeat.decode(repeat))
fun Task.toEntity() = TaskEntity(
    id = id, title = title, listId = listId, notes = notes, due = due, time = time, priority = priority, done = done,
    completedAt = if (done) (completedAt ?: System.currentTimeMillis()) else null,
    subtasksDone = subtasksDone, subtasksTotal = subtasksTotal, reminder = reminder, archived = archived,
    repeat = repeat?.encode().orEmpty(),
)
fun ListEntity.toModel() = TaskList(id, name, shape, color, position)
