@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo

import app.kairo.navigation.Route
import app.kairo.navigation.Tab
import app.kairo.ui.calendar.CalendarScreen
import app.kairo.ui.calendar.CalendarsDrawerContent
import app.kairo.ui.calendar.EventPage
import app.kairo.ui.finance.FinanceAction
import app.kairo.ui.finance.FinanceDrawerContent
import app.kairo.ui.finance.FinanceScreen
import app.kairo.ui.finance.TxPage
import app.kairo.ui.finance.ReportScreen
import app.kairo.ui.focus.FocusScreen
import app.kairo.ui.habits.HabitPage
import app.kairo.ui.habits.HabitsTab
import app.kairo.ui.lists.EditListsScreen
import app.kairo.ui.lists.ListsDrawerContent
import app.kairo.ui.money.MoneyScreen
import app.kairo.ui.nav.FloatingNavBar
import app.kairo.ui.tasks.TasksScreen

import kotlinx.coroutines.launch

import androidx.compose.ui.Modifier
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource

import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

/** One tab's screen inside the pager. */
@Composable
internal fun TabPage(
    tab: Tab, app: KairoApplication, open: (Route) -> Unit, onVoice: () -> Unit, onDrawer: () -> Unit, onFocusBack: () -> Unit,
    newHabit: Boolean, onNewHabitDone: () -> Unit,
) {
    when (tab) {
        Tab.Tasks -> TasksScreen(
            onOpen = { t -> open(Route.TaskRoute(t.id)) },
            onSearch = { open(Route.SearchRoute) },
            onVoice = onVoice,
            onLists = onDrawer,
            // The Account page (stats + settings) isn't designed yet; Settings stands in.
            onAccount = { open(Route.SettingsRoute) },
        )
        Tab.Habits -> HabitsTab(
            newHabit, onNewHabitDone,
            onSearch = { open(Route.SearchRoute) }, onVoice = onVoice, onAccount = { open(Route.SettingsRoute) },
            onOpenHabit = { open(Route.HabitRoute(it)) },
        )
        Tab.Calendar -> CalendarScreen(
            app.repository, app.phoneCalendar, onOpenTask = { t -> open(Route.TaskRoute(t.id)) },
            onOpenEvent = { e -> open(Route.EventRoute(e.eventId, e.start.toString(), e.end.toString(), e.allDay)) },
            onMenu = onDrawer,
            onSearch = { open(Route.SearchRoute) }, onVoice = onVoice, onAccount = { open(Route.SettingsRoute) },
        )
        Tab.Finance -> FinanceScreen(
            app.money, onSearch = { open(Route.SearchRoute) }, onVoice = onVoice,
            onAccount = { open(Route.SettingsRoute) }, onReports = { open(Route.ReportRoute) }, onOpenTx = { open(Route.TxRoute(it)) }, onBills = { open(Route.RecurringRoute) },
            onMenu = onDrawer,
        )
        Tab.Focus -> FocusScreen(app, onBack = onFocusBack)
    }
}

/** Saved stacks from before the pager could hold a second tab route; keep only the bottom one. */
internal val StackSaver = listSaver<SnapshotStateList<Route>, String>(
    save = { stack -> stack.map { it.encode() } },
    restore = { saved ->
        saved.map(Route::decode).filterIndexed { i, r -> i == 0 || r !is Route.TabRoute }.toMutableStateList()
    },
)

/** The floating nav and its create menu. Items top to bottom; the current tab's own item sits next to the + (canvas menuDefs). */
@Composable
internal fun AppNav(
    currentTab: Tab, menuOpen: Boolean, collapsed: Boolean,
    onSelect: (Tab) -> Unit, onMenu: (Boolean) -> Unit, onCreate: (String) -> Unit, modifier: Modifier = Modifier,
) {
    val items = mutableListOf(
        "List" to R.drawable.ic_checklist, "Transfer" to R.drawable.ic_swap_vert, "Income" to R.drawable.ic_savings,
        "Expense" to R.drawable.ic_receipt_long, "Event" to R.drawable.ic_event,
        "Habit" to R.drawable.ic_local_fire_department, "Task" to R.drawable.ic_task_alt,
    )
    val own = when (currentTab) { Tab.Finance -> "Expense"; Tab.Calendar -> "Event"; Tab.Habits -> "Habit"; else -> null }
    own?.let { o -> items.indexOfFirst { it.first == o }.takeIf { it >= 0 }?.let { items.add(items.removeAt(it)) } }
    FloatingNavBar(selected = currentTab, onSelect = onSelect, menuOpen = menuOpen, onMenu = onMenu, collapsed = collapsed, modifier = modifier) {
        items.forEach { (label, icon) ->
            FloatingActionButtonMenuItem(
                onClick = { onMenu(false); onCreate(label) },
                text = { Text(label) },
                icon = { Icon(painterResource(icon), null) },
            )
        }
    }
}

/** The Tasks drawer (canvas "drawer" board): a real M3 modal navigation drawer over the whole app. */
@Composable
internal fun KairoDrawer(
    state: androidx.compose.material3.DrawerState,
    tab: Tab,
    onNewList: () -> Unit, onEditLists: () -> Unit, onTasks: () -> Unit, onArchived: (Long) -> Unit,
    onFinance: (FinanceAction) -> Unit,
    content: @Composable () -> Unit,
) {
    // Back closes an open drawer first (otherwise the shell's Back took you to the Tasks tab).
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.activity.compose.BackHandler(enabled = state.isOpen) { scope.launch { state.close() } }
    DrawerHost(state, tab, onNewList, onEditLists, onTasks, onArchived, onFinance, content)
}

@Composable
private fun DrawerHost(
    state: androidx.compose.material3.DrawerState, tab: Tab,
    onNewList: () -> Unit, onEditLists: () -> Unit, onTasks: () -> Unit, onArchived: (Long) -> Unit,
    onFinance: (FinanceAction) -> Unit, content: @Composable () -> Unit,
) = androidx.compose.material3.ModalNavigationDrawer(
    drawerState = state,
    gesturesEnabled = state.isOpen,
    drawerContent = {
        if (tab == Tab.Calendar) CalendarsDrawerContent()
        else if (tab == Tab.Finance) FinanceDrawerContent(onFinance)
        else ListsDrawerContent(onNewList, onEditLists, onTasks, onArchived)
    },
    content = content,
)

/** Finance drawer → a page or a sheet ("account", "ivy", "export" in [CreateSheets]). */
internal fun financeAction(a: FinanceAction, open: (Route) -> Unit, sheet: (String) -> Unit) = when (a) {
    FinanceAction.Bills -> open(Route.RecurringRoute)
    FinanceAction.Reports -> open(Route.ReportRoute)
    FinanceAction.AddAccount -> sheet("account")
    FinanceAction.Import -> sheet("ivy")
    FinanceAction.Export -> sheet("export")
}

/** v2 pages with their own bottom toolbar or main button: the floating nav steps aside on these. */
internal fun fullPage(r: Route) = r is Route.HabitRoute || r is Route.EventRoute || r is Route.TxRoute || r == Route.ReportRoute || r == Route.RecurringRoute

/** The v2 detail pages opened from a tab. */
@Composable
internal fun V2Page(route: Route, app: KairoApplication, back: () -> Unit) {
    when (route) {
        Route.ListsRoute -> EditListsScreen(onBack = back)
        is Route.HabitRoute -> HabitPage(route.habitId, onBack = back)
        is Route.EventRoute -> EventPage(app.phoneCalendar, route.eventId,
            java.time.LocalDateTime.parse(route.start), java.time.LocalDateTime.parse(route.end), route.allDay, onBack = back)
        is Route.TxRoute -> TxPage(app.money, route.txId, onBack = back)
        Route.ReportRoute -> ReportScreen(app.money, onBack = back)
        else -> {}
    }
}

