@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.calendar

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kairo.R
import app.kairo.data.calendar.EventDetails
import app.kairo.data.calendar.PhoneCalendar
import app.kairo.ui.theme.KairoTheme
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Canvas "event page" board, for a phone-calendar event (read-only in Kairo, so no Edit / Duplicate
 * / Delete toolbar): badge, title, calendar chip; When, Repeat, Reminder; location with Directions,
 * a meeting link with Join; the description; and Open in Google Calendar.
 */
@Composable
fun EventPage(calendar: PhoneCalendar, eventId: Long, start: LocalDateTime, end: LocalDateTime, allDay: Boolean, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val d by produceState<EventDetails?>(null, eventId) { value = calendar.details(eventId) }
    val e = d
    fun launch(i: Intent) = try { ctx.startActivity(i) } catch (_: ActivityNotFoundException) {}
    val link = e?.let { meetingLink(it.description + " " + it.location) }
    Box(Modifier.fillMaxSize().background(k.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 168.dp)) {
            IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes(), modifier = Modifier.padding(top = 8.dp)) {
                Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = c.onSurface)
            }
            if (e == null) return@Column
            Column(Modifier.padding(start = 4.dp, top = 8.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(56.dp).background(c.tertiary, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_event_filled), null, Modifier.size(28.dp), tint = c.onTertiary)
                }
                Text(e.title, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Medium), color = c.onSurface)
                if (e.calendar.isNotEmpty()) Row(Modifier.height(32.dp).background(k.chip, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(Color(e.color), CircleShape))
                    Text(e.calendar, style = MaterialTheme.typography.labelLarge, color = c.onSurface, modifier = Modifier.padding(start = 8.dp))
                }
            }
            val rows = listOfNotNull(
                Triple(R.drawable.ic_schedule, dayText(start, end, allDay), timeText(start, end, allDay)),
                repeatText(e.rrule)?.let { Triple(R.drawable.ic_repeat, it, null) },
                Triple(R.drawable.ic_notifications, reminderText(e.reminders, allDay), null),
            )
            rows.forEachIndexed { i, (icon, t, sub) -> InfoRow(icon, t, sub, i, rows.size) }
            if (e.location.isNotBlank()) { Spacer(Modifier.height(12.dp))
                InfoRow(R.drawable.ic_location_on, e.location, null, 0, 1, action = "Directions") {
                    launch(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(e.location))))
                }
            }
            if (link != null) { Spacer(Modifier.height(12.dp))
                InfoRow(R.drawable.ic_video_call, linkTag(link), link.removePrefix("https://").removePrefix("http://"), 0, 1, action = "Join") {
                    launch(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                }
            }
            if (e.description.isNotBlank()) Text(e.description.trim(), style = MaterialTheme.typography.bodyLarge, color = c.onSurface,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp))
        }
        ExtendedFloatingActionButton(
            onClick = {
                val z = if (allDay) ZoneOffset.UTC else ZoneId.systemDefault()
                launch(Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId))
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.atZone(z).toInstant().toEpochMilli())
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.atZone(z).toInstant().toEpochMilli()))
            },
            shape = RoundedCornerShape(20.dp), containerColor = c.primary, contentColor = c.onPrimary,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp).height(64.dp),
            icon = { Icon(painterResource(R.drawable.ic_open_in_new), null) },
            text = { Text("Open in Google Calendar", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)) },
        )
    }
}

/** Canvas .crw.epr: icon, text (and a sub line), and an optional tonal action (Directions, Join). */
@Composable
private fun InfoRow(icon: Int, title: String, sub: String?, i: Int, n: Int, action: String? = null, onAction: () -> Unit = {}) {
    val c = MaterialTheme.colorScheme
    val big = 28.dp; val small = 8.dp
    val shape = RoundedCornerShape(
        topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
        bottomStart = if (i == n - 1) big else small, bottomEnd = if (i == n - 1) big else small,
    )
    Row(
        Modifier.fillMaxWidth().padding(bottom = 3.dp).background(KairoTheme.colors.item, shape).heightIn(min = 64.dp).padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Icon(painterResource(icon), null, tint = c.onSurfaceVariant) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.onSurface)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), color = c.onSurfaceVariant)
        }
        if (action != null) FilledTonalButton(onClick = onAction, shapes = ButtonDefaults.shapes(), contentPadding = PaddingValues(horizontal = 16.dp)) { Text(action) }
    }
}

private fun dayText(s: LocalDateTime, e: LocalDateTime, allDay: Boolean): String {
    val last = if (allDay) e.toLocalDate().minusDays(1) else e.toLocalDate()
    return s.toLocalDate().format(ShortDay) + if (last != s.toLocalDate()) " – " + last.format(ShortDay) else ""
}

private fun timeText(s: LocalDateTime, e: LocalDateTime, allDay: Boolean) = if (allDay) "All day" else s.format(Clock) + " – " + e.format(Clock)

/** "Repeats weekly", "Repeats every 2 weeks", from the event's RRULE. */
internal fun repeatText(rrule: String?): String? {
    if (rrule.isNullOrBlank()) return null
    val p = rrule.split(';').mapNotNull { it.split('=').takeIf { kv -> kv.size == 2 }?.let { (a, b) -> a to b } }.toMap()
    val n = p["INTERVAL"]?.toIntOrNull() ?: 1
    val unit = when (p["FREQ"]) { "DAILY" -> "day"; "WEEKLY" -> "week"; "MONTHLY" -> "month"; "YEARLY" -> "year"; else -> return "Repeats" }
    return if (n == 1) "Repeats " + mapOf("day" to "daily", "week" to "weekly", "month" to "monthly", "year" to "yearly")[unit] else "Repeats every $n ${unit}s"
}

internal fun reminderText(mins: List<Int>, allDay: Boolean = false): String = if (mins.isEmpty()) "No reminder" else mins.joinToString(", ") { m ->
    // All-day reminders count from midnight: -540 = on the day at 9:00 AM; 360 = the day before at 6:00 PM.
    if (allDay) {
        val days = if (m <= 0) 0 else (m + 1439) / 1440
        val at = java.time.LocalTime.ofSecondOfDay((((days * 1440 - m) % 1440 + 1440) % 1440) * 60L).format(Clock)
        return@joinToString when (days) { 0 -> "On the day at $at"; 1 -> "1 day before at $at"; else -> "$days days before at $at" }
    }
    when {
        m <= 0 -> "At the start"; m % 1440 == 0 -> "${m / 1440} day" + (if (m / 1440 > 1) "s" else "") + " before"; m % 60 == 0 -> "${m / 60} hour" + (if (m / 60 > 1) "s" else "") + " before"; else -> "$m min before" }
}

/** A meeting link in the description or location (Meet, Zoom, Teams, Webex), else none. */
internal fun meetingLink(text: String): String? =
    Regex("""https?://[^\s<>"]*(meet\.google\.com|zoom\.us|teams\.microsoft\.com|teams\.live\.com|webex\.com)[^\s<>"]*""").find(text)?.value

private fun linkTag(url: String) = when {
    "meet.google" in url -> "Google Meet"; "zoom.us" in url -> "Zoom"; "teams." in url -> "Microsoft Teams"; "webex" in url -> "Webex"; else -> "Meeting link"
}
