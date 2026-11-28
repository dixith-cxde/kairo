@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class DateChoice(val date: LocalDate?, val time: LocalTime?, val reminder: Boolean)

/**
 * Date & reminder drawer (canvas DatePicker board): Date / Duration group, four big quick picks
 * (the picked one turns solid primary and its corners tighten full → 16), month grid in a
 * 28-corner container with tertiary dots on days that have tasks, Time / Reminder rows, Clear + Done.
 */
@Composable
fun DateSheet(
    initial: DateChoice,
    taskDays: Set<LocalDate> = emptySet(),
    title: String = "Date and reminder",
    onDismiss: () -> Unit,
    onDone: (DateChoice) -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val today = LocalDate.now()
    val ctx = LocalContext.current

    var date by rememberSaveable { mutableStateOf(initial.date?.toString()) }
    var time by rememberSaveable { mutableStateOf(initial.time?.toString()) }
    var reminder by rememberSaveable { mutableStateOf(initial.reminder) }
    var month by rememberSaveable { mutableStateOf(YearMonth.from(initial.date ?: today).toString()) }
    var durationMode by rememberSaveable { mutableStateOf(false) }
    var editingTime by rememberSaveable { mutableStateOf(false) }
    val picked = date?.let(LocalDate::parse)
    val pickedTime = time?.let(LocalTime::parse)

    fun close(after: () -> Unit) = scope.launch { sheet.hide() }.invokeOnCompletion { after() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(start = 4.dp))
            // Date / Duration: M3 Expressive connected button group.
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                listOf(false to "Date", true to "Duration").forEachIndexed { i, (mode, label) ->
                    ToggleButton(
                        checked = durationMode == mode,
                        onCheckedChange = { durationMode = mode },
                        shapes = if (i == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                    ) {
                        Icon(painterResource(if (mode) R.drawable.ic_timelapse else R.drawable.ic_event), null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(label)
                    }
                }
            }

            AnimatedContent(durationMode, label = "mode") { dur ->
                if (!dur) {
                    // Four big quick picks.
                    val nextMon = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickPick(R.drawable.ic_wb_sunny, "Today", picked == today, Modifier.weight(1f)) { date = today.toString() }
                        QuickPick(R.drawable.ic_wb_twilight, "Tomorrow", picked == today.plusDays(1), Modifier.weight(1f)) { date = today.plusDays(1).toString() }
                        QuickPick(R.drawable.ic_next_week, "Next Mon", picked == nextMon, Modifier.weight(1f)) { date = nextMon.toString() }
                        QuickPick(R.drawable.ic_event_busy, "No date", picked == null, Modifier.weight(1f)) { date = null; time = null }
                    }
                } else {
                    // Duration: "from now" picks that set both date and time.
                    val now = LocalDateTime.now().withSecond(0).withNano(0)
                    val opts = listOf(
                        "In 1 hour" to now.plusHours(1),
                        "In 3 hours" to now.plusHours(3),
                        "Tonight" to today.atTime(20, 0),
                        "Weekend" to today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).atTime(10, 0),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        opts.forEach { (label, at) ->
                            val on = picked == at.toLocalDate() && pickedTime == at.toLocalTime()
                            QuickPick(R.drawable.ic_timelapse, label, on, Modifier.weight(1f)) {
                                date = at.toLocalDate().toString(); time = at.toLocalTime().toString()
                            }
                        }
                    }
                }
            }

            // Month grid.
            val ym = YearMonth.parse(month)
            Column(Modifier.clip(RoundedCornerShape(28.dp)).background(k.item).padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)) {
                Row(Modifier.height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleMediumEmphasized,
                        modifier = Modifier.weight(1f).padding(start = 12.dp))
                    IconButton(onClick = { month = ym.minusMonths(1).toString() }, shapes = IconButtonDefaults.shapes()) {
                        Icon(painterResource(R.drawable.ic_chevron_left), "Previous month")
                    }
                    IconButton(onClick = { month = ym.plusMonths(1).toString() }, shapes = IconButtonDefaults.shapes()) {
                        Icon(painterResource(R.drawable.ic_chevron_right), "Next month")
                    }
                }
                val motion = MaterialTheme.motionScheme
                AnimatedContent(
                    ym,
                    transitionSpec = {
                        val d = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(motion.defaultSpatialSpec()) { it * d / 3 } + fadeIn(motion.defaultEffectsSpec())) togetherWith
                            (slideOutHorizontally(motion.defaultSpatialSpec()) { -it * d / 3 } + fadeOut(motion.fastEffectsSpec()))
                    },
                    label = "grid",
                ) { m -> DayGrid(m, picked, today, taskDays) { date = it.toString() } }
            }

            // Time / Reminder rows (segmented list).
            SegmentedListItem(
                onClick = { editingTime = !editingTime },
                shapes = ListItemDefaults.segmentedShapes(0, 2),
                colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                leadingContent = { Icon(painterResource(R.drawable.ic_schedule), null) },
                trailingContent = {
                    Text(pickedTime?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) ?: "None",
                        style = MaterialTheme.typography.labelLargeEmphasized, color = c.primary)
                },
            ) { Text("Time") }
            AnimatedVisibility(
                editingTime,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(),
            ) {
                val init = pickedTime ?: LocalTime.of(9, 0)
                var th by remember { mutableStateOf(init.hour) }
                var tm by remember { mutableStateOf(init.minute) }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(k.item).padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    ExpressiveTimePicker(th, tm, { h, m -> th = h; tm = m }, is24 = android.text.format.DateFormat.is24HourFormat(ctx))
                    Row {
                        TextButton(onClick = { time = null; editingTime = false }, shapes = ButtonDefaults.shapes()) { Text("No time") }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            time = LocalTime.of(th, tm).toString()
                            if (date == null) date = today.toString()
                            editingTime = false
                        }, shapes = ButtonDefaults.shapes()) { Text("Set time") }
                    }
                }
            }
            SegmentedListItem(
                onClick = { reminder = !reminder },
                shapes = ListItemDefaults.segmentedShapes(1, 2),
                colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                leadingContent = { Icon(painterResource(R.drawable.ic_notifications), null) },
                supportingContent = { Text(if (pickedTime != null) "At the due time" else "At 9:00 AM on the day") },
                trailingContent = { Switch(reminder, onCheckedChange = { reminder = it }) },
            ) { Text("Reminder") }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { close { onDone(DateChoice(null, null, false)) } }, shapes = ButtonDefaults.shapes()) { Text("Clear") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
                        close { onDone(DateChoice(picked, if (picked == null) null else pickedTime, reminder && picked != null)) }
                    },
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 32.dp),
                ) { Text("Done") }
            }
        }
    }
}

/** Big quick pick: full pill at rest; picked = solid primary with corners tightening to 16dp. */
@Composable
private fun QuickPick(icon: Int, label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val corner by animateDpAsState(if (on) 16.dp else 36.dp, motion.fastSpatialSpec(), label = "qpCorner")
    val bg by animateColorAsState(if (on) c.primary else KairoTheme.colors.item, motion.defaultEffectsSpec(), label = "qpBg")
    val fg by animateColorAsState(if (on) c.onPrimary else c.onSurface, motion.defaultEffectsSpec(), label = "qpFg")
    Column(
        modifier.height(72.dp).clip(RoundedCornerShape(corner)).background(bg)
            .clickable(role = Role.RadioButton, onClick = onClick).semantics { selected = on },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(icon), null, tint = fg, modifier = Modifier.size(22.dp))
        Text(label, style = if (on) MaterialTheme.typography.labelMediumEmphasized else MaterialTheme.typography.labelMedium,
            color = fg, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DayGrid(ym: YearMonth, picked: LocalDate?, today: LocalDate, taskDays: Set<LocalDate>, onPick: (LocalDate) -> Unit) {
    val c = MaterialTheme.colorScheme
    val lead = ym.atDay(1).dayOfWeek.value % 7 // Sunday first, as on the canvas
    val cells = List(lead) { null } + (1..ym.lengthOfMonth()).map { ym.atDay(it) }
    Column {
        Row {
            (0..6).forEach { i ->
                Text(DayOfWeek.SUNDAY.plus(i.toLong()).getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp))
            }
        }
        cells.chunked(7).forEach { week ->
            Row {
                (0 until 7).forEach { i ->
                    val d = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (d != null) {
                            val sel = d == picked
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(if (sel) c.primary else Color.Transparent)
                                    .clickable(role = Role.Button) { onPick(d) }
                                    .semantics { contentDescription = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${d.dayOfMonth}",
                                    style = if (sel || d == today) MaterialTheme.typography.bodyLargeEmphasized else MaterialTheme.typography.bodyLarge,
                                    color = when { sel -> c.onPrimary; d == today -> c.primary; d.isBefore(today) -> c.onSurfaceVariant; else -> c.onSurface })
                                if (d in taskDays && !sel) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(4.dp).background(c.tertiary, CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}
