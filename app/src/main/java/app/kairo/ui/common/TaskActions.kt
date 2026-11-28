@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

/** Swipe a task row (v2 canvas): swipe right = complete, swipe left = delete (with Undo). */
@Composable
fun TaskSwipe(task: Task, enabled: Boolean, onComplete: (Task) -> Unit, content: @Composable () -> Unit) {
    val app = LocalKairoApp.current
    val snackbar = LocalAppSnackbar.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    ExpressiveSwipe(
        enabled = enabled,
        rightIcon = if (task.done) R.drawable.ic_close else R.drawable.ic_check,
        rightLabel = if (task.done) "Mark not done" else "Complete",
        leftIcon = R.drawable.ic_delete, leftLabel = "Delete",
        onRight = { onComplete(task) },
        onLeft = { deleteTaskWithUndo(app, snackbar, haptic, scope, task) },
        content = content,
    )
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

