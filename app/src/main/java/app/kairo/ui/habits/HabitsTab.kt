@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.habits

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import app.kairo.ui.common.SelectionTopBar
import app.kairo.ui.common.rememberSelection
import app.kairo.ui.common.rememberReorder
import app.kairo.ui.common.reorderHandle
import app.kairo.ui.common.reorderItem
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.habits.Goal
import app.kairo.habits.Habit
import app.kairo.ui.common.EmptyState
import app.kairo.ui.common.LocalAppSnackbar
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.common.TabSearchBar
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * v2 Habits tab (canvas v87–v88): search bar, the week card, then the picked day's habits (due
 * ones, then rest-day ones dimmed) and an Archived fold. Due habits sort pending → done → not
 * done, as a snapshot taken when you pick a day, so a row you just checked stays put.
 */
@Composable
fun HabitsTab(
    newHabit: Boolean, onNewHabitDone: () -> Unit,
    onSearch: () -> Unit, onVoice: () -> Unit, onAccount: () -> Unit, onOpenHabit: (Long) -> Unit,
) {
    val app = LocalKairoApp.current
    val all by app.habits.all.collectAsStateWithLifecycle(initialValue = null)
    val snackbar = LocalAppSnackbar.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    var weekOff by rememberSaveable { mutableStateOf(0) }
    var picked by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var archOpen by rememberSaveable { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<Habit?>(null) }
    var edit by remember { mutableStateOf<Habit?>(null) }
    val selection = rememberSelection()
    val listState = rememberLazyListState()
    val reorder = rememberReorder(listState)
    // While selecting, the list shows your own order (drag handles reorder it), as on the canvas.
    val order = remember { mutableStateListOf<Long>() }
    val habits = all ?: return
    val day = LocalDate.ofEpochDay(picked)
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(weekOff.toLong())
    val active = habits.filter { !it.archived }
    val archived = habits.filter { it.archived }

    // Snapshot order: taken when the day or the set of habits changes, not on every check.
    val ids = active.map { it.id }
    val rank = remember(picked, ids) { active.associate { it.id to if (it.done(day)) 1 else if (it.dropped(day)) 2 else 0 } }
    val due = active.filter { it.scheduled(day) }.sortedBy { rank[it.id] ?: 0 }
    val rest = active.filter { !it.scheduled(day) }
    if (!selection.active || order.isEmpty()) { order.clear(); order.addAll(active.map { it.id }) }
    val byId = active.associateBy { it.id }
    val list = if (selection.active) order.mapNotNull { byId[it] } else due + rest

    /** Runs a change to one day and offers Undo (rule 9: haptics on complete and undo). */
    fun act(h: Habit, message: String, completes: Boolean, change: suspend () -> Unit) {
        val before = h.logs[day]
        if (completes) haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
        scope.launch {
            change()
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(message, "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                app.habits.restore(h.id, day, before)
            }
        }
    }
    val when_ = if (day == today) "today" else "on " + day.format(DayFmt)
    fun onTap(h: Habit) {
        val was = h.done(day)
        val msg = when {
            was -> if (h.goal == Goal.Yes) "Check-in removed" else "Reset"
            h.goal == Goal.Count -> minOf(h.target, h.count(day) + h.e.step).let { n -> if (n >= h.target) "${h.name} · goal reached" else "${num(n)} of ${num(h.target)} ${h.e.unit}".trim() }
            h.perWeek != null -> "${h.name} · ${minOf(h.weekDone(day, today) + 1, h.perWeek!!)} of ${h.perWeek} this week"
            day == today -> "${h.name} · ${h.streak(today) + 1}-day streak"
            else -> "Checked in for ${day.format(DayFmt)}"
        }
        act(h, msg, completes = !was) { app.habits.tap(h, day) }
    }
    fun onSwipe(h: Habit, done: Boolean) {
        if (if (done) h.done(day) else h.dropped(day)) {
            scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(if (done) "Already done $when_" else "Already not done $when_") }
            return
        }
        val msg = when {
            !done -> "Not done $when_"
            h.goal == Goal.Count -> minOf(h.target, (if (h.dropped(day)) 0.0 else h.count(day)) + h.e.step).let { n ->
                if (n >= h.target) "${h.name} · goal reached" else "${num(n)} of ${num(h.target)} ${h.e.unit}".trim() }
            day == today && h.perWeek == null -> "${h.name} · ${h.streak(today) + 1}-day streak"
            else -> "Done $when_"
        }
        act(h, msg, completes = done) { app.habits.swipe(h, day, done) }
    }

    Box(Modifier.fillMaxSize().background(KairoTheme.colors.bg)) {
    Column(Modifier.fillMaxSize()) {
        TabSearchBar("Search in habits", onSearch = onSearch, onVoice = onVoice, onMenu = null, onAccount = onAccount)
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 168.dp)) {
            if (active.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = "No habits yet", body = "Pick something small to do every day, and check it off here.",
                        size = 120.dp, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.padding(top = 32.dp),
                        icon = R.drawable.ic_local_fire_department_filled,
                    ) { Button(onClick = { adding = true }, shapes = ButtonDefaults.shapes()) { Text("New habit") } }
                }
            } else {
                item(key = "week") {
                    WeekCard(active, monday, day, today, onWeek = { d ->
                        weekOff = (weekOff + d).coerceAtMost(0)
                        val m = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(weekOff.toLong())
                        picked = (if (weekOff == 0) today else m.plusDays(6)).toEpochDay()
                    }) { picked = it.toEpochDay() }
                    Spacer(Modifier.height(16.dp))
                }
                if (day != today) item(key = "dayhead") {
                    Text(day.format(DayFmt), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                }
                itemsIndexed(list, key = { _, h -> "h${h.id}" }) { i, h ->
                    val motion = MaterialTheme.motionScheme
                    val key = "h${h.id}"
                    Column((if (reorder.dragging == key) Modifier.reorderItem(reorder, key) else Modifier.animateItem(placementSpec = motion.defaultSpatialSpec(), fadeInSpec = motion.defaultEffectsSpec(), fadeOutSpec = motion.fastEffectsSpec()))) {
                        HabitRow(h, day, today, if (h.scheduled(day)) RowKind.Due else RowKind.Rest, i, list.size,
                            onTap = { onTap(h) }, onSwipe = { onSwipe(h, it) }, onOpen = { onOpenHabit(h.id) },
                            selected = h.id in selection, selecting = selection.active, onSelect = { selection.toggle(h.id) },
                            handle = if (!selection.active) null else Modifier.reorderHandle(reorder, key, canSwap = { (it as? String)?.startsWith("h") == true },
                                onMove = { from, to ->
                                    val a = order.indexOf((from as String).drop(1).toLong()); val b = order.indexOf((to as String).drop(1).toLong())
                                    if (a >= 0 && b >= 0) order.add(b, order.removeAt(a))
                                },
                                onDone = { val ids = order.toList(); scope.launch { app.habits.reorder(ids, habits) } }))
                        if (i < list.lastIndex) Spacer(Modifier.height(3.dp))
                    }
                }
            }
            if (archived.isNotEmpty()) {
                item(key = "arch") { ArchivedFold(archived.size, archOpen) { archOpen = !archOpen } }
                if (archOpen) itemsIndexed(archived, key = { _, h -> "a${h.id}" }) { i, h ->
                    Column(Modifier.animateItem()) {
                        Spacer(Modifier.height(3.dp))
                        HabitRow(h, day, today, RowKind.Archived, i + 1, archived.size + 1, onTap = {}, onSwipe = {}, onOpen = { onOpenHabit(h.id) },
                            selected = h.id in selection, selecting = selection.active, onSelect = { selection.toggle(h.id) })
                    }
                }
            }
        }
    }
    SelectionTopBar(selection, active.map { it.id }, Modifier.align(Alignment.TopCenter))
    HabitSelectionToolbar(selection, habits, day, onEdit = { edit = it }, modifier = Modifier.align(Alignment.BottomCenter))
    }
    if (adding || newHabit) HabitSheet(onDismiss = { adding = false; onNewHabitDone() })
    edit?.let { h -> HabitSheet(h, onDismiss = { edit = null }) }
    open?.let { h -> HabitDetailSheet(h, onEdit = { edit = h; open = null }, onDismiss = { open = null }) }
}

/** Canvas .frow: "Archived" with a count, opens into the archived rows below. */
@Composable
private fun ArchivedFold(n: Int, expanded: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val turn by animateFloatAsState(if (expanded) 180f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "chev")
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.shapes(shape = if (expanded) RoundedCornerShape(28.dp, 28.dp, 8.dp, 8.dp) else RoundedCornerShape(28.dp)),
        colors = ListItemDefaults.segmentedColors(containerColor = KairoTheme.colors.item),
        modifier = Modifier.padding(top = 16.dp),
        leadingContent = { Icon(painterResource(R.drawable.ic_archive), null, tint = c.onSurfaceVariant) },
        trailingContent = {
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box(Modifier.background(c.secondaryContainer, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("$n", style = MaterialTheme.typography.labelMedium, color = c.onSecondaryContainer)
                }
                Icon(painterResource(R.drawable.ic_expand_more), null, Modifier.padding(start = 8.dp).size(24.dp).rotate(turn), tint = c.onSurfaceVariant)
            }
        },
    ) { Text("Archived", style = MaterialTheme.typography.titleMedium) }
}

private val DayFmt = DateTimeFormatter.ofPattern("EEE d MMM")
