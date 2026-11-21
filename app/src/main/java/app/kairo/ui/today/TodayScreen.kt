@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.today

import app.kairo.ui.common.LocalOpenSearch
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.remember
import app.kairo.habits.Habit
import app.kairo.ui.habits.HabitDetailSheet
import app.kairo.ui.habits.HabitSheet
import app.kairo.ui.habits.habitsSection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import app.kairo.ui.common.Entrance
import app.kairo.ui.common.LocalAppSnackbar
import app.kairo.ui.common.Selection
import app.kairo.ui.common.SelectBadge
import app.kairo.ui.common.DateChoice
import app.kairo.ui.common.DateSheet
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.common.SelectionTopBar
import app.kairo.ui.common.TaskSelectionToolbar
import app.kairo.ui.common.selectableRowColors
import app.kairo.ui.common.countUp
import app.kairo.ui.common.rememberEntrance
import app.kairo.ui.common.rememberSelection
import app.kairo.ui.common.slowSpin
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.ui.common.EmptyState
import app.kairo.ui.common.TabHeader
import app.kairo.ui.common.TaskSwipe
import app.kairo.ui.common.TaskOptionsSheet
import app.kairo.ui.common.MoreButton
import app.kairo.ui.common.fullBleed
import app.kairo.ui.onboarding.tourTarget
import app.kairo.data.model.Priority
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import app.kairo.ui.shapes.polygon
import app.kairo.ui.theme.KairoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Everything the Today screen shows. Built from the database in step 4. */
data class TodayState(
    val overdue: List<Task>,
    val today: List<Task>,
    val completed: List<Task>,
    val upcoming: List<Task>,
    val lists: Map<Long, TaskList>,
) {
    val total get() = today.size + completed.size
    val done get() = completed.size
    val next get() = today.firstOrNull { it.time != null }
}

@Composable
fun TodayScreen(
    state: TodayState,
    onToggle: (Task) -> Unit,
    onOpen: (Task) -> Unit,
    modifier: Modifier = Modifier,
    onSettings: () -> Unit = {},
    onFocus: () -> Unit = {},
) {
    val k = KairoTheme.colors
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(TodayFilter.All) }
    val everything = state.overdue + state.today + state.completed + state.upcoming
    val snackbar = LocalAppSnackbar.current
    val selection = rememberSelection()
    val entrance = rememberEntrance()
    var rescheduleOverdue by remember { mutableStateOf(false) }
    val app = LocalKairoApp.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Completing shows "Completed · Undo". Unchecking a done task just flips it back.
    val toggle: (Task) -> Unit = { t ->
        onToggle(t)
        if (!t.done) scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar("Completed “${t.title}”", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: undo
                onToggle(t.copy(done = true))
            }
        }
    }

    // Habits live with the tasks (owner, 2026-11-21).
    val kairoApp = LocalKairoApp.current
    val habits by kairoApp.habits.habits.collectAsStateWithLifecycle(initialValue = emptyList())
    var newHabit by remember { mutableStateOf(false) }
    var openHabit by remember { mutableStateOf<Habit?>(null) }
    var editHabit by remember { mutableStateOf<Habit?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = k.bg,
        bottomBar = {
            TaskSelectionToolbar(selection, everything, state.lists.values.toList(), onOpen)
        },
    ) { _ ->
      Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                bottom = 140.dp, // clear the floating nav
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") {
                TabHeader(
                    title = "Today",
                    subtitle = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                    modifier = Modifier.fullBleed(),
                    searchModifier = Modifier.tourTarget("search", 24.dp),
                    actions = {
                        FilledTonalIconButton(onClick = onFocus, shapes = IconButtonDefaults.shapes(), modifier = Modifier.tourTarget("focus", 24.dp)) {
                            Icon(painterResource(R.drawable.ic_timer), contentDescription = "Focus timer")
                        }
                        FilledTonalIconButton(onClick = onSettings, shapes = IconButtonDefaults.shapes(), modifier = Modifier.tourTarget("settings", 24.dp)) {
                            Icon(painterResource(R.drawable.ic_settings_filled), contentDescription = "Settings")
                        }
                    },
                    below = { Box(Modifier.tourTarget("filters", 16.dp)) { FilterChips(state, filter) { filter = it } } },
                )
            }
            if (filter == TodayFilter.All || filter == TodayFilter.Today) run {
                habitsSection(habits, onAdd = { newHabit = true }, onOpen = { openHabit = it }) { h ->
                    if (!h.doneOn(LocalDate.now())) haptic.performHapticFeedback(HapticFeedbackType.ToggleOn) // rule 9: complete
                    kairoApp.appScope.launch { kairoApp.habits.toggle(h) }
                }
            }
            // A blank day (nothing due, nothing done, nothing overdue): one calm empty page instead of
            // a 0/0 ring and empty headers (owner, 2026-11-21).
            val blank = state.total == 0 && state.overdue.isEmpty() && state.completed.isEmpty()
            if (blank && (filter == TodayFilter.All || filter == TodayFilter.Today)) {
                item(key = "blank") {
                    EmptyState(
                        title = "A clear day",
                        body = "Nothing planned for today. Add a task with + or say it to the Quick add widget.",
                        size = 220.dp,
                    )
                }
                return@LazyColumn
            }
            item(key = "hero") { with(entrance) { HeroCard(state, Modifier.enter("hero", 0).tourTarget("hero", 32.dp)) } }

            val all = filter == TodayFilter.All
            if (state.overdue.isNotEmpty() && (all || filter == TodayFilter.Overdue)) {
                item(key = "h-overdue") {
                    SectionHeader("Overdue · ${state.overdue.size}", MaterialTheme.colorScheme.error) {
                        TextButton(shapes = ButtonDefaults.shapes(), onClick = { rescheduleOverdue = true }) { Text("Reschedule") }
                    }
                }
                taskGroup(state.overdue, state.lists, toggle, onOpen, overdue = true, selection = selection, entrance = entrance)
            }

            if (all || filter == TodayFilter.Today) {
                item(key = "h-today") {
                    SectionHeader("Today · ${state.today.size} left", MaterialTheme.colorScheme.primary) {
                        Text("Sorted by time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                taskGroup(state.today, state.lists, toggle, onOpen, selection = selection, entrance = entrance)
            }

            if (filter == TodayFilter.Upcoming) {
                state.upcoming.groupBy { it.due!! }.forEach { (day, tasks) ->
                    item(key = "h-up-$day") {
                        SectionHeader(upcomingLabel(day), MaterialTheme.colorScheme.primary) {
                            Text("${tasks.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    taskGroup(tasks, state.lists, toggle, onOpen, keyPrefix = "u", selection = selection, entrance = entrance)
                }
            }

            if (all) {
                item(key = "completed-toggle") {
                    CompletedToggle(
                        count = state.completed.size, open = showCompleted,
                        groupSize = if (showCompleted) state.completed.size + 1 else 1,
                    ) { showCompleted = !showCompleted }
                }
                if (showCompleted) taskGroup(
                    state.completed, state.lists, toggle, onOpen, keyPrefix = "c", selection = selection, entrance = entrance, indexOffset = 1,
                )
            } else if (filter == TodayFilter.Done) {
                item(key = "h-done") { SectionHeader("Done today · ${state.completed.size}", MaterialTheme.colorScheme.primary) {} }
                taskGroup(state.completed, state.lists, toggle, onOpen, keyPrefix = "c", selection = selection, entrance = entrance)
            }

            val empty = when (filter) {
                TodayFilter.All -> null
                TodayFilter.Overdue -> "Nothing overdue".takeIf { state.overdue.isEmpty() }
                TodayFilter.Today -> null // the Today header already says "0 left"
                TodayFilter.Upcoming -> "Nothing in the next 7 days".takeIf { state.upcoming.isEmpty() }
                TodayFilter.Done -> "Nothing done yet today".takeIf { state.completed.isEmpty() }
            }
            if (empty != null) item(key = "empty") {
                EmptyState(
                    title = empty,
                    body = when (filter) { TodayFilter.Overdue -> "You're caught up."; TodayFilter.Upcoming -> "The week ahead is open."
                        else -> "Tick something off and it shows here." },
                    size = 180.dp,
                )
            }
        }
        // Selection bar floats over the header instead of pushing the list down.
        SelectionTopBar(selection, everything.map { it.id }, Modifier.align(Alignment.TopCenter))
      }
    }
    if (newHabit) HabitSheet(onDismiss = { newHabit = false })
    editHabit?.let { h -> HabitSheet(h, onDismiss = { editHabit = null }) }
    openHabit?.let { h -> HabitDetailSheet(h, onEdit = { editHabit = h; openHabit = null }, onDismiss = { openHabit = null }) }
    if (rescheduleOverdue) DateSheet(
        initial = DateChoice(LocalDate.now(), null, true),
        title = "Reschedule ${state.overdue.size} overdue",
        onDismiss = { rescheduleOverdue = false },
    ) { ch ->
        val ids = state.overdue.map { it.id }
        scope.launch { app.repository.reschedule(ids, ch.date, ch.time, ch.reminder) }
        rescheduleOverdue = false
    }
}

/** Hero moment #2 (rule 10): progress ring on primaryContainer, a slow Cookie12 shape behind it. */
@Composable
private fun HeroCard(state: TodayState, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val left = state.today.size
    val target = if (state.total == 0) 0f else state.done / state.total.toFloat()
    // Ring fills from empty on first show and glides on every change (slow spatial spring).
    val ring = remember { Animatable(0f) }
    val ringSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(target) { ring.animateTo(target, ringSpec) }
    val doneShown by countUp(state.done)
    val spin by slowSpin()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(c.primaryContainer),
    ) {
        // Decoration must not size the card: matchParentSize + unbounded wrap.
        Box(Modifier.matchParentSize()) {
          Box(
            Modifier
                .align(Alignment.TopEnd)
                .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                .offset(x = 56.dp, y = (-64).dp)
                .size(210.dp)
                .rotate(12f + spin)
                .alpha(0.10f)
                .background(c.onPrimaryContainer, MaterialShapes.Cookie12Sided.toShape()),
          )
        }
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .semantics { contentDescription = "${state.done} of ${state.total} tasks done" },
                contentAlignment = Alignment.Center,
            ) {
                CircularWavyProgressIndicator(
                    progress = { ring.value.coerceIn(0f, 1f) },
                    color = c.primary,
                    trackColor = c.onPrimaryContainer.copy(alpha = 0.18f),
                    modifier = Modifier.fillMaxSize(),
                )
                if (left == 0 && state.total > 0) {
                    // All done: the ring closes around a check on a Cookie shape that pops in.
                    val pop = remember { Animatable(0.4f) }
                    val popSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
                    LaunchedEffect(Unit) { pop.animateTo(1f, popSpec) }
                    Box(
                        Modifier.size(56.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationZ = spin }
                            .background(c.primary, MaterialShapes.Cookie7Sided.toShape()),
                        contentAlignment = Alignment.Center,
                    ) { Icon(painterResource(R.drawable.ic_check), null, tint = c.onPrimary, modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = -spin }) }
                } else Text(
                    "$doneShown/${state.total}",
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = c.onPrimaryContainer,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when { state.total == 0 -> "Nothing due today"; left == 0 -> "All done for today"; else -> "$left to go today" },
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = c.onPrimaryContainer,
                )
                if (left == 0 && state.total > 0) Text(
                    "${state.done} of ${state.total} finished. The rest of the day is yours.",
                    style = MaterialTheme.typography.bodyMedium, color = c.onPrimaryContainer.copy(alpha = 0.85f),
                )
                state.next?.let { n ->
                    Text(
                        "Next: ${n.title} · ${n.time!!.format(TimeFormat)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onPrimaryContainer.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Which part of the day the list shows (chips at the top). */
enum class TodayFilter(val label: String) { All("All"), Overdue("Overdue"), Today("Today"), Upcoming("Upcoming"), Done("Done") }

/**
 * The section switcher: the same Expressive connected ButtonGroup as Stats and Money (owner,
 * 2026-11-21) — pressing one springs it wider and squeezes its neighbours.
 */
@Composable
private fun FilterChips(state: TodayState, filter: TodayFilter, onPick: (TodayFilter) -> Unit) {
    app.kairo.ui.common.ConnectedGroup(
        TodayFilter.entries, filter, { it.label }, Modifier.padding(bottom = 12.dp),
    ) { onPick(it) }
}

private fun upcomingLabel(day: LocalDate): String =
    if (day == LocalDate.now().plusDays(1)) "Tomorrow" else day.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))

@Composable
private fun SectionHeader(title: String, color: Color, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 4.dp)
            .heightIn(min = 40.dp), // room for the Expressive text button
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmallEmphasized, color = color, modifier = Modifier.weight(1f))
        trailing()
    }
}

private fun LazyListScope.taskGroup(
    tasks: List<Task>,
    lists: Map<Long, TaskList>,
    onToggle: (Task) -> Unit,
    onOpen: (Task) -> Unit,
    overdue: Boolean = false,
    keyPrefix: String = "t",
    selection: Selection? = null,
    entrance: Entrance? = null,
    indexOffset: Int = 0, // > 0 when the group starts with a header segment (Completed)
) {
    itemsIndexed(tasks, key = { _, t -> "$keyPrefix${t.id}" }) { i, task ->
        val motion = MaterialTheme.motionScheme
        Column(
            // A completed row fades out and the rows below slide up (collapse).
            Modifier.animateItem(
                fadeInSpec = motion.defaultEffectsSpec(),
                placementSpec = motion.defaultSpatialSpec(),
                fadeOutSpec = motion.fastEffectsSpec(),
            ).then(entrance?.run { Modifier.enter("$keyPrefix${task.id}", i + 1) } ?: Modifier),
        ) {
            if (indexOffset > 0) Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
            TaskRow(task, lists[task.listId] ?: TaskList.None, i + indexOffset, tasks.size + indexOffset, onToggle, onOpen, overdue, selection)
            if (i < tasks.lastIndex && indexOffset == 0) Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
        }
    }
}

@Composable
fun TaskRow(
    task: Task,
    list: TaskList,
    index: Int,
    count: Int,
    onToggle: (Task) -> Unit,
    onOpen: (Task) -> Unit,
    overdue: Boolean = false,
    selection: Selection? = null,
) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val selected = selection != null && task.id in selection
    val prio = when (task.priority) {
        Priority.High -> k.prioHigh
        Priority.Medium -> k.prioMed
        Priority.Low -> k.prioLow
        Priority.None -> c.onSurfaceVariant
    }
    val motion = MaterialTheme.motionScheme
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    // Hero moment #1 (rule 10): tick -> burst + strike, then the row leaves when the burst settles.
    var shownDone by remember(task.id, task.done) { mutableStateOf(task.done) }
    val burst = remember { Animatable(0f) }
    val titleColor by animateColorAsState(
        if (shownDone) c.onSurfaceVariant else c.onSurface, motion.defaultEffectsSpec(), label = "title",
    )
    var options by remember { mutableStateOf(false) }
    if (options) TaskOptionsSheet(task, onOpen, onToggle, onDismiss = { options = false })
    // Swipe left = done, swipe right = delete (owner, 2026-11-21); off while selecting.
    TaskSwipe(task, enabled = selection?.active != true, onComplete = onToggle) {
    // M3 Expressive selectable segmented item: it morphs to the selected shape and color itself.
    SegmentedListItem(
        selected = selected,
        onClick = { if (selection?.active == true) selection.toggle(task.id) else onOpen(task) },
        onLongClick = selection?.let { { it.toggle(task.id) } },
        onLongClickLabel = "Select",
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = selectableRowColors(),
        leadingContent = {
          if (selection?.active == true) SelectBadge(selected) else
          Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        val v = burst.value
                        scaleX = 0.4f + v; scaleY = 0.4f + v
                        alpha = if (v == 0f) 0f else (1f - v).coerceIn(0f, 1f)
                        rotationZ = v * 30f
                    }
                    .background(c.tertiary, MaterialShapes.SoftBurst.toShape()),
            )
            Checkbox(
                checked = shownDone,
                onCheckedChange = { checked ->
                    if (checked) {
                        shownDone = true
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn) // rule 9: complete
                        scope.launch {
                            burst.snapTo(0f)
                            burst.animateTo(1f, motion.fastSpatialSpec())
                            onToggle(task)
                        }
                    } else {
                        shownDone = false
                        onToggle(task)
                    }
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = c.primary,
                    uncheckedColor = prio,
                    checkmarkColor = c.onPrimary,
                ),
                modifier = Modifier.semantics {
                    contentDescription = (if (task.done) "Mark not done: " else "Complete ") + task.title
                },
            )
          }
        },
        supportingContent = { TaskMeta(task, list, overdue) },
        trailingContent = if (selection?.active == true) null else { { MoreButton("More for ${task.title}") { options = true } } },
    ) {
        Text(
            task.title,
            style = MaterialTheme.typography.bodyLarge,
            color = titleColor,
            textDecoration = if (shownDone) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    }
}

/** "9:30 AM · ◆ Work · ☑ 2/5 · 🔔" — 12/16 meta line, onSurfaceVariant. */
@Composable
private fun TaskMeta(task: Task, list: TaskList, overdue: Boolean) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.bodySmall
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        @Composable fun dot() = Text("·", style = style, color = c.onSurfaceVariant)
        @Composable fun icon(res: Int, tint: Color = c.onSurfaceVariant) =
            Icon(painterResource(res), contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))

        if (overdue) {
            val days = task.due?.let { java.time.temporal.ChronoUnit.DAYS.between(it, LocalDate.now()) } ?: 0
            Text(
                when { days == 1L -> "Yesterday"; days in 2..6 -> "$days days ago"; else -> task.due?.format(DateTimeFormatter.ofPattern("EEE, d MMM")) ?: "" },
                style = style, color = c.error,
            )
            dot()
        } else if (task.time != null) {
            icon(R.drawable.ic_schedule)
            Text(task.time.format(TimeFormat), style = style, color = c.onSurfaceVariant)
            dot()
        }
        Box(Modifier.size(12.dp).background(k.accent(list.color), list.shape.polygon.toShape()))
        Text(list.name, style = style, color = c.onSurfaceVariant)
        if (task.subtasksTotal > 0) {
            dot(); icon(R.drawable.ic_checklist)
            Text("${task.subtasksDone}/${task.subtasksTotal}", style = style, color = c.onSurfaceVariant)
        }
        if (task.repeat != null) { dot(); icon(R.drawable.ic_repeat) }
        if (task.reminder) { dot(); icon(R.drawable.ic_notifications) }
        if (task.priority == Priority.High) { dot(); icon(R.drawable.ic_flag_filled, k.prioHigh) }
    }
}

@Composable
private fun CompletedToggle(count: Int, open: Boolean, groupSize: Int, onClick: () -> Unit) {
    val k = KairoTheme.colors
    val turn by androidx.compose.animation.core.animateFloatAsState(
        if (open) 180f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "chevron",
    )
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(0, groupSize),
        colors = ListItemDefaults.segmentedColors(containerColor = k.item),
        modifier = Modifier.padding(top = 12.dp),
        leadingContent = {
            Icon(
                painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                modifier = Modifier.rotate(turn),
            )
        },
        trailingContent = {
            Box(
                Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text("$count", style = MaterialTheme.typography.labelMediumEmphasized, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        },
    ) {
        Text("Completed", style = MaterialTheme.typography.titleSmallEmphasized)
    }
}

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

fun fakeTodayState(): TodayState =
    buildTodayState(FakeData.tasks, FakeData.lists.associateBy { it.id }, LocalDate.now())

@Preview(name = "Dark", showSystemUi = true)
@Composable
private fun TodayDark() = KairoTheme(darkTheme = true) { TodayScreen(fakeTodayState(), onToggle = {}, onOpen = {}) }

@Preview(name = "Light", showSystemUi = true)
@Composable
private fun TodayLight() = KairoTheme(darkTheme = false) { TodayScreen(fakeTodayState(), onToggle = {}, onOpen = {}) }
