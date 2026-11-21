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
    val repository by lazy { TaskRepository(database) }
    val reminders by lazy { ReminderScheduler(this) }
    val phoneCalendar by lazy { PhoneCalendar(this) }
    val settings by lazy { SettingsStore(this) }
    val money by lazy { MoneyRepository(database) }

    override fun onCreate() {
        super.onCreate()
        val debug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        ReminderScheduler.createChannel(this)
        appScope.launch {
            repository.seedIfEmpty(sampleTasks = debug)
            money.seedIfEmpty(sample = debug)
            // Keep alarms in step with the database: any change to a task re-syncs its reminder.
            repository.tasks.collect { reminders.sync(it) }
        }
        // Widgets follow the data: refresh shortly after tasks or money change.
        appScope.launch {
            combine(repository.tasks, money.transactions) { a, b -> a.size + b.size }.collectLatest {
                delay(500)
                updateAllWidgets(this@KairoApplication)
            }
        }
    }
}
