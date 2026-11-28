@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package app.kairo.ui.lists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import app.kairo.ui.common.EmptyState
import app.kairo.ui.common.LocalAppSnackbar
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch

/** Route id for Trash (shares ListRoute; real lists have positive ids, smart lists -1…-5). */
const val TRASH_ID = -6L

/**
 * Canvas "trash" board: deleted tasks, "N days left" each (kept 30 days, then deleted for good).
 * Each row's ⋮ has Restore | Delete forever.
 */
@Composable
fun TrashScreen(onBack: () -> Unit) {
    val app = LocalKairoApp.current
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val scope = rememberCoroutineScope()
    val snackbar = LocalAppSnackbar.current
    val haptic = LocalHapticFeedback.current
    val trash by app.repository.trash.collectAsStateWithLifecycle(initialValue = emptyList())
    val now = System.currentTimeMillis()
    Scaffold(
        containerColor = k.bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Trash", style = MaterialTheme.typography.titleLargeEmphasized)
                        Text("Deleted for good after 30 days", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(shapes = IconButtonDefaults.shapes(), onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = k.bg),
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 48.dp)) {
            if (trash.isEmpty()) item(key = "empty") {
                EmptyState("Trash is empty", "", Modifier.padding(top = 24.dp), size = 160.dp, icon = R.drawable.ic_auto_delete)
            }
            itemsIndexed(trash, key = { _, p -> "t${p.first.id}" }) { i, (t, at) ->
                val left = (30 - ((now - at) / 86_400_000L)).coerceAtLeast(1)
                Column(Modifier.animateItem()) {
                    SegmentedListItem(
                        onClick = {}, shapes = ListItemDefaults.segmentedShapes(i, trash.size),
                        colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                        supportingContent = { Text(if (left == 1L) "1 day left" else "$left days left", color = if (left <= 3) c.error else c.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")) },
                        trailingContent = {
                            TrashMenu(t,
                                onRestore = { scope.launch { app.repository.restoreFromTrash(listOf(t.id)); snackbar.showSnackbar("Restored “${t.title}”") } },
                                onPurge = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Reject) // rule 9: delete
                                    scope.launch { app.repository.purge(listOf(t.id)) }
                                })
                        },
                    ) { Text(t.title, color = c.onSurfaceVariant) }
                    if (i < trash.lastIndex) Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
                }
            }
        }
    }
}

/** Canvas rowMenu in Trash: Restore | Delete forever (danger), the Expressive grouped menu. */
@Composable
private fun TrashMenu(t: Task, onRestore: () -> Unit, onPurge: () -> Unit) {
    val c = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_more_vert), "More actions for ${t.title}", tint = c.onSurfaceVariant) }
        DropdownMenuPopup(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 2)) {
                DropdownMenuItem(text = { Text("Restore") }, onClick = { open = false; onRestore() }, shape = MenuDefaults.standaloneItemShape,
                    leadingIcon = { Icon(painterResource(R.drawable.ic_restore), null, Modifier.size(20.dp)) })
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(1, 2)) {
                DropdownMenuItem(text = { Text("Delete forever", color = c.error) }, onClick = { open = false; onPurge() }, shape = MenuDefaults.standaloneItemShape,
                    leadingIcon = { Icon(painterResource(R.drawable.ic_delete_forever), null, Modifier.size(20.dp), tint = c.error) })
            }
        }
    }
}
