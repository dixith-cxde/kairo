@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package app.kairo.ui.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.model.ListColor
import app.kairo.ui.theme.KairoTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** What the habit drawer edits (canvas `hS`). days: Monday = 0, null = every day. */
internal data class HabitDraft(
    val name: String, val icon: String, val shape: String, val color: ListColor,
    val goal: String, val target: Double, val unit: String, val step: Double,
    val days: Set<Int>?, val perWeek: Int?, val remind: Boolean, val time: LocalTime,
)

/** Keeps the draft across rotation and process death (a turned phone used to drop what you typed). */
internal val DraftSaver = androidx.compose.runtime.saveable.listSaver<HabitDraft, Any>(
    save = { d -> listOf(d.name, d.icon, d.shape, d.color.name, d.goal, d.target, d.unit, d.step, d.days?.sorted()?.joinToString(",") ?: "-", d.perWeek ?: 0, d.remind, d.time.toSecondOfDay()) },
    restore = { v ->
        HabitDraft(v[0] as String, v[1] as String, v[2] as String, ListColor.valueOf(v[3] as String), v[4] as String, v[5] as Double, v[6] as String, v[7] as Double,
            (v[8] as String).takeIf { it != "-" }?.split(',')?.map { it.toInt() }?.toSet(), (v[9] as Int).takeIf { it > 0 }, v[10] as Boolean, LocalTime.ofSecondOfDay((v[11] as Int).toLong()))
    },
)

/** Count units and what one tap adds (canvas UNITS). */
internal val Units = listOf("glasses" to 1.0, "pages" to 10.0, "reps" to 10.0, "steps" to 1000.0, "km" to 0.5, "times" to 1.0)

/** Time goal stops: 5 min apart to 1 h, then 15 min to 2 h, then 30 min to 5 h (canvas SLV). */
internal val Minutes = listOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 75, 90, 105, 120, 150, 180, 210, 240, 270, 300)

/** Reminder quick picks (canvas QT). */
internal val QuickTimes = listOf(LocalTime.of(7, 0), LocalTime.of(9, 0), LocalTime.of(12, 0), LocalTime.of(18, 0), LocalTime.of(21, 0))

internal fun minText(m: Int) = if (m < 60) "$m min" else "${m / 60} h" + if (m % 60 != 0) " ${m % 60} min" else ""

internal fun scheduleText(perWeek: Int?, days: Set<Int>?): String = perWeek?.let { "$it time" + (if (it == 1) "" else "s") + " a week" } ?: when (days) {
    null -> "Every day"
    setOf(0, 1, 2, 3, 4) -> "Weekdays"
    setOf(5, 6) -> "Weekends"
    else -> days.sorted().joinToString(", ") { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[it] }
}

private val Clock = DateTimeFormatter.ofPattern("h:mm a")
internal fun clock(t: LocalTime): String = t.format(Clock)
private fun shortClock(t: LocalTime) = ((t.hour % 12).takeIf { it != 0 } ?: 12).toString() + (if (t.minute != 0) ":%02d".format(t.minute) else "") + if (t.hour < 12) " AM" else " PM"

/** "20 pages · Weekdays · 9:00 PM" under the preview's name. */
internal fun draftMeta(d: HabitDraft) = (when (d.goal) { "count" -> num(d.target) + " " + d.unit.ifBlank { "times" } + " · "; "time" -> minText(d.target.toInt()) + " · "; else -> "" }) +
    scheduleText(d.perWeek, d.days) + if (d.remind) " · " + clock(d.time) else ""

/** Canvas .hgi: one item of a grouped card (28dp outer, 8dp inner corners, 3dp apart). */
@Composable
internal fun HItem(i: Int, n: Int, label: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val big = 28.dp; val small = 8.dp
    val shape = RoundedCornerShape(
        topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
        bottomStart = if (i == n - 1) big else small, bottomEnd = if (i == n - 1) big else small,
    )
    Column(modifier.fillMaxWidth().clip(shape).background(KairoTheme.colors.item).padding(horizontal = 8.dp, vertical = 12.dp)) {
        if (label != null) Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 12.dp))
        content()
    }
}

/** The preview row: the habit's shape filled with its colour and icon (tap to pick an icon), name and meta. */
@Composable
internal fun PreviewRow(d: HabitDraft, iconOpen: Boolean, onIcon: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val hc = k.accent(d.color)
    Row(Modifier.fillMaxWidth().background(k.item, RoundedCornerShape(28.dp)).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).semantics { contentDescription = if (iconOpen) "Close icons" else "Choose icon" }.clickable(role = Role.Button, onClick = onIcon),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(44.dp).background(hc, HabitLook.polygon(d.shape).toShape()), contentAlignment = Alignment.Center) {
                HabitLook.icon(d.icon, true)?.let { Icon(painterResource(it), null, Modifier.size(22.dp), tint = HabitLook.onColor()) }
            }
            Box(Modifier.align(Alignment.BottomEnd).offset(6.dp, 6.dp).size(22.dp).background(k.item, CircleShape).padding(2.dp).background(c.primary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_edit), null, Modifier.size(13.dp), tint = c.onPrimary)
            }
        }
        Column(Modifier.padding(start = 16.dp).weight(1f)) {
            Text(d.name.ifBlank { "Habit name" }, style = MaterialTheme.typography.bodyLarge, color = if (d.name.isBlank()) c.onSurfaceVariant else c.onSurface, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_event_repeat), null, Modifier.size(16.dp), tint = hc)
                Text(" " + draftMeta(d), style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), color = c.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/** Canvas .hicons: 24 icons, 6 a row; the picked one sits on a filled circle in the habit colour. */
@Composable
internal fun IconGrid(open: Boolean, d: HabitDraft, onPick: (String) -> Unit) {
    val k = KairoTheme.colors
    AnimatedVisibility(open) {
        Column(Modifier.padding(top = 8.dp).fillMaxWidth().background(k.chip, RoundedCornerShape(28.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HabitLook.icons.chunked(6).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { (icon, label) ->
                        val on = icon == d.icon
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(if (on) CircleShape else RoundedCornerShape(16.dp)).background(if (on) k.accent(d.color) else Color.Transparent)
                                .semantics { contentDescription = label }.clickable(role = Role.RadioButton) { onPick(icon) },
                            contentAlignment = Alignment.Center,
                        ) {
                            HabitLook.icon(icon, on)?.let { Icon(painterResource(it), null, tint = if (on) HabitLook.onColor() else MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}

/** Canvas .hshapes: the ten habit shapes, 5 a row; the picked one turns and gets a ring. */
@Composable
internal fun ShapeGrid(d: HabitDraft, onPick: (String) -> Unit) {
    val k = KairoTheme.colors
    val hc = k.accent(d.color)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HabitLook.shapes.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { s ->
                    val on = s == d.shape
                    val turn by animateFloatAsState(if (on) 30f else 0f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "turn")
                    val grow by animateFloatAsState(if (on) 1.1f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "grow")
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(if (on) CircleShape else RoundedCornerShape(16.dp))
                            .background(if (on) hc.copy(alpha = 0.16f) else k.chip)
                            .then(if (on) Modifier.border(2.dp, hc, CircleShape) else Modifier)
                            .semantics { contentDescription = s }.clickable(role = Role.RadioButton) { onPick(s) },
                        contentAlignment = Alignment.Center,
                    ) { Box(Modifier.size(30.dp).rotate(turn).scale(grow).background(hc, HabitLook.polygon(s).toShape())) }
                }
            }
        }
    }
}

/** Canvas .swts: seven colour swatches; the picked one is a rounded square with a check. */
@Composable
internal fun ColorSwatches(picked: ListColor, onPick: (ListColor) -> Unit) {
    val k = KairoTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ListColor.entries.forEach { col ->
            val on = col == picked
            Box(
                Modifier.weight(1f).aspectRatio(1f).clip(if (on) RoundedCornerShape(12.dp) else CircleShape).background(k.accent(col))
                    .semantics { contentDescription = col.name }.clickable(role = Role.RadioButton) { onPick(col) },
                contentAlignment = Alignment.Center,
            ) { if (on) Icon(painterResource(R.drawable.ic_check), null, tint = HabitLook.onColor()) }
        }
    }
}

/** A connected button group (Goal, days, times a week, reminder times): picked = solid primary. */
@Composable
internal fun <T> Connected(options: List<T>, isOn: (T) -> Boolean, label: (T) -> String, icon: (T) -> Int? = { null }, height: Int = 48, onPick: (T) -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        options.forEachIndexed { i, o ->
            ToggleButton(
                checked = isOn(o), onCheckedChange = { onPick(o) },
                shapes = when (i) { 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(); options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes(); else -> ButtonGroupDefaults.connectedMiddleButtonShapes() },
                colors = ToggleButtonDefaults.colors(containerColor = k.chip, contentColor = c.onSurface, checkedContainerColor = c.primary, checkedContentColor = c.onPrimary),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.weight(1f).height(height.dp),
            ) {
                icon(o)?.let { Icon(painterResource(it), null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)) }
                Text(label(o), style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"), maxLines = 1, softWrap = false)
            }
        }
    }
}

/** Single-choice small chips (canvas .pchips small), wrapping. */
@Composable
internal fun <T> Chips(options: List<T>, isOn: (T) -> Boolean, label: (T) -> String, lead: (@Composable () -> Unit)? = null, onPick: (T) -> Unit) {
    val c = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
        lead?.invoke()
        options.forEach { o ->
            FilterChip(
                selected = isOn(o), onClick = { onPick(o) }, label = { Text(label(o)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = c.primary, selectedLabelColor = c.onPrimary),
            )
        }
    }
}

internal fun timeLabel(t: LocalTime) = shortClock(t)
