package app.kairo

import android.app.Application
import android.content.pm.ApplicationInfo
import app.kairo.data.TaskRepository
import app.kairo.data.db.KairoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** App-wide objects, created once. (Manual wiring: small app, no DI framework needed.) */
class KairoApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob())
    val database by lazy { KairoDatabase.create(this) }
    val repository by lazy { TaskRepository(database) }

    override fun onCreate() {
        super.onCreate()
        val debug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        appScope.launch { repository.seedIfEmpty(sampleTasks = debug) }
    }
}
