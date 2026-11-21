package app.kairo.navigation

import androidx.annotation.DrawableRes
import app.kairo.R

/** The five top-level destinations, in nav order (docs/04-screens.md). */
enum class Tab(
    val label: String,
    @param:DrawableRes val icon: Int,         // Material Symbols Rounded, outlined
    @param:DrawableRes val selectedIcon: Int, // same symbol, filled
) {
    Today("Today", R.drawable.ic_today, R.drawable.ic_today_filled),
    Lists("Lists", R.drawable.ic_checklist, R.drawable.ic_checklist_filled),
    Calendar("Calendar", R.drawable.ic_calendar_month, R.drawable.ic_calendar_month_filled),
    Money("Money", R.drawable.ic_account_balance_wallet, R.drawable.ic_account_balance_wallet_filled),
    Stats("Stats", R.drawable.ic_bar_chart, R.drawable.ic_bar_chart_filled),
}
