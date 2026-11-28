package app.kairo.habits

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import app.kairo.KairoApplication
import app.kairo.MainActivity
import app.kairo.R
import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import app.kairo.data.share
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * One daily alarm per habit with a reminder time (request code 8,000,000 + id). It's set for the
 * next time that hasn't passed; when it fires it shows the notification and sets tomorrow's.
 * A habit already checked today is skipped for today.
 */
class HabitReminders(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun sync(habits: List<Habit>) {
        habits.forEach { h -> if (h.reminderTime == null) cancel(h.id) else schedule(h) }
    }

    fun schedule(h: Habit) {
        val t = h.reminderTime ?: return
        val now = LocalDateTime.now()
        var at = LocalDateTime.of(LocalDate.now(), t)
        if (!at.isAfter(now) || h.doneOn(LocalDate.now())) at = at.plusDays(1)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = PendingIntent.getBroadcast(
            context, CODE + h.id.toInt(),
            Intent(context, HabitReminderReceiver::class.java).putExtra(EXTRA, h.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (alarms.canScheduleExactAlarms()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    fun cancel(id: Long) {
        PendingIntent.getBroadcast(
            context, CODE + id.toInt(), Intent(context, HabitReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let { alarms.cancel(it); it.cancel() }
    }

    companion object {
        const val CHANNEL = "habits"
        const val EXTRA = "habitId"
        const val ACTION_DONE = "app.kairo.HABIT_DONE"
        const val CODE = 8_000_000

        fun createChannel(context: Context) {
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(CHANNEL, "Habit reminders", NotificationManager.IMPORTANCE_HIGH))
        }
    }
}

/** Alarm fired: notify (unless already checked today) and set tomorrow's. */
class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(HabitReminders.EXTRA, -1)
        val app = context.applicationContext as KairoApplication
        val pending = goAsync()
        app.appScope.launch {
            try {
                val h = app.habits.habits.first().firstOrNull { it.id == id } ?: return@launch
                if (!h.doneOn(LocalDate.now())) notify(context, h, app.settings.settings.first().appLock)
                HabitReminders(context).schedule(h.copy(logs = h.logs + (LocalDate.now() to DayLog(1, 0.0)))) // → tomorrow
            } finally { pending.finish() }
        }
    }

    private fun notify(context: Context, h: Habit, private: Boolean) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val done = PendingIntent.getBroadcast(
            context, HabitReminders.CODE + 500_000 + h.id.toInt(),
            Intent(context, HabitActionReceiver::class.java).setAction(HabitReminders.ACTION_DONE).putExtra(HabitReminders.EXTRA, h.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val open = PendingIntent.getActivity(
            context, HabitReminders.CODE + h.id.toInt(), Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val streak = h.streak()
        val n = NotificationCompat.Builder(context, HabitReminders.CHANNEL)
            .setSmallIcon(R.drawable.ic_task_alt)
            .setContentTitle(h.name)
            .setContentText(if (streak > 0) "Keep your $streak-day streak going" else "Daily habit")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open).setAutoCancel(true)
            .addAction(0, "Done", done)
            .apply {
                if (private) setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(
                    NotificationCompat.Builder(context, HabitReminders.CHANNEL).setSmallIcon(R.drawable.ic_task_alt)
                        .setContentTitle("Habit reminder").setContentText("Unlock to see it").build(),
                )
            }
            .build()
        NotificationManagerCompat.from(context).notify(HabitReminders.CODE + h.id.toInt(), n)
    }
}

/** The notification's Done button checks today. */
class HabitActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(HabitReminders.EXTRA, -1)
        val app = context.applicationContext as KairoApplication
        NotificationManagerCompat.from(context).cancel(HabitReminders.CODE + id.toInt())
        val pending = goAsync()
        app.appScope.launch { try { app.habits.set(id, LocalDate.now(), true) } finally { pending.finish() } }
    }
}
