package app.kairo

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.kairo.ui.today.TodayScreen
import app.kairo.ui.today.TodayViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import app.kairo.navigation.Tab
import app.kairo.ui.nav.FloatingNavBar
import app.kairo.ui.screens.TabPlaceholder
import app.kairo.ui.theme.KairoTheme

/**
 * The app shell: the current tab's screen, with the floating nav drawn on top.
 *
 * Navigation 3: we own the back stack (a list of Tabs) and NavDisplay shows its last item.
 * Today is always at the bottom, so Back from any other tab returns to Today, then leaves the app
 * (Android's rule for top-level destinations).
 */
@Composable
fun KairoApp() {
    // rememberSaveable keeps the stack across rotation and process death (saved as tab names).
    val backStack: SnapshotStateList<Tab> = rememberSaveable(saver = TabStackSaver) {
        mutableListOf(Tab.Today).toMutableStateList()
    }
    val motion = MaterialTheme.motionScheme

    fun select(tab: Tab) {
        when {
            tab == Tab.Today -> while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            backStack.size > 1 -> backStack[1] = tab
            else -> backStack.add(tab)
        }
    }

    val app = LocalContext.current.applicationContext as KairoApplication

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
    val navOffset by animateDpAsState(
        targetValue = if (navVisible) 0.dp else 140.dp,
        animationSpec = motion.defaultSpatialSpec(),
        label = "navOffset",
    )

    Box(Modifier.fillMaxSize().background(KairoTheme.colors.bg).nestedScroll(hideOnScroll)) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeAt(backStack.lastIndex) },
            // Top-level switches cross-fade (no slide): they are peers, not deeper levels.
            transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
            popTransitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
            predictivePopTransitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
            entryProvider = { tab ->
                NavEntry(tab) {
                    when (tab) {
                        Tab.Today -> {
                            val vm: TodayViewModel = viewModel(factory = viewModelFactory { initializer { TodayViewModel(app.repository) } })
                            val state by vm.state.collectAsStateWithLifecycle()
                            state?.let { TodayScreen(it, onToggle = vm::toggle) }
                        }
                        else -> TabPlaceholder(tab, comingIn = tab.comingIn())
                    }
                }
            },
        )
        FloatingNavBar(
            selected = backStack.last(),
            onSelect = { navVisible = true; select(it) },
            onCreate = { /* Quick add sheet: step 6 */ },
            modifier = Modifier.align(Alignment.BottomCenter).offset { IntOffset(0, navOffset.roundToPx()) },
        )
    }
}

private fun Tab.comingIn() = when (this) {
    Tab.Today -> "Coming in step 3"
    Tab.Lists -> "Coming in step 8"
    Tab.Calendar -> "Coming in step 10"
    Tab.Money -> "Coming in step 14"
    Tab.Stats -> "Coming in step 11"
}

private val TabStackSaver = listSaver<SnapshotStateList<Tab>, String>(
    save = { stack -> stack.map { it.name } },
    restore = { names -> names.map(Tab::valueOf).toMutableStateList() },
)

