package app.kairo.data.calendar

import android.Manifest
import android.accounts.Account
import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.callbackFlow
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Instances
import android.provider.CalendarContract.Calendars as C
import android.provider.CalendarContract.Events as E
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

/** A calendar on the phone Kairo may add events to (Google account calendars, local ones). */
data class PhoneCal(val id: Long, val name: String, val account: String, val color: Int, val primary: Boolean)

/**
 * Reads events through the Android Calendar Provider: no sign-in, only the READ_CALENDAR permission.
 * Google Calendar events appear because the phone already syncs them (docs/06-data-and-calendar.md).
 */
class PhoneCalendar(private val context: Context) {

    /**
     * Emits whenever anything in the phone's calendar changes (a Google Calendar sync landing,
     * an event edited in another app). Push-based: no polling, no battery cost while idle.
     */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.debounce(300) // a Google sync fires dozens of callbacks; reload once it settles

    /** Ask every synced calendar account to sync now (what "pull to refresh" does). */
    suspend fun requestSync() = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext
        val accounts = mutableSetOf<Account>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.ACCOUNT_TYPE),
            null, null, null,
        )?.use { c -> while (c.moveToNext()) accounts += Account(c.getString(0), c.getString(1)) }
        val extras = Bundle().apply {
            putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
            putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
        }
        accounts.filter { it.type != CalendarContract.ACCOUNT_TYPE_LOCAL }.forEach { a ->
            runCatching { ContentResolver.requestSync(a, CalendarContract.AUTHORITY, extras) }
        }
    }

    fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun canWrite() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Calendars you can add events to (contributor access or better), primary ones first. */
    suspend fun writableCalendars(): List<PhoneCal> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val out = mutableListOf<PhoneCal>()
        context.contentResolver.query(
            C.CONTENT_URI,
            arrayOf(C._ID, C.CALENDAR_DISPLAY_NAME, C.ACCOUNT_NAME, C.CALENDAR_COLOR, C.IS_PRIMARY),
            "${C.CALENDAR_ACCESS_LEVEL} >= ${C.CAL_ACCESS_CONTRIBUTOR} AND ${C.VISIBLE} = 1", null, null,
        )?.use { c ->
            while (c.moveToNext()) out += PhoneCal(c.getLong(0), c.getString(1) ?: "Calendar", c.getString(2) ?: "", c.getInt(3), c.getInt(4) == 1)
        }
        out.sortedByDescending { it.primary }
    }

    /**
     * Adds one event through the Calendar Provider; Google Calendar uploads it on its next sync.
     * All-day events are stored at UTC midnight with an exclusive end, as the provider expects.
     */
    suspend fun addEvent(calendarId: Long, title: String, start: LocalDateTime, end: LocalDateTime, allDay: Boolean): Long? =
        withContext(Dispatchers.IO) {
            if (!canWrite()) return@withContext null
            val zone = if (allDay) ZoneOffset.UTC else ZoneId.systemDefault()
            val s = if (allDay) start.toLocalDate().atStartOfDay() else start
            val e = if (allDay) end.toLocalDate().plusDays(1).atStartOfDay() else end
            val values = android.content.ContentValues().apply {
                put(E.CALENDAR_ID, calendarId)
                put(E.TITLE, title)
                put(E.DTSTART, s.atZone(zone).toInstant().toEpochMilli())
                put(E.DTEND, e.atZone(zone).toInstant().toEpochMilli())
                put(E.ALL_DAY, if (allDay) 1 else 0)
                put(E.EVENT_TIMEZONE, if (allDay) "UTC" else ZoneId.systemDefault().id)
            }
            val id = context.contentResolver.insert(E.CONTENT_URI, values)?.let { ContentUris.parseId(it) }
            // Reminder on by default (owner, 2026-11-21): a notification 10 minutes before
            // (all-day: at the start of the day).
            if (id != null) runCatching {
                context.contentResolver.insert(android.provider.CalendarContract.Reminders.CONTENT_URI, android.content.ContentValues().apply {
                    put(android.provider.CalendarContract.Reminders.EVENT_ID, id)
                    put(android.provider.CalendarContract.Reminders.MINUTES, if (allDay) 0 else 10)
                    put(android.provider.CalendarContract.Reminders.METHOD, android.provider.CalendarContract.Reminders.METHOD_ALERT)
                })
            }
            id
        }

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
