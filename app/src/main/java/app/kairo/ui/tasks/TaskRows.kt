@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package app.kairo.ui.tasks

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.model.Priority
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import app.kairo.ui.common.Selection
import app.kairo.ui.common.SelectBadge
import app.kairo.ui.common.TaskOptionsSheet
import app.kairo.ui.common.TaskSwipe
import app.kairo.ui.common.selectableRowColors
import app.kairo.ui.shapes.polygon
import app.kairo.ui.theme.KairoTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit
@Composable
internal fun SectionTitle(title: String, count: Int, error: Boolean, modifier: Modifier = Modifier, trailing: @Composable () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().padding(start = 8.dp, end = 0.dp, top = 12.dp, bottom = 0.dp).heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                append(title)
                withStyle(SpanStyle(color = c.onSurfaceVariant, fontWeight = FontWeight.Medium)) { append(" · $count") }
            },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (error) c.error else c.primary,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

internal fun LazyListScope.rows(
    s: Section, lists: Map<Long, TaskList>, today: LocalDate, selection: Selection,
    toggle: (Task) -> Unit, onOpen: (Task) -> Unit, sectionIndex: Int,
) {
    itemsIndexed(s.tasks, key = { _, t -> "${s.key}-${t.id}" }) { i, t ->
        val motion = MaterialTheme.motionScheme
        Column(
            Modifier.animateItem(
                fadeInSpec = motion.defaultEffectsSpec(), placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = motion.fastEffectsSpec(),
            ),
        ) {
            TaskRowV2(t, lists[t.listId], i, s.tasks.size, today, selection, toggle, onOpen, noDay = s.key.startsWith("u") && s.key != "u")
            if (i < s.tasks.lastIndex) Spacer(Modifier.height(3.dp))
        }
    }
}

/** Canvas .row: priority bar, title, meta line, flag, ⋮. Segmented corners 28 outside / 8 inside. */
@Composable
fun TaskRowV2(
    task: Task, list: TaskList?, index: Int, count: Int, today: LocalDate, selection: Selection,
    toggle: (Task) -> Unit, onOpen: (Task) -> Unit, noDay: Boolean = false,
) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val selected = task.id in selection
    var options by remember { mutableStateOf(false) }
    if (options) TaskOptionsSheet(task, onOpen, toggle, onDismiss = { options = false })
    val titleColor by animateColorAsState(if (task.done) c.onSurfaceVariant else c.onSurface, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "title")
    val big = 28.dp; val small = 8.dp
    val shape = RoundedCornerShape(
        topStart = if (index == 0) big else small, topEnd = if (index == 0) big else small,
        bottomStart = if (index == count - 1) big else small, bottomEnd = if (index == count - 1) big else small,
    )
    TaskSwipe(task, enabled = !selection.active, onComplete = toggle) {
        SegmentedListItem(
            selected = selected,
            onClick = { if (selection.active) selection.toggle(task.id) else onOpen(task) },
            onLongClick = { selection.toggle(task.id) },
            onLongClickLabel = "Select",
            shapes = ListItemDefaults.shapes(shape = shape, selectedShape = RoundedCornerShape(big), pressedShape = RoundedCornerShape(big)),
            colors = selectableRowColors(),
            contentPadding = PaddingValues(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            modifier = Modifier.heightIn(min = 72.dp),
            leadingContent = {
                if (selection.active) SelectBadge(selected)
                else {
                    val bar = when { task.done -> Color.Transparent; task.priority == Priority.High -> c.error
                        task.priority == Priority.Medium -> k.prioMed; task.priority == Priority.Low -> k.prioLow; else -> Color.Transparent }
                    Box(Modifier.size(width = 4.dp, height = 24.dp).background(bar, RoundedCornerShape(2.dp)))
                }
            },
            supportingContent = { Meta(task, list, today, noDay) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.priority == Priority.High && !task.done)
                        Icon(painterResource(R.drawable.ic_flag_filled), "High priority", tint = c.error, modifier = Modifier.size(20.dp))
                    if (!selection.active) IconButton(onClick = { options = true }, shapes = IconButtonDefaults.shapes()) {
                        Icon(painterResource(R.drawable.ic_more_vert), "More actions for ${task.title}", tint = c.onSurfaceVariant)
                    }
                }
            },
        ) {
            Text(
                task.title, style = MaterialTheme.typography.bodyLarge, color = titleColor,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** "9:30 AM · ● Work · 2/5 · ↻ · 🔔" (canvas .meta: 14/20, late dates in error). */
@Composable
internal fun Meta(task: Task, list: TaskList?, today: LocalDate, noDay: Boolean) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.bodyMedium
    val due = task.due
    val late = !task.done && due != null && due < today
    val time = task.time?.format(Clock)
    val days = due?.let { ChronoUnit.DAYS.between(today, it) }
    val whenText = when {
        due == null -> time
        late -> if (days == -1L) "Yesterday" else "${-days!!} days ago"
        due == today || noDay -> time
        days == 1L -> "Tomorrow" + (time?.let { " $it" } ?: "")
        else -> due.format(ShortDay) + (time?.let { " $it" } ?: "")
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        @Composable fun dot() = Text("·", style = style, color = c.onSurfaceVariant)
        if (whenText != null) {
            Text(whenText, style = if (late) style.copy(fontWeight = FontWeight.Medium) else style, color = if (late) c.error else c.onSurfaceVariant, maxLines = 1)
            dot()
        }
        if (list == null) Box(Modifier.size(10.dp).background(c.onSurfaceVariant, CircleShape))
        else Box(Modifier.size(10.dp).background(k.accent(list.color), list.shape.polygon.toShape()))
        Text(list?.name ?: "Inbox", style = style, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (task.subtasksTotal > 0) { dot(); Text("${task.subtasksDone}/${task.subtasksTotal}", style = style, color = c.onSurfaceVariant) }
        if (task.repeat != null) Icon(painterResource(R.drawable.ic_repeat), "Repeats", Modifier.size(16.dp), tint = c.onSurfaceVariant)
        if (task.reminder && task.time != null) Icon(painterResource(R.drawable.ic_notifications), "Reminder", Modifier.size(16.dp), tint = c.onSurfaceVariant)
    }
}

/** The v2 row for other screens (Calendar day list, a list's page, search), with the 1.3.1 call shape. */
@Composable
fun TaskRow(
    task: Task, list: TaskList, index: Int, count: Int, onToggle: (Task) -> Unit, onOpen: (Task) -> Unit,
    @Suppress("UNUSED_PARAMETER") overdue: Boolean = false, selection: Selection? = null,
) {
    val own = app.kairo.ui.common.rememberSelection()
    TaskRowV2(task, list.takeIf { it.id != 0L }, index, count, LocalDate.now(), selection ?: own, onToggle, onOpen)
}
