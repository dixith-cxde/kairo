@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.theme.KairoTheme

/**
 * Canvas "drawer" board: the Tasks drawer only manages lists (picking a list happens in the row
 * under the hero). Kairo + open count; Lists: New list, Edit lists; then Tasks (here), Archived.
 * Trash (deleted tasks kept 30 days) is designed but needs its own data step; not shown yet.
 */
@Composable
/** [onArchived] opens a smart list by id: Archived, or Trash ([TRASH_ID]). */
fun ListsDrawerContent(onNewList: () -> Unit, onEditLists: () -> Unit, onTasks: () -> Unit, onArchived: (Long) -> Unit) {
    val app = LocalKairoApp.current
    val c = MaterialTheme.colorScheme
    val tasks by app.repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val archived by app.repository.archived.collectAsStateWithLifecycle(initialValue = emptyList())
    val lists by app.repository.lists.collectAsStateWithLifecycle(initialValue = emptyList())
    val trash by app.repository.trash.collectAsStateWithLifecycle(initialValue = emptyList())
    val open = tasks.count { !it.done }
    ModalDrawerSheet(drawerContainerColor = KairoTheme.colors.bg, drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 16.dp)) {
            Text("Kairo", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 12.dp))
            Text("$open open task" + if (open == 1) "" else "s", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp))
            Text("Lists", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = c.primary,
                modifier = Modifier.padding(start = 12.dp, top = 24.dp, bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                DrawerItem(R.drawable.ic_add, "New list", 0, 2, tint = c.primary, onClick = onNewList)
                DrawerItem(R.drawable.ic_edit, "Edit lists", 1, 2, count = lists.size, onClick = onEditLists)
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                DrawerItem(R.drawable.ic_task_alt, "Tasks", 0, 3, count = open, selected = true, onClick = onTasks)
                DrawerItem(R.drawable.ic_archive, "Archived", 1, 3, count = archived.size, onClick = { onArchived(SmartList.Archived.id) })
                DrawerItem(R.drawable.ic_delete, "Trash", 2, 3, count = trash.size, onClick = { onArchived(TRASH_ID) })
            }
        }
    }
}

@Composable
private fun DrawerItem(
    icon: Int, label: String, index: Int, size: Int,
    count: Int? = null, tint: Color? = null, selected: Boolean = false, onClick: () -> Unit,
) {
    val n = count
    val c = MaterialTheme.colorScheme
    val big = 28.dp; val small = 8.dp
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.shapes(
            shape = if (selected) RoundedCornerShape(big) else RoundedCornerShape(
                topStart = if (index == 0) big else small, topEnd = if (index == 0) big else small,
                bottomStart = if (index == size - 1) big else small, bottomEnd = if (index == size - 1) big else small,
            ),
            pressedShape = RoundedCornerShape(big),
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = if (selected) c.secondaryContainer else KairoTheme.colors.item,
            contentColor = if (selected) c.onSecondaryContainer else c.onSurface,
        ),
        leadingContent = { Icon(painterResource(icon), null, tint = tint ?: if (selected) c.onSecondaryContainer else c.onSurfaceVariant) },
        trailingContent = n?.let { { CountBadge(it, selected) } },
    ) { Text(label, color = if (tint != null) tint else if (selected) c.onSecondaryContainer else c.onSurface) }
}

@Composable
private fun CountBadge(n: Int, selected: Boolean) {
    val c = MaterialTheme.colorScheme
    Box(Modifier.background(if (selected) c.onSecondaryContainer.copy(alpha = 0.16f) else c.surfaceContainerHighest, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text("$n", style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), color = if (selected) c.onSecondaryContainer else c.onSurfaceVariant)
    }
}
