package app.kairo.navigation

/** Everything that can be on the back stack: a tab, or a screen opened from one. */
sealed interface Route {
    data class TabRoute(val tab: Tab) : Route
    data class TaskRoute(val taskId: Long) : Route
    data class ListRoute(val listId: Long) : Route
    data object SettingsRoute : Route
    data class AccountRoute(val accountId: Long) : Route
    data object BudgetsRoute : Route
    data object InsightsRoute : Route
    data object RecurringRoute : Route
    data object FocusRoute : Route
    data object SearchRoute : Route
    data object ListsRoute : Route
    data object StatsRoute : Route
    data class HabitRoute(val habitId: Long) : Route
    /** One occurrence of a phone-calendar event (start/end as ISO local date-times). */
    data class TxRoute(val txId: Long) : Route
    data object ReportRoute : Route
    data class EventRoute(val eventId: Long, val start: String, val end: String, val allDay: Boolean) : Route

    /** Saved as short strings ("tab:Today", "task:12") so the stack survives process death. */
    fun encode(): String = when (this) {
        is TabRoute -> "tab:${tab.name}"
        is TaskRoute -> "task:$taskId"
        is ListRoute -> "list:$listId"
        SettingsRoute -> "settings:"
        is AccountRoute -> "account:$accountId"
        BudgetsRoute -> "budgets:"
        InsightsRoute -> "insights:"
        RecurringRoute -> "recurring:"
        FocusRoute -> "focus:"
        SearchRoute -> "search:"
        ListsRoute -> "lists:"
        StatsRoute -> "stats:"
        is HabitRoute -> "habit:$habitId"
        is TxRoute -> "tx:$txId"
        ReportRoute -> "report:"
        is EventRoute -> "event:$eventId|$start|$end|$allDay"
    }

    companion object {
        fun decode(s: String): Route {
            val (kind, v) = s.split(":", limit = 2)
            return when (kind) {
                "task" -> TaskRoute(v.toLong())
                "list" -> ListRoute(v.toLong())
                "settings" -> SettingsRoute
                "account" -> AccountRoute(v.toLong())
                "budgets" -> BudgetsRoute
                "insights" -> InsightsRoute
                "recurring" -> RecurringRoute
                "focus" -> FocusRoute
                "search" -> SearchRoute
                "lists" -> ListsRoute
                "stats" -> StatsRoute
                "habit" -> HabitRoute(v.toLong())
                "tx" -> TxRoute(v.toLong())
                "report" -> ReportRoute
                "event" -> v.split("|").let { p -> EventRoute(p[0].toLong(), p[1], p[2], p[3].toBoolean()) }
                else -> TabRoute(Tab.fromSaved(v))
            }
        }
    }
}
