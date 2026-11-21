@file:OptIn(ExperimentalMaterial3Api::class)

package app.kairo.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import app.kairo.KairoApplication
import app.kairo.R
import app.kairo.data.db.TransactionEntity
import app.kairo.data.db.TxKind
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import kotlinx.coroutines.launch

/** App-wide objects for screens that act on data directly (multi-select actions). */
val LocalKairoApp = staticCompositionLocalOf<KairoApplication> { error("no app") }
/** One snackbar host for the whole app, drawn above the floating nav. */
val LocalAppSnackbar = staticCompositionLocalOf { SnackbarHostState() }

/** Which items are selected (by id). Long-press starts selection; tapping then toggles. */
@Stable
class Selection(initial: List<Long> = emptyList()) {
    val ids = mutableStateListOf<Long>().apply { addAll(initial) }
    val active get() = ids.isNotEmpty()
    fun toggle(id: Long) { if (id in ids) ids.remove(id) else ids.add(id) }
    fun clear() = ids.clear()
    operator fun contains(id: Long) = id in ids
}

@Composable
fun rememberSelection(): Selection = rememberSaveable(
    saver = listSaver(save = { it.ids.toList() }, restore = { Selection(it) }),
) { Selection() }

/** Contextual top bar (M3 pattern): count, close, actions. Slides down over the screen's own bar. */
@Composable
fun SelectionBar(visible: Boolean, count: Int, onClose: () -> Unit, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit) {
    val motion = MaterialTheme.motionScheme
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(motion.defaultSpatialSpec()) { -it } + fadeIn(motion.defaultEffectsSpec()),
        exit = slideOutVertically(motion.fastSpatialSpec()) { -it } + fadeOut(motion.fastEffectsSpec()),
        modifier = modifier,
    ) {
        TopAppBar(
            title = { Text("$count selected", style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 1) },
            navigationIcon = { IconButton(onClick = onClose) { Icon(painterResource(R.drawable.ic_close), "Clear selection") } },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        )
    }
}

/**
 * Actions for selected tasks. One selected: open, duplicate, move, archive, delete.
 * Several: complete, move, archive, delete. Delete and archive offer Undo.
 */
@Composable
fun TaskSelectionBar(
    selection: Selection,
    tasks: List<Task>,
    lists: List<TaskList>,
    onOpen: (Task) -> Unit,
    archivedView: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val app = LocalKairoApp.current
    val repo = app.repository
    val snackbar = LocalAppSnackbar.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val chosen = tasks.filter { it.id in selection.ids }
    var moveMenu by remember { mutableStateOf(false) }

    fun withUndo(message: String, undo: suspend () -> Unit) = scope.launch {
        snackbar.currentSnackbarData?.dismiss()
        if (snackbar.showSnackbar(message, "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: undo
            undo()
        }
    }

    SelectionBar(selection.active, selection.ids.size, onClose = selection::clear, modifier = modifier) {
        val ids = selection.ids.toList()
        if (chosen.size == 1) {
            IconButton(onClick = { selection.clear(); onOpen(chosen[0]) }) { Icon(painterResource(R.drawable.ic_open_in_full), "Open") }
            IconButton(onClick = { scope.launch { repo.duplicate(chosen[0]) }; selection.clear() }) {
                Icon(painterResource(R.drawable.ic_content_copy), "Duplicate")
            }
        } else if (!archivedView) {
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn) // rule 9: complete
                scope.launch { repo.setDone(ids, true) }; selection.clear()
                withUndo("Completed ${ids.size} tasks") { repo.setDone(ids, false) }
            }) { Icon(painterResource(R.drawable.ic_check), "Complete") }
        }
        if (!archivedView) Box {
            IconButton(onClick = { moveMenu = true }) { Icon(painterResource(R.drawable.ic_drive_file_move), "Move to list") }
            DropdownMenu(moveMenu, onDismissRequest = { moveMenu = false }) {
                lists.forEach { l ->
                    DropdownMenuItem(text = { Text(l.name) }, onClick = {
                        scope.launch { repo.move(ids, l.id) }; moveMenu = false; selection.clear()
                    })
                }
            }
        }
        IconButton(onClick = {
            scope.launch { repo.setArchived(ids, !archivedView) }; selection.clear()
            withUndo(if (archivedView) "Unarchived ${ids.size}" else "Archived ${ids.size}") { repo.setArchived(ids, archivedView) }
        }) {
            Icon(painterResource(if (archivedView) R.drawable.ic_unarchive else R.drawable.ic_archive), if (archivedView) "Unarchive" else "Archive")
        }
        IconButton(onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.Reject) // rule 9: delete
            selection.clear()
            scope.launch {
                val rows = repo.deleteAll(ids)
                withUndo(if (rows.size == 1) "Deleted “${rows[0].title}”" else "Deleted ${rows.size} tasks") { repo.restore(rows) }
            }
        }) { Icon(painterResource(R.drawable.ic_delete), "Delete") }
    }
}

/** Actions for selected transactions: duplicate (one), delete (any) with Undo. */
@Composable
fun MoneySelectionBar(selection: Selection, txs: List<TransactionEntity>, modifier: Modifier = Modifier) {
    val money = LocalKairoApp.current.money
    val snackbar = LocalAppSnackbar.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val chosen = txs.filter { it.id in selection.ids }
    SelectionBar(selection.active, selection.ids.size, onClose = selection::clear, modifier = modifier) {
        if (chosen.size == 1 && chosen[0].kind != TxKind.Transfer) {
            IconButton(onClick = { scope.launch { money.duplicate(chosen[0]) }; selection.clear() }) {
                Icon(painterResource(R.drawable.ic_content_copy), "Duplicate for today")
            }
        }
        IconButton(onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.Reject) // rule 9: delete
            val ids = selection.ids.toList(); selection.clear()
            scope.launch {
                val rows = money.deleteAll(ids)
                snackbar.currentSnackbarData?.dismiss()
                if (snackbar.showSnackbar("Deleted ${ids.size}", "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    money.restore(rows)
                }
            }
        }) { Icon(painterResource(R.drawable.ic_delete), "Delete") }
    }
}
