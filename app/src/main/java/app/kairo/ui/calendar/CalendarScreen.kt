@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.calendar

import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import kotlin.math.roundToInt
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clipToBounds
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.TaskRepository
import app.kairo.data.calendar.CalEvent
import app.kairo.data.calendar.PhoneCalendar
import app.kairo.data.model.Task
import app.kairo.ui.common.LocalOpenSearch
import app.kairo.ui.common.SelectionTopBar
import app.kairo.ui.common.TabHeader
import app.kairo.ui.common.TaskSelectionToolbar
import app.kairo.ui.common.fullBleed
import app.kairo.ui.common.rememberEntrance
import app.kairo.ui.common.rememberSelection
import app.kairo.ui.common.slowSpin
import app.kairo.ui.theme.KairoTheme
import app.kairo.ui.today.TaskRow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private enum class CalView(val label: String, val icon: Int) {
    Month("Month", R.drawable.ic_calendar_month), Week("Week", R.drawable.ic_date_range), Day("Day", R.drawable.ic_today)
}

/** Pagers are "infinite": a big page count centred on the day the screen opened. */
private const val Center = 1200
private const val PageCount = Center * 2 + 1

@Composable
fun CalendarScreen(repo: TaskRepository, calendar: PhoneCalendar, onOpenTask: (Task) -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val today = LocalDate.now()
    // Page 'Center' of every pager is this date (saved, so pages keep their meaning after process death).
    val base = LocalDate.parse(rememberSaveable { LocalDate.now().toString() })
    val baseWeek = remember(base) { base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val monthAt: (Int) -> YearMonth = { YearMonth.from(base).plusMonths((it - Center).toLong()) }
    val weekAt: (Int) -> LocalDate = { baseWeek.plusWeeks((it - Center).toLong()) }
    val dayAt: (Int) -> LocalDate = { base.plusDays((it - Center).toLong()) }
    fun pageFor(v: CalView, d: LocalDate): Int = Center + when (v) {
        CalView.Month -> ChronoUnit.MONTHS.between(YearMonth.from(base), YearMonth.from(d))
        CalView.Week -> ChronoUnit.WEEKS.between(baseWeek, d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
        CalView.Day -> ChronoUnit.DAYS.between(base, d)
    }.toInt().coerceIn(-Center, Center)

    var viewName by rememberSaveable { mutableStateOf(CalView.Month.name) }
    val view = CalView.valueOf(viewName)
    var selected by rememberSaveable { mutableStateOf(today.toString()) }
    val day = LocalDate.parse(selected)
    val monthPager = rememberPagerState(initialPage = pageFor(CalView.Month, day)) { PageCount }
    val weekPager = rememberPagerState(initialPage = pageFor(CalView.Week, day)) { PageCount }
    val dayPager = rememberPagerState(initialPage = pageFor(CalView.Day, day)) { PageCount }
    fun pagerFor(v: CalView) = when (v) { CalView.Month -> monthPager; CalView.Week -> weekPager; CalView.Day -> dayPager }
    val pager = pagerFor(view)
    fun show(v: CalView, d: LocalDate = LocalDate.parse(selected)) {
        selected = d.toString()
        pagerFor(v).requestScrollToPage(pageFor(v, d))
        viewName = v.name
    }

    // Keep the selected day inside whatever page you swipe to.
    LaunchedEffect(view) {
        val p = pagerFor(view)
        // drop(1): the first value is the page show() just asked for (or a stale one before layout).
        snapshotFlow { p.settledPage }.drop(1).collect { page ->
            val cur = LocalDate.parse(selected)
            when (view) {
                CalView.Month -> { val m = monthAt(page); if (YearMonth.from(cur) != m) selected = (if (m == YearMonth.now()) LocalDate.now() else m.atDay(1)).toString() }
                CalView.Week -> { val s = weekAt(page); if (cur.isBefore(s) || cur.isAfter(s.plusDays(6))) selected = s.plusDays(cur.dayOfWeek.value - 1L).toString() }
                CalView.Day -> selected = dayAt(page).toString()
            }
        }
    }

    var granted by remember { mutableStateOf(calendar.hasPermission()) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val scope = rememberCoroutineScope()

    val tasks by repo.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val lists by repo.lists.collectAsStateWithLifecycle(initialValue = emptyList())
    val listMap = remember(lists) { lists.associateBy { it.id } }
    // Reload when the visible month changes, permission is granted, or the phone's calendar changes.
    val settled = pager.settledPage
    val anchor = when (view) { CalView.Month -> monthAt(settled); CalView.Week -> YearMonth.from(weekAt(settled).plusDays(3)); CalView.Day -> YearMonth.from(dayAt(settled)) }
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(granted) { if (granted) calendar.changes().collect { tick++ } }
    // Covers the month either side too, so neighbouring pages already have their events mid-swipe.
    val events by produceState(emptyList<CalEvent>(), anchor, granted, tick) {
        value = calendar.events(anchor.minusMonths(1).atDay(1).minusDays(7), anchor.plusMonths(1).atEndOfMonth().plusDays(8))
    }

    val tasksByDay = remember(tasks) { tasks.filter { it.due != null }.groupBy { it.due!! } }
    // Events grouped by day once per load; each day cell used to filter the whole list (up to 3x per cell).
    val eventsByDay = remember(events) {
        val m = HashMap<LocalDate, MutableList<CalEvent>>()
        events.forEach { e ->
            generateSequence(e.start.toLocalDate()) { it.plusDays(1) }
                .takeWhile { !it.isAfter(e.end.toLocalDate()) }.filter { occursOn(e, it) }
                .forEach { m.getOrPut(it) { mutableListOf() } += e }
        }
        m
    }
    val eventDays = eventsByDay.keys
    fun eventsOn(d: LocalDate): List<CalEvent> = eventsByDay[d].orEmpty()
    val dayTasks = tasksByDay[day].orEmpty().sortedWith(compareBy(nullsLast()) { it.time })
    val dayEvents = eventsOn(day)

    // Blocks for the week/day time grid: events in tertiary, tasks in their list's colour.
    fun taskSlot(t: Task, startMin: Int, endMin: Int): Slot {
        val l = listMap[t.listId]
        return Slot("t${t.id}", t.title, startMin, endMin,
            l?.let { k.container(it.color) } ?: c.secondaryContainer, l?.let { k.onContainer(it.color) } ?: c.onSecondaryContainer,
            t, t.done, t.time?.format(TimeFmt))
    }
    val data = DayData(
        timed = { d ->
            eventsOn(d).filter { !it.allDay }.map { e ->
                val s = if (e.start.toLocalDate().isBefore(d)) 0 else e.start.toLocalTime().toSecondOfDay() / 60
                val en = if (e.end.toLocalDate().isAfter(d)) 24 * 60 else e.end.toLocalTime().toSecondOfDay() / 60
                Slot("e${e.eventId}-${e.start}", e.title, s, maxOf(s, en), c.tertiaryContainer, c.onTertiaryContainer, null, false,
                    "${e.start.toLocalTime().format(TimeFmt)} – ${e.end.toLocalTime().format(TimeFmt)}")
            } + tasksByDay[d].orEmpty().mapNotNull { t ->
                t.time?.let { tm -> val s = tm.toSecondOfDay() / 60; taskSlot(t, s, minOf(s + 30, 24 * 60)) }
            }
        },
        allDay = { d ->
            eventsOn(d).filter { it.allDay }.map { e ->
                Slot("e${e.eventId}-${e.start}", e.title, 0, 0, c.tertiaryContainer, c.onTertiaryContainer, null, false, null)
            } + tasksByDay[d].orEmpty().filter { it.time == null }.map { taskSlot(it, 0, 0) }
        },
        taskCount = { d -> tasksByDay[d].orEmpty().count { !it.done } },
        eventCount = { d -> eventsOn(d).size },
    )

    // The month the visible page sits in (a week counts as the month its Thursday is in).
    val shownMonth = when (view) {
        CalView.Month -> monthAt(settled)
        CalView.Week -> YearMonth.from(weekAt(settled).plusDays(3))
        CalView.Day -> YearMonth.from(dayAt(settled))
    }
    val motion = MaterialTheme.motionScheme
    val spatial = motion.defaultSpatialSpec<Float>()
    val effects = motion.defaultEffectsSpec<Float>()

    val selection = rememberSelection()
    val entrance = rememberEntrance()
    // + → Task / Event lands on the day you're looking at.
    LaunchedEffect(selected) { CalendarFocus.day = LocalDate.parse(selected) }

    // Big month title, search, then one row: the view menu on the left and the chevrons on the right.
    val header: @Composable (Modifier) -> Unit = { mod ->
        TabHeader(
            title = shownMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()),
            subtitle = "${shownMonth.year}",
            modifier = mod,
            below = {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ViewMenu(
                        view = view,
                        onPick = { if (it != view) show(it) },
                        onToday = {
                            selected = today.toString()
                            scope.launch { pager.animateScrollToPage(pageFor(view, today), animationSpec = spatial) }
                        },
                        onRefresh = { scope.launch { calendar.requestSync(); tick++ } },
                    )
                    Spacer(Modifier.weight(1f))
                    val unit = view.label.lowercase()
                    FilledTonalIconButton(shapes = IconButtonDefaults.shapes(), onClick = {
                        scope.launch { pager.animateScrollToPage(pager.currentPage - 1, animationSpec = spatial) }
                    }) { Icon(painterResource(R.drawable.ic_chevron_left), "Previous $unit") }
                    FilledTonalIconButton(shapes = IconButtonDefaults.shapes(), onClick = {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1, animationSpec = spatial) }
                    }) { Icon(painterResource(R.drawable.ic_chevron_right), "Next $unit") }
                }
            },
        )
    }

    Scaffold(
        containerColor = k.bg,
        bottomBar = { TaskSelectionToolbar(selection, dayTasks, lists, onOpenTask) },
    ) { _ ->
      Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = view,
            transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) },
            modifier = Modifier.fillMaxSize(),
            label = "calView",
        ) { v ->
            when (v) {
              // Week and Day: the header stays put; swipe on the dates to change week/day, the hours only scroll.
              // The header collapses as the hours scroll up and comes back at the top (owner, 2026-11-21:
              // "can't scroll past the calendar view").
              CalView.Week -> CollapsingHeader(header = {
                  header(Modifier)
                  if (!granted) PermissionCard(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) { ask.launch(Manifest.permission.READ_CALENDAR) }
              }) {
                  WeekView(weekPager, weekAt, today, day, data, onPickDay = { show(CalView.Day, it) }, onOpenTask, Modifier.weight(1f).padding(top = 8.dp))
              }
              CalView.Day -> CollapsingHeader(header = {
                  header(Modifier)
                  if (!granted) PermissionCard(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) { ask.launch(Manifest.permission.READ_CALENDAR) }
              }) {
                  DayView(dayPager, dayAt, today, data, onOpenTask, Modifier.weight(1f))
              }
              CalView.Month -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp), // clear the floating nav
                modifier = Modifier.fillMaxSize(),
              ) {
                item(key = "header") { header(Modifier.fullBleed()) }
                item(key = "grid") {
                    // Swipe for months. Every month draws six weeks, so the height never jumps.
                    HorizontalPager(monthPager, Modifier.fillMaxWidth().padding(top = 8.dp), pageSpacing = 16.dp, verticalAlignment = Alignment.Top) { page ->
                        MonthGrid(monthAt(page), today, day, tasksByDay.keys, eventDays) { d -> selected = d.toString() }
                    }
                }
                if (!granted) item(key = "perm") {
                    PermissionCard(Modifier.padding(top = 24.dp)) { ask.launch(Manifest.permission.READ_CALENDAR) }
                }
                item(key = "hero") { Box(Modifier.padding(top = 16.dp)) { DayHero(day, today, dayTasks.count { !it.done }, dayEvents.size) } }
                if (dayEvents.isEmpty() && dayTasks.isEmpty()) item(key = "empty") {
                    Box(Modifier.animateItem(fadeInSpec = motion.defaultEffectsSpec(), placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = motion.fastEffectsSpec())) { EmptyDay() }
                }
                itemsIndexed(dayEvents, key = { _, e -> "e${e.eventId}-${e.start}" }) { i, e ->
                    Column(
                        Modifier
                            .animateItem(fadeInSpec = motion.defaultEffectsSpec(), placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = motion.fastEffectsSpec())
                            .then(with(entrance) { Modifier.enter("e${e.eventId}-${e.start}", i) }),
                    ) {
                        SegmentedListItem(
                            shapes = ListItemDefaults.segmentedShapes(i, dayEvents.size + dayTasks.size),
                            colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                            leadingContent = {
                                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                    Icon(painterResource(R.drawable.ic_event), null, tint = c.tertiary)
                                }
                            },
                            supportingContent = {
                                Text(
                                    (if (e.allDay) "All day" else "${e.start.toLocalTime().format(TimeFmt)} – ${e.end.toLocalTime().format(TimeFmt)}") + " · ${e.calendar}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                        ) { Text(e.title, style = MaterialTheme.typography.bodyLarge) }
                        Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
                    }
                }
                itemsIndexed(dayTasks, key = { _, t -> "t${t.id}" }) { i, t ->
                    Column(
                        Modifier
                            .animateItem(fadeInSpec = motion.defaultEffectsSpec(), placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = motion.fastEffectsSpec())
                            .then(with(entrance) { Modifier.enter("$day-t${t.id}", dayEvents.size + i) }),
                    ) {
                        (listMap[t.listId] ?: app.kairo.data.model.TaskList.None).let { l ->
                            TaskRow(t, l, dayEvents.size + i, dayEvents.size + dayTasks.size,
                                onToggle = { scope.launch { repo.setDone(it, !it.done) } }, onOpen = onOpenTask, selection = selection)
                        }
                        Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
                    }
                }
              }
            }
        }
        // Selection bar floats over the header instead of pushing the content down.
        SelectionTopBar(selection, dayTasks.map { it.id }, Modifier.align(Alignment.TopCenter))
      }
    }
}

/**
 * The view switcher: one tonal pill (current view's icon, name, chevron) that opens the Expressive
 * grouped menu: Month / Week / Day, then Today and Refresh.
 */
@Composable
private fun ViewMenu(view: CalView, onPick: (CalView) -> Unit, onToday: () -> Unit, onRefresh: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilledTonalButton(
            onClick = { open = true },
            shapes = ButtonDefaults.shapes(),
            contentPadding = PaddingValues(start = 16.dp, end = 12.dp),
            modifier = Modifier.height(40.dp).semantics { contentDescription = "View: ${view.label}. Change view" },
        ) {
            Icon(painterResource(view.icon), null, Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(view.label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.size(4.dp))
            Icon(painterResource(R.drawable.ic_expand_more), null, Modifier.size(ButtonDefaults.IconSize))
        }
        DropdownMenuPopup(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 2)) {
                CalView.entries.forEach { v ->
                    SelectableDropdownMenuItem(
                        selected = v == view,
                        onClick = { onPick(v); open = false },
                        text = { Text(v.label) },
                        shapes = MenuDefaults.itemShapes(),
                        leadingIcon = { Icon(painterResource(v.icon), null, Modifier.size(20.dp)) },
                    )
                }
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(1, 2)) {
                DropdownMenuItem(
                    text = { Text("Today") },
                    onClick = { onToday(); open = false },
                    shape = MenuDefaults.leadingItemShape,
                    leadingIcon = { Icon(painterResource(R.drawable.ic_event_available), null, Modifier.size(20.dp)) },
                )
                DropdownMenuItem(
                    text = { Text("Refresh") },
                    onClick = { onRefresh(); open = false },
                    shape = MenuDefaults.trailingItemShape,
                    leadingIcon = { Icon(painterResource(R.drawable.ic_repeat), null, Modifier.size(20.dp)) },
                )
            }
        }
    }
}

/**
 * A header that scrolls away first: scrolling the content up eats the header's height before the
 * content moves; scrolling down past the content's top brings it back. Clipped, never overlapping.
 */
@Composable
private fun CollapsingHeader(header: @Composable () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    var headerH by remember { mutableStateOf(0) }
    var off by remember { mutableStateOf(0f) } // 0 (shown) .. -headerH (gone)
    val conn = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            fun take(dy: Float): androidx.compose.ui.geometry.Offset {
                val next = (off + dy).coerceIn(-headerH.toFloat(), 0f)
                val used = next - off
                off = next
                return androidx.compose.ui.geometry.Offset(0f, used)
            }
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource) =
                if (available.y < 0) take(available.y) else androidx.compose.ui.geometry.Offset.Zero
            override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource) =
                if (available.y > 0) take(available.y) else androidx.compose.ui.geometry.Offset.Zero
        }
    }
    Column(Modifier.fillMaxSize().clipToBounds().nestedScroll(conn)) {
        Column(
            Modifier
                .layout { m, cs ->
                    val p = m.measure(cs.copy(maxHeight = androidx.compose.ui.unit.Constraints.Infinity))
                    val y = off.roundToInt()
                    layout(p.width, (p.height + y).coerceAtLeast(0)) { p.place(0, y) }
                }
                .onSizeChanged { headerH = it.height },
        ) { header() }
        // Once the header is gone, keep the content clear of the status bar.
        val bar = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Spacer(Modifier.height(bar * (if (headerH == 0) 0f else (-off / headerH).coerceIn(0f, 1f))))
        content()
    }
}

/** Whether an event shows on [d]. All-day ends are exclusive; a timed event ending at midnight doesn't spill over. */
private fun occursOn(e: CalEvent, d: LocalDate): Boolean {
    val s = e.start.toLocalDate()
    val en = e.end.toLocalDate()
    if (d.isBefore(s)) return false
    if (e.allDay) return d.isBefore(en) || (s == en && d == s)
    return d.isBefore(en) || (d == en && (e.end.toLocalTime() > LocalTime.MIDNIGHT || e.start == e.end))
}


@Composable
private fun PermissionCard(modifier: Modifier = Modifier, onAllow: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(KairoTheme.colors.item).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("See your calendar here", style = MaterialTheme.typography.titleMediumEmphasized)
        Text(
            "Kairo can show events from the calendars on this phone, including Google Calendar. Nothing is uploaded; there's no sign-in.",
            style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant,
        )
        Button(onClick = onAllow, shapes = ButtonDefaults.shapes()) { Text("Allow calendar access") }
    }
}

/** A free day: the shared empty state (random nested shapes, no icon). */
@Composable
private fun EmptyDay() {
    app.kairo.ui.common.EmptyState(
        title = "Nothing planned",
        body = "No tasks or events on this day. Tap + to add one.",
        size = 176.dp,
    )
}

/** Rounded, gapped day cells; today is a Cookie9 number; dots = tasks (primary), events (tertiary). */
@Composable
private fun MonthGrid(
    ym: YearMonth, today: LocalDate, selected: LocalDate,
    taskDays: Set<LocalDate>, eventDays: Set<LocalDate>, onPick: (LocalDate) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val first = DayOfWeek.MONDAY
    val lead = (ym.atDay(1).dayOfWeek.value - first.value + 7) % 7
    val cells = (0 until 42).map { ym.atDay(1).minusDays(lead.toLong()).plusDays(it.toLong()) }
        .chunked(7) // always six weeks, so swiping between months never changes the height
    Column(
        Modifier.clip(RoundedCornerShape(28.dp)).background(k.item).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row {
            (0..6).forEach { i ->
                Text(
                    first.plus(i.toLong()).getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
            }
        }
        cells.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { d ->
                    val inMonth = d.month == ym.month
                    val isToday = d == today
                    val isSel = d == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel && !isToday) c.secondaryContainer else Color.Transparent)
                            .clickable(role = Role.Button) { onPick(d) }
                            .semantics { contentDescription = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isToday) Box(Modifier.size(36.dp).background(c.primary, MaterialShapes.Cookie9Sided.toShape()))
                        Text(
                            "${d.dayOfMonth}",
                            style = if (isToday) MaterialTheme.typography.titleSmallEmphasized else MaterialTheme.typography.bodyMedium,
                            color = when {
                                isToday -> c.onPrimary
                                isSel -> c.onSecondaryContainer
                                inMonth -> c.onSurface
                                else -> c.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                        )
                        Row(
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (d in taskDays) Box(Modifier.size(4.dp).background(c.primary, CircleShape))
                            if (d in eventDays) Box(Modifier.size(4.dp).background(c.tertiary, CircleShape))
                        }
                    }
                }
            }
        }
    }
}

/** Sunny badge "SAT 21" + summary (canvas Calendar day hero). */
@Composable
internal fun DayHero(day: LocalDate, today: LocalDate, tasks: Int, events: Int) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val spin by slowSpin(40_000)
        val pop = remember { Animatable(1f) }
        val popSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
        LaunchedEffect(day) { pop.snapTo(0.7f); pop.animateTo(1f, popSpec) }
        Box(Modifier.size(64.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }, contentAlignment = Alignment.Center) {
          Box(Modifier.matchParentSize().graphicsLayer { rotationZ = spin }.background(c.tertiaryContainer, MaterialShapes.Sunny.toShape()))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                    style = MaterialTheme.typography.labelSmallEmphasized, color = c.onTertiaryContainer)
                Text("${day.dayOfMonth}", style = MaterialTheme.typography.titleLargeEmphasized, color = c.onTertiaryContainer)
            }
        }
        val label = when (day) { today -> "Today"; today.plusDays(1) -> "Tomorrow"; today.minusDays(1) -> "Yesterday"
            else -> day.format(DateTimeFormatter.ofPattern("EEEE")) }
        Text(
            "$label · $tasks ${if (tasks == 1) "task" else "tasks"} · $events ${if (events == 1) "event" else "events"}",
            style = MaterialTheme.typography.titleMediumEmphasized,
        )
    }
}

private val TimeFmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
