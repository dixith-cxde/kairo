package app.kairo.data.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Instances
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** A phone-calendar event occurrence (read-only in Kairo). */
data class CalEvent(
    val eventId: Long,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean,
    val color: Int,
    val calendar: String,
)

/**
 * Reads events through the Android Calendar Provider: no sign-in, only the READ_CALENDAR permission.
 * Google Calendar events appear because the phone already syncs them (docs/06-data-and-calendar.md).
 */
class PhoneCalendar(private val context: Context) {

    fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Instances (not Events) so repeating events expand into each occurrence. */
    suspend fun events(from: LocalDate, toExclusive: LocalDate): List<CalEvent> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val zone = ZoneId.systemDefault()
        val uri = Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, from.atStartOfDay(zone).toInstant().toEpochMilli())
            ContentUris.appendId(it, toExclusive.atStartOfDay(zone).toInstant().toEpochMilli())
        }.build()
        val cols = arrayOf(
            Instances.EVENT_ID, Instances.TITLE, Instances.BEGIN, Instances.END,
            Instances.ALL_DAY, Instances.DISPLAY_COLOR, Instances.CALENDAR_DISPLAY_NAME,
        )
        val out = mutableListOf<CalEvent>()
        context.contentResolver.query(uri, cols, "${Instances.VISIBLE} = 1", null, "${Instances.BEGIN} ASC")?.use { c ->
            while (c.moveToNext()) {
                val allDay = c.getInt(4) == 1
                // All-day events are stored in UTC midnight; read them in UTC so they land on the right date.
                val z = if (allDay) ZoneOffset.UTC else zone
                out += CalEvent(
                    eventId = c.getLong(0),
                    title = c.getString(1) ?: "(No title)",
                    start = LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getLong(2)), z),
                    end = LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getLong(3)), z),
                    allDay = allDay,
                    color = c.getInt(5),
                    calendar = c.getString(6) ?: "",
                )
            }
        }
        out
    }
}
