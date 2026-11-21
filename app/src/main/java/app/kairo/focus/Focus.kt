package app.kairo.focus

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import app.kairo.KairoApplication
import app.kairo.MainActivity
import app.kairo.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A finished focus session (for "Focused today" and later stats). Room v4. */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val startedAt: Long,
    val minutes: Int,
)

@Dao
interface FocusDao {
    @Insert suspend fun insert(s: FocusSessionEntity)
    @Query("SELECT COALESCE(SUM(minutes), 0) FROM focus_sessions WHERE startedAt >= :since")
    fun minutesSince(since: Long): Flow<Int>
}

enum class Phase { Idle, Focus, Break }

data class FocusState(
    val phase: Phase = Phase.Idle,
    val running: Boolean = false,
    val endAt: Long = 0,          // when running: wall-clock end
    val remainingMs: Long = 0,    // when paused: time left
    val session: Int = 1,         // 1-based, of `sessions`
    val sessions: Int = 4,
    val focusMin: Int = 25,
    val breakMin: Int = 5,
    val taskId: Long? = null,
) {
    val phaseMs get() = (if (phase == Phase.Break) breakMin else focusMin) * 60_000L
    fun leftMs(now: Long = System.currentTimeMillis()) = when {
        phase == Phase.Idle -> focusMin * 60_000L
        running -> (endAt - now).coerceAtLeast(0)
        else -> remainingMs
    }
}

/**
 * Pomodoro-style focus timer. The state is just "when does this phase end", saved to
 * SharedPreferences, so it survives the app being closed. An exact alarm ends each phase, and the
 * notification counts down by itself (Chronometer) — no foreground service, no polling.
 */
class FocusController(private val app: KairoApplication) {
    private val prefs = app.getSharedPreferences("focus", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<FocusState> = _state.asStateFlow()
    private var phaseStartedAt = prefs.getLong("phaseStartedAt", 0)

    fun start(taskId: Long? = _state.value.taskId) {
        val s = _state.value
        val phase = if (s.phase == Phase.Idle) Phase.Focus else s.phase
        val ms = if (s.phase == Phase.Idle || s.remainingMs == 0L) (if (phase == Phase.Break) s.breakMin else s.focusMin) * 60_000L else s.remainingMs
        phaseStartedAt = System.currentTimeMillis()
        set(s.copy(phase = phase, running = true, endAt = System.currentTimeMillis() + ms, remainingMs = 0, taskId = taskId))
    }

    fun pause() {
        val s = _state.value
        if (!s.running) return
        set(s.copy(running = false, remainingMs = s.leftMs()))
    }

    fun stop() = set(FocusState(sessions = _state.value.sessions, focusMin = _state.value.focusMin, breakMin = _state.value.breakMin, taskId = _state.value.taskId))

    fun addFive() {
        val s = _state.value
        if (s.running) set(s.copy(endAt = s.endAt + 5 * 60_000)) else if (s.phase != Phase.Idle) set(s.copy(remainingMs = s.remainingMs + 5 * 60_000))
    }

    fun setTask(id: Long?) = set(_state.value.copy(taskId = id))
    /**
     * New timing. While paused, the current phase restarts with the new length (that's what
     * "change the timer" means when you tap the clock); while running it applies from the next phase.
     */
    fun configure(focusMin: Int, breakMin: Int, sessions: Int) {
        val s = _state.value.copy(focusMin = focusMin, breakMin = breakMin, sessions = sessions.coerceAtLeast(_state.value.session))
        set(if (!s.running && s.phase != Phase.Idle) s.copy(remainingMs = s.phaseMs) else s)
    }

    /** Phase time is up (alarm fired, or the screen noticed). Focus → break → next focus. */
    fun finishPhase() {
        val s = _state.value
        if (s.phase == Phase.Idle) return
        if (s.running && s.endAt > System.currentTimeMillis() + 1000) return // not actually done
        if (s.phase == Phase.Focus) {
            app.appScope.launch {
                app.database.focus().insert(FocusSessionEntity(taskId = s.taskId, startedAt = phaseStartedAt.takeIf { it > 0 } ?: (s.endAt - s.phaseMs), minutes = (s.phaseMs / 60_000).toInt()))
            }
            if (s.session >= s.sessions) { notifyDone("All ${s.sessions} sessions done", "Nice work."); stop(); return }
            notifyDone("Focus session ${s.session} done", "Time for a ${s.breakMin}-minute break")
            phaseStartedAt = System.currentTimeMillis()
            set(s.copy(phase = Phase.Break, running = true, endAt = System.currentTimeMillis() + s.breakMin * 60_000L, remainingMs = 0))
        } else {
            notifyDone("Break's over", "Ready for session ${s.session + 1}?")
            set(s.copy(phase = Phase.Focus, running = false, remainingMs = s.focusMin * 60_000L, session = s.session + 1))
        }
    }

    // ── persistence, alarm, notifications ──

    private fun set(s: FocusState) {
        _state.value = s
        prefs.edit()
            .putString("phase", s.phase.name).putBoolean("running", s.running).putLong("endAt", s.endAt)
            .putLong("remainingMs", s.remainingMs).putInt("session", s.session).putInt("sessions", s.sessions)
            .putInt("focusMin", s.focusMin).putInt("breakMin", s.breakMin).putLong("taskId", s.taskId ?: -1)
            .putLong("phaseStartedAt", phaseStartedAt)
            .apply()
        val alarms = app.getSystemService(AlarmManager::class.java)
        if (s.running) {
            if (alarms.canScheduleExactAlarms()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.endAt, alarmIntent())
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.endAt, alarmIntent())
            notifyRunning(s)
        } else {
            alarms.cancel(alarmIntent())
            NotificationManagerCompat.from(app).cancel(RUNNING_ID)
        }
    }

    private fun load() = FocusState(
        phase = Phase.valueOf(prefs.getString("phase", Phase.Idle.name)!!),
        running = prefs.getBoolean("running", false),
        endAt = prefs.getLong("endAt", 0), remainingMs = prefs.getLong("remainingMs", 0),
        session = prefs.getInt("session", 1), sessions = prefs.getInt("sessions", 4),
        focusMin = prefs.getInt("focusMin", 25), breakMin = prefs.getInt("breakMin", 5),
        taskId = prefs.getLong("taskId", -1).takeIf { it >= 0 },
    )

    private fun alarmIntent() = PendingIntent.getBroadcast(
        app, 7_000_001, Intent(app, FocusReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun openApp() = PendingIntent.getActivity(
        app, 7_000_002, Intent(app, MainActivity::class.java).putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_FOCUS),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun canNotify() = NotificationManagerCompat.from(app).areNotificationsEnabled()

    /** Ongoing notification whose clock counts down on its own. */
    private fun notifyRunning(s: FocusState) {
        if (!canNotify()) return
        val n = NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(if (s.phase == Phase.Break) "Break" else "Focus · session ${s.session} of ${s.sessions}")
            .setUsesChronometer(true).setChronometerCountDown(true).setWhen(s.endAt).setShowWhen(true)
            .setOngoing(true).setSilent(true).setContentIntent(openApp())
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .build()
        runCatching { NotificationManagerCompat.from(app).notify(RUNNING_ID, n) }
    }

    private fun notifyDone(title: String, text: String) {
        NotificationManagerCompat.from(app).cancel(RUNNING_ID)
        if (!canNotify()) return
        val n = NotificationCompat.Builder(app, DONE_CHANNEL)
            .setSmallIcon(R.drawable.ic_timer).setContentTitle(title).setContentText(text)
            .setAutoCancel(true).setContentIntent(openApp()).setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching { NotificationManagerCompat.from(app).notify(DONE_ID, n) }
    }

    companion object {
        const val CHANNEL = "focus"
        const val DONE_CHANNEL = "focus_done"
        private const val RUNNING_ID = 7_000_010
        private const val DONE_ID = 7_000_011

        fun createChannels(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Focus timer", NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel(DONE_CHANNEL, "Focus finished", NotificationManager.IMPORTANCE_HIGH))
        }
    }
}

/** The phase's exact alarm. */
class FocusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as KairoApplication).focus.finishPhase()
    }
}
