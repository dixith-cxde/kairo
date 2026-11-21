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
import androidx.compose.ui.tooling.preview.Preview
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

    Box(Modifier.fillMaxSize().background(KairoTheme.surfaces.bg)) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeAt(backStack.lastIndex) },
            // Top-level switches cross-fade (no slide): they are peers, not deeper levels.
            transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
            popTransitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
            predictivePopTransitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
            entryProvider = { tab -> NavEntry(tab) { TabPlaceholder(tab, comingIn = tab.comingIn()) } },
        )
        FloatingNavBar(
            selected = backStack.last(),
            onSelect = ::select,
            onCreate = { /* Quick add sheet: step 6 */ },
            modifier = Modifier.align(Alignment.BottomCenter),
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

@Preview(name = "Dark", showSystemUi = true)
@Composable
private fun KairoAppDark() {
    KairoTheme(darkTheme = true) { KairoApp() }
}

@Preview(name = "Light", showSystemUi = true)
@Composable
private fun KairoAppLight() {
    KairoTheme(darkTheme = false) { KairoApp() }
}
