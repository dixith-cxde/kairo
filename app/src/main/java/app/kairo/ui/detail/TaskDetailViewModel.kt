package app.kairo.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kairo.data.TaskRepository
import app.kairo.data.model.Task
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskDetailViewModel(private val repo: TaskRepository, taskId: Long) : ViewModel() {
    val task: StateFlow<Task?> = repo.task(taskId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Every edit is saved straight away; there is no separate "save" step. */
    fun edit(change: (Task) -> Task) {
        val t = task.value ?: return
        viewModelScope.launch { repo.update(change(t)) }
    }

    fun toggleDone() { task.value?.let { t -> viewModelScope.launch { repo.setDone(t, !t.done) } } }
    fun delete(after: () -> Unit) { task.value?.let { t -> viewModelScope.launch { repo.delete(t); after() } } }
}
