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
import kotlinx.coroutines.flow.callbackFlow
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

    /**
     * Emits whenever anything in the phone's calendar changes (a Google Calendar sync landing,
     * an event edited in another app). Push-based: no polling, no battery cost while idle.
     */
    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }

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
