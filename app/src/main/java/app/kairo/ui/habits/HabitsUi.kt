@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import app.kairo.habits.Habit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.reminders.rememberAskNotifications
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.shapes.polygon
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Habits on the Today tab: one row per habit with its shape, streak and a check for today.
 * Tap a row for the monthly heat map. Habits are daily (owner, 2026-11-21).
 */
fun LazyListScope.habitsSection(habits: List<Habit>, onAdd: () -> Unit, onOpen: (Habit) -> Unit, onToggle: (Habit) -> Unit) {
    item(key = "h-habits") {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val done = habits.count { it.doneOn(LocalDate.now()) }
            Text(
                if (habits.isEmpty()) "Habits" else "Habits · $done of ${habits.size}",
                style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAdd, shapes = ButtonDefaults.shapes()) { Text("New habit") }
        }
    }
    habits.forEachIndexed { i, h ->
        item(key = "habit-${h.id}") {
            val k = KairoTheme.colors
            val c = MaterialTheme.colorScheme
            val today = h.doneOn(LocalDate.now())
            SegmentedListItem(
                onClick = { onOpen(h) },
                shapes = ListItemDefaults.segmentedShapes(i, habits.size),
                colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                leadingContent = { Box(Modifier.size(32.dp).background(k.accent(h.color), h.shape.polygon.toShape())) },
                supportingContent = {
                    val s = h.streak()
                    Text(
                        (if (s > 0) "$s-day streak" else "Daily") + (h.reminderTime?.let { " · " + it.format(TimeFmt) } ?: ""),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), // rule 8
                    )
                },
                trailingContent = {
                    FilledIconToggleButton(checked = today, onCheckedChange = { onToggle(h) }, shapes = IconButtonDefaults.toggleableShapes()) {
                        Icon(painterResource(R.drawable.ic_check), if (today) "Done today" else "Mark done today")
                    }
                },
            ) { Text(h.name, color = c.onSurface) }
            if (i < habits.lastIndex) Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
        }
    }
}

/** New habit (or edit [habit]): name and an optional daily reminder time. */
@Composable
fun HabitSheet(habit: Habit? = null, onDismiss: () -> Unit) {
    val app = LocalKairoApp.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val askNotifications = rememberAskNotifications()
    var name by remember { mutableStateOf(habit?.name.orEmpty()) }
    var remind by remember { mutableStateOf(habit == null || habit.reminderTime != null) }
    val time = rememberTimePickerState(habit?.reminderTime?.hour ?: 9, habit?.reminderTime?.minute ?: 0)
    fun close() = scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (habit == null) "New habit" else "Edit habit", style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(start = 4.dp))
            OutlinedTextField(name, { name = it }, label = { Text("e.g. Take medication") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            SegmentedListItem(
                onClick = { if (!remind) askNotifications(); remind = !remind },
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                colors = ListItemDefaults.segmentedColors(containerColor = KairoTheme.colors.item),
                trailingContent = { Switch(remind, { if (it) askNotifications(); remind = it }) },
                supportingContent = { Text("Every day at this time") },
            ) { Text("Remind me") }
            if (remind) TimePicker(time, Modifier.align(Alignment.CenterHorizontally))
            Row {
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
                        val t = if (remind) LocalTime.of(time.hour, time.minute) else null
                        app.appScope.launch {
                            if (habit == null) app.habits.add(name, ListShape.choices.random(), ListColor.entries.random(), t)
                            else app.habits.update(habit.copy(name = name.trim(), reminderTime = t))
                        }
                        close()
                    },
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = PaddingValues(horizontal = 32.dp),
                ) { Text("Save") }
            }
        }
    }
}

/** Habit detail: streaks and a monthly heat map. Tap a day (up to today) to fix it. */
@Composable
fun HabitDetailSheet(habit: Habit, onEdit: () -> Unit, onDismiss: () -> Unit) {
    val app = LocalKairoApp.current
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val habits by app.habits.habits.collectAsStateWithLifecycle(initialValue = emptyList())
    val h = habits.firstOrNull { it.id == habit.id } ?: habit
    var month by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).background(k.accent(h.color), h.shape.polygon.toShape()))
                Text(h.name, style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(start = 12.dp).weight(1f))
                IconButton(onClick = onEdit, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_edit), "Edit") }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject) // rule 9: delete
                    app.appScope.launch { app.habits.delete(h.id) }; onDismiss()
                }, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_delete), "Delete") }
            }
            val inMonth = h.days.count { YearMonth.from(it) == month }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("${h.streak()}", "streak", Modifier.weight(1f))
                Stat("${h.best()}", "best", Modifier.weight(1f))
                Stat("$inMonth", "this month", Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 4.dp))
                IconButton(onClick = { month = month.minusMonths(1) }, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_chevron_left), "Previous month") }
                IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.now(), shapes = IconButtonDefaults.shapes()) {
                    Icon(painterResource(R.drawable.ic_chevron_right), "Next month")
                }
            }
            // Heat map: Monday-first month grid; done days in the habit's color.
            Row { listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            } }
            val lead = month.atDay(1).dayOfWeek.value - 1
            val cells = (0 until lead).map { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
            cells.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0 until 7).forEach { i ->
                        val d = week.getOrNull(i)
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (d != null) {
                                val done = h.doneOn(d)
                                val future = d.isAfter(today)
                                Box(
                                    Modifier.fillMaxWidth().aspectRatio(1f)
                                        .background(if (done) k.accent(h.color) else c.surfaceContainerHighest.copy(alpha = if (future) 0.4f else 1f), RoundedCornerShape(8.dp))
                                        .clickable(enabled = !future) { app.appScope.launch { app.habits.set(h.id, d, !done) } },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${d.dayOfMonth}", style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum",
                                        fontWeight = if (d == today) FontWeight.Bold else null),
                                        color = if (done) c.surface else c.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.background(KairoTheme.colors.item, RoundedCornerShape(16.dp)).padding(12.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmallEmphasized.copy(fontFeatureSettings = "tnum"))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


private val TimeFmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
