@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package app.kairo.ui.tasks

import androidx.compose.material3.MaterialTheme

import app.kairo.ui.common.TabSearchBar

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.model.Task
import app.kairo.ui.common.DateChoice
import app.kairo.ui.common.DateSheet
import app.kairo.ui.common.EmptyState
import app.kairo.ui.common.LocalAppSnackbar
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.common.SelectionTopBar
import app.kairo.ui.common.TaskSelectionToolbar
import app.kairo.ui.common.rememberEntrance
import app.kairo.ui.common.rememberSelection
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
/** The date view (connected group at the top). Today is the default view (canvas v65). */
enum class TaskView { All, Overdue, Today, Upcoming, Done }

/** One titled group of rows. */
internal data class Section(val key: String, val title: String, val tasks: List<Task>, val error: Boolean = false, val reschedule: Boolean = false)

/**
 * v2 Tasks tab (canvas v65, approved 2026-11-26): search bar + avatar, the date view group, the
 * compact hero, a single-select list filter row, then sections of segmented rows. Rows have no
 * checkbox: swipe right completes, swipe left deletes, long-press selects, ⋮ opens the row menu.
 */
@Composable
fun TasksScreen(
    onOpen: (Task) -> Unit,
    onSearch: () -> Unit,
    onVoice: () -> Unit,
    onLists: () -> Unit,
    onAccount: () -> Unit,
) {
    val app = LocalKairoApp.current
    val all by app.repository.tasks.collectAsStateWithLifecycle(initialValue = null)
    val lists by app.repository.lists.collectAsStateWithLifecycle(initialValue = emptyList())
    var view by rememberSaveable { mutableStateOf(TaskView.Today) }
    // null = every list; 0 = Inbox (tasks with no list); else a list id.
    var pickedList by rememberSaveable { mutableStateOf<Long?>(null) }
    val selection = rememberSelection()
    val snackbar = LocalAppSnackbar.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var rescheduleOverdue by remember { mutableStateOf<List<Long>?>(null) }
    val tasks0 = all ?: return
    val today = LocalDate.now()
    val listMap = lists.associateBy { it.id }
    val tasks = when (pickedList) {
        null -> tasks0
        0L -> tasks0.filter { it.listId == null || it.listId !in listMap }
        else -> tasks0.filter { it.listId == pickedList }
    }

    // Complete / not done with Undo (rule 9: haptic on complete and undo).
    val toggle: (Task) -> Unit = { t ->
        if (!t.done) haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
        scope.launch { app.repository.setDone(t, !t.done) }
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(if (t.done) "Marked not done" else "Completed", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                app.repository.setDone(t.copy(done = !t.done), t.done)
            }
        }
    }

    val byTime = compareBy<Task, java.time.LocalTime?>(nullsLast()) { it.time }
    val byDay = compareBy<Task>({ it.due }, { it.time ?: java.time.LocalTime.MAX })
    val open = tasks.filter { !it.done }
    val overdue = open.filter { it.due != null && it.due < today }.sortedWith(byDay)
    val todayOpen = open.filter { it.due == today }.sortedWith(byTime)
    val upcoming = open.filter { it.due != null && it.due > today && it.due <= today.plusDays(7) }.sortedWith(byDay)
    val later = open.filter { it.due != null && it.due > today.plusDays(7) }.sortedWith(byDay)
    val noDate = open.filter { it.due == null }
    val doneToday = tasks.filter { it.done && (it.due == null || !it.due.isAfter(today)) && doneOn(it, today) }

    val sections = when (view) {
        TaskView.All -> listOf(
            Section("o", "Overdue", overdue, error = true, reschedule = true), Section("t", "Today", todayOpen),
            Section("u", "Next 7 days", upcoming), Section("l", "Later", later), Section("n", "No date", noDate),
            Section("d", "Done", doneToday),
        )
        TaskView.Overdue -> listOf(Section("o", "Overdue", overdue, error = true, reschedule = true))
        TaskView.Today -> listOf(Section("t", "To do", todayOpen), Section("d", "Done today", doneToday))
        TaskView.Upcoming -> upcoming.groupBy { it.due!! }.map { (d, ts) ->
            Section("u$d", if (d == today.plusDays(1)) "Tomorrow" else d.format(Weekday), ts.sortedWith(byTime))
        } + Section("l", "Later", later)
        TaskView.Done -> listOf(Section("d", "Done today", doneToday))
    }.filter { it.tasks.isNotEmpty() }

    // Hero: today's tasks in the current list filter.
    val heroDone = tasks.count { it.done && it.due == today }
    val heroTotal = heroDone + todayOpen.size
    val next = todayOpen.firstOrNull { it.time != null } ?: todayOpen.firstOrNull()
    val heroTitle = when (val p = pickedList) { null -> "Today"; 0L -> "Inbox"; else -> listMap[p]?.name ?: "Today" }
    val nextLine = when {
        todayOpen.isEmpty() -> if (heroTotal > 0) "All done. The rest of the day is yours" else "Nothing due"
        else -> "Next" + (next?.time?.let { " " + it.format(Clock) } ?: "") + " · " + next!!.title
    }

    val entrance = rememberEntrance()
    val k = KairoTheme.colors
    Box(Modifier.fillMaxSize().background(k.bg)) {
        Column(Modifier.fillMaxSize()) {
            TabSearchBar("Search in tasks", onSearch = onSearch, onVoice = onVoice, onMenu = onLists, onAccount = onAccount)
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 168.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "views") { ViewGroup(view) { view = it; selection.clear() } }
                item(key = "hero") { with(entrance) { Hero(heroTitle, nextLine, heroDone, heroTotal, Modifier.enter("hero", 0)) } }
                item(key = "lists") { ListRow(lists, pickedList) { pickedList = it; selection.clear() } }
                sections.forEachIndexed { si, s ->
                    item(key = "h-${s.key}") {
                        SectionTitle(s.title, s.tasks.size, s.error, Modifier.animateItem()) {
                            if (s.reschedule) TextButton(
                                onClick = { rescheduleOverdue = s.tasks.map { it.id } }, shapes = ButtonDefaults.shapes(),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                            ) {
                                Icon(painterResource(R.drawable.ic_event_upcoming), null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (s.tasks.size > 1) "Reschedule all" else "Reschedule")
                            }
                        }
                    }
                    rows(s, listMap, today, selection, toggle, onOpen, si)
                }
                if (sections.isEmpty()) item(key = "empty") { Empty(view, heroTotal > 0) }
            }
        }
        SelectionTopBar(selection, sections.flatMap { s -> s.tasks.map { it.id } }, Modifier.align(Alignment.TopCenter).statusBarsPadding())
        TaskSelectionToolbar(selection, tasks, lists, onOpen, modifier = Modifier.align(Alignment.BottomCenter))
    }
    rescheduleOverdue?.let { ids ->
        DateSheet(
            initial = DateChoice(today, null, true),
            title = if (ids.size > 1) "Reschedule ${ids.size} tasks" else "Reschedule",
            onDismiss = { rescheduleOverdue = null },
        ) { ch ->
            scope.launch { app.repository.reschedule(ids, ch.date, ch.time, ch.reminder) }
            rescheduleOverdue = null
        }
    }
}

/** Done today: completed today (or a done task due today when the completion time is unknown). */
internal fun doneOn(t: Task, today: LocalDate): Boolean {
    val at = t.completedAt ?: return t.due == today
    val day = java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    return day == today
}

// ---------------------------------------------------------------- header
@Composable
internal fun Empty(view: TaskView, hadToday: Boolean) {
    val (title, body) = when (view) {
        TaskView.Today -> if (hadToday) "All done" to "The rest of the day is yours." else "A clear day" to "Nothing due today."
        TaskView.Overdue -> "Nothing overdue" to "You're caught up."
        TaskView.Upcoming -> "Nothing coming up" to "The week ahead is open."
        TaskView.Done -> "Nothing done yet" to "Finished tasks show here."
        TaskView.All -> "No tasks" to "Add one with +."
    }
    EmptyState(title = title, body = body, size = 120.dp, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.padding(top = 24.dp),
        icon = if (view == TaskView.Overdue || view == TaskView.Done) R.drawable.ic_check else R.drawable.ic_task_alt)
}

internal val Clock: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
internal val Weekday: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE")
internal val ShortDay: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
