@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.calendar

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.TaskRepository
import app.kairo.data.calendar.CalEvent
import app.kairo.data.calendar.PhoneCalendar
import app.kairo.data.model.Task
import app.kairo.ui.common.EmptyState
import app.kairo.ui.common.TabSearchBar
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * v2 Calendar tab (canvas v89–v111). Month: the grid, the summary card and the day list. Week: the
 * week strip with busy bars, then the picked day's hours. Day: the summary and the hours. Swipe the
 * grid, strip or hours sideways (or use ‹ ›) to move. Events come from the phone's calendars.
 */
@Composable
fun CalendarScreen(
    repo: TaskRepository, calendar: PhoneCalendar, onOpenTask: (Task) -> Unit,
    onSearch: () -> Unit = {}, onVoice: () -> Unit = {}, onAccount: () -> Unit = {}, onOpenEvent: (CalEvent) -> Unit = {}, onMenu: (() -> Unit)? = null,
) {
    val today = LocalDate.now()
    var viewName by rememberSaveable { mutableStateOf(CalView.Month.name) }
    val view = CalView.valueOf(viewName)
    var picked by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    val day = LocalDate.ofEpochDay(picked)
    LaunchedEffect(picked) { CalendarFocus.day = day } // + → Task / Event lands on this day
    val scope = rememberCoroutineScope()

    var granted by remember { mutableStateOf(calendar.hasPermission()) }
    var later by rememberSaveable { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(granted) { if (granted) calendar.changes().collect { tick++ } }
    val anchor = YearMonth.from(day)
    val events by produceState(emptyList<CalEvent>(), anchor, granted, tick) {
        value = if (granted) calendar.events(anchor.minusMonths(1).atDay(1).minusDays(7), anchor.plusMonths(1).atEndOfMonth().plusDays(8)) else emptyList()
    }
    // What the calendars drawer hides (kept in Settings).
    val hidden = app.kairo.ui.common.LocalKairoApp.current.settings.settings.collectAsStateWithLifecycle(initialValue = app.kairo.data.settings.AppSettings()).value.calHidden
    val byDay = remember(events, hidden) { eventsByDay(events.filter { "phone:${it.calendarId}" !in hidden }) }
    val tasks0 by repo.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks = if ("tasks" in hidden) emptyList() else tasks0.filter { "list:${it.listId ?: 0}" !in hidden }
    val lists by repo.lists.collectAsStateWithLifecycle(initialValue = emptyList())
    val listMap = remember(lists) { lists.associateBy { it.id } }
    val tasksByDay = remember(tasks) { tasks.filter { it.due != null }.groupBy { it.due!! } }
    fun ev(d: LocalDate) = byDay[d].orEmpty()
    fun tk(d: LocalDate) = tasksByDay[d].orEmpty()

    fun go(dir: Int) {
        picked = when (view) {
            CalView.Day -> day.plusDays(dir.toLong())
            CalView.Week -> day.plusWeeks(dir.toLong())
            CalView.Month -> YearMonth.from(day).plusMonths(dir.toLong()).let { m -> if (m == YearMonth.from(today)) today else m.atDay(1) }
        }.toEpochDay()
    }
    val swipe = Modifier.pointerInput(view) {
        var dx = 0f
        detectHorizontalDragGestures(onDragStart = { dx = 0f }, onDragEnd = { if (kotlin.math.abs(dx) > 80.dp.toPx()) go(if (dx < 0) 1 else -1) }) { _, d -> dx += d }
    }

    // Summary card (canvas calCard): counts only.
    val dE = ev(day); val dT = tk(day).sortedWith(compareBy(nullsLast()) { it.time })
    val open = dT.count { !it.done }
    val busy = load(dE, dT, day)
    val nowMin = LocalTime.now().toSecondOfDay() / 60
    val next = if (day == today) dE.mapNotNull { e -> span(e, day)?.let { e to it.first } }.filter { it.second > nowMin }.minByOrNull { it.second } else null
    val label = when (day) { today -> "Today"; today.plusDays(1) -> "Tomorrow"; today.minusDays(1) -> "Yesterday"; else -> day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
    val big = if (dE.isNotEmpty()) "${dE.size} event" + (if (dE.size == 1) "" else "s") else if (open > 0) "No events" else "Free day"
    val chips = listOfNotNull(
        if (dT.isNotEmpty()) R.drawable.ic_task_alt to ("$open task" + (if (open == 1) "" else "s") + (if (dT.size > open) " · ${dT.size - open} done" else "")) else null,
        if (busy > 0) R.drawable.ic_hourglass_bottom to (hoursMinutes(busy) + " booked") else null,
        next?.let { (e, m) -> R.drawable.ic_arrow_forward to ("Next " + LocalTime.of(m / 60, m % 60).format(Clock) + " · " + e.title) },
    )

    // Blocks for the hours: phone events on tertiaryContainer, tasks in their list colour.
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val on = app.kairo.ui.habits.HabitLook.onColor()
    fun taskColors(t: Task) = listMap[t.listId]?.let { k.accent(it.color) to on } ?: (c.secondaryContainer to c.onSecondaryContainer)
    val timed = dE.mapNotNull { e -> span(e, day)?.let { (a, b) -> Block("e${e.eventId}${e.start}", e.title, fmtRange(a, b), a, b, c.tertiaryContainer, c.onTertiaryContainer, null, e, false) } } +
        dT.mapNotNull { t -> t.time?.let { tm -> val a = tm.toSecondOfDay() / 60; val (bg, fg) = taskColors(t); Block("t${t.id}", t.title, tm.format(Clock), a, minOf(a + 30, 1440), bg, fg, t, null, t.done) } }
    val allDay = dE.filter { span(it, day) == null }.map { e -> Block("e${e.eventId}${e.start}", e.title, "All day", 0, 0, c.tertiaryContainer, c.onTertiaryContainer, null, e, false) } +
        dT.filter { it.time == null }.map { t -> val (bg, fg) = taskColors(t); Block("t${t.id}", t.title, "", 0, 0, bg, fg, t, null, t.done) }
    val openBlock: (Block) -> Unit = { b -> b.task?.let(onOpenTask) ?: b.event?.let(onOpenEvent) }
    val toggle: (Task) -> Unit = { t -> scope.launch { repo.setDone(t, !t.done) } }

    val title = when (view) {
        CalView.Day -> "${day.dayOfMonth} " + day.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
        CalView.Week -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(3).month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
        CalView.Month -> day.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
    }
    val list = rememberLazyListState()
    val density = LocalDensity.current
    // Week and Day open on the hours, an hour before now (canvas calScroll).
    LaunchedEffect(view) {
        if (view != CalView.Month) list.scrollToItem(6, with(density) { (HourHeight * (maxOf(0, LocalTime.now().hour - 1)).toFloat()).roundToPx() })
    }
    Column(Modifier.fillMaxSize().background(k.bg)) {
        TabSearchBar("Search in calendar", onSearch = onSearch, onVoice = onVoice, onMenu = onMenu, onAccount = onAccount)
        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 168.dp)) {
            item(key = "banner") { if (!granted && !later) CalAccessBanner(onAllow = { ask.launch(Manifest.permission.READ_CALENDAR) }, onLater = { later = true }) }
            item(key = "head") { CalHeader(title, day.year, onToday = if (day != today) ({ picked = today.toEpochDay() }) else null) }
            item(key = "row") { ViewRow(view, onPick = { viewName = it.name }, onPrev = { go(-1) }, onNext = { go(1) }) }
            item(key = "top") {
                when (view) {
                    CalView.Month -> androidx.compose.foundation.layout.Box(swipe) {
                        MonthGrid(YearMonth.from(day), today, day, { d -> tk(d).any { !it.done } }, { d -> ev(d).isNotEmpty() }) { picked = it.toEpochDay() }
                    }
                    CalView.Week -> androidx.compose.foundation.layout.Box(swipe) {
                        WeekStrip(day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today, day, { d -> load(ev(d), tk(d), d) },
                            { d -> tk(d).any { !it.done } }, { d -> ev(d).isNotEmpty() }) { picked = it.toEpochDay() }
                    }
                    CalView.Day -> {}
                }
            }
            item(key = "sum") { SummaryCard(label, big, chips) }
            if (view == CalView.Month) {
                item(key = "gap") { Spacer(Modifier.height(16.dp)) }
                val n = dE.size + dT.size
                if (n == 0) item(key = "empty") {
                    EmptyState("Nothing planned", "", size = 120.dp, color = c.tertiaryContainer, icon = R.drawable.ic_event_available)
                }
                itemsIndexed(dE, key = { _, e -> "e${e.eventId}${e.start}" }) { i, e ->
                    Column(Modifier.animateItem()) { EventRow(e, day, i, n) { onOpenEvent(e) }; Spacer(Modifier.height(3.dp)) }
                }
                itemsIndexed(dT, key = { _, t -> "t${t.id}" }) { i, t ->
                    Column(Modifier.animateItem()) { CalTaskRow(t, listMap[t.listId], dE.size + i, n, toggle, onOpenTask); Spacer(Modifier.height(3.dp)) }
                }
            } else {
                item(key = "allday") { AllDayRow(allDay, openBlock) }
                item(key = "hours") { androidx.compose.foundation.layout.Box(swipe.padding(top = if (allDay.isEmpty()) 16.dp else 0.dp)) { DayTimeline(timed, day == today, openBlock) } }
            }
        }
    }
}

private fun fmtRange(a: Int, b: Int): String {
    fun t(m: Int) = if (m >= 1440) "12:00 AM" else LocalTime.of(m / 60, m % 60).format(Clock)
    return t(a) + " – " + t(b)
}

