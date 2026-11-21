package app.kairo.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kairo.R
import app.kairo.data.TaskRepository
import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Smart lists are views over all tasks. Negative ids so they never clash with real list ids. */
enum class SmartList(val id: Long, val label: String, val icon: Int, val matches: (Task, LocalDate) -> Boolean) {
    Inbox(-1, "Inbox", R.drawable.ic_inbox, { t, _ -> !t.done && t.due == null }),
    Today(-2, "Today", R.drawable.ic_today, { t, d -> !t.done && t.due != null && !t.due.isAfter(d) }),
    Next7(-3, "Next 7 days", R.drawable.ic_date_range, { t, d -> !t.done && t.due != null && t.due.isAfter(d) && !t.due.isAfter(d.plusDays(7)) }),
    Completed(-4, "Completed", R.drawable.ic_task_alt, { t, _ -> t.done }),
    /** Shows archived tasks (they're hidden everywhere else); actions there unarchive. */
    Archived(-5, "Archived", R.drawable.ic_archive, { _, _ -> true });

    companion object { fun of(id: Long) = entries.firstOrNull { it.id == id } }
}

data class ListsState(
    val smartCounts: Map<SmartList, Int>,
    val lists: List<TaskList>,
    val openCounts: Map<Long, Int>,
)

class ListsViewModel(private val repo: TaskRepository) : ViewModel() {
    val state: StateFlow<ListsState?> = combine(repo.tasks, repo.lists, repo.archived) { tasks, lists, archived ->
        val today = LocalDate.now()
        ListsState(
            smartCounts = SmartList.entries.associateWith { s ->
                if (s == SmartList.Archived) archived.size else tasks.count { s.matches(it, today) }
            },
            lists = lists,
            openCounts = tasks.filter { !it.done }.groupingBy { it.listId }.eachCount(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun create(name: String, shape: ListShape, color: ListColor) {
        viewModelScope.launch { repo.addList(TaskList(0, name.trim(), shape, color, position = Int.MAX_VALUE)) }
    }
}

enum class ListFilter(val label: String) { Open("Open"), Upcoming("Upcoming"), NoDate("No date"), Done("Done") }

data class ListDetailState(
    val title: String,
    val list: TaskList?,          // null for smart lists
    val smart: SmartList?,
    val sections: List<Pair<String, List<Task>>>,
    val lists: Map<Long, TaskList>,
)

class ListDetailViewModel(private val repo: TaskRepository, private val listId: Long) : ViewModel() {
    val filter = kotlinx.coroutines.flow.MutableStateFlow(ListFilter.Open)

    private val source = if (SmartList.of(listId) == SmartList.Archived) repo.archived else repo.tasks

    val state: StateFlow<ListDetailState?> = combine(source, repo.lists, filter) { tasks, lists, f ->
        val today = LocalDate.now()
        val smart = SmartList.of(listId)
        val list = lists.firstOrNull { it.id == listId }
        val mine = when {
            smart != null -> tasks.filter { smart.matches(it, today) }
            else -> tasks.filter { it.listId == listId }
        }
        val shown = when (f) {
            ListFilter.Open -> mine.filter { !it.done }
            ListFilter.Upcoming -> mine.filter { !it.done && it.due != null && !it.due.isBefore(today) }
            ListFilter.NoDate -> mine.filter { !it.done && it.due == null }
            ListFilter.Done -> mine.filter { it.done }
        }.let { if (smart == SmartList.Completed || smart == SmartList.Archived) mine else it }
        ListDetailState(
            title = smart?.label ?: list?.name ?: "",
            list = list,
            smart = smart,
            sections = sectionize(shown, today),
            lists = lists.associateBy { it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggle(t: Task) { viewModelScope.launch { repo.setDone(t, !t.done) } }

    private fun sectionize(tasks: List<Task>, today: LocalDate): List<Pair<String, List<Task>>> {
        val sorted = tasks.sortedWith(compareBy<Task, LocalDate?>(nullsLast()) { it.due }.thenBy(nullsLast()) { it.time })
        return listOf(
            "Done" to sorted.filter { it.done },
            "Overdue" to sorted.filter { !it.done && it.due != null && it.due.isBefore(today) },
            "Today" to sorted.filter { !it.done && it.due == today },
            "Upcoming" to sorted.filter { !it.done && it.due != null && it.due.isAfter(today) },
            "No date" to sorted.filter { !it.done && it.due == null },
        ).filter { it.second.isNotEmpty() }
    }
}
