@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.toShape
import app.kairo.R
import app.kairo.data.calendar.CalEvent
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import app.kairo.ui.common.TaskSwipe
import app.kairo.ui.theme.KairoTheme
import java.time.LocalDate

private fun shape(i: Int, n: Int): RoundedCornerShape {
    val big = 28.dp; val small = 8.dp
    return RoundedCornerShape(
        topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
        bottomStart = if (i == n - 1) big else small, bottomEnd = if (i == n - 1) big else small,
    )
}

/** Canvas .crw for an event: a round tinted icon tile, title, "10:30 AM – 11:30 AM · Calendar". Phone events are read-only. */
@Composable
internal fun EventRow(e: CalEvent, day: LocalDate, i: Int, n: Int, onOpen: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    SegmentedListItem(
        onClick = onOpen,
        shapes = ListItemDefaults.shapes(shape = shape(i, n), pressedShape = RoundedCornerShape(28.dp)),
        colors = ListItemDefaults.segmentedColors(containerColor = k.item),
        contentPadding = PaddingValues(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        modifier = Modifier.heightIn(min = 64.dp),
        leadingContent = {
            Box(Modifier.size(40.dp).background(c.tertiary.copy(alpha = 0.22f).compositeOver(k.item), CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_event_filled), null, Modifier.size(20.dp), tint = c.tertiary)
            }
        },
        supportingContent = {
            val when_ = span(e, day)?.let { (a, b) -> fmt(a) + " – " + fmt(b) }
                ?: if (e.allDay && e.start.toLocalDate() == e.end.toLocalDate().minusDays(1)) "All day"
                else if (e.allDay) "All day · " + e.start.toLocalDate().format(ShortDay) + " – " + e.end.toLocalDate().minusDays(1).format(ShortDay)
                else "All day"
            Text("$when_ · ${e.calendar}", style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
    ) { Text(e.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

/** Canvas .crw for a task: a round check in its list colour (a filled rounded square when done), title, "9:30 AM · Work". */
@Composable
internal fun CalTaskRow(t: Task, list: TaskList?, i: Int, n: Int, onToggle: (Task) -> Unit, onOpen: (Task) -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val ring = list?.let { k.accent(it.color) } ?: c.onSurfaceVariant
    TaskSwipe(t, enabled = true, onComplete = onToggle) {
        SegmentedListItem(
            onClick = { onOpen(t) },
            shapes = ListItemDefaults.shapes(shape = shape(i, n), pressedShape = RoundedCornerShape(28.dp)),
            colors = ListItemDefaults.segmentedColors(containerColor = k.item),
            contentPadding = PaddingValues(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            modifier = Modifier.heightIn(min = 64.dp),
            leadingContent = {
                val corner by animateDpAsState(if (t.done) 8.dp else 10.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "ck")
                val fill by animateColorAsState(if (t.done) c.primary else Color.Transparent, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "ckfill")
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Checkbox, onClickLabel = if (t.done) "Mark not done" else "Complete") { onToggle(t) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(20.dp).clip(RoundedCornerShape(corner)).background(fill)
                            .then(if (t.done) Modifier else Modifier.border(2.dp, ring, RoundedCornerShape(corner))),
                        contentAlignment = Alignment.Center,
                    ) { if (t.done) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp), tint = c.onPrimary) }
                }
            },
            supportingContent = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    t.time?.let { Text(it.format(Clock) + " ·", style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")) }
                    Box(Modifier.size(8.dp).background(ring, CircleShape))
                    Text(list?.name ?: "Inbox", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
            },
        ) {
            Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, textDecoration = if (t.done) TextDecoration.LineThrough else null,
                color = if (t.done) c.onSurfaceVariant else c.onSurface)
        }
    }
}

private fun fmt(min: Int): String = java.time.LocalTime.of((min / 60) % 24, min % 60).format(Clock).let { if (min >= 24 * 60) "12:00 AM" else it }

/** Canvas calendar access banner (top of the tab, like "Reminders are off"): Cookie-9 icon, title, Allow / Not now. */
@Composable
internal fun CalAccessBanner(onAllow: () -> Unit, onLater: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    Row(
        Modifier.padding(top = 8.dp).clip(RoundedCornerShape(28.dp)).background(k.item).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(48.dp).background(c.tertiaryContainer, androidx.compose.material3.MaterialShapes.Cookie9Sided.toShape()), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_calendar_month), null, tint = c.onTertiaryContainer)
        }
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text("Show your Google Calendar", style = MaterialTheme.typography.titleMedium, color = c.onSurface)
            Text("Events from this phone’s calendars. No sign-in.", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Button(onClick = onAllow, shapes = androidx.compose.material3.ButtonDefaults.shapes()) { Text("Allow") }
                androidx.compose.material3.TextButton(onClick = onLater, shapes = androidx.compose.material3.ButtonDefaults.shapes()) { Text("Not now") }
            }
        }
    }
}
