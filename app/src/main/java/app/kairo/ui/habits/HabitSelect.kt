@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.habits.Goal
import app.kairo.habits.Habit
import app.kairo.ui.common.LocalAppSnackbar
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.common.LocalSelecting
import app.kairo.ui.common.Selection
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Canvas habits select board: a vibrant floating toolbar with a FAB. One habit: Edit, Log (count or
 * time), Pause/Resume, Archive, Delete. Several: Pause/Resume, Archive, Delete. The FAB marks the
 * picked day Done for the due ones (Reset when they're all done). Changes offer Undo.
 */
@Composable
fun HabitSelectionToolbar(selection: Selection, habits: List<Habit>, day: LocalDate, onEdit: (Habit) -> Unit, modifier: Modifier = Modifier) {
    val app = LocalKairoApp.current
    val snackbar = LocalAppSnackbar.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val motion = MaterialTheme.motionScheme
    val chosen = habits.filter { it.id in selection.ids }
    val one = chosen.singleOrNull()
    val allArch = chosen.isNotEmpty() && chosen.all { it.archived }
    val allPaused = chosen.isNotEmpty() && chosen.all { it.paused(day) }
    val due = chosen.filter { !it.archived && it.scheduled(day) }
    val allDone = due.isNotEmpty() && due.all { it.done(day) }
    var pausing by remember { mutableStateOf<List<Habit>?>(null) }
    var deleting by remember { mutableStateOf<Habit?>(null) }
    var logging by remember { mutableStateOf<Habit?>(null) }
    val selecting = LocalSelecting.current
    DisposableEffect(selection.active) {
        if (selection.active) selecting.value++
        onDispose { if (selection.active) selecting.value-- }
    }
    fun n(k: Int) = "$k habit" + if (k == 1) "" else "s"
    fun undoable(msg: String, undo: suspend () -> Unit) = scope.launch {
        snackbar.currentSnackbarData?.dismiss()
        if (snackbar.showSnackbar(msg, "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: undo
            undo()
        }
    }

    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = selection.active,
            enter = slideInVertically(motion.defaultSpatialSpec()) { it } + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.8f) + fadeIn(motion.defaultEffectsSpec()),
            exit = slideOutVertically(motion.fastSpatialSpec()) { it } + scaleOut(motion.fastSpatialSpec(), targetScale = 0.8f) + fadeOut(motion.fastEffectsSpec()),
        ) {
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                floatingActionButton = {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = {
                        if (due.isEmpty()) return@VibrantFloatingActionButton
                        val before = due.map { it.id to it.logs[day] }
                        if (!allDone) haptic.performHapticFeedback(HapticFeedbackType.ToggleOn) // rule 9: complete
                        scope.launch { due.forEach { if (allDone) app.habits.clear(it.id, day) else app.habits.swipe(it, day, true) } }
                        selection.clear()
                        undoable(if (allDone) (if (due.size > 1) n(due.size) + " reset" else "Check-in removed") else if (due.size > 1) n(due.size) + " done" else due[0].name + " · done") {
                            before.forEach { (id, log) -> app.habits.restore(id, day, log) }
                        }
                    }) {
                        Icon(painterResource(if (allArch) R.drawable.ic_unarchive else if (allDone) R.drawable.ic_restart_alt else R.drawable.ic_check),
                            if (allDone) "Reset" else "Done")
                    }
                },
            ) {
                if (one != null) Action(R.drawable.ic_edit, "Edit") { selection.clear(); onEdit(one) }
                if (one != null && !one.archived && one.goal != Goal.Yes) Action(if (one.goal == Goal.Time) R.drawable.ic_timer else R.drawable.hi_water_full_filled, "Log") { logging = one; selection.clear() }
                if (!allArch) Action(if (allPaused) R.drawable.ic_play_arrow else R.drawable.ic_pause, if (allPaused) "Resume" else "Pause") {
                    if (allPaused) {
                        scope.launch { chosen.forEach { app.habits.resume(it) } }; selection.clear()
                        undoable(if (chosen.size > 1) n(chosen.size) + " resumed" else "Habit resumed") { chosen.forEach { app.habits.update(it.e) } }
                    } else pausing = chosen
                }
                Action(if (allArch) R.drawable.ic_unarchive else R.drawable.ic_archive, if (allArch) "Unarchive" else "Archive") {
                    scope.launch { chosen.forEach { app.habits.setArchived(it, !allArch) } }; selection.clear()
                    undoable(if (allArch) n(chosen.size) + " unarchived" else n(chosen.size) + " archived · reminders off") { chosen.forEach { app.habits.update(it.e) } }
                }
                Action(R.drawable.ic_delete, "Delete") {
                    if (one != null) { deleting = one; return@Action }
                    haptic.performHapticFeedback(HapticFeedbackType.Reject) // rule 9: delete
                    scope.launch { chosen.forEach { app.habits.delete(it.id) } }; selection.clear()
                    undoable(n(chosen.size) + " deleted") { chosen.forEach { app.habits.restore(it) } }
                }
            }
        }
    }
    pausing?.let { hs -> PauseSheet(onDismiss = { pausing = null }) { days ->
        scope.launch { hs.forEach { app.habits.setPause(it, days) } }; pausing = null; selection.clear()
    } }
    deleting?.let { h -> DeleteHabitSheet(h, onDismiss = { deleting = null }) {
        haptic.performHapticFeedback(HapticFeedbackType.Reject) // rule 9: delete
        scope.launch { app.habits.delete(h.id) }; deleting = null; selection.clear()
        undoable("Habit deleted") { app.habits.restore(h) }
    } }
    logging?.let { h -> LogSheet(h, day, onDismiss = { logging = null }) { v -> scope.launch { app.habits.log(h.id, day, v) }; logging = null } }
}

@Composable
private fun Action(icon: Int, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, shapes = IconButtonDefaults.shapes(), modifier = Modifier.semantics { contentDescription = label }) { Icon(painterResource(icon), null) }
}
