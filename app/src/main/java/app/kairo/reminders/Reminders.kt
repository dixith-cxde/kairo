package app.kairo.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.kairo.KairoApplication
import app.kairo.MainActivity
import app.kairo.R
import app.kairo.data.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Task reminders with AlarmManager. One alarm per task (request code = task id), so scheduling
 * again replaces the old alarm. The receiver re-checks the database, so a stale alarm for a task
 * that was done or deleted just does nothing.
 */
class ReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    /** When a task should ping: its time, or 9:00 on its date if it has no time. */
    fun triggerAt(t: Task): LocalDateTime? {
        if (!t.reminder || t.done || t.due == null) return null
        val at = LocalDateTime.of(t.due, t.time ?: LocalTime.of(9, 0))
        return at.takeIf { it.isAfter(LocalDateTime.now()) }
    }

    /** What each task's alarm was last set to (null = none), so a change only touches the tasks that moved. */
    private val synced = HashMap<Long, LocalDateTime?>()

    /**
     * Called on every change to the tasks table. Only tasks whose alarm time changed reach
     * AlarmManager; tasks that disappeared (deleted or archived) get their alarm cancelled.
     */
    @Synchronized
    fun sync(tasks: List<Task>, force: Boolean = false) {
        if (force) synced.clear() // boot / clock / timezone change: same local times, new instants
        val seen = HashSet<Long>(tasks.size)
        for (t in tasks) {
            seen += t.id
            val at = triggerAt(t)
            if (synced.containsKey(t.id) && synced[t.id] == at) continue
            if (at != null) schedule(t.id, at) else if (synced[t.id] != null || !synced.containsKey(t.id)) cancel(t.id)
            synced[t.id] = at
        }
        val gone = synced.keys.filter { it !in seen }
        gone.forEach { id -> if (synced[id] != null) cancel(id); synced.remove(id) }
    }

    fun schedule(taskId: Long, at: LocalDateTime) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = alarmIntent(taskId)
        // Exact when allowed (Android 12+ asks the user on 13+); otherwise Android may delay a few minutes.
        if (alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    /** FLAG_NO_CREATE: only look up an existing alarm; don't create a PendingIntent just to cancel it. */
    fun cancel(taskId: Long) {
        PendingIntent.getBroadcast(
            context, taskId.toInt(), Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let { alarms.cancel(it); it.cancel() }
    }

    private fun alarmIntent(taskId: Long): PendingIntent = PendingIntent.getBroadcast(
        context, taskId.toInt(),
        Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_TASK, taskId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL = "reminders"
        const val EXTRA_TASK = "taskId"
        const val ACTION_DONE = "app.kairo.DONE"
        const val ACTION_SNOOZE = "app.kairo.SNOOZE"

        fun createChannel(context: Context) {
            val ch = NotificationChannel(CHANNEL, "Task reminders", NotificationManager.IMPORTANCE_HIGH)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
}

/** Alarm fired → show the notification with Done and Snooze actions. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_TASK, -1)
        val app = context.applicationContext as KairoApplication
        val pending = goAsync()
        app.appScope.launch {
            try {
                val task = app.repository.task(id).first()
                if (task != null && !task.done) notify(context, task, private = app.settings.settings.first().appLock)
            } finally { pending.finish() }
        }
    }

    /** [private]: app lock is on, so the lock screen shows "Task reminder" instead of the task's name. */
    private fun notify(context: Context, task: Task, private: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        fun action(a: String) = PendingIntent.getBroadcast(
            context, (task.id * 10 + a.length).toInt(),
            Intent(context, ReminderActionReceiver::class.java).setAction(a).putExtra(ReminderScheduler.EXTRA_TASK, task.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val open = PendingIntent.getActivity(
            context, task.id.toInt(), Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL)
            .setSmallIcon(R.drawable.ic_task_alt)
            .setContentTitle(task.title)
            .setContentText(task.time?.let { "Due ${it}" } ?: "Due today")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Done", action(ReminderScheduler.ACTION_DONE))
            .addAction(0, "Snooze 10 min", action(ReminderScheduler.ACTION_SNOOZE))
            .apply {
                if (private) setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(
                    NotificationCompat.Builder(context, ReminderScheduler.CHANNEL)
                        .setSmallIcon(R.drawable.ic_task_alt).setContentTitle("Task reminder")
                        .setContentText("Unlock to see it").setCategory(NotificationCompat.CATEGORY_REMINDER).build(),
                )
            }
            .build()
        NotificationManagerCompat.from(context).notify(task.id.toInt(), n)
    }
}

/** Notification buttons. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_TASK, -1)
        val app = context.applicationContext as KairoApplication
        NotificationManagerCompat.from(context).cancel(id.toInt())
        val pending = goAsync()
        app.appScope.launch {
            try {
                when (intent.action) {
                    ReminderScheduler.ACTION_DONE -> app.repository.task(id).first()?.let { app.repository.setDone(it, true) }
                    ReminderScheduler.ACTION_SNOOZE -> app.reminders.schedule(id, LocalDateTime.now().plusMinutes(10))
                }
            } finally { pending.finish() }
        }
    }
}

/** Alarms are cleared on reboot (and on time-zone / app update); schedule them again. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as KairoApplication
        val pending = goAsync()
        app.appScope.launch {
            try { app.reminders.sync(app.repository.tasks.first(), force = true) } finally { pending.finish() }
        }
    }
}
