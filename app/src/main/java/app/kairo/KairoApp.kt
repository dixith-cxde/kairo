package app.kairo

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import app.kairo.ui.quickadd.QuickAddSheet
import app.kairo.ui.screens.TabPlaceholder
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
fun KairoApp() {
    val backStack: SnapshotStateList<Route> = rememberSaveable(saver = StackSaver) {
        mutableListOf<Route>(Route.TabRoute(Tab.Today)).toMutableStateList()
    }
    val motion = MaterialTheme.motionScheme
    val app = LocalContext.current.applicationContext as KairoApplication
    val scope = rememberCoroutineScope()
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    val lists by app.repository.lists.collectAsStateWithLifecycle(initialValue = emptyList())

    val top = backStack.last()
    val currentTab = backStack.filterIsInstance<Route.TabRoute>().last().tab

    fun select(tab: Tab) {
        backStack.clear() // atomic within this event; NavDisplay never sees an empty stack
        backStack.add(Route.TabRoute(Tab.Today))
        if (tab != Tab.Today) backStack.add(Route.TabRoute(tab))
    }
    fun open(route: Route) = backStack.add(route)
    fun back() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }

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
    val navOffset by animateDpAsState(
        targetValue = if (navVisible && onTab) 0.dp else 160.dp,
        animationSpec = motion.defaultSpatialSpec(),
        label = "navOffset",
    )

    // Fade through: new screen fades in while growing slightly; old one fades out quickly.
    val enter = { fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.94f) }
    val exit = { fadeOut(motion.fastEffectsSpec()) }

    Box(Modifier.fillMaxSize().background(KairoTheme.colors.bg).nestedScroll(hideOnScroll)) {
        NavDisplay(
            backStack = backStack,
            onBack = ::back,
            transitionSpec = { enter() togetherWith exit() },
            popTransitionSpec = { enter() togetherWith exit() },
            predictivePopTransitionSpec = { enter() togetherWith exit() },
            entryProvider = { route ->
                NavEntry(route, contentKey = route.encode()) {
                    when (route) {
                        is Route.TabRoute -> when (route.tab) {
                            Tab.Today -> {
                                val vm: TodayViewModel = viewModel(factory = viewModelFactory { initializer { TodayViewModel(app.repository) } })
                                val state by vm.state.collectAsStateWithLifecycle()
                                state?.let { TodayScreen(it, onToggle = vm::toggle, onOpen = { t -> open(Route.TaskRoute(t.id)) }) }
                            }
                            else -> TabPlaceholder(route.tab, comingIn = route.tab.comingIn())
                        }
                        is Route.TaskRoute -> {
                            val vm: TaskDetailViewModel = viewModel(
                                key = "task-${route.taskId}",
                                factory = viewModelFactory { initializer { TaskDetailViewModel(app.repository, route.taskId) } },
                            )
                            TaskDetailScreen(vm, lists = lists, onBack = ::back)
                        }
                        is Route.ListRoute -> TabPlaceholder(Tab.Lists, comingIn = "List detail: step 8")
                    }
                }
            },
        )
        FloatingNavBar(
            selected = currentTab,
            onSelect = { navVisible = true; select(it) },
            onCreate = { showQuickAdd = true },
            modifier = Modifier.align(Alignment.BottomCenter).offset { IntOffset(0, navOffset.roundToPx()) },
        )
        if (showQuickAdd) {
            QuickAddSheet(
                lists = lists,
                defaultDue = if (currentTab == Tab.Today) LocalDate.now() else null,
                onAdd = { task -> scope.launch { app.repository.add(task) } },
                onDismiss = { showQuickAdd = false },
            )
        }
    }
}

private fun Tab.comingIn() = when (this) {
    Tab.Today -> ""
    Tab.Lists -> "Coming in step 8"
    Tab.Calendar -> "Coming in step 10"
    Tab.Money -> "Coming in step 14"
    Tab.Stats -> "Coming in step 11"
}

private val StackSaver = listSaver<SnapshotStateList<Route>, String>(
    save = { stack -> stack.map { it.encode() } },
    restore = { saved -> saved.map(Route::decode).toMutableStateList() },
)
