@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.habits

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.habits.Goal
import app.kairo.habits.Habit
import app.kairo.ui.common.LocalAppSnackbar
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.common.slowSpin
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * v2 habit page (canvas "habit page" board): the streak on the habit's own shape, three stat tiles,
 * Today's amount, Month / Year history, the reminder and repeat rows, and a floating toolbar
 * (Edit · Archive · Pause · Delete) beside the main action (Done today, Add 1, Start timer…).
 */
@Composable
fun HabitPage(id: Long, onBack: () -> Unit) {
    val app = LocalKairoApp.current
    val all by app.habits.all.collectAsStateWithLifecycle(initialValue = null)
    val h = all?.firstOrNull { it.id == id } ?: return
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val scope = rememberCoroutineScope()
    val snackbar = LocalAppSnackbar.current
    val haptic = LocalHapticFeedback.current
    val today = LocalDate.now()
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var year by rememberSaveable { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<String?>(null) }
    val timer = rememberHabitTimer(h)
    val narrow = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 390
    val streak = h.streak(today)
    val unit = if (h.perWeek != null) "week" else "day"
    val ym = YearMonth.parse(month)
    val mDays = (1..ym.lengthOfMonth()).map { ym.atDay(it) }.filter { !it.isAfter(today) && !it.isBefore(h.createdAt) }
    val mDone = mDays.count { h.done(it) }
    val mDue = mDays.count { h.scheduled(it) }

    fun undoable(msg: String, before: app.kairo.habits.DayLog?, day: LocalDate, change: suspend () -> Unit) = scope.launch {
        change()
        snackbar.currentSnackbarData?.dismiss()
        if (snackbar.showSnackbar(msg, "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm); app.habits.restore(h.id, day, before)
        }
    }
    val due0 = h.scheduled(today)
    val done0 = h.done(today)
    val main: Triple<Int, String, () -> Unit> = when {
        h.archived -> Triple(R.drawable.ic_unarchive, "Unarchive") { scope.launch { app.habits.setArchived(h, false) } }
        h.paused(today) -> Triple(R.drawable.ic_play_arrow, "Resume") { scope.launch { app.habits.resume(h) } }
        h.goal == Goal.Time && !done0 -> if (timer.running) Triple(R.drawable.ic_pause, "Pause · " + timer.clock) { timer.pause() }
            else Triple(R.drawable.ic_timer, "Start timer") { timer.start() }
        h.goal == Goal.Count && !done0 -> Triple(R.drawable.ic_add, "Add ${num(h.e.step)}") {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
            undoable("${num(minOf(h.target, h.count(today) + h.e.step))} of ${num(h.target)} ${h.e.unit}".trim(), h.logs[today], today) { app.habits.tap(h, today) }
        }
        done0 -> Triple(R.drawable.ic_close, if (h.goal == Goal.Yes) "Undo today" else "Reset today") {
            undoable("Check-in removed", h.logs[today], today) { app.habits.clear(h.id, today) }
        }
        due0 -> Triple(R.drawable.ic_check, "Done today") {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
            undoable("${h.name} · ${streak + 1}-$unit streak", h.logs[today], today) { app.habits.tap(h, today) }
        }
        else -> Triple(R.drawable.hi_bedtime_filled, "Rest day") {}
    }

    Box(Modifier.fillMaxSize().background(k.bg)) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 168.dp)) {
            item(key = "top") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = c.onSurface) }
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.background(k.chip, RoundedCornerShape(16.dp)).padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_repeat), null, Modifier.size(18.dp), tint = c.onSurfaceVariant)
                        Text(" " + schedule(h), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                    }
                }
            }
            item(key = "hero") { Hero(h, streak, unit, timer, today) }
            item(key = "stats") {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Stat("$streak", " $unit" + if (streak == 1) "" else "s", "Current streak", 0, Modifier.weight(1f))
                    val best = h.best(today)
                    Stat("$best", " $unit" + if (best == 1) "" else "s", "Best streak", 1, Modifier.weight(1f))
                    Stat("$mDone", if (h.perWeek != null) " times" else "/$mDue", "This month", 2, Modifier.weight(1f))
                }
            }
            if (h.goal != Goal.Yes) item(key = "log") {
                SegmentedListItem(
                    onClick = { sheet = "log" }, shapes = ListItemDefaults.shapes(shape = RoundedCornerShape(28.dp)),
                    colors = ListItemDefaults.segmentedColors(containerColor = k.item), modifier = Modifier.padding(top = 8.dp),
                    leadingContent = { Icon(painterResource(if (h.goal == Goal.Time) R.drawable.ic_timer else R.drawable.hi_water_full_filled), null, tint = c.onSurfaceVariant) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${num(timer.value)} of ${num(h.target)} ${if (h.goal == Goal.Time) "min" else h.e.unit}".trim(), color = c.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            Icon(painterResource(R.drawable.ic_edit), null, Modifier.padding(start = 12.dp).size(20.dp), tint = c.onSurfaceVariant)
                        }
                    },
                ) { Text("Today") }
            }
            item(key = "hist") { SectionTitle("History") }
            item(key = "view") { ViewToggle(year) { year = it } }
            item(key = "cal") {
                if (year) YearHistory(h, today)
                else MonthHistory(h, ym, today, onMonth = { month = ym.plusMonths(it.toLong()).toString() }) { d ->
                    val was = h.done(d)
                    undoable(if (was) "Check-in removed" else "Checked in for " + d.format(DayFmt), h.logs[d], d) {
                        if (was) app.habits.clear(h.id, d) else app.habits.swipe(h, d, true)
                    }
                }
            }
            item(key = "notes") { HabitNotes(h.id, today) }
            item(key = "set") { SectionTitle("Settings") }
            item(key = "rows") {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    SettingRow(R.drawable.ic_notifications, "Reminder", h.reminderTime?.format(Clock) ?: "Off", top = true) { sheet = "remind" }
                    SettingRow(R.drawable.ic_repeat, "Repeat on", schedule(h), top = false) { sheet = "repeat" }
                }
                Text("Started ${h.createdAt.format(Started)} · ${h.total()} check-ins in all", style = MaterialTheme.typography.labelMedium,
                    color = c.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp, top = 16.dp))
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalFloatingToolbar(expanded = true, colors = FloatingToolbarDefaults.standardFloatingToolbarColors(toolbarContainerColor = k.navBg), contentPadding = PaddingValues(horizontal = 4.dp)) {
                IconButton(onClick = { sheet = "edit" }, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_edit), "Edit habit") }
                IconButton(onClick = { scope.launch { app.habits.setArchived(h, !h.archived) } }, shapes = IconButtonDefaults.shapes()) {
                    Icon(painterResource(if (h.archived) R.drawable.ic_unarchive else R.drawable.ic_archive), if (h.archived) "Unarchive" else "Archive")
                }
                IconButton(onClick = { if (h.paused(today)) scope.launch { app.habits.resume(h) } else sheet = "pause" }, shapes = IconButtonDefaults.shapes()) {
                    Icon(painterResource(if (h.paused(today)) R.drawable.ic_play_arrow else R.drawable.ic_pause), if (h.paused(today)) "Resume" else "Pause")
                }
                IconButton(onClick = { sheet = "delete" }, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_delete), "Delete habit", tint = c.error) }
            }
            ExtendedFloatingActionButton(
                onClick = main.third, shape = RoundedCornerShape(20.dp), containerColor = c.primary, contentColor = c.onPrimary,
                modifier = Modifier.weight(1f).height(64.dp),
                // On 360dp phones the label only fits without the icon and at 14sp (canvas boards are 390dp).
                icon = { if (!narrow) Icon(painterResource(main.first), null, Modifier.size(24.dp)) },
                text = { Text(if (narrow) main.second.removeSuffix(" today") else main.second, style = (if (narrow) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium).copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
            )
        }
    }
    when (sheet) {
        "log" -> LogSheet(h, today, onDismiss = { sheet = null }) { v -> undoable("Logged ${num(v)}", h.logs[today], today) { app.habits.log(h.id, today, v) } }
        "pause" -> PauseSheet(onDismiss = { sheet = null }) { days -> scope.launch { app.habits.setPause(h, days) } }
        "delete" -> DeleteHabitSheet(h, onDismiss = { sheet = null }) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: delete
            val copy = h
            app.appScope.launch {
                app.habits.delete(copy.id)
                if (snackbar.showSnackbar("Deleted ${copy.name}", "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) app.habits.restore(copy)
            }
            onBack()
        }
        "edit" -> HabitSheet(h, onDelete = { sheet = "delete" }, onDismiss = { if (sheet == "edit") sheet = null })
        "remind", "repeat" -> HabitSheet(h, part = sheet, onDismiss = { sheet = null })
    }
}

/** Streak on the habit's own shape (104dp, slowly turning), name and today's status line. */
@Composable
private fun Hero(h: Habit, streak: Int, unit: String, timer: HabitTimer, today: LocalDate) {
    val c = MaterialTheme.colorScheme
    val spin by slowSpin(24_000)
    val on = HabitLook.onColor()
    Row(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().rotate(spin).background(KairoTheme.colors.accent(h.color), HabitLook.polygon(h.shapeName).toShape()))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$streak", style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), color = on)
                Text(("$unit streak").uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = on)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(h.name, style = MaterialTheme.typography.headlineMedium, color = c.onSurface)
            val status = when {
                h.archived -> "Archived · no reminders"
                h.paused(today) -> "Paused" + (h.e.pauseTo?.let { " until " + it.format(DayFmt) } ?: "") + " · streak kept"
                else -> (h.reminderTime?.let { "Reminder at " + it.format(Clock) } ?: "No reminder") + " · " + when {
                    h.goal == Goal.Time && !h.done(today) -> if (timer.running) "timer running · " + timer.clock else "${timer.value.toInt()} of ${num(h.target)} min today"
                    h.goal == Goal.Count && !h.done(today) -> "${num(h.count(today))} of ${num(h.target)} ${h.e.unit} today".replace("  ", " ")
                    h.done(today) -> "done today"
                    h.dropped(today) -> "marked not done today"
                    h.scheduled(today) -> "not done yet today"
                    else -> "rest day today"
                }
            }
            Text(status, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun Stat(v: String, unit: String, label: String, i: Int, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    val shape = when (i) { 0 -> RoundedCornerShape(28.dp, 8.dp, 8.dp, 28.dp); 2 -> RoundedCornerShape(8.dp, 28.dp, 28.dp, 8.dp); else -> RoundedCornerShape(8.dp) }
    Column(modifier.background(KairoTheme.colors.item, shape).padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
        Text(buildAnnotatedString {
            append(v); withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.onSurfaceVariant)) { append(unit) }
        }, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"), color = c.onSurface, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
    }
}

@Composable
private fun SectionTitle(t: String) = Text(t, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp))

@Composable
private fun SettingRow(icon: Int, label: String, value: String, top: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.shapes(shape = if (top) RoundedCornerShape(28.dp, 28.dp, 8.dp, 8.dp) else RoundedCornerShape(8.dp, 8.dp, 28.dp, 28.dp), pressedShape = RoundedCornerShape(28.dp)),
        colors = ListItemDefaults.segmentedColors(containerColor = KairoTheme.colors.item),
        leadingContent = { Icon(painterResource(icon), null, tint = c.onSurfaceVariant) },
        trailingContent = { Text(value, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant) },
    ) { Text(label) }
}

internal fun schedule(h: Habit): String = scheduleText(h.perWeek, h.weekdays)

private val Clock = DateTimeFormatter.ofPattern("h:mm a")
private val DayFmt = DateTimeFormatter.ofPattern("EEE d MMM")
private val Started = DateTimeFormatter.ofPattern("d MMM yyyy")
