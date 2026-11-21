@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.model.Task
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch

/** Delete a task with "Deleted · Undo" (rule 9 haptics on delete and undo). */
fun deleteTaskWithUndo(app: app.kairo.KairoApplication, snackbar: androidx.compose.material3.SnackbarHostState,
                       haptic: androidx.compose.ui.hapticfeedback.HapticFeedback, scope: kotlinx.coroutines.CoroutineScope, task: Task) {
    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    scope.launch {
        val rows = app.repository.deleteAll(listOf(task.id))
        snackbar.currentSnackbarData?.dismiss()
        val r = snackbar.showSnackbar("Deleted “${task.title}”", actionLabel = "Undo", duration = SnackbarDuration.Short)
        if (r == SnackbarResult.ActionPerformed) { haptic.performHapticFeedback(HapticFeedbackType.Confirm); app.repository.restore(rows) }
    }
}

/**
 * Swipe a task row (owner, 2026-11-21): swipe left = complete, swipe right = delete (with Undo).
 * The revealed side shows the action on its color; the icon grows as you pass the threshold.
 */
@Composable
fun TaskSwipe(task: Task, enabled: Boolean, onComplete: (Task) -> Unit, content: @Composable () -> Unit) {
    val app = LocalKairoApp.current
    val snackbar = LocalAppSnackbar.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val state = rememberSwipeToDismissBoxState()
    val c = MaterialTheme.colorScheme
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = enabled,
        enableDismissFromEndToStart = enabled,
        onDismiss = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> onComplete(task)   // swipe left: done
                SwipeToDismissBoxValue.StartToEnd -> deleteTaskWithUndo(app, snackbar, haptic, scope, task) // swipe right: delete
                else -> {}
            }
            scope.launch { state.reset() }
        },
        backgroundContent = {
            val dir = state.dismissDirection
            val delete = dir == SwipeToDismissBoxValue.StartToEnd
            val past = state.targetValue != SwipeToDismissBoxValue.Settled
            val grow by animateFloatAsState(if (past) 1.25f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "swipeIcon")
            Row(
                Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                    .background(if (dir == SwipeToDismissBoxValue.Settled) c.surfaceContainerHigh else if (delete) c.errorContainer else c.tertiaryContainer)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (delete) Arrangement.Start else Arrangement.End,
            ) {
                if (dir != SwipeToDismissBoxValue.Settled) Icon(
                    painterResource(if (delete) R.drawable.ic_delete else R.drawable.ic_check),
                    if (delete) "Delete" else "Complete",
                    tint = if (delete) c.onErrorContainer else c.onTertiaryContainer,
                    modifier = Modifier.size(24.dp).graphicsLayer { scaleX = grow; scaleY = grow },
                )
            }
        },
    ) { content() }
}

/** The ⋮ drawer for one task: open, done, duplicate, reschedule, move, archive, delete. */
@Composable
fun TaskOptionsSheet(task: Task, onOpen: (Task) -> Unit, onToggle: (Task) -> Unit, onDismiss: () -> Unit) {
    val app = LocalKairoApp.current
    val snackbar = LocalAppSnackbar.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lists by app.repository.lists.collectAsStateWithLifecycle(initialValue = emptyList())
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    var reschedule by remember { mutableStateOf(false) }
    var move by remember { mutableStateOf(false) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun close(then: () -> Unit = {}) = scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss(); then() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleLargeEmphasized, maxLines = 2,
                modifier = Modifier.padding(start = 4.dp, bottom = 16.dp))
            val items = listOf<Triple<Int, String, () -> Unit>>(
                Triple(R.drawable.ic_open_in_full, "Open") { close { onOpen(task) } },
                Triple(R.drawable.ic_check, if (task.done) "Mark not done" else "Mark done") { close { onToggle(task) } },
                Triple(R.drawable.ic_event, "Reschedule") { reschedule = true },
                Triple(R.drawable.ic_drive_file_move, "Move to list") { move = true },
                Triple(R.drawable.ic_content_copy, "Duplicate") { scope.launch { app.repository.duplicate(task) }; close() },
                Triple(R.drawable.ic_archive, "Archive") { scope.launch { app.repository.setArchived(listOf(task.id), true) }; close() },
            )
            items.forEachIndexed { i, (icon, label, act) ->
                SegmentedListItem(
                    onClick = act,
                    shapes = ListItemDefaults.segmentedShapes(i, items.size),
                    colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                    leadingContent = { Icon(painterResource(icon), null, tint = c.onSurfaceVariant) },
                ) { Text(label) }
                Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
            }
            Spacer(Modifier.height(12.dp))
            SegmentedListItem(
                onClick = { close { deleteTaskWithUndo(app, snackbar, haptic, scope, task) } },
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                colors = ListItemDefaults.segmentedColors(containerColor = c.errorContainer),
                leadingContent = { Icon(painterResource(R.drawable.ic_delete), null, tint = c.onErrorContainer) },
            ) { Text("Delete", color = c.onErrorContainer) }
        }
    }
    if (reschedule) DateSheet(initial = DateChoice(task.due, task.time, task.reminder), title = "Reschedule", onDismiss = { reschedule = false }) { ch ->
        scope.launch { app.repository.reschedule(listOf(task.id), ch.date, ch.time, ch.reminder) }
        reschedule = false; close()
    }
    if (move) ListPickerSheet(lists, title = "Move to…", selectedId = task.listId, onDismiss = { move = false }) { l ->
        scope.launch { app.repository.move(listOf(task.id), l.id.takeIf { it > 0 }) }
        move = false; close()
    }
}

/** The ⋮ button at the end of a row (tonal circle, like the owner's reference). */
@Composable
fun MoreButton(label: String, onClick: () -> Unit) {
    androidx.compose.material3.FilledTonalIconButton(
        onClick = onClick, shapes = androidx.compose.material3.IconButtonDefaults.shapes(),
        colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(containerColor = KairoTheme.colors.itemHigh),
        modifier = Modifier.size(40.dp),
    ) { Icon(painterResource(R.drawable.ic_more_vert), label, Modifier.size(20.dp)) }
}

