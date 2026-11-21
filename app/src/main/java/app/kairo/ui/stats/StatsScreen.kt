@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.stats

import app.kairo.ui.common.LocalOpenSearch
import app.kairo.ui.common.SearchPill
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.data.TaskRepository
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import app.kairo.ui.theme.KairoTheme
import app.kairo.ui.shapes.MorphShape
import app.kairo.ui.common.countUp
import app.kairo.ui.common.slowSpin
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

enum class Range(val label: String, val badge: RoundedPolygon, val span: String, val previous: String, val unit: String) {
    // docs/03-design-system.md: Stats badge morphs Cookie9 → Clover4Leaf → Sunny.
    Week("Week", MaterialShapes.Cookie9Sided, "last 7 days", "previous 7 days", "day"),
    Month("Month", MaterialShapes.Clover4Leaf, "last 30 days", "previous 30 days", "week"),
    Year("Year", MaterialShapes.Sunny, "last 12 months", "previous year", "month"),
}

@Composable
private fun CardTitle(title: String, hint: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary)
        Caption(hint)
    }
}

@Composable
private fun Caption(text: String) =
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

private data class Stats(
    val total: Int,
    val change: Int?,                 // % vs previous period
    val bars: List<Pair<String, Int>>,
    val onTime: Float,                // 0..1
    val streak: Int,
    val byList: List<Pair<TaskList, Int>>,
    val heat: Map<LocalDate, Int>,    // this month
)

private fun compute(tasks: List<Task>, lists: List<TaskList>, range: Range, today: LocalDate): Stats {
    val zone = ZoneId.systemDefault()
    val done = tasks.filter { it.done && it.completedAt != null }
        .map { it to Instant.ofEpochMilli(it.completedAt!!).atZone(zone).toLocalDate() }
    val days = when (range) { Range.Week -> 7L; Range.Month -> 30L; Range.Year -> 365L }
    val start = today.minusDays(days - 1)
    val inRange = done.filter { !it.second.isBefore(start) && !it.second.isAfter(today) }
    val prev = done.count { !it.second.isBefore(start.minusDays(days)) && it.second.isBefore(start) }
    val bars = when (range) {
        Range.Week -> (0 until 7).map { i -> start.plusDays(i.toLong()).let { d ->
            d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) to inRange.count { it.second == d } } }
        Range.Month -> (0 until 5).map { w -> start.plusDays(w * 6L).let { s ->
            "W${w + 1}" to inRange.count { !it.second.isBefore(s) && it.second.isBefore(s.plusDays(6)) } } }
        Range.Year -> (11 downTo 0).map { m -> YearMonth.from(today).minusMonths(m.toLong()).let { ym ->
            ym.month.getDisplayName(TextStyle.NARROW, Locale.getDefault()) to inRange.count { YearMonth.from(it.second) == ym } } }
    }
    val dated = inRange.filter { it.first.due != null }
    val onTime = if (dated.isEmpty()) 0f else dated.count { !it.second.isAfter(it.first.due) } / dated.size.toFloat()
    val doneDays = done.map { it.second }.toSet()
    var streak = 0
    var d = if (today in doneDays) today else today.minusDays(1)
    while (d in doneDays) { streak++; d = d.minusDays(1) }
    val byList = inRange.groupingBy { it.first.listId }.eachCount()
        .mapNotNull { (id, n) -> lists.firstOrNull { it.id == id }?.let { it to n } }.sortedByDescending { it.second }
    val month = YearMonth.from(today)
    val heat = done.filter { YearMonth.from(it.second) == month }.groupingBy { it.second }.eachCount()
    return Stats(
        total = inRange.size,
        change = if (prev == 0) null else ((inRange.size - prev) * 100f / prev).roundToInt(),
        bars = bars, onTime = onTime, streak = streak, byList = byList, heat = heat,
    )
}

@Composable
fun StatsScreen(repo: TaskRepository) {
    val tasks by repo.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val lists by repo.lists.collectAsStateWithLifecycle(initialValue = emptyList())
    var range by rememberSaveable { mutableStateOf(Range.Week) }
    val today = LocalDate.now()
    val s = remember(tasks, lists, range) { compute(tasks, lists, range, today) }
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = k.bg,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Stats") },
                actions = { SearchPill(LocalOpenSearch.current, Modifier.padding(end = 12.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = k.bg, scrolledContainerColor = k.bg),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // M3 Expressive connected button group.
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                Range.entries.forEachIndexed { i, r ->
                    ToggleButton(
                        checked = r == range,
                        onCheckedChange = { range = r },
                        shapes = when (i) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            Range.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                    ) { Text(r.label) }
                }
            }

            // Editorial total + morphing badge.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    val shown by countUp(s.total)
                    Text(
                        "$shown",
                        style = MaterialTheme.typography.displayLargeEmphasized.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.5f),
                        color = c.onSurface,
                    )
                    Text("tasks done · ${range.span}", style = MaterialTheme.typography.titleMedium, color = c.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MorphBadge(range, s.change)
                    Caption("vs ${range.previous}")
                }
            }

            Card {
                CardTitle("Done per ${range.unit}", "Highlighted: your best ${range.unit}")
                Bars(s.bars, range)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.weight(1f)) {
                    CardTitle("On time", "Done by the due date")
                    val ring by androidx.compose.animation.core.animateFloatAsState(
                        s.onTime, MaterialTheme.motionScheme.slowSpatialSpec(), label = "onTime",
                    )
                    val pct by countUp((s.onTime * 100).roundToInt())
                    Box(Modifier.size(96.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                        CircularWavyProgressIndicator(progress = { ring.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxSize())
                        Text("$pct%", style = MaterialTheme.typography.titleLargeEmphasized)
                    }
                }
                Card(Modifier.weight(1f)) {
                    CardTitle("Streak", "Days in a row with a task done")
                    val spin by slowSpin(30_000)
                    Box(Modifier.size(96.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                        Box(Modifier.matchParentSize().graphicsLayer { rotationZ = spin }.background(c.tertiaryContainer, MaterialShapes.Sunny.toShape()))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${s.streak}", style = MaterialTheme.typography.headlineMediumEmphasized, color = c.onTertiaryContainer)
                            Text(if (s.streak == 1) "day" else "days", style = MaterialTheme.typography.labelMedium, color = c.onTertiaryContainer)
                        }
                    }
                }
            }

            if (s.byList.isNotEmpty()) Card {
                CardTitle("By list", "Where your done tasks came from")
                val total = s.byList.sumOf { it.second }.toFloat()
                Row(Modifier.fillMaxWidth().height(16.dp).clip(CircleShape), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    s.byList.forEach { (l, n) -> Box(Modifier.weight(n / total).fillMaxHeight().background(k.accent(l.color))) }
                }
                s.byList.take(4).forEach { (l, n) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(10.dp).background(k.accent(l.color), CircleShape))
                        Text(l.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text("$n", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                    }
                }
            }

            Card {
                CardTitle(YearMonth.from(today).month.getDisplayName(TextStyle.FULL, Locale.getDefault()), "Each square is a day · darker = more done")
                Heatmap(YearMonth.from(today), s.heat)
                // Legend: less → more.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.align(Alignment.End)) {
                    Caption("Less")
                    listOf(0f, 0.35f, 0.65f, 1f).forEach { a ->
                        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (a == 0f) c.surfaceContainerHighest else c.primary.copy(alpha = 0.3f + 0.7f * a)))
                    }
                    Caption("More")
                }
            }
        }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(28.dp)).background(KairoTheme.colors.item).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** Badge that morphs (Morph between range shapes) and turns when the range changes. */
@Composable
private fun MorphBadge(range: Range, change: Int?) {
    val c = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    var from by remember { mutableStateOf(range) }
    var to by remember { mutableStateOf(range) }
    val progress = remember { Animatable(1f) }
    val turn = remember { Animatable(0f) }
    LaunchedEffect(range) {
        if (range == to) return@LaunchedEffect
        from = to; to = range
        progress.snapTo(0f)
        coroutineScope {
            launch { progress.animateTo(1f, motion.defaultSpatialSpec()) }
            launch { turn.animateTo(turn.value + 60f, motion.defaultSpatialSpec()) }
        }
    }
    val morph = remember(from, to) { Morph(from.badge, to.badge) }
    Box(
        Modifier
            .size(104.dp)
            .semantics { contentDescription = change?.let { "${if (it >= 0) "Up" else "Down"} $it% from last ${range.label.lowercase()}" } ?: "No change data" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { rotationZ = turn.value }
                .background(c.primaryContainer, MorphShape(morph, progress.value)),
        )
        Text(
            change?.let { (if (it >= 0) "+" else "") + "$it%" } ?: "—",
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = c.onPrimaryContainer,
        )
    }
}

/** Bars, best bar in tertiary (canvas Stats board). */
@Composable
private fun Bars(bars: List<Pair<String, Int>>, range: Range) {
    val c = MaterialTheme.colorScheme
    // Bars grow up from zero, one after another, whenever the range changes.
    val grow = remember(range) { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(range) { grow.animateTo(1f, spec) }
    val max = (bars.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val best = bars.indexOfFirst { it.second == max && max > 0 }
    Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        bars.forEachIndexed { i, (label, n) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text("$n", style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                Box(
                    Modifier.fillMaxWidth()
                        .height((100f * n / max * (grow.value * bars.size - i * 0.5f).coerceIn(0f, 1f)).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (i == best) c.tertiary else c.primary.copy(alpha = if (n == 0) 0.2f else 1f)),
                )
                Text(label, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Heatmap(ym: YearMonth, heat: Map<LocalDate, Int>) {
    val c = MaterialTheme.colorScheme
    val max = (heat.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val lead = ym.atDay(1).dayOfWeek.value - 1
    val cells = List(lead) { null } + (1..ym.lengthOfMonth()).map { ym.atDay(it) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until 7).forEach { i ->
                    val d = week.getOrNull(i)
                    val n = d?.let { heat[it] } ?: 0
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    d == null -> androidx.compose.ui.graphics.Color.Transparent
                                    n == 0 -> c.surfaceContainerHighest
                                    else -> c.primary.copy(alpha = 0.3f + 0.7f * n / max)
                                },
                            ),
                    )
                }
            }
        }
    }
}
