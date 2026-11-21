@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.calendar

import app.kairo.ui.common.LocalOpenSearch
import app.kairo.ui.common.SearchPill
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import app.kairo.ui.theme.KairoTheme
import app.kairo.ui.today.TaskRow
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import app.kairo.ui.common.SelectionTopBar
import app.kairo.ui.common.TaskSelectionToolbar
import app.kairo.ui.common.rememberEntrance
import app.kairo.ui.common.rememberSelection
import app.kairo.ui.common.slowSpin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(repo: TaskRepository, calendar: PhoneCalendar, onOpenTask: (Task) -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val today = LocalDate.now()
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val ym = YearMonth.parse(month)
    var selected by rememberSaveable { mutableStateOf(today.toString()) }
    val day = LocalDate.parse(selected)
    var granted by remember { mutableStateOf(calendar.hasPermission()) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val scope = rememberCoroutineScope()

    val tasks by repo.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val lists by repo.lists.collectAsStateWithLifecycle(initialValue = emptyList())
    val listMap = remember(lists) { lists.associateBy { it.id } }
    // Reload when the month changes, permission is granted, or the phone's calendar changes.
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(granted) { if (granted) calendar.changes().collect { tick++ } }
    var refreshing by remember { mutableStateOf(false) }
    val events by produceState(emptyList<CalEvent>(), ym, granted, tick) {
        value = calendar.events(ym.atDay(1).minusDays(7), ym.atEndOfMonth().plusDays(8))
        refreshing = false
    }
    val pull = rememberPullToRefreshState()

    val tasksByDay = remember(tasks) { tasks.filter { it.due != null }.groupBy { it.due!! } }
    val eventDays = remember(events) {
        events.flatMap { e -> generateSequence(e.start.toLocalDate()) { it.plusDays(1) }
            .takeWhile { !it.isAfter(if (e.allDay) e.end.toLocalDate().minusDays(1) else e.end.toLocalDate()) }.map { it } }.toSet()
    }
    val dayTasks = tasksByDay[day].orEmpty().sortedWith(compareBy(nullsLast()) { it.time })
    val dayEvents = events.filter { e -> !day.isBefore(e.start.toLocalDate()) && (day.isBefore(e.end.toLocalDate()) || (!e.allDay && day == e.end.toLocalDate())) }

    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val selection = rememberSelection()
    val entrance = rememberEntrance()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = k.bg,
        bottomBar = { TaskSelectionToolbar(selection, dayTasks, lists, onOpenTask) },
        topBar = {
          Box {
            LargeFlexibleTopAppBar(
                title = { Text(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy"))) },
                actions = {
                    SearchPill(LocalOpenSearch.current)
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = { month = ym.minusMonths(1).toString() }) { Icon(painterResource(R.drawable.ic_chevron_left), "Previous month") }
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = { month = ym.plusMonths(1).toString() }) { Icon(painterResource(R.drawable.ic_chevron_right), "Next month") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = k.bg, scrolledContainerColor = k.bg),
                scrollBehavior = scroll,
            )
            SelectionTopBar(selection, dayTasks.map { it.id })
          }
        },
    ) { padding ->
      // Pull down to sync: M3 Expressive pull-to-refresh with the morphing LoadingIndicator.
      PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            scope.launch { calendar.requestSync(); tick++ }
        },
        state = pull,
        modifier = Modifier.padding(top = padding.calculateTopPadding()),
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(state = pull, isRefreshing = refreshing, modifier = Modifier.align(Alignment.TopCenter))
        },
      ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "grid") {
                // Months slide in the direction you move (spatial spring), like turning a page.
                val motion = MaterialTheme.motionScheme
                AnimatedContent(
                    targetState = ym,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(motion.defaultSpatialSpec()) { w -> dir * w / 3 } + fadeIn(motion.defaultEffectsSpec())) togetherWith
                            (slideOutHorizontally(motion.defaultSpatialSpec()) { w -> -dir * w / 3 } + fadeOut(motion.fastEffectsSpec()))
                    },
                    label = "month",
                ) { m -> MonthGrid(m, today, day, tasksByDay.keys, eventDays) { d -> selected = d.toString() } }
            }
            if (!granted) item(key = "perm") {
                Column(
                    Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(k.item).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("See your calendar here", style = MaterialTheme.typography.titleMediumEmphasized)
                    Text(
                        "Kairo can show events from the calendars on this phone, including Google Calendar. Nothing is uploaded; there's no sign-in.",
                        style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant,
                    )
                    Button(onClick = { ask.launch(Manifest.permission.READ_CALENDAR) }) { Text("Allow calendar access") }
                }
            }
            item(key = "hero") { DayHero(day, today, dayTasks.count { !it.done }, dayEvents.size) }
            itemsIndexed(dayEvents, key = { _, e -> "e${e.eventId}-${e.start}" }) { i, e ->
                Column(with(entrance) { Modifier.enter("e${e.eventId}-${e.start}", i) }) {
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
                Column(with(entrance) { Modifier.enter("$day-t${t.id}", dayEvents.size + i) }) {
                    listMap[t.listId]?.let { l ->
                        TaskRow(t, l, dayEvents.size + i, dayEvents.size + dayTasks.size,
                            onToggle = { scope.launch { repo.setDone(it, !it.done) } }, onOpen = onOpenTask, selection = selection)
                    }
                    Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
                }
            }
        }
      }
    }
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
        .chunked(7).filter { week -> week.any { it.month == ym.month } }
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
private fun DayHero(day: LocalDate, today: LocalDate, tasks: Int, events: Int) {
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
