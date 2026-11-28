@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.kairo.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import app.kairo.habits.Habit
import app.kairo.habits.HabitEntity
import app.kairo.reminders.rememberAskNotifications
import app.kairo.ui.calendar.SwitchLine
import app.kairo.ui.common.ExpressiveTimePicker
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * v2 New / Edit habit drawer (canvas "new habit" / "edit habit"): preview row (tap the shape for
 * icons), name, Shape + Colour, then Goal, Repeat on and Remind me. [part] "repeat" or "remind"
 * shows only that item (the habit page's rows). Create / Save / Done.
 */
@Composable
fun HabitSheet(habit: Habit? = null, part: String? = null, onDelete: (() -> Unit)? = null, onDismiss: () -> Unit) {
    val app = LocalKairoApp.current
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val askNotifications = rememberAskNotifications()
    val all by app.habits.habits.collectAsStateWithLifecycle(initialValue = emptyList())
    var d by rememberSaveable(stateSaver = DraftSaver) {
        mutableStateOf(habit?.let { h ->
            HabitDraft(h.name, h.icon.ifEmpty { "star" }, h.shapeName, h.color, h.e.goal, h.e.target, h.e.unit, h.e.step, h.weekdays, h.perWeek,
                h.reminderTime != null, h.reminderTime ?: LocalTime.of(9, 0))
        } ?: HabitDraft("", "star", HabitLook.shapes.random(), ListColor.entries.random(), "yes", 1.0, "", 1.0, null, null, true, LocalTime.of(9, 0)))
    }
    var iconOpen by rememberSaveable { mutableStateOf(false) }
    var other by rememberSaveable { mutableStateOf(d.time !in QuickTimes) }
    val name = d.name.trim()
    val clash = name.isNotEmpty() && all.any { it.id != habit?.id && it.name.equals(name, ignoreCase = true) }
    val ok = name.isNotEmpty() && !clash
    fun close() = scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
    fun save() {
        if (!ok) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
        val t = if (d.remind) d.time else null
        val target = when (d.goal) { "yes" -> 1.0; else -> d.target.coerceAtLeast(0.5) }
        val unit = when (d.goal) { "count" -> d.unit.ifBlank { "times" }; "time" -> "min"; else -> "" }
        val step = if (d.goal == "count") d.step else 1.0
        val days = if (d.perWeek != null) null else d.days?.sorted()?.joinToString(",")
        app.appScope.launch {
            if (habit == null) app.habits.add(HabitEntity(name = name, shape = ListShape.Square, color = d.color, reminderTime = t, icon = d.icon, hshape = d.shape,
                goal = d.goal, target = target, unit = unit, step = step, days = days, perWeek = d.perWeek))
            else app.habits.update(habit.e.copy(name = name, color = d.color, reminderTime = t, icon = d.icon, hshape = d.shape,
                goal = d.goal, target = target, unit = unit, step = step, days = days, perWeek = d.perWeek))
        }
        close()
    }
    val title = when (part) { "remind" -> "Reminder"; "repeat" -> "Repeat on"; else -> if (habit == null) "New habit" else "Edit habit" }
    val sub = when {
        part != null -> habit?.name.orEmpty()
        habit == null -> "Shows in Habits and on the week rings"
        else -> habit.streak().let { s -> "$s-day streak · " } + habit.total() + " check-ins"
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = k.bg) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).imePadding()) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = c.onSurface, modifier = Modifier.padding(start = 8.dp))
            Text(sub, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), color = c.primary, modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 16.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                if (part == null) {
                    PreviewRow(d, iconOpen) { iconOpen = !iconOpen }
                    IconGrid(iconOpen, d) { d = d.copy(icon = it); iconOpen = false }
                    Text("Name", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = c.primary, modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp))
                    BasicTextField(
                        d.name, { if (it.length <= 40) d = d.copy(name = it) }, singleLine = true, cursorBrush = SolidColor(c.primary),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.onSurface),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { save() }),
                        modifier = Modifier.fillMaxWidth().background(k.item, RoundedCornerShape(16.dp)).padding(horizontal = 20.dp, vertical = 18.dp),
                        decorationBox = { inner -> Box { if (d.name.isEmpty()) Text("e.g. Stretch for 5 minutes", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant); inner() } },
                    )
                    if (clash) Row(Modifier.padding(start = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_error), null, Modifier.size(18.dp), tint = c.error)
                        Text("  You already have a habit called “$name”", style = MaterialTheme.typography.bodyMedium, color = c.error)
                    }
                    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        HItem(0, 2, "Shape") { ShapeGrid(d) { d = d.copy(shape = it) } }
                        HItem(1, 2, "Colour") { ColorSwatches(d.color) { d = d.copy(color = it) } }
                    }
                }
                val items = if (part == null) 3 else 1
                Column(Modifier.padding(top = if (part == null) 16.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (part == null) HItem(0, 3, "Goal") { GoalBody(d) { d = it } }
                    if (part == null || part == "repeat") HItem(if (part == null) 1 else 0, items, if (part == null) "Repeat on" else null) { RepeatBody(d) { d = it } }
                    if (part == null || part == "remind") HItem(if (part == null) 2 else 0, items, null) {
                        Box(Modifier.padding(horizontal = 8.dp)) {
                            SwitchLine("Remind me", d.remind) { on -> if (on) askNotifications(); d = d.copy(remind = on) }
                        }
                        Text(if (d.remind) clock(d.time) else "Off", style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), color = c.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp))
                        if (d.remind) {
                            Box(Modifier.padding(top = 8.dp)) {
                                Connected(QuickTimes + listOf(null), { t -> if (t == null) other else !other && t == d.time }, { t -> t?.let(::timeLabel) ?: "Other" }, height = 40) { t ->
                                    if (t == null) other = true else { other = false; d = d.copy(time = t) }
                                }
                            }
                            // The dial needs its full height; while typing, the time shows as one line (see above).
                            if (other && !WindowInsets.isImeVisible) ExpressiveTimePicker(d.time.hour, d.time.minute, { h, m -> d = d.copy(time = LocalTime.of(h, m)) },
                                Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp))
                        }
                    }
                }
                if (habit != null && part == null && onDelete != null) {
                    androidx.compose.material3.TextButton(
                        onClick = { scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss(); onDelete() } }, shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.textButtonColors(containerColor = k.item, contentColor = c.error),
                        modifier = Modifier.padding(top = 24.dp).fillMaxWidth().height(56.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_delete), null)
                        Text("  Delete habit", modifier = Modifier.weight(1f))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.Reject); close() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).height(56.dp)) { Text("Cancel") }
                Button(onClick = ::save, enabled = ok, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1.4f).height(56.dp)) {
                    Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp))
                    Text("  " + if (habit == null) "Create" else if (part != null) "Done" else "Save")
                }
            }
        }
    }
}

/** Goal: Yes or no | Count | Time; a count takes an amount and a unit, a time a slider (5 min – 5 h). */
@Composable
private fun GoalBody(d: HabitDraft, set: (HabitDraft) -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    Connected(listOf("yes", "count", "time"), { it == d.goal }, { when (it) { "yes" -> "Yes or no"; "count" -> "Count"; else -> "Time" } },
        icon = { when (it) { "yes" -> R.drawable.ic_check; "count" -> R.drawable.ic_pin; else -> R.drawable.ic_timer } }) { g ->
        set(when (g) {
            "yes" -> d.copy(goal = "yes", target = 1.0, unit = "")
            "count" -> if (d.goal == "count") d else d.copy(goal = "count", target = 8.0, unit = "glasses", step = 1.0)
            else -> if (d.goal == "time") d else d.copy(goal = "time", target = 10.0, unit = "min")
        })
    }
    if (d.goal == "count") {
        var text by remember { mutableStateOf(num(d.target)) }
        Box(Modifier.padding(top = 12.dp)) {
            Chips(Units, { it.first == d.unit }, { it.first }, lead = {
                BasicTextField(
                    text, { v -> if (v.length <= 6 && v.count { it == '.' } <= 1 && v.all { it.isDigit() || it == '.' }) { text = v; v.toDoubleOrNull()?.takeIf { it > 0 }?.let { set(d.copy(target = it)) } } },
                    singleLine = true, cursorBrush = SolidColor(c.primary), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.onSurface, fontFeatureSettings = "tnum"),
                    modifier = Modifier.width(72.dp).height(40.dp).background(k.chip, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }) { (u, st) -> set(d.copy(unit = u, step = st)) }
        }
    }
    if (d.goal == "time") {
        val idx = Minutes.indexOfFirst { it >= d.target.toInt() }.let { if (it < 0) Minutes.lastIndex else it }
        Text(minText(Minutes[idx]), style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), color = c.onSurface, modifier = Modifier.padding(start = 8.dp, top = 12.dp))
        // M3 Expressive slider; continuous so no tick dots, snapped to the canvas stops.
        Slider(value = idx.toFloat(), onValueChange = { set(d.copy(target = Minutes[Math.round(it)].toDouble())) }, valueRange = 0f..Minutes.lastIndex.toFloat(),
            modifier = Modifier.padding(horizontal = 8.dp))
    }
}

/** Repeat on: Every day | Weekdays | Times a week, then the seven days or 1–6 a week. */
@Composable
private fun RepeatBody(d: HabitDraft, set: (HabitDraft) -> Unit) {
    val preset = when { d.perWeek != null -> "Times a week"; d.days == null -> "Every day"; d.days == setOf(0, 1, 2, 3, 4) -> "Weekdays"; else -> "" }
    Box(Modifier.padding(horizontal = 8.dp)) {
        Chips(listOf("Every day", "Weekdays", "Times a week"), { it == preset }, { it }) { p ->
            set(when (p) { "Every day" -> d.copy(days = null, perWeek = null); "Weekdays" -> d.copy(days = setOf(0, 1, 2, 3, 4), perWeek = null); else -> d.copy(perWeek = d.perWeek ?: 3, days = null) })
        }
    }
    Box(Modifier.padding(top = 12.dp)) {
        if (d.perWeek == null) {
            Connected((0..6).toList(), { d.days?.contains(it) ?: true }, { listOf("M", "T", "W", "T", "F", "S", "S")[it] }) { i ->
                val cur = d.days ?: (0..6).toSet()
                val next = if (i in cur) cur - i else cur + i
                if (next.isNotEmpty()) set(d.copy(days = next.takeIf { it.size < 7 }))
            }
        } else Connected((1..6).toList(), { it == d.perWeek }, { "$it" }) { set(d.copy(perWeek = it)) }
    }
}
