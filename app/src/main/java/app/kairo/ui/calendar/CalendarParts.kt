@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.calendar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kairo.R
import app.kairo.ui.theme.KairoTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Canvas .chd: "November 2026" (month large, year small) and Go to today on the right. */
@Composable
internal fun CalHeader(title: String, year: Int, onToday: (() -> Unit)?) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Medium), color = c.onSurface)
        Text("$year", style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), color = c.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp).weight(1f))
        IconButton(onClick = { onToday?.invoke() }, enabled = onToday != null, shapes = IconButtonDefaults.shapes()) {
            Icon(painterResource(R.drawable.ic_today), "Go to today", tint = c.onSurfaceVariant.copy(alpha = if (onToday != null) 1f else 0.38f))
        }
    }
}

/** Canvas .crow: Month / Week / Day (real connected toggle buttons), then ‹ ›. */
@Composable
internal fun ViewRow(view: CalView, onPick: (CalView) -> Unit, onPrev: () -> Unit, onNext: () -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).padding(end = 4.dp), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            CalView.entries.forEachIndexed { i, v ->
                ToggleButton(
                    checked = v == view, onCheckedChange = { onPick(v) },
                    shapes = when (i) { 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(); 2 -> ButtonGroupDefaults.connectedTrailingButtonShapes(); else -> ButtonGroupDefaults.connectedMiddleButtonShapes() },
                    colors = ToggleButtonDefaults.colors(containerColor = k.chip, contentColor = c.onSurfaceVariant, checkedContainerColor = c.primary, checkedContentColor = c.onPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f).height(40.dp),
                ) { Text(v.name, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
            }
        }
        val unit = view.name.lowercase()
        IconButton(onClick = onPrev, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_chevron_left), "Previous $unit", tint = c.onSurface) }
        IconButton(onClick = onNext, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_chevron_right), "Next $unit", tint = c.onSurface) }
    }
}

/** Today is ringed by a Cookie-9; the picked day sits on a filled one (canvas .ck ring / fill). */
@Composable
private fun DayMark(picked: Boolean, today: Boolean, size: Int) {
    val c = MaterialTheme.colorScheme
    val shape = MaterialShapes.Cookie9Sided.toShape()
    when {
        picked -> Box(Modifier.size(size.dp).background(c.primary, shape))
        today -> Box(Modifier.size(size.dp).border(2.dp, c.primary, shape))
    }
}

@Composable
private fun Dots(task: Boolean, event: Boolean, onFill: Boolean, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    Row(modifier.height(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (task) Box(Modifier.size(5.dp).background(if (onFill) c.onPrimary else c.primary, CircleShape))
        if (event) Box(Modifier.size(5.dp).then(if (onFill) Modifier.border(1.5.dp, c.onPrimary, CircleShape) else Modifier.background(c.tertiary, CircleShape)))
    }
}

/** Canvas .ccal: six weeks, Monday first, in an item card; dots for open tasks (primary) and events (tertiary). */
@Composable
internal fun MonthGrid(month: YearMonth, today: LocalDate, picked: LocalDate, hasTask: (LocalDate) -> Boolean, hasEvent: (LocalDate) -> Boolean, onPick: (LocalDate) -> Unit) {
    val c = MaterialTheme.colorScheme
    val first = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(28.dp)).background(KairoTheme.colors.item).padding(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant, modifier = Modifier.weight(1f).height(24.dp).padding(top = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        (0 until 6).forEach { w ->
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until 7).forEach { i ->
                    val d = first.plusDays((w * 7 + i).toLong())
                    val on = d == picked; val isToday = d == today; val out = YearMonth.from(d) != month
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.RadioButton) { onPick(d) }
                            .semantics { contentDescription = d.format(Aria) + if (isToday) ", today" else "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        DayMark(on, isToday, 40)
                        Text(
                            "${d.dayOfMonth}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum", fontWeight = if (on || isToday) FontWeight.Bold else FontWeight.Normal),
                            color = when { on -> c.onPrimary; isToday -> c.primary; out -> c.onSurfaceVariant.copy(alpha = 0.5f); else -> c.onSurface },
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Dots(hasTask(d), hasEvent(d), on, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
                    }
                }
            }
        }
    }
}

/** Canvas .cweek: the week in a card, a pill bar per day for how full it is (blue; the picked day green; empty grey). */
@Composable
internal fun WeekStrip(monday: LocalDate, today: LocalDate, picked: LocalDate, loadOf: (LocalDate) -> Int, hasTask: (LocalDate) -> Boolean, hasEvent: (LocalDate) -> Boolean, onPick: (LocalDate) -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    Row(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(28.dp)).background(k.item).padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 8.dp)) {
        (0..6).forEach { i ->
            val d = monday.plusDays(i.toLong())
            val on = d == picked; val isToday = d == today
            val ld = loadOf(d)
            val h by animateDpAsState((6 + 34 * minOf(1f, ld / 480f)).dp, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "bar")
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable(role = Role.RadioButton) { onPick(d) }
                    .semantics { contentDescription = d.format(Aria) + ", " + if (ld > 0) hoursMinutes(ld) + " booked" else "free" }
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                    color = if (on) c.onSurface else if (isToday) c.primary else c.onSurfaceVariant)
                Box(Modifier.height(40.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.width(24.dp).height(h).background(if (ld == 0) c.outlineVariant else if (on) k.chartGreen else k.chartBlue, CircleShape))
                }
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    DayMark(on, isToday, 44)
                    Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum", fontWeight = if (on || isToday) FontWeight.Bold else FontWeight.Medium),
                        color = when { on -> c.onPrimary; isToday -> c.primary; else -> c.onSurface })
                }
                Dots(hasTask(d), hasEvent(d), false, Modifier)
            }
        }
    }
}

/** Canvas .csumc: the picked day in counts only (the date is already on screen). */
@Composable
internal fun SummaryCard(label: String, big: String, chips: List<Pair<Int, String>>) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(28.dp)).background(k.item).padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
        Text(big, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"), color = c.onSurface)
        if (chips.isNotEmpty()) FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            chips.forEach { (icon, t) ->
                Row(Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(k.chip).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(icon), null, Modifier.size(16.dp), tint = c.onSurfaceVariant)
                    Text(t, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontFeatureSettings = "tnum"), color = c.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

private val Aria = DateTimeFormatter.ofPattern("EEEE d MMMM")
