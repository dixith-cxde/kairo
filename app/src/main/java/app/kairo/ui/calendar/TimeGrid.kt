@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.calendar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.model.Task
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

/** One block on the time grid or one chip in the all-day strip (an event or a task). */
internal data class Slot(
    val key: String,
    val title: String,
    val startMin: Int,
    val endMin: Int,
    val container: Color,
    val content: Color,
    val task: Task?,
    val done: Boolean,
    val sub: String?,
)

/** What a week/day page needs to draw a date. */
internal class DayData(
    val timed: (LocalDate) -> List<Slot>,
    val allDay: (LocalDate) -> List<Slot>,
    val taskCount: (LocalDate) -> Int,
    val eventCount: (LocalDate) -> Int,
)

private val HourHeight = 56.dp
private val Gutter = 44.dp
private const val MinBlockMinutes = 22 // a 30-min task still reads as a block, not a line

/** Week view: swipe for weeks; header (tap a day to open it), all-day strip, scrollable hours. */
@Composable
internal fun WeekView(
    pager: PagerState, weekAt: (Int) -> LocalDate, today: LocalDate, selected: LocalDate,
    data: DayData, onPickDay: (LocalDate) -> Unit, onOpenTask: (Task) -> Unit, modifier: Modifier = Modifier,
) {
    val k = KairoTheme.colors
    val now by rememberNow()
    // One vertical scroll shared by every week, so swiping keeps the same hours in view.
    val scroll = rememberHourScroll(now)
    HorizontalPager(pager, modifier, verticalAlignment = Alignment.Top) { page ->
        val days = remember(page) { val s = weekAt(page); (0L..6L).map { s.plusDays(it) } }
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).clip(RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp)).background(k.item)) {
            WeekHeader(days, today, selected, onPickDay)
            AllDayStrip(days, data.allDay, maxPerDay = 2, onOpenTask)
            HourScroll(scroll) { TimeGrid(days, today, now, data.timed, onOpenTask) }
        }
    }
}

/** Day view: swipe for days; the Sunny day hero, all-day strip and one column of hours. */
@Composable
internal fun DayView(
    pager: PagerState, dayAt: (Int) -> LocalDate, today: LocalDate,
    data: DayData, onOpenTask: (Task) -> Unit, modifier: Modifier = Modifier,
) {
    val k = KairoTheme.colors
    val now by rememberNow()
    val scroll = rememberHourScroll(now)
    HorizontalPager(pager, modifier, verticalAlignment = Alignment.Top) { page ->
        val d = dayAt(page)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            DayHero(d, today, data.taskCount(d), data.eventCount(d))
            Column(Modifier.weight(1f).clip(RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp)).background(k.item).padding(top = 8.dp)) {
                AllDayStrip(listOf(d), data.allDay, maxPerDay = Int.MAX_VALUE, onOpenTask)
                HourScroll(scroll) { TimeGrid(listOf(d), today, now, data.timed, onOpenTask) }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.HourScroll(scroll: ScrollState, content: @Composable () -> Unit) {
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)) {
        Spacer(Modifier.height(8.dp))
        content()
        Spacer(Modifier.height(120.dp)) // clear the floating nav
    }
}

/** Starts an hour before now (or at 7am if that's earlier in the day), like Google Calendar. */
@Composable
private fun rememberHourScroll(now: LocalTime): ScrollState {
    val hour = max(7, now.hour - 1).coerceAtMost(17)
    val px = with(LocalDensity.current) { (HourHeight * hour).roundToPx() }
    return rememberScrollState(px)
}

/** The current time, ticking each minute (for the "now" line). */
@Composable
private fun rememberNow(): State<LocalTime> = produceState(LocalTime.now()) {
    while (true) {
        delay((60 - LocalTime.now().second) * 1000L)
        value = LocalTime.now()
    }
}

@Composable
private fun WeekHeader(days: List<LocalDate>, today: LocalDate, selected: LocalDate, onPick: (LocalDate) -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp)) {
        Spacer(Modifier.width(Gutter - 4.dp))
        days.forEach { d ->
            val isToday = d == today
            val isSel = d == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSel && !isToday) c.secondaryContainer else Color.Transparent)
                    .clickable(role = Role.Button) { onPick(d) }
                    .semantics { contentDescription = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSel && !isToday) c.onSecondaryContainer else c.onSurfaceVariant,
                )
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    if (isToday) Box(Modifier.matchParentSize().background(c.primary, MaterialShapes.Cookie9Sided.toShape()))
                    Text(
                        "${d.dayOfMonth}",
                        style = if (isToday) MaterialTheme.typography.titleSmallEmphasized else MaterialTheme.typography.bodyMedium,
                        color = when { isToday -> c.onPrimary; isSel -> c.onSecondaryContainer; else -> c.onSurface },
                    )
                }
            }
        }
    }
}

@Composable
private fun AllDayStrip(days: List<LocalDate>, chipsFor: (LocalDate) -> List<Slot>, maxPerDay: Int, onOpenTask: (Task) -> Unit) {
    val c = MaterialTheme.colorScheme
    val all = days.map(chipsFor)
    if (all.all { it.isEmpty() }) return
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 6.dp)) {
        Box(Modifier.width(Gutter - 4.dp).padding(top = 2.dp, end = 8.dp), contentAlignment = Alignment.TopEnd) {
            Icon(painterResource(R.drawable.ic_today), "All day", tint = c.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
        all.forEach { chips ->
            Column(Modifier.weight(1f).padding(horizontal = 1.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                chips.take(maxPerDay).forEach { s ->
                    Text(
                        s.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = s.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (s.done) TextDecoration.LineThrough else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (s.done) 0.6f else 1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(s.container)
                            .then(if (s.task != null) Modifier.clickable { onOpenTask(s.task) } else Modifier)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
                if (chips.size > maxPerDay) {
                    Text("+${chips.size - maxPerDay}", style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

/** Hours 0–23 down the side, one column per day; blocks placed by start/end, overlaps share the width. */
@Composable
private fun TimeGrid(
    days: List<LocalDate>, today: LocalDate, now: LocalTime,
    slotsFor: (LocalDate) -> List<Slot>, onOpenTask: (Task) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val line = c.outlineVariant
    val is24 = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val fmt = remember(is24) { DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h a", Locale.getDefault()) }
    Row(Modifier.fillMaxWidth().height(HourHeight * 24).padding(start = 4.dp, end = 4.dp)) {
        Column(Modifier.width(Gutter - 4.dp)) {
            (0 until 24).forEach { h ->
                Box(Modifier.height(HourHeight).fillMaxWidth()) {
                    if (h > 0) Text(
                        LocalTime.of(h, 0).format(fmt),
                        style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant, maxLines = 1,
                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp).offset(y = (-7).dp),
                    )
                }
            }
        }
        Row(
            Modifier.weight(1f).fillMaxHeight().drawBehind {
                val hp = HourHeight.toPx()
                val w = 1.dp.toPx()
                for (h in 0..24) drawLine(line, Offset(0f, h * hp), Offset(size.width, h * hp), w)
                for (i in 1 until days.size) {
                    val x = size.width * i / days.size
                    drawLine(line, Offset(x, 0f), Offset(x, size.height), w)
                }
            },
        ) {
            days.forEach { d ->
                DayColumn(slotsFor(d), if (d == today) now else null, onOpenTask, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun DayColumn(slots: List<Slot>, now: LocalTime?, onOpenTask: (Task) -> Unit, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    val placed = remember(slots) { layoutLanes(slots) }
    BoxWithConstraints(modifier.padding(horizontal = 1.dp)) {
        placed.forEach { p ->
            val s = p.slot
            val w = maxWidth / p.lanes
            val minutes = max(s.endMin - s.startMin, MinBlockMinutes)
            val h = HourHeight * (minutes / 60f)
            Column(
                Modifier
                    .offset(x = w * p.lane, y = HourHeight * (s.startMin / 60f))
                    .width(w)
                    .height(h)
                    .padding(1.dp)
                    .alpha(if (s.done) 0.6f else 1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(s.container)
                    .then(if (s.task != null) Modifier.clickable(role = Role.Button) { onOpenTask(s.task) } else Modifier)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Text(
                    s.title, style = MaterialTheme.typography.labelMedium, color = s.content,
                    maxLines = if (h >= 48.dp) 2 else 1, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (s.done) TextDecoration.LineThrough else null,
                )
                if (s.sub != null && h >= 44.dp) Text(
                    s.sub, style = MaterialTheme.typography.labelSmall, color = s.content, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (now != null) {
            val y = HourHeight * (now.toSecondOfDay() / 3600f)
            Box(Modifier.offset(y = y - 1.dp).fillMaxWidth().height(2.dp).background(c.primary))
            Box(Modifier.offset(x = (-3).dp, y = y - 5.dp).size(10.dp).background(c.primary, CircleShape))
        }
    }
}

private class Placed(val slot: Slot, val lane: Int, val lanes: Int)

/** Greedy lanes per cluster of overlapping blocks (each cluster splits its width evenly). */
private fun layoutLanes(slots: List<Slot>): List<Placed> {
    val out = mutableListOf<Placed>()
    var cluster = mutableListOf<Pair<Slot, Int>>()
    var laneEnds = mutableListOf<Int>()
    var clusterEnd = -1
    fun flush() {
        val n = laneEnds.size
        cluster.forEach { (s, l) -> out += Placed(s, l, n) }
        cluster = mutableListOf(); laneEnds = mutableListOf(); clusterEnd = -1
    }
    for (s in slots.sortedWith(compareBy<Slot>({ it.startMin }, { -it.endMin }))) {
        val end = max(s.endMin, s.startMin + MinBlockMinutes)
        if (cluster.isNotEmpty() && s.startMin >= clusterEnd) flush()
        val free = laneEnds.indexOfFirst { it <= s.startMin }
        val lane = if (free == -1) { laneEnds += end; laneEnds.lastIndex } else { laneEnds[free] = end; free }
        cluster += s to lane
        clusterEnd = max(clusterEnd, end)
    }
    flush()
    return out
}
