package app.kairo.navigation

/** Everything that can be on the back stack: a tab, or a screen opened from one. */
sealed interface Route {
    data class TabRoute(val tab: Tab) : Route
    data class TaskRoute(val taskId: Long) : Route
    data class ListRoute(val listId: Long) : Route
    data object SettingsRoute : Route

    /** Saved as short strings ("tab:Today", "task:12") so the stack survives process death. */
    fun encode(): String = when (this) {
        is TabRoute -> "tab:${tab.name}"
        is TaskRoute -> "task:$taskId"
        is ListRoute -> "list:$listId"
        SettingsRoute -> "settings:"
    }

    companion object {
        fun decode(s: String): Route {
            val (kind, v) = s.split(":", limit = 2)
            return when (kind) {
                "task" -> TaskRoute(v.toLong())
                "list" -> ListRoute(v.toLong())
                "settings" -> SettingsRoute
                else -> TabRoute(Tab.valueOf(v))
            }
        }
    }
}
