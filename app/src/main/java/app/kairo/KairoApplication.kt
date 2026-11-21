package app.kairo

import android.app.Application
import android.content.pm.ApplicationInfo
import app.kairo.data.TaskRepository
import app.kairo.data.db.KairoDatabase
import app.kairo.data.calendar.PhoneCalendar
import app.kairo.data.settings.SettingsStore
import app.kairo.data.money.MoneyRepository
import app.kairo.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import app.kairo.widgets.updateAllWidgets

/** App-wide objects, created once. (Manual wiring: small app, no DI framework needed.) */
class KairoApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob())
    val database by lazy { KairoDatabase.create(this) }
    val repository by lazy { TaskRepository(database, appScope) }
    val reminders by lazy { ReminderScheduler(this) }
    val phoneCalendar by lazy { PhoneCalendar(this) }
    val settings by lazy { SettingsStore(this) }
    val money by lazy { MoneyRepository(database, appScope) }
    val habits by lazy { app.kairo.habits.HabitRepository(this, appScope) }
    val focus by lazy { app.kairo.focus.FocusController(this) }
    val updater by lazy { app.kairo.update.Updater(this) }

    override fun onCreate() {
        super.onCreate()
        val debug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        ReminderScheduler.createChannel(this)
        app.kairo.focus.FocusController.createChannels(this)
        app.kairo.habits.HabitReminders.createChannel(this)
        // Habit reminders follow the habits (new time, removed, checked today).
        appScope.launch { habits.habits.collect { app.kairo.habits.HabitReminders(this@KairoApplication).sync(it) } }
        appScope.launch {
            // One-time cleanup of the old default lists/accounts (only ones never used).
            val prefs = getSharedPreferences("seed", MODE_PRIVATE)
            val clean = !debug && !prefs.getBoolean("defaultsCleaned", false)
            repository.seedIfEmpty(sampleTasks = debug, cleanDefaults = clean)
            money.seedIfEmpty(sample = debug, cleanDefaults = clean)
            if (clean) prefs.edit().putBoolean("defaultsCleaned", true).apply()
            // Keep alarms in step with the database: any change to a task re-syncs its reminder.
            repository.tasks.collect { reminders.sync(it) }
        }
        // Widgets follow the data: refresh shortly after tasks or money change.
        appScope.launch {
            // Content changes, or settings (palette, theme) so the widgets match the app.
            // Only the widgets whose data changed are redrawn.
            var last: List<Any?>? = null
            combine(repository.tasks, money.transactions, money.budgets, settings.settings) { a, b, c, s -> listOf(a, b, c, s) }.collectLatest { now ->
                delay(500)
                val prev = last
                updateAllWidgets(
                    this@KairoApplication,
                    tasks = prev == null || prev[0] != now[0],
                    money = prev == null || prev[1] != now[1] || prev[2] != now[2],
                    theme = prev == null || prev[3] != now[3],
                )
                last = now
            }
        }
        appScope.launch { runCatching { app.kairo.widgets.publishWidgetPreviews(this@KairoApplication) } }
    }
}
