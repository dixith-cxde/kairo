package app.kairo.data

import app.kairo.data.db.KairoDatabase
import app.kairo.data.db.ListEntity
import app.kairo.data.db.TaskEntity
import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import app.kairo.data.model.Priority
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

/** The one place screens get data from. Screens see domain models, never Room entities. */
class TaskRepository(private val db: KairoDatabase) {

    val tasks: Flow<List<Task>> = db.tasks().observeAll().map { it.map(TaskEntity::toModel) }
    val lists: Flow<List<TaskList>> = db.lists().observeAll().map { it.map(ListEntity::toModel) }

    fun task(id: Long): Flow<Task?> = db.tasks().observe(id).map { it?.toModel() }

    suspend fun setDone(task: Task, done: Boolean) =
        db.tasks().setDone(task.id, done, if (done) System.currentTimeMillis() else null)

    suspend fun add(task: Task): Long = db.tasks().insert(task.toEntity())
    suspend fun update(task: Task) = db.tasks().update(task.toEntity())
    suspend fun delete(task: Task) = db.tasks().delete(task.id)

    suspend fun addList(list: TaskList): Long = db.lists().insert(ListEntity(name = list.name, shape = list.shape, color = list.color))
    suspend fun updateList(list: TaskList) = db.lists().update(ListEntity(list.id, list.name, list.shape, list.color, list.position))
    suspend fun reorderLists(lists: List<TaskList>) =
        db.lists().updateAll(lists.mapIndexed { i, l -> ListEntity(l.id, l.name, l.shape, l.color, i) })
    suspend fun deleteList(list: TaskList) = db.lists().delete(list.id)

    /** First launch: the default lists. Debug builds also get the canvas sample tasks. */
    suspend fun seedIfEmpty(sampleTasks: Boolean) {
        if (db.lists().count() > 0) return
        val ids = defaultLists.map { (name, shape, color) -> db.lists().insert(ListEntity(name = name, shape = shape, color = color)) }
        if (!sampleTasks) return
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
    }

    private val defaultLists = listOf(
        Triple("Work", ListShape.Cookie9, ListColor.Indigo),
        Triple("Home", ListShape.Arch, ListColor.Amber),
        Triple("Groceries", ListShape.Clover, ListColor.Green),
        Triple("Health", ListShape.Sunny, ListColor.Teal),
        Triple("Family", ListShape.Pentagon, ListColor.Pink),
        Triple("Personal", ListShape.Gem, ListColor.Coral),
        Triple("Reading", ListShape.Cookie6, ListColor.Violet),
    )
}

fun TaskEntity.toModel() = Task(id, title, listId, due, time, priority, done, subtasksDone, subtasksTotal, reminder, notes)
fun Task.toEntity() = TaskEntity(
    id = id, title = title, listId = listId, notes = notes, due = due, time = time, priority = priority, done = done,
    completedAt = if (done) System.currentTimeMillis() else null,
    subtasksDone = subtasksDone, subtasksTotal = subtasksTotal, reminder = reminder,
)
fun ListEntity.toModel() = TaskList(id, name, shape, color, position)
