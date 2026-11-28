package app.kairo.navigation

import androidx.annotation.DrawableRes
import app.kairo.R

/** The five v2 destinations, in nav order (v2 canvas: Tasks · Habits · Calendar · Finance · Focus). */
enum class Tab(
    val label: String,
    @param:DrawableRes val icon: Int,         // Material Symbols Rounded, outlined
    @param:DrawableRes val selectedIcon: Int, // filled where we have the filled symbol
) {
    Tasks("Tasks", R.drawable.ic_task_alt, R.drawable.ic_task_alt),
    Habits("Habits", R.drawable.ic_local_fire_department, R.drawable.ic_local_fire_department_filled),
    Calendar("Calendar", R.drawable.ic_calendar_month, R.drawable.ic_calendar_month_filled),
    Finance("Finance", R.drawable.ic_account_balance_wallet, R.drawable.ic_account_balance_wallet_filled),
    Focus("Focus", R.drawable.ic_timer, R.drawable.ic_timer_filled);

    companion object {
        /** Saved stacks from 1.3.x name the old tabs; map them onto the new ones. */
        fun fromSaved(name: String): Tab = when (name) {
            "Today", "Lists", "Stats" -> Tasks
            "Money" -> Finance
            else -> entries.firstOrNull { it.name == name } ?: Tasks
        }
    }
}
