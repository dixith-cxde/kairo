@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.habits

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
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
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.habits.Goal
import app.kairo.habits.Habit
import app.kairo.ui.common.ExpressiveSwipe
import app.kairo.ui.common.selectableRowColors
import app.kairo.ui.theme.KairoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class RowKind { Due, Rest, Archived }

/**
 * Canvas .hrow: the habit's own shape is the check (outline → filled, a count or time fills from
 * the bottom), then name, a meta line and ›. Swipe right = done, left = not done, for the picked day.
 */
@Composable
fun HabitRow(
    h: Habit, day: LocalDate, today: LocalDate, kind: RowKind, index: Int, count: Int,
    onTap: () -> Unit, onSwipe: (Boolean) -> Unit, onOpen: () -> Unit,
    selected: Boolean = false, selecting: Boolean = false, onSelect: (() -> Unit)? = null, handle: Modifier? = null,
) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val hc = k.accent(h.color)
    val done = h.done(day)
    val no = !done && h.dropped(day)
    val dim = kind != RowKind.Due
    val big = 28.dp; val small = 8.dp
    val shape = RoundedCornerShape(
        topStart = if (index == 0) big else small, topEnd = if (index == 0) big else small,
        bottomStart = if (index == count - 1) big else small, bottomEnd = if (index == count - 1) big else small,
    )
    ExpressiveSwipe(
        enabled = kind == RowKind.Due && !selecting,
        rightIcon = R.drawable.ic_check, rightLabel = "Done", leftIcon = R.drawable.ic_close, leftLabel = "Not done",
        onRight = { onSwipe(true) }, onLeft = { onSwipe(false) },
    ) {
        SegmentedListItem(
            selected = selected,
            onClick = { if (selecting && onSelect != null) onSelect() else onOpen() },
            onLongClick = onSelect, onLongClickLabel = "Select",
            shapes = ListItemDefaults.shapes(shape = shape, selectedShape = RoundedCornerShape(big), pressedShape = RoundedCornerShape(big)),
            colors = selectableRowColors(),
            contentPadding = PaddingValues(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            modifier = Modifier.heightIn(min = 72.dp),
            leadingContent = { ShapeCheck(h, day, done, no, enabled = !dim && !selecting, modifier = Modifier.alpha(if (dim) 0.5f else 1f), onTap = onTap) },
            supportingContent = {
                val (icon, text) = meta(h, day, today, kind, done, no)
                Row(Modifier.alpha(if (dim) 0.5f else 1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(painterResource(icon), null, Modifier.size(16.dp), tint = if (no) c.onSurfaceVariant else hc)
                    Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), color = c.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            trailingContent = {
                // While selecting, the handle drags the row to reorder (canvas habits select board).
                if (handle != null) Icon(painterResource(R.drawable.ic_drag_indicator), "Drag to reorder " + h.name, handle.size(40.dp).padding(8.dp), tint = c.onSurfaceVariant)
                else Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(20.dp), tint = c.onSurfaceVariant)
            },
        ) {
            Text(h.name, style = MaterialTheme.typography.bodyLarge, color = if (no) c.onSurfaceVariant else c.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.alpha(if (dim) 0.5f else 1f))
        }
    }
}

/** The shape as a check: outline in the habit colour; filled when done; dashed grey with ✕ when not done. */
@Composable
private fun ShapeCheck(h: Habit, day: LocalDate, done: Boolean, no: Boolean, enabled: Boolean, modifier: Modifier, onTap: () -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val hc = k.accent(h.color)
    val on = HabitLook.onColor()
    val shape = HabitLook.polygon(h.shapeName).toShape()
    val fill by animateFloatAsState(if (done) 1f else 0f, motion.fastSpatialSpec(), label = "fill")
    val part = if (!done && !no && h.goal != Goal.Yes) (h.count(day) / h.target).toFloat().coerceIn(0f, 1f) else 0f
    val level by animateFloatAsState(part, motion.slowSpatialSpec(), label = "level")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.88f else 1f, motion.fastSpatialSpec(), label = "press")
    val line = if (no) c.onSurfaceVariant else hc
    Box(
        modifier.size(48.dp).graphicsLayer { scaleX = press; scaleY = press }
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Checkbox, interactionSource = interaction, indication = ripple(bounded = false, radius = 24.dp),
                onClickLabel = tapLabel(h, done), onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(40.dp).drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                val path = (outline as? Outline.Generic)?.path ?: Path().apply { addOutline(outline) }
                val stroke = Stroke(2.dp.toPx(), pathEffect = if (no) PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())) else null)
                onDrawBehind {
                    if (level > 0f) clipRect(top = size.height * (1 - level)) { drawPath(path, hc.copy(alpha = 0.35f)) }
                    if (fill > 0f) drawPath(path, hc.copy(alpha = fill))
                    if (fill < 1f) drawPath(path, line.copy(alpha = 1f - fill), style = stroke)
                }
            },
        )
        val iconRes = when { no -> R.drawable.ic_close; else -> HabitLook.icon(h.icon, done) }
        val tint = if (done) on else if (no) c.onSurfaceVariant else hc
        if (iconRes != null) Icon(painterResource(iconRes), null, Modifier.size(20.dp), tint = tint)
        else Text((h.name.trim().firstOrNull() ?: '?').uppercase(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = tint)
    }
}

private fun tapLabel(h: Habit, done: Boolean) = when (h.goal) {
    Goal.Yes -> if (done) "Undo" else "Check in"
    Goal.Count -> if (done) "Reset" else "Add ${num(h.e.step)}"
    Goal.Time -> if (done) "Reset" else "Mark ${num(h.target)} min done"
}

/** Meta line, in the canvas order: archived, paused, rest, not done, progress, streak. */
private fun meta(h: Habit, d: LocalDate, today: LocalDate, kind: RowKind, done: Boolean, no: Boolean): Pair<Int, String> {
    val pz = h.paused(d)
    return when {
        kind == RowKind.Archived -> R.drawable.ic_archive to "Archived"
        pz -> R.drawable.hi_pause_circle_filled to ("Paused" + (h.e.pauseTo?.let { " · back " + it.plusDays(1).format(Back) } ?: ""))
        kind == RowKind.Rest -> R.drawable.hi_bedtime_filled to "Rest day"
        no -> R.drawable.hi_block to if (d == today) "Not done today" else "Not done"
        !done && h.goal == Goal.Time -> R.drawable.ic_timer_filled to (if (h.count(d) > 0) "${h.count(d).toInt()} of ${num(h.target)} min" else "${num(h.target)} min")
        !done && h.goal == Goal.Count -> R.drawable.hi_water_full_filled to "${num(h.count(d))} of ${num(h.target)} ${h.e.unit}".trim()
        !done && h.perWeek != null -> R.drawable.hi_date_range_filled to h.weekDone(d, today).let { w ->
            (if (w >= h.perWeek!!) "Goal met · " else "") + "$w of ${h.perWeek} this week"
        }
        else -> R.drawable.ic_local_fire_department_filled to h.streak(today).let { s ->
            val unit = if (h.perWeek != null) "week" else "day"
            if (s > 0) "$s $unit" + (if (s == 1) "" else "s") else "Start today"
        }
    }
}

internal fun num(v: Double): String = if (v % 1.0 == 0.0) "%,d".format(v.toLong()) else "%.1f".format(v)

private val Back = DateTimeFormatter.ofPattern("EEE d MMM")
