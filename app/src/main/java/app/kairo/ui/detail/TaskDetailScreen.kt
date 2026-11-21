@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.detail

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.model.Priority
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import app.kairo.reminders.rememberAskNotifications
import app.kairo.ui.shapes.polygon
import app.kairo.ui.common.ListPickerMenu
import app.kairo.ui.common.DateChoice
import app.kairo.ui.common.DateSheet
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.theme.KairoTheme
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TaskDetailScreen(vm: TaskDetailViewModel, lists: List<TaskList>, onBack: () -> Unit) {
    val task by vm.task.collectAsStateWithLifecycle()
    val t = task ?: return
    val list = lists.firstOrNull { it.id == t.listId } ?: return
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val askNotifications = rememberAskNotifications()

    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var listMenu by remember { mutableStateOf(false) }
    var title by remember(t.id) { mutableStateOf(t.title) }
    var notes by remember(t.id) { mutableStateOf(t.notes) }
    // Save text edits as you type (Room writes are cheap); skip no-op writes.
    LaunchedEffect(title) { if (title.isNotBlank() && title != t.title) vm.edit { it.copy(title = title.trim()) } }
    LaunchedEffect(notes) { if (notes != t.notes) vm.edit { it.copy(notes = notes) } }

    Scaffold(
        containerColor = k.bg,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = k.bg),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                // List chip (with the list's shape) — tap to move the task.
                Box {
                    AssistChip(
                        onClick = { listMenu = true },
                        label = { Text(list.name) },
                        leadingIcon = { Box(Modifier.size(14.dp).background(k.accent(list.color), list.shape.polygon.toShape())) },
                    )
                    ListPickerMenu(listMenu, lists, t.listId, onDismiss = { listMenu = false }) { l -> vm.edit { it.copy(listId = l.id) } }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Checkbox(checked = t.done, onCheckedChange = {
                        if (it) haptic.performHapticFeedback(HapticFeedbackType.ToggleOn) // rule 9
                        vm.toggleDone()
                    })
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = MaterialTheme.typography.headlineSmallEmphasized.copy(
                            color = if (t.done) c.onSurfaceVariant else c.onSurface,
                            textDecoration = if (t.done) TextDecoration.LineThrough else null,
                        ),
                        cursorBrush = SolidColor(c.primary),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.weight(1f),
                    )
                }

                dueLabel(t)?.let { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        color = c.onTertiaryContainer,
                        modifier = Modifier
                            .padding(start = 12.dp, top = 8.dp)
                            .background(c.tertiaryContainer, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                Spacer(Modifier.height(20.dp))
                val rows = 4
                DetailRow(0, rows, R.drawable.ic_event, "Date", t.due?.format(DateFmt) ?: "No date") { pickDate = true }
                Gap()
                DetailRow(1, rows, R.drawable.ic_schedule, "Time", t.time?.format(TimeFmt) ?: "No time") { pickTime = true }
                Gap()
                DetailRow(
                    2, rows, R.drawable.ic_notifications, "Reminder", if (t.reminder) "On" else "Off",
                    trailing = { Switch(checked = t.reminder, onCheckedChange = { on -> if (on) askNotifications(); vm.edit { it.copy(reminder = on) } }) },
                ) { if (!t.reminder) askNotifications(); vm.edit { it.copy(reminder = !it.reminder) } }
                Gap()
                DetailRow(
                    3, rows,
                    if (t.priority == Priority.High) R.drawable.ic_flag_filled else R.drawable.ic_flag,
                    "Priority", if (t.priority == Priority.None) "None" else t.priority.name,
                ) { vm.edit { it.copy(priority = Priority.entries[(it.priority.ordinal + 1) % Priority.entries.size]) } }

                Text(
                    "Notes", style = MaterialTheme.typography.titleSmallEmphasized, color = c.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(k.item, MaterialTheme.shapes.large)
                        .padding(16.dp),
                ) {
                    if (notes.isEmpty()) Text("Add notes", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant)
                    BasicTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.onSurface),
                        cursorBrush = SolidColor(c.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(140.dp)) // clear the floating toolbar
            }

            // M3 Expressive floating toolbar + solid Done FAB (canvas TaskDetail board).
            HorizontalFloatingToolbar(
                expanded = true,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.Confirm); onBack() },
                        containerColor = c.primary,
                        contentColor = c.onPrimary,
                    ) { Icon(painterResource(R.drawable.ic_check), "Done") }
                },
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors(toolbarContainerColor = k.navBg),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
            ) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { pickDate = true }) { Icon(painterResource(R.drawable.ic_event), "Date") }
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { if (!t.reminder) askNotifications(); vm.edit { it.copy(reminder = !it.reminder) } }) {
                    Icon(painterResource(R.drawable.ic_notifications), "Reminder")
                }
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject) // rule 9: delete
                    vm.delete(onBack)
                }) { Icon(painterResource(R.drawable.ic_delete), "Delete") }
            }
        }
    }

    if (pickDate || pickTime) {
        // One drawer for date, time and reminder (canvas DatePicker board) — no dialogs.
        val allTasks by LocalKairoApp.current.repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
        DateSheet(
            initial = DateChoice(t.due, t.time, t.reminder),
            taskDays = allTasks.mapNotNull { it.due }.toSet(),
            onDismiss = { pickDate = false; pickTime = false },
        ) { ch ->
            if (ch.reminder && !t.reminder) askNotifications()
            vm.edit { it.copy(due = ch.date, time = ch.time, reminder = ch.reminder) }
            pickDate = false; pickTime = false
        }
    }
}

/** Detail row: icon in a Cookie4 shape (docs/04-screens.md), label, value. */
@Composable
private fun DetailRow(
    index: Int, count: Int, icon: Int, label: String, value: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.segmentedColors(containerColor = KairoTheme.colors.item),
        leadingContent = {
            Box(
                Modifier.size(40.dp).background(c.secondaryContainer, MaterialShapes.Cookie4Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(icon), null, tint = c.onSecondaryContainer, modifier = Modifier.size(20.dp)) }
        },
        supportingContent = { Text(value) },
        trailingContent = trailing,
    ) { Text(label, style = MaterialTheme.typography.bodyLarge) }
}

@Composable private fun Gap() = Spacer(Modifier.height(ListItemDefaults.SegmentedGap))

private val DateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM")
private val TimeFmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/** "Due in 1 h 20 min" / "Due today" / "Overdue · 2 days" pill. */
private fun dueLabel(t: Task): String? {
    if (t.done || t.due == null) return null
    val today = LocalDate.now()
    if (t.time != null) {
        val mins = Duration.between(LocalDateTime.now(), LocalDateTime.of(t.due, t.time)).toMinutes()
        if (mins in 0..(24 * 60)) return "Due in " + (if (mins >= 60) "${mins / 60} h ${mins % 60} min" else "$mins min")
        if (mins < 0 && t.due == today) return "Overdue"
    }
    return when {
        t.due == today -> "Due today"
        t.due.isBefore(today) -> "Overdue · ${Duration.between(t.due.atStartOfDay(), today.atStartOfDay()).toDays()} d"
        t.due == today.plusDays(1) -> "Due tomorrow"
        else -> null
    }
}
