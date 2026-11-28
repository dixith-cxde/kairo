@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.habits

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kairo.R
import app.kairo.habits.Habit
import app.kairo.ui.theme.KairoTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

private val Letters = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Canvas .hweek: "This week · 21 of 27 check-ins · 78%", arrows back through earlier weeks, and a
 * ring per day (done / due). A full day turns into a filled Cookie-9. Tap a day to show it below.
 */
@Composable
fun WeekCard(habits: List<Habit>, monday: LocalDate, picked: LocalDate, today: LocalDate, onWeek: (Int) -> Unit, onPick: (LocalDate) -> Unit) {
    val c = MaterialTheme.colorScheme
    val days = (0..6).map { monday.plusDays(it.toLong()) }
    val info = days.map { d -> val due = habits.filter { it.dueForRing(d) }; due.size to due.count { it.done(d) } }
    val past = days.indices.filter { !days[it].isAfter(today) }
    val due = past.sumOf { info[it].first }; val done = past.sumOf { info[it].second }
    val title = when (monday) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) -> "This week"
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1) -> "Last week"
        else -> monday.format(Short) + " – " + monday.plusDays(6).format(Short)
    }
    val thisWeek = monday == today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(KairoTheme.colors.item).padding(horizontal = 8.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = c.onSurface)
                Text(
                    "$done of $due check-ins" + if (due > 0) " · ${(100f * done / due).roundToInt()}%" else "",
                    style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp),
                )
            }
            IconButton(onClick = { onWeek(-1) }, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(40.dp)) {
                Icon(painterResource(R.drawable.ic_chevron_left), "Previous week", tint = c.onSurface)
            }
            IconButton(onClick = { onWeek(1) }, enabled = !thisWeek, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(40.dp)) {
                Icon(painterResource(R.drawable.ic_chevron_right), "Next week", tint = c.onSurface.copy(alpha = if (thisWeek) 0.38f else 1f))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            days.forEachIndexed { i, d ->
                val (n, dn) = info[i]
                DayRing(
                    letter = Letters[i], day = d.dayOfMonth, progress = if (d.isAfter(today) || n == 0) 0f else dn / n.toFloat(),
                    selected = d == picked, isToday = d == today, future = d.isAfter(today),
                    aria = d.format(Full) + ", " + if (d.isAfter(today)) "coming up" else "$dn of $n habits done",
                    modifier = Modifier.weight(1f),
                ) { onPick(d) }
            }
        }
    }
}

@Composable
private fun DayRing(letter: String, day: Int, progress: Float, selected: Boolean, isToday: Boolean, future: Boolean, aria: String, modifier: Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) 12.dp else 20.dp, motion.fastSpatialSpec(), label = "dayCorner")
    val bg by animateColorAsState(if (selected) c.secondaryContainer else Color.Transparent, motion.defaultEffectsSpec(), label = "dayBg")
    val fg = if (selected) c.onSecondaryContainer else c.onSurfaceVariant
    val p by animateFloatAsState(progress, motion.slowSpatialSpec(), label = "ring")
    val full = progress >= 1f
    val pop by animateFloatAsState(if (full) 1f else 0f, motion.fastSpatialSpec(), label = "full")
    Column(
        modifier.height(84.dp).alpha(if (future) 0.38f else 1f).clip(RoundedCornerShape(corner)).background(bg)
            .selectable(selected, enabled = !future, role = Role.RadioButton, interactionSource = interaction, indication = ripple(), onClick = onClick)
            .semantics { contentDescription = aria }
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(letter, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isToday && !selected) c.primary else fg)
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            val arc = if (selected) c.onSecondaryContainer else c.primary
            Canvas(Modifier.fillMaxSize().padding(2.dp)) {
                val sw = 4.dp.toPx()
                val st = Stroke(sw, cap = StrokeCap.Round)
                val inset = sw / 2
                val tl = androidx.compose.ui.geometry.Offset(inset, inset)
                val sz = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw)
                val gap = 23.4f // canvas G = 6.5 of 100
                if (pop < 1f) {
                    if (p <= 0.001f) drawArc(fg.copy(alpha = 0.24f), -90f, 360f, false, tl, sz, style = st)
                    else {
                        drawArc(arc, -90f, p * 360f, false, tl, sz, style = st)
                        val rest = 360f - p * 360f - 2 * gap
                        if (rest > 0f) drawArc(fg.copy(alpha = 0.24f), -90f + p * 360f + gap, rest, false, tl, sz, style = st)
                    }
                }
            }
            if (pop > 0f) Box(
                Modifier.size(44.dp).graphicsLayer { scaleX = 0.4f + 0.6f * pop; scaleY = 0.4f + 0.6f * pop; rotationZ = -60f * (1 - pop); alpha = pop }
                    .background(if (selected) c.onSecondaryContainer else c.primary, MaterialShapes.Cookie9Sided.toShape()),
            )
            Text(
                "$day", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = when { full && selected -> c.secondaryContainer; full -> c.onPrimary; selected -> c.onSecondaryContainer; else -> c.onSurface },
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            )
        }
    }
}

private val Short = DateTimeFormatter.ofPattern("d MMM")
private val Full = DateTimeFormatter.ofPattern("EEEE d")
