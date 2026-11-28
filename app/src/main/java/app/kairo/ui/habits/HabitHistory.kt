@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.habits.Habit
import app.kairo.ui.theme.KairoTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Canvas .hcal (Month): Monday-first grid; a done day sits on a Cookie-9 in the habit's colour,
 * a missed day is a grey square, a rest day an outline, today has a primary ring. Tap a past day
 * to check it in or out.
 */
@Composable
fun MonthHistory(h: Habit, month: YearMonth, today: LocalDate, onMonth: (Int) -> Unit, onDay: (LocalDate) -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val hc = k.accent(h.color)
    val on = HabitLook.onColor()
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - 1
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(k.item).padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)) {
        Row(Modifier.padding(start = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(month.format(MonthFmt), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = c.onSurface, modifier = Modifier.weight(1f))
            val canPrev = month.atEndOfMonth() >= h.createdAt.minusMonths(12)
            IconButton(onClick = { onMonth(-1) }, enabled = canPrev, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(40.dp)) {
                Icon(painterResource(R.drawable.ic_chevron_left), "Previous month", tint = c.onSurface.copy(alpha = if (canPrev) 1f else 0.38f))
            }
            val canNext = month < YearMonth.from(today)
            IconButton(onClick = { onMonth(1) }, enabled = canNext, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(40.dp)) {
                Icon(painterResource(R.drawable.ic_chevron_right), "Next month", tint = c.onSurface.copy(alpha = if (canNext) 1f else 0.38f))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant, modifier = Modifier.weight(1f).height(24.dp).padding(top = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        val cells = (0 until ((lead + month.lengthOfMonth() + 6) / 7) * 7).map { i -> if (i < lead || i >= lead + month.lengthOfMonth()) null else first.plusDays((i - lead).toLong()) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { d ->
                    Box(Modifier.weight(1f).aspectRatio(1f)) { if (d != null) DayCell(h, d, today, hc, on, onDay) }
                }
            }
        }
        Legend(hc)
    }
}

@Composable
private fun DayCell(h: Habit, d: LocalDate, today: LocalDate, hc: Color, on: Color, onDay: (LocalDate) -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val fut = d.isAfter(today)
    val rest = !fut && !h.scheduled(d)
    val done = h.done(d)
    val label = d.format(Aria) + ": " + when { fut -> "coming up"; done -> "done"; rest -> "rest day"; else -> "not done" }
    Box(
        Modifier.fillMaxSize()
            .then(if (d == today) Modifier.border(2.dp, c.primary, RoundedCornerShape(12.dp)).padding(3.dp) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .then(
                when {
                    done -> Modifier.background(hc, MaterialShapes.Cookie9Sided.toShape())
                    fut -> Modifier
                    rest -> Modifier.border(1.dp, c.outlineVariant, RoundedCornerShape(12.dp))
                    else -> Modifier.background(k.chip)
                },
            )
            .clickable(enabled = !fut && !rest, role = Role.Checkbox) { onDay(d) }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${d.dayOfMonth}", style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum", fontWeight = if (done || d == today) FontWeight.SemiBold else FontWeight.Normal),
            color = when { done -> on; fut -> c.onSurfaceVariant.copy(alpha = 0.5f); rest -> c.onSurfaceVariant; else -> c.onSurface },
        )
    }
}

/** Canvas .hyr (Year): 53 weeks of small squares, newest on the right, scrolls back. */
@Composable
fun YearHistory(h: Habit, today: LocalDate) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val hc = k.accent(h.color)
    val lastMon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weeks = (52 downTo 0).map { lastMon.minusWeeks(it.toLong()) }
    val scroll = rememberScrollState(Int.MAX_VALUE)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(k.item).padding(12.dp)) {
        Text("Last 12 months", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = c.onSurface, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Row(Modifier.horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            weeks.forEach { mon ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(if (mon.dayOfMonth <= 7) mon.format(MonShort) else " ", style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant, modifier = Modifier.height(14.dp))
                    (0..6).forEach { i ->
                        val d = mon.plusDays(i.toLong())
                        val mod = Modifier.size(14.dp).clip(RoundedCornerShape(4.dp))
                        Box(
                            when {
                                d.isAfter(today) -> mod
                                h.done(d) -> mod.background(hc)
                                !h.scheduled(d) -> mod.border(1.dp, c.outlineVariant, RoundedCornerShape(4.dp))
                                else -> mod.background(k.chip).alpha(if (d.isBefore(h.createdAt)) 0.35f else 1f)
                            }.then(if (d == today) Modifier.border(2.dp, c.primary, RoundedCornerShape(4.dp)) else Modifier),
                        )
                    }
                }
            }
        }
        Legend(hc)
    }
}

@Composable
private fun Legend(hc: Color) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    Row(Modifier.padding(start = 8.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(Triple("Done", hc, false), Triple("Missed", k.chip, false), Triple("Rest day", Color.Transparent, true)).forEach { (t, col, ring) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(col).then(if (ring) Modifier.border(1.dp, c.outlineVariant, RoundedCornerShape(4.dp)) else Modifier))
                Text(t, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

private val MonthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
private val MonShort = DateTimeFormatter.ofPattern("MMM")
private val Aria = DateTimeFormatter.ofPattern("EEEE d MMMM")
