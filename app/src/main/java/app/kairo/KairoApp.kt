@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation3.scene.Scene
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import app.kairo.ui.common.LocalAppSnackbar
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.common.LocalSelecting
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import app.kairo.data.db.TxKind
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import app.kairo.navigation.Route
import app.kairo.navigation.Tab
import app.kairo.ui.detail.TaskDetailScreen
import app.kairo.ui.detail.TaskDetailViewModel
import app.kairo.ui.nav.FloatingNavBar
import app.kairo.ui.onboarding.TourOverlay
import app.kairo.ui.onboarding.WelcomeScreen
import app.kairo.ui.focus.FocusScreen
import app.kairo.ui.search.SearchScreen
import app.kairo.ui.common.LocalOpenSearch
import app.kairo.ui.money.AccountScreen
import app.kairo.ui.money.AddTransactionSheet
import app.kairo.ui.money.BudgetsScreen
import app.kairo.ui.money.InsightsScreen
import app.kairo.ui.money.MoneyScreen
import app.kairo.ui.money.RecurringScreen
import app.kairo.ui.settings.SettingsScreen
import app.kairo.ui.stats.StatsScreen
import app.kairo.ui.calendar.CalendarScreen
import app.kairo.ui.lists.ListDetailScreen
import app.kairo.ui.lists.ListDetailViewModel
import app.kairo.ui.lists.ListsScreen
import app.kairo.ui.lists.ListsViewModel
import app.kairo.ui.quickadd.QuickAddSheet
import app.kairo.ui.theme.KairoTheme
import app.kairo.ui.today.TodayScreen
import app.kairo.ui.today.TodayViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The app shell: the current screen, with the floating nav drawn on top of tab screens.
 *
 * Navigation 3: we own the back stack (a list of Routes) and NavDisplay shows its last item.
 * Today is always at the bottom; another tab sits on top of it; detail screens stack on top of a tab.
 */
@Composable
fun KairoApp(action: String? = null, onActionHandled: () -> Unit = {}) {
    val backStack: SnapshotStateList<Route> = rememberSaveable(saver = StackSaver) {
        mutableListOf<Route>(Route.TabRoute(Tab.Today)).toMutableStateList()
    }
    val motion = MaterialTheme.motionScheme
    val app = LocalContext.current.applicationContext as KairoApplication
    val scope = rememberCoroutineScope()
    // Which create sheet is open: "task", "Expense", "Income", "Transfer" (TxKind names), or null.
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var quickAddText by rememberSaveable { mutableStateOf("") } // from the widget mic
    var createMenu by rememberSaveable { mutableStateOf(false) }
    var newListSignal by remember { mutableIntStateOf(0) }
    val lists by app.repository.lists.collectAsStateWithLifecycle(initialValue = emptyList())

    val top = backStack.last()
    // The five tabs live side by side in one pager (new in this step): a swipe drags the next tab
    // in under your finger and lets go on a fling, like Google's own apps, instead of jumping on a
    // detected gesture. The back stack only holds the pager (at the bottom) and screens above it.
    val pager = rememberPagerState { Tab.entries.size }
    val currentTab = Tab.entries[pager.currentPage]

    fun popToTabs() { while (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    fun select(tab: Tab, animate: Boolean = true) {
        popToTabs()
        scope.launch {
            if (animate) pager.animateScrollToPage(tab.ordinal, animationSpec = motion.defaultSpatialSpec())
            else pager.scrollToPage(tab.ordinal)
        }
    }
    fun open(route: Route) = backStack.add(route)
    fun back() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }

    // Widget shortcuts.
    LaunchedEffect(action) {
        when {
            action == null -> {}
            action.startsWith("${MainActivity.ACTION_QUICK_ADD}:") -> {
                select(Tab.Today, animate = false); quickAddText = action.substringAfter(':'); sheet = "task"
            }
            action.startsWith("task:") -> action.substringAfter(':').toLongOrNull()?.let { open(Route.TaskRoute(it)) }
        }
        when (action) {
            MainActivity.ACTION_QUICK_ADD -> { select(Tab.Today, animate = false); quickAddText = ""; sheet = "task" }
            MainActivity.ACTION_ADD_EXPENSE -> { select(Tab.Money, animate = false); sheet = TxKind.Expense.name }
            MainActivity.ACTION_ADD_INCOME -> { select(Tab.Money, animate = false); sheet = TxKind.Income.name }
            MainActivity.ACTION_ADD_TRANSFER -> { select(Tab.Money, animate = false); sheet = TxKind.Transfer.name }
            MainActivity.ACTION_FOCUS -> { select(Tab.Today, animate = false); open(Route.FocusRoute) }
            MainActivity.ACTION_STATS -> select(Tab.Stats, animate = false)
        }
        if (action != null) onActionHandled()
    }

    // The floating nav hides while content scrolls down and returns on scroll up (canvas AppBars board).
    var navVisible by remember { mutableStateOf(true) }
    val hideOnScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -4f) navVisible = false else if (available.y > 4f) navVisible = true
                return Offset.Zero
            }
        }
    }
    val onTab = top is Route.TabRoute
    // Screens in selection mode show their own floating toolbar; the nav steps aside.
    val selecting = remember { mutableStateOf(0) }
    val navOffset by animateDpAsState(
        // Only selection mode moves the whole group away; scrolling tucks the bar but keeps the +,
        // and detail screens keep the nav (owner, 2026-11-21).
        targetValue = if (selecting.value == 0 && top != Route.FocusRoute) 0.dp else 160.dp, // Focus is full-screen
        animationSpec = motion.defaultSpatialSpec(),
        label = "navOffset",
    )

    // Directional motion: a tab to the right slides in from the right, to the left from the left;
    // detail screens come in from the right and go back to the right. Spatial spring = slight bounce.
    fun depth(scene: Scene<Route>): Int = when (val r = Route.decode(scene.entries.last().contentKey as String)) {
        is Route.TabRoute -> r.tab.ordinal
        else -> 100 + scene.entries.size
    }
    fun slide(dir: Int): ContentTransform =
        (slideInHorizontally(motion.slowSpatialSpec()) { w -> dir * w / 2 } +
            fadeIn(motion.defaultEffectsSpec()) +
            scaleIn(motion.slowSpatialSpec(), initialScale = 0.92f)) togetherWith
            (slideOutHorizontally(motion.slowSpatialSpec()) { w -> -dir * w / 3 } +
                fadeOut(motion.defaultEffectsSpec()) +
                scaleOut(motion.slowSpatialSpec(), targetScale = 0.92f))

    BackHandler(enabled = backStack.size == 1 && pager.currentPage != 0) { select(Tab.Today) }
    // The nav comes back whenever the tab or screen changes.
    LaunchedEffect(pager.currentPage, backStack.size) { navVisible = true }

    val snackbar = remember { SnackbarHostState() }
    CompositionLocalProvider(
        LocalKairoApp provides app, LocalAppSnackbar provides snackbar, LocalSelecting provides selecting,
        LocalOpenSearch provides { open(Route.SearchRoute) },
    ) {
    Box(Modifier.fillMaxSize().background(KairoTheme.colors.bg).nestedScroll(hideOnScroll)) {
        NavDisplay(
            backStack = backStack,
            onBack = ::back,
            transitionSpec = { slide(if (depth(targetState) >= depth(initialState)) 1 else -1) },
            popTransitionSpec = { slide(if (depth(targetState) > depth(initialState)) 1 else -1) },
            predictivePopTransitionSpec = { slide(-1) },
            entryProvider = { route ->
                NavEntry(route, contentKey = route.encode()) {
                    when (route) {
                        is Route.TabRoute -> HorizontalPager(
                            state = pager,
                            // No tab swiping while a screen is selecting rows (its toolbar owns the gestures).
                            // Off: rows swipe to complete/delete instead (owner, 2026-11-21). Tabs change from the nav.
                            userScrollEnabled = false,
                            key = { Tab.entries[it].name },
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            TabPage(Tab.entries[page], app, newListSignal, ::open)
                        }
                        is Route.TaskRoute -> {
                            val vm: TaskDetailViewModel = viewModel(
                                key = "task-${route.taskId}",
                                factory = viewModelFactory { initializer { TaskDetailViewModel(app.repository, route.taskId) } },
                            )
                            TaskDetailScreen(vm, lists = lists, onBack = ::back)
                        }
                        Route.SettingsRoute -> SettingsScreen(app.settings, onBack = ::back)
                        is Route.AccountRoute -> AccountScreen(app.money, route.accountId, onBack = ::back)
                        Route.BudgetsRoute -> BudgetsScreen(app.money, onBack = ::back)
                        Route.InsightsRoute -> InsightsScreen(app.money, onBack = ::back)
                        Route.RecurringRoute -> RecurringScreen(app.money, onBack = ::back)
                        Route.FocusRoute -> FocusScreen(app, onBack = ::back)
                        Route.SearchRoute -> SearchScreen(
                            app, onBack = ::back,
                            onOpenTask = { t -> open(Route.TaskRoute(t.id)) },
                            onOpenList = { open(Route.ListRoute(it)) },
                            onOpenAccount = { open(Route.AccountRoute(it)) },
                        )
                        is Route.ListRoute -> {
                            val vm: ListDetailViewModel = viewModel(
                                key = "list-${route.listId}",
                                factory = viewModelFactory { initializer { ListDetailViewModel(app.repository, route.listId) } },
                            )
                            ListDetailScreen(vm, onBack = ::back, onOpenTask = { t -> open(Route.TaskRoute(t.id)) })
                        }
                    }
                }
            },
        )
        // Dim the screen under the nav and the menu (the × stays bright).
        val scrim by animateFloatAsState(if (createMenu) 0.32f else 0f, motion.defaultEffectsSpec(), label = "scrim")
        if (scrim > 0f) Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim))
                .clickable(interactionSource = null, indication = null) { createMenu = false },
        )
        FloatingNavBar(
            selected = currentTab,
            position = { pager.currentPage + pager.currentPageOffsetFraction },
            onSelect = { navVisible = true; select(it) },
            onCreate = { navVisible = true; createMenu = !createMenu },
            menuOpen = createMenu,
            collapsed = !navVisible,
            modifier = Modifier.align(Alignment.BottomCenter).offset { IntOffset(0, navOffset.roundToPx()) },
        )
        // Create menu (M3 Expressive FAB menu). Our + lives in the nav group, so the menu's own
        // button slot is an empty spacer sitting exactly over it; the items rise above it.
        FloatingActionButtonMenu(
            expanded = createMenu,
            button = { Spacer(Modifier.size(64.dp)) },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 20.dp),
        ) {
            listOf(
                Triple("Task", R.drawable.ic_task_alt) { sheet = "task" },
                Triple("Expense", R.drawable.ic_payments) { select(Tab.Money); sheet = TxKind.Expense.name },
                Triple("Income", R.drawable.ic_savings) { select(Tab.Money); sheet = TxKind.Income.name },
                Triple("Transfer", R.drawable.ic_swap_vert) { select(Tab.Money); sheet = TxKind.Transfer.name },
                Triple("List", R.drawable.ic_checklist) { select(Tab.Lists); newListSignal++ },
            ).forEach { (label, icon, act) ->
                FloatingActionButtonMenuItem(
                    onClick = { createMenu = false; act() },
                    text = { Text(label) },
                    icon = { Icon(painterResource(icon), null) },
                )
            }
        }

        // One snackbar for the app, above the floating nav (64dp + 20dp + gap).
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 92.dp))

        // First run: welcome pages, then the spotlight tour on Today (Stash's pattern).
        val prefs by app.settings.settings.collectAsStateWithLifecycle(initialValue = null)
        prefs?.let { p ->
            if (!p.onboarded) WelcomeScreen { scope.launch { app.settings.setOnboarded(true) } }
            else if (!p.tourDone && backStack.size == 1 && currentTab == Tab.Today && sheet == null && !createMenu)
                TourOverlay { scope.launch { app.settings.setTourDone(true) } }
        }

        when (val sh = sheet) {
            null -> {}
            "task" -> QuickAddSheet(
                lists = lists,
                defaultDue = if (currentTab == Tab.Today) LocalDate.now() else null,
                initialText = quickAddText,
                onAdd = { task -> scope.launch { app.repository.add(task) } },
                onDismiss = { sheet = null },
            )
            else -> {
                val accounts by app.money.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
                val categories by app.money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
                if (accounts.isNotEmpty()) AddTransactionSheet(
                    app.money, accounts, categories, initialKind = TxKind.valueOf(sh), onDismiss = { sheet = null },
                )
            }
        }
    }
    }
}

/** One tab's screen inside the pager. */
@Composable
private fun TabPage(tab: Tab, app: KairoApplication, newListSignal: Int, open: (Route) -> Unit) {
    when (tab) {
        Tab.Today -> {
            val vm: TodayViewModel = viewModel(factory = viewModelFactory { initializer { TodayViewModel(app.repository) } })
            val state by vm.state.collectAsStateWithLifecycle()
            state?.let { TodayScreen(it, onToggle = vm::toggle, onOpen = { t -> open(Route.TaskRoute(t.id)) }, onSettings = { open(Route.SettingsRoute) }, onFocus = { open(Route.FocusRoute) }) }
        }
        Tab.Lists -> {
            val vm: ListsViewModel = viewModel(factory = viewModelFactory { initializer { ListsViewModel(app.repository) } })
            ListsScreen(vm, onOpenList = { id -> open(Route.ListRoute(id)) }, newListSignal = newListSignal)
        }
        Tab.Calendar -> CalendarScreen(app.repository, app.phoneCalendar, onOpenTask = { t -> open(Route.TaskRoute(t.id)) })
        Tab.Money -> MoneyScreen(
            app.money,
            onAccount = { open(Route.AccountRoute(it)) },
            onBudgets = { open(Route.BudgetsRoute) },
            onInsights = { open(Route.InsightsRoute) },
            onRecurring = { open(Route.RecurringRoute) },
        )
        Tab.Stats -> StatsScreen(app.repository)
    }
}

/** Saved stacks from before the pager could hold a second tab route; keep only the bottom one. */
private val StackSaver = listSaver<SnapshotStateList<Route>, String>(
    save = { stack -> stack.map { it.encode() } },
    restore = { saved ->
        saved.map(Route::decode).filterIndexed { i, r -> i == 0 || r !is Route.TabRoute }.toMutableStateList()
    },
)
