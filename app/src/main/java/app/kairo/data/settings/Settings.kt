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
)

private val Context.store by preferencesDataStore("settings")

/** Small key-value settings on disk (DataStore), exposed as a Flow like the database. */
class SettingsStore(private val context: Context) {
    private val theme = stringPreferencesKey("theme")
    private val wallpaper = booleanPreferencesKey("wallpaper")
    private val seed = stringPreferencesKey("seed")
    private val contrast = stringPreferencesKey("contrast")

    val settings: Flow<AppSettings> = context.store.data.map { p: Preferences ->
        AppSettings(
            theme = p[theme]?.let(ThemeMode::valueOf) ?: ThemeMode.Dark,
            wallpaperColor = p[wallpaper] ?: false,
            seed = p[seed]?.let(Seed::valueOf) ?: Seed.Default,
            contrast = p[contrast]?.let(Contrast::valueOf) ?: Contrast.Standard,
        )
    }

    suspend fun setTheme(v: ThemeMode) = context.store.edit { it[theme] = v.name }
    suspend fun setWallpaper(v: Boolean) = context.store.edit { it[wallpaper] = v }
    /** Picking a seed turns wallpaper color off (canvas). */
    suspend fun setSeed(v: Seed) = context.store.edit { it[seed] = v.name; it[wallpaper] = false }
    suspend fun setContrast(v: Contrast) = context.store.edit { it[contrast] = v.name }
}
