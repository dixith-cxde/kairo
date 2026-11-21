@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.common

import androidx.compose.material3.ButtonDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import app.kairo.KairoApplication
import app.kairo.R
import app.kairo.data.db.TransactionEntity
import app.kairo.data.db.TxKind
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import app.kairo.ui.shapes.MorphShape
import app.kairo.ui.shapes.polygon
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch

/** App-wide objects for screens that act on data directly (multi-select actions). */
val LocalKairoApp = staticCompositionLocalOf<KairoApplication> { error("no app") }
/** One snackbar host for the whole app, drawn above the floating nav. */
val LocalAppSnackbar = staticCompositionLocalOf { SnackbarHostState() }
/** Count of screens currently in selection mode; the floating nav hides while it's > 0. */
val LocalSelecting = staticCompositionLocalOf<MutableState<Int>> { mutableStateOf(0) }

/** Which items are selected (by id). Long-press starts selection; tapping then toggles. */
@Stable
class Selection(initial: List<Long> = emptyList()) {
    val ids = mutableStateListOf<Long>().apply { addAll(initial) }
    val active get() = ids.isNotEmpty()
    fun toggle(id: Long) { if (id in ids) ids.remove(id) else ids.add(id) }
    fun clear() = ids.clear()
    fun setAll(all: List<Long>) { ids.clear(); ids.addAll(all) }
    operator fun contains(id: Long) = id in ids
}

@Composable
fun rememberSelection(): Selection = rememberSaveable(
    saver = listSaver(save = { it.ids.toList() }, restore = { Selection(it) }),
) { Selection() }

/**
 * Selection indicator for a row's leading slot (Expressive pattern used by Google's apps):
 * an outlined circle that morphs into a filled Cookie9 with a check when selected.
 */
@Composable
fun SelectBadge(selected: Boolean, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val p by animateFloatAsState(if (selected) 1f else 0f, motion.fastSpatialSpec(), label = "sel")
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    Box(modifier.size(40.dp), contentAlignment = Alignment.Center) {
        if (p < 0.05f) Box(Modifier.size(24.dp).border(2.dp, c.outline, CircleShape))
        Box(
            Modifier.size((24 + 12 * p).dp).background(c.primary.copy(alpha = p.coerceIn(0f, 1f)), MorphShape(morph, p)),
            contentAlignment = Alignment.Center,
        ) {
            if (p > 0.3f) Icon(painterResource(R.drawable.ic_check), null, tint = c.onPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

/** Selected-row colors for segmented items: primaryContainer, the M3 selected role. */
@Composable
fun selectableRowColors() = ListItemDefaults.segmentedColors(
    containerColor = KairoTheme.colors.item,
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedSupportingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedLeadingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedTrailingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

/** Top contextual bar: close, "N selected", select all. Actions live in the bottom toolbar. */
@Composable
fun SelectionTopBar(selection: Selection, allIds: List<Long>, modifier: Modifier = Modifier) {
    val motion = MaterialTheme.motionScheme
    AnimatedVisibility(
        visible = selection.active,
        enter = slideInVertically(motion.defaultSpatialSpec()) { -it } + fadeIn(motion.defaultEffectsSpec()),
        exit = slideOutVertically(motion.fastSpatialSpec()) { -it } + fadeOut(motion.fastEffectsSpec()),
        modifier = modifier,
    ) {
        TopAppBar(
            title = {
                val n by countUp(selection.ids.size)
                Text("$n selected", style = MaterialTheme.typography.titleLargeEmphasized, maxLines = 1)
            },
            navigationIcon = {
                IconButton(onClick = selection::clear, shapes = IconButtonDefaults.shapes()) {
                    Icon(painterResource(R.drawable.ic_close), "Clear selection")
                }
            },
            actions = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { if (selection.ids.size == allIds.size) selection.clear() else selection.setAll(allIds) }) {
                    Text(if (selection.ids.size == allIds.size) "None" else "All")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = KairoTheme.colors.bg),
        )
    }
}

/** Bottom M3 Expressive floating toolbar (vibrant) that pops up while selecting. Hides the nav. */
@Composable
fun SelectionToolbar(visible: Boolean, modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val motion = MaterialTheme.motionScheme
    val selecting = LocalSelecting.current
    DisposableEffect(visible) {
        if (visible) selecting.value++
        onDispose { if (visible) selecting.value-- }
    }
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(motion.defaultSpatialSpec()) { it } + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.8f) + fadeIn(motion.defaultEffectsSpec()),
            exit = slideOutVertically(motion.fastSpatialSpec()) { it } + scaleOut(motion.fastSpatialSpec(), targetScale = 0.8f) + fadeOut(motion.fastEffectsSpec()),
        ) {
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                content = content,
            )
        }
    }
}

@Composable
private fun ToolbarAction(icon: Int, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, shapes = IconButtonDefaults.shapes(), modifier = Modifier.semantics { contentDescription = label }) {
        Icon(painterResource(icon), null)
    }
}

/**
 * Actions for selected tasks (in the floating toolbar). One: open, duplicate, move, archive, delete.
 * Several: complete, move, archive, delete. Delete, archive and complete offer Undo.
 */
@Composable
fun TaskSelectionToolbar(
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
    var moving by remember { mutableStateOf(false) }

    fun withUndo(message: String, undo: suspend () -> Unit) = scope.launch {
        snackbar.currentSnackbarData?.dismiss()
        if (snackbar.showSnackbar(message, "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: undo
            undo()
        }
    }

    SelectionToolbar(selection.active, modifier) {
        val ids = selection.ids.toList()
        if (chosen.size == 1) {
            ToolbarAction(R.drawable.ic_open_in_full, "Open") { selection.clear(); onOpen(chosen[0]) }
            ToolbarAction(R.drawable.ic_content_copy, "Duplicate") { scope.launch { repo.duplicate(chosen[0]) }; selection.clear() }
        } else if (!archivedView) {
            ToolbarAction(R.drawable.ic_check, "Complete") {
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn) // rule 9: complete
                scope.launch { repo.setDone(ids, true) }; selection.clear()
                withUndo("Completed ${ids.size} tasks") { repo.setDone(ids, false) }
            }
        }
        if (!archivedView) ToolbarAction(R.drawable.ic_drive_file_move, "Move to list") { moving = true }
        ToolbarAction(if (archivedView) R.drawable.ic_unarchive else R.drawable.ic_archive, if (archivedView) "Unarchive" else "Archive") {
            scope.launch { repo.setArchived(ids, !archivedView) }; selection.clear()
            withUndo(if (archivedView) "Unarchived ${ids.size}" else "Archived ${ids.size}") { repo.setArchived(ids, archivedView) }
        }
        ToolbarAction(R.drawable.ic_delete, "Delete") {
            haptic.performHapticFeedback(HapticFeedbackType.Reject) // rule 9: delete
            selection.clear()
            scope.launch {
                val rows = repo.deleteAll(ids)
                withUndo(if (rows.size == 1) "Deleted “${rows[0].title}”" else "Deleted ${rows.size} tasks") { repo.restore(rows) }
            }
        }
    }
    if (moving) ListPickerSheet(lists, title = "Move ${selection.ids.size} to…", onDismiss = { moving = false }) { l ->
        val ids = selection.ids.toList()
        scope.launch { repo.move(ids, l.id) }
        moving = false; selection.clear()
    }
}

/** Actions for selected transactions: duplicate (one), delete (any) with Undo. */
@Composable
fun MoneySelectionToolbar(selection: Selection, txs: List<TransactionEntity>, modifier: Modifier = Modifier) {
    val money = LocalKairoApp.current.money
    val snackbar = LocalAppSnackbar.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val chosen = txs.filter { it.id in selection.ids }
    SelectionToolbar(selection.active, modifier) {
        if (chosen.size == 1 && chosen[0].kind != TxKind.Transfer) {
            ToolbarAction(R.drawable.ic_content_copy, "Duplicate for today") { scope.launch { money.duplicate(chosen[0]) }; selection.clear() }
        }
        ToolbarAction(R.drawable.ic_delete, "Delete") {
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
        }
    }
}

/** Pick a list in a bottom sheet: each list as a segmented row with its own shape. */
@Composable
fun ListPickerSheet(lists: List<TaskList>, title: String, selectedId: Long? = null, onDismiss: () -> Unit, onPick: (TaskList) -> Unit) {
    val k = KairoTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(title, style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
            lists.forEachIndexed { i, l ->
                SegmentedListItem(
                    selected = l.id == selectedId,
                    onClick = { onPick(l) },
                    shapes = ListItemDefaults.segmentedShapes(i, lists.size),
                    colors = selectableRowColors(),
                    leadingContent = { Box(Modifier.size(32.dp).background(k.accent(l.color), l.shape.polygon.toShape())) },
                ) { Text(l.name) }
                if (i < lists.lastIndex) Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
            }
        }
    }
}

/** M3 Expressive grouped menu for choosing a list (rounded group, selected item highlighted). */
@Composable
fun ListPickerMenu(expanded: Boolean, lists: List<TaskList>, selectedId: Long?, onDismiss: () -> Unit, onPick: (TaskList) -> Unit) {
    val k = KairoTheme.colors
    DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1)) {
            lists.forEach { l ->
                SelectableDropdownMenuItem(
                    selected = l.id == selectedId,
                    onClick = { onPick(l); onDismiss() },
                    text = { Text(l.name) },
                    shapes = MenuDefaults.itemShapes(),
                    leadingIcon = { Box(Modifier.size(18.dp).background(k.accent(l.color), l.shape.polygon.toShape())) },
                )
            }
        }
    }
}

