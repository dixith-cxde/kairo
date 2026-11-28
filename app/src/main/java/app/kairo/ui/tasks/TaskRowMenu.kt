@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.model.Task
import app.kairo.ui.common.DateChoice
import app.kairo.ui.common.DateSheet
import app.kairo.ui.common.ListPickerSheet
import app.kairo.ui.common.LocalAppSnackbar
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.common.deleteTaskWithUndo
import kotlinx.coroutines.launch

private class MenuEntry(val icon: Int, val label: String, val danger: Boolean = false, val act: () -> Unit)

/**
 * The row ⋮ (canvas rowMenu): the Expressive grouped menu anchored to the button (rule 12: menus,
 * not dialogs). Groups: Open · Mark done | Reschedule · Move to list | Duplicate · Archive | Delete.
 */
@Composable
fun TaskRowMenu(task: Task, onOpen: (Task) -> Unit, onToggle: (Task) -> Unit) {
    val app = LocalKairoApp.current
    val snackbar = LocalAppSnackbar.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val c = MaterialTheme.colorScheme
    val lists by app.repository.lists.collectAsStateWithLifecycle(initialValue = emptyList())
    var open by remember { mutableStateOf(false) }
    var reschedule by remember { mutableStateOf(false) }
    var move by remember { mutableStateOf(false) }
    fun pick(f: () -> Unit) = { open = false; f() }
    val groups = listOf(
        listOf(MenuEntry(R.drawable.ic_open_in_full, "Open", act = pick { onOpen(task) }),
            MenuEntry(if (task.done) R.drawable.ic_radio_button_unchecked else R.drawable.ic_check_circle, if (task.done) "Mark not done" else "Mark done", act = pick { onToggle(task) })),
        listOf(MenuEntry(R.drawable.ic_event, "Reschedule", act = pick { reschedule = true }),
            MenuEntry(R.drawable.ic_drive_file_move, "Move to list", act = pick { move = true })),
        listOf(MenuEntry(R.drawable.ic_content_copy, "Duplicate", act = pick { scope.launch { app.repository.duplicate(task) } }),
            MenuEntry(R.drawable.ic_archive, "Archive", act = pick { scope.launch { app.repository.setArchived(listOf(task.id), true) } })),
        listOf(MenuEntry(R.drawable.ic_delete, "Move to trash", danger = true, act = pick { deleteTaskWithUndo(app, snackbar, haptic, scope, task) })),
    )
    Box {
        IconButton(onClick = { open = true }, shapes = IconButtonDefaults.shapes()) {
            Icon(painterResource(R.drawable.ic_more_vert), "More actions for ${task.title}", tint = c.onSurfaceVariant)
        }
        DropdownMenuPopup(expanded = open, onDismissRequest = { open = false }) {
            groups.forEachIndexed { gi, g ->
                DropdownMenuGroup(shapes = MenuDefaults.groupShape(gi, groups.size)) {
                    g.forEachIndexed { i, e ->
                        DropdownMenuItem(
                            text = { Text(e.label, color = if (e.danger) c.error else c.onSurface) }, onClick = e.act,
                            shape = when { g.size == 1 -> MenuDefaults.standaloneItemShape; i == 0 -> MenuDefaults.leadingItemShape; i == g.lastIndex -> MenuDefaults.trailingItemShape; else -> MenuDefaults.middleItemShape },
                            leadingIcon = { Icon(painterResource(e.icon), null, Modifier.size(20.dp), tint = if (e.danger) c.error else c.onSurfaceVariant) },
                        )
                    }
                }
                if (gi < groups.lastIndex) Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            }
        }
    }
    if (reschedule) DateSheet(initial = DateChoice(task.due, task.time, task.reminder), title = "Reschedule", onDismiss = { reschedule = false }) { ch ->
        scope.launch { app.repository.reschedule(listOf(task.id), ch.date, ch.time, ch.reminder) }
        reschedule = false
    }
    if (move) ListPickerSheet(lists, title = "Move to…", selectedId = task.listId, onDismiss = { move = false }) { l ->
        scope.launch { app.repository.move(listOf(task.id), l.id.takeIf { it > 0 }) }
        move = false
    }
}
