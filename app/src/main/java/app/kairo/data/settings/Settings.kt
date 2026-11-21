package app.kairo.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { System, Light, Dark }
enum class Contrast { Standard, Medium, High }
/** Default = Stash's #79D0FD; the six user seeds from the canvas Settings board. */
enum class Seed { Default, Indigo, Clay, Moss, Rose, Teal, Violet }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.Dark,   // rule 1: dark is the default
    val wallpaperColor: Boolean = false,
    val seed: Seed = Seed.Default,
    val contrast: Contrast = Contrast.Standard,
    val appLock: Boolean = false,
    val lockAfterSec: Int = 0,               // 0 = every time, 60, 300 (same choices as Stash)
    val blockScreenshots: Boolean = false,   // FLAG_SECURE: no screenshots, recordings or recents preview
    val onboarded: Boolean = false,          // welcome pages seen
    val tourDone: Boolean = false,           // spotlight tour seen
)

private val Context.store by preferencesDataStore("settings")

/** Small key-value settings on disk (DataStore), exposed as a Flow like the database. */
class SettingsStore(private val context: Context) {
    private val theme = stringPreferencesKey("theme")
    private val wallpaper = booleanPreferencesKey("wallpaper")
    private val seed = stringPreferencesKey("seed")
    private val contrast = stringPreferencesKey("contrast")
    private val appLock = booleanPreferencesKey("appLock")
    private val lockAfter = androidx.datastore.preferences.core.intPreferencesKey("lockAfter")
    private val secure = booleanPreferencesKey("blockScreenshots")
    private val onboardedKey = booleanPreferencesKey("onboarded")
    private val tourKey = booleanPreferencesKey("tourDone")

    val settings: Flow<AppSettings> = context.store.data.map { p: Preferences ->
        AppSettings(
            theme = p[theme]?.let(ThemeMode::valueOf) ?: ThemeMode.Dark,
            wallpaperColor = p[wallpaper] ?: false,
            seed = p[seed]?.let(Seed::valueOf) ?: Seed.Default,
            contrast = p[contrast]?.let(Contrast::valueOf) ?: Contrast.Standard,
            appLock = p[appLock] ?: false,
            lockAfterSec = p[lockAfter] ?: 0,
            blockScreenshots = p[secure] ?: false,
            onboarded = p[onboardedKey] ?: false,
            tourDone = p[tourKey] ?: false,
        )
    }

    suspend fun setTheme(v: ThemeMode) = context.store.edit { it[theme] = v.name }
    suspend fun setWallpaper(v: Boolean) = context.store.edit { it[wallpaper] = v }
    /** Picking a seed turns wallpaper color off (canvas). */
    suspend fun setSeed(v: Seed) = context.store.edit { it[seed] = v.name; it[wallpaper] = false }
    suspend fun setContrast(v: Contrast) = context.store.edit { it[contrast] = v.name }
    suspend fun setAppLock(v: Boolean) = context.store.edit { it[appLock] = v }
    suspend fun setLockAfter(sec: Int) = context.store.edit { it[lockAfter] = sec }
    suspend fun setBlockScreenshots(v: Boolean) = context.store.edit { it[secure] = v }
    suspend fun setOnboarded(v: Boolean) = context.store.edit { it[onboardedKey] = v }
    suspend fun setTourDone(v: Boolean) = context.store.edit { it[tourKey] = v }
}
