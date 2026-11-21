package app.kairo.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kairo.data.TaskRepository
import app.kairo.data.model.Task
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Holds Today's state outside the UI so it survives rotation, and talks to the repository.
 * The screen only reads `state` and calls `toggle`.
 */
class TodayViewModel(private val repo: TaskRepository) : ViewModel() {

    val state: StateFlow<TodayState?> = combine(repo.tasks, repo.lists) { tasks, lists ->
        buildTodayState(tasks, lists.associateBy { it.id }, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggle(task: Task) {
        viewModelScope.launch { repo.setDone(task, !task.done) }
    }
}

fun buildTodayState(tasks: List<Task>, lists: Map<Long, app.kairo.data.model.TaskList>, today: LocalDate) = TodayState(
    overdue = tasks.filter { !it.done && it.due != null && it.due < today },
    today = tasks.filter { !it.done && it.due == today }.sortedWith(compareBy(nullsLast()) { it.time }),
    // Done tasks due today count toward today's ring.
    completed = tasks.filter { it.done && it.due == today },
    // Next 7 days, for the Upcoming chip.
    upcoming = tasks.filter { !it.done && it.due != null && it.due > today && it.due <= today.plusDays(7) }
        .sortedWith(compareBy<Task>({ it.due }, { it.time ?: java.time.LocalTime.MAX })),
    lists = lists,
)
