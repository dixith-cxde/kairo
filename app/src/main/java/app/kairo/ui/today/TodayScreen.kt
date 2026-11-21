@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.model.Priority
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import app.kairo.ui.shapes.polygon
import app.kairo.ui.theme.KairoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Everything the Today screen shows. Built from the database in step 4. */
data class TodayState(
    val overdue: List<Task>,
    val today: List<Task>,
    val completed: List<Task>,
    val lists: Map<Long, TaskList>,
) {
    val total get() = today.size + completed.size
    val done get() = completed.size
    val next get() = today.firstOrNull { it.time != null }
}

@Composable
fun TodayScreen(
    state: TodayState,
    onToggle: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val k = KairoTheme.colors
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showCompleted by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = k.bg,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Today") },
                subtitle = { Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))) },
                actions = {
                    IconButton(onClick = { /* Search: later */ }) {
                        Icon(painterResource(R.drawable.ic_search), contentDescription = "Search")
                    }
                    IconButton(onClick = { /* Settings: step 12 */ }) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = k.bg, scrolledContainerColor = k.bg),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = 120.dp, // clear the floating nav
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "hero") { HeroCard(state) }

            if (state.overdue.isNotEmpty()) {
                item(key = "h-overdue") {
                    SectionHeader("Overdue · ${state.overdue.size}", MaterialTheme.colorScheme.error) {
                        TextButton(onClick = { /* Reschedule: step 7 */ }) { Text("Reschedule") }
                    }
                }
                taskGroup(state.overdue, state.lists, onToggle, overdue = true)
            }

            item(key = "h-today") {
                SectionHeader("Today · ${state.today.size} left", MaterialTheme.colorScheme.primary) {
                    Text("Sorted by time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            taskGroup(state.today, state.lists, onToggle)

            item(key = "completed-toggle") {
                CompletedToggle(count = state.completed.size, open = showCompleted) { showCompleted = !showCompleted }
            }
            if (showCompleted) taskGroup(state.completed, state.lists, onToggle, keyPrefix = "c")
        }
    }
}

/** Hero moment #2 (rule 10): progress ring on primaryContainer, a slow Cookie12 shape behind it. */
@Composable
private fun HeroCard(state: TodayState) {
    val c = MaterialTheme.colorScheme
    val left = state.today.size
    val progress = if (state.total == 0) 0f else state.done / state.total.toFloat()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(c.primaryContainer),
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 56.dp, y = (-64).dp)
                .size(210.dp)
                .rotate(12f)
                .alpha(0.10f)
                .background(c.onPrimaryContainer, MaterialShapes.Cookie12Sided.toShape()),
        )
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .semantics { contentDescription = "${state.done} of ${state.total} tasks done" },
                contentAlignment = Alignment.Center,
            ) {
                CircularWavyProgressIndicator(
                    progress = { progress },
                    color = c.primary,
                    trackColor = c.onPrimaryContainer.copy(alpha = 0.18f),
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    "${state.done}/${state.total}",
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = c.onPrimaryContainer,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (left == 0) "All done for today" else "$left to go today",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = c.onPrimaryContainer,
                )
                state.next?.let { n ->
                    Text(
                        "Next: ${n.title} · ${n.time!!.format(TimeFormat)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onPrimaryContainer.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 8.dp)
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmallEmphasized, color = color, modifier = Modifier.weight(1f))
        trailing()
    }
}

private fun LazyListScope.taskGroup(
    tasks: List<Task>,
    lists: Map<Long, TaskList>,
    onToggle: (Task) -> Unit,
    overdue: Boolean = false,
    keyPrefix: String = "t",
) {
    itemsIndexed(tasks, key = { _, t -> "$keyPrefix${t.id}" }) { i, task ->
        TaskRow(task, lists.getValue(task.listId), i, tasks.size, onToggle, overdue)
        if (i < tasks.lastIndex) Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
    }
}

@Composable
fun TaskRow(
    task: Task,
    list: TaskList,
    index: Int,
    count: Int,
    onToggle: (Task) -> Unit,
    overdue: Boolean = false,
) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val prio = when (task.priority) {
        Priority.High -> k.prioHigh
        Priority.Medium -> k.prioMed
        Priority.Low -> k.prioLow
        Priority.None -> c.onSurfaceVariant
    }
    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.segmentedColors(containerColor = k.item),
        leadingContent = {
            Checkbox(
                checked = task.done,
                onCheckedChange = { onToggle(task) },
                colors = CheckboxDefaults.colors(
                    checkedColor = c.primary,
                    uncheckedColor = prio,
                    checkmarkColor = c.onPrimary,
                ),
                modifier = Modifier.semantics {
                    contentDescription = (if (task.done) "Mark not done: " else "Complete ") + task.title
                },
            )
        },
        supportingContent = { TaskMeta(task, list, overdue) },
    ) {
        Text(
            task.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (task.done) c.onSurfaceVariant else c.onSurface,
            textDecoration = if (task.done) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "9:30 AM · ◆ Work · ☑ 2/5 · 🔔" — 12/16 meta line, onSurfaceVariant. */
@Composable
private fun TaskMeta(task: Task, list: TaskList, overdue: Boolean) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.bodySmall
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        @Composable fun dot() = Text("·", style = style, color = c.onSurfaceVariant)
        @Composable fun icon(res: Int, tint: Color = c.onSurfaceVariant) =
            Icon(painterResource(res), contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))

        if (overdue) {
            Text("Yesterday".takeIf { task.due == LocalDate.now().minusDays(1) } ?: task.due.toString(), style = style, color = c.error)
            dot()
        } else if (task.time != null) {
            icon(R.drawable.ic_schedule)
            Text(task.time.format(TimeFormat), style = style, color = c.onSurfaceVariant)
            dot()
        }
        Box(Modifier.size(12.dp).background(k.accent(list.color), list.shape.polygon.toShape()))
        Text(list.name, style = style, color = c.onSurfaceVariant)
        if (task.subtasksTotal > 0) {
            dot(); icon(R.drawable.ic_checklist)
            Text("${task.subtasksDone}/${task.subtasksTotal}", style = style, color = c.onSurfaceVariant)
        }
        if (task.reminder) { dot(); icon(R.drawable.ic_notifications) }
        if (task.priority == Priority.High) { dot(); icon(R.drawable.ic_flag_filled, k.prioHigh) }
    }
}

@Composable
private fun CompletedToggle(count: Int, open: Boolean, onClick: () -> Unit) {
    val k = KairoTheme.colors
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(0, 1),
        colors = ListItemDefaults.segmentedColors(containerColor = k.item),
        modifier = Modifier.padding(top = 12.dp),
        leadingContent = {
            Icon(
                painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                modifier = Modifier.rotate(if (open) 180f else 0f),
            )
        },
        trailingContent = {
            Box(
                Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text("$count", style = MaterialTheme.typography.labelMediumEmphasized, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        },
    ) {
        Text("Completed", style = MaterialTheme.typography.titleSmallEmphasized)
    }
}

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

fun fakeTodayState(): TodayState =
    buildTodayState(FakeData.tasks, FakeData.lists.associateBy { it.id }, LocalDate.now())

@Preview(name = "Dark", showSystemUi = true)
@Composable
private fun TodayDark() = KairoTheme(darkTheme = true) { TodayScreen(fakeTodayState(), onToggle = {}) }

@Preview(name = "Light", showSystemUi = true)
@Composable
private fun TodayLight() = KairoTheme(darkTheme = false) { TodayScreen(fakeTodayState(), onToggle = {}) }
