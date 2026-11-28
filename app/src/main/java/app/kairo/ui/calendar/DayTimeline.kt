package app.kairo.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kairo.R
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.delay
import java.time.LocalTime

/** One hour on the timeline (canvas HH = 56). */
internal val HourHeight = 56.dp

/** Canvas .callday: the day's all-day events and undated-time tasks as chips, beside a today icon. */
@Composable
internal fun AllDayRow(chips: List<Block>, onOpen: (Block) -> Unit) {
    if (chips.isEmpty()) return
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(painterResource(R.drawable.ic_today), "All day", Modifier.padding(top = 6.dp).width(36.dp).size(18.dp), tint = c.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            chips.forEach { b ->
                Text(
                    b.title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = b.fg, maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(b.bg).clickable { onOpen(b) }.padding(horizontal = 10.dp, vertical = 6.dp)
                        .alpha(if (b.done) 0.5f else 1f),
                )
            }
        }
    }
}

/**
 * Canvas .ctl: the picked day's 24 hours in an item card. Hour lines with labels in a 52dp gutter,
 * blocks in lanes (12dp corners; under 44dp they go one line), and today's now line: the line sits
 * behind blocks, its dot on top.
 */
@Composable
internal fun DayTimeline(blocks: List<Block>, isToday: Boolean, onOpen: (Block) -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val now by produceState(LocalTime.now()) { while (true) { delay(30_000); value = LocalTime.now() } }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(k.item).padding(top = 14.dp, bottom = 14.dp, end = 8.dp)) {
        Box(Modifier.fillMaxWidth().height(HourHeight * 24)) {
            (0 until 24).forEach { h ->
                Row(Modifier.offset(y = HourHeight * h - 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (h == 0) "" else LocalTime.of(h, 0).format(HourFmt),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFeatureSettings = "tnum"), color = c.onSurfaceVariant,
                        modifier = Modifier.width(46.dp).padding(end = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                    HorizontalDivider(color = c.outlineVariant)
                }
            }
            BoxWithConstraints(Modifier.padding(start = 56.dp).fillMaxWidth().height(HourHeight * 24)) {
                val nowTop = HourHeight * (now.toSecondOfDay() / 3600f)
                if (isToday) Box(Modifier.offset(y = nowTop - 1.dp).fillMaxWidth().height(2.dp).background(c.primary))
                val w = maxWidth
                lanes(blocks).forEach { p ->
                    val b = p.block
                    val top = HourHeight * (b.start / 60f) + 1.dp
                    val h = HourHeight * (maxOf(b.end - b.start, MinBlock) / 60f) - 2.dp
                    val short = h < 44.dp
                    val laneW = w / p.lanes
                    Column(
                        Modifier.offset(x = laneW * p.lane, y = top).width(laneW - 2.dp).height(h)
                            .clip(RoundedCornerShape(12.dp)).background(b.bg).clickable { onOpen(b) }
                            .alpha(if (b.done) 0.5f else 1f)
                            .semantics { contentDescription = "${b.title}, ${b.sub}" + if (b.done) ", done" else "" }
                            .padding(horizontal = 10.dp, vertical = if (short) 0.dp else 6.dp),
                        verticalArrangement = if (short) Arrangement.Center else Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            b.title + if (short) "  " + b.sub else "", maxLines = 1, overflow = TextOverflow.Ellipsis, color = b.fg,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            textDecoration = if (b.done) TextDecoration.LineThrough else null,
                        )
                        if (!short) Text(b.sub, style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"), color = b.fg.copy(alpha = 0.85f), maxLines = 1)
                    }
                }
                if (isToday) Box(Modifier.offset(x = (-6).dp, y = nowTop - 6.dp).size(12.dp).background(k.item, CircleShape).padding(2.dp).background(c.primary, CircleShape))
            }
        }
    }
}

private val HourFmt = java.time.format.DateTimeFormatter.ofPattern("h a")
