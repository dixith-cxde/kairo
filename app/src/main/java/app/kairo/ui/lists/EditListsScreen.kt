@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.model.TaskList
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch

/**
 * Canvas "edit lists" board: each list with its colour tag and open count; tap to rename, recolour
 * or delete; ↑ ↓ reorder (the canvas drags by the handle; drag comes with the list step). Inbox is
 * permanent and always first, so it isn't listed here.
 */
@Composable
fun EditListsScreen(onBack: () -> Unit) {
    val app = LocalKairoApp.current
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val scope = rememberCoroutineScope()
    val lists0 by app.repository.lists.collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks by app.repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val lists = lists0.sortedBy { it.position }
    val open = tasks.filter { !it.done }.groupingBy { it.listId }.eachCount()
    var editing by remember { mutableStateOf<TaskList?>(null) }
    var creating by remember { mutableStateOf(false) }
    fun move(i: Int, d: Int) {
        val l = lists.toMutableList(); val j = i + d
        if (j !in l.indices) return
        l.add(j, l.removeAt(i)); scope.launch { app.repository.reorderLists(l) }
    }
    LazyColumn(Modifier.fillMaxSize().background(k.bg).statusBarsPadding(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 168.dp)) {
        item(key = "top") {
            IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes(), modifier = Modifier.padding(top = 8.dp)) {
                Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = c.onSurface)
            }
            Text("Edit lists", style = MaterialTheme.typography.headlineMedium, color = c.onSurface, modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 16.dp))
        }
        itemsIndexed(lists, key = { _, l -> l.id }) { i, l ->
            val big = 28.dp; val small = 8.dp
            SegmentedListItem(
                onClick = { editing = l },
                shapes = ListItemDefaults.shapes(
                    shape = RoundedCornerShape(
                        topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
                        bottomStart = if (i == lists.lastIndex) big else small, bottomEnd = if (i == lists.lastIndex) big else small,
                    ),
                    pressedShape = RoundedCornerShape(big),
                ),
                colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                modifier = Modifier.animateItem(placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
                leadingContent = { Icon(painterResource(R.drawable.ic_label_filled), null, tint = k.accent(l.color)) },
                supportingContent = { val n = open[l.id] ?: 0; Text("$n open task" + if (n == 1) "" else "s") },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (i > 0) IconButton(onClick = { move(i, -1) }, shapes = IconButtonDefaults.shapes()) {
                            Icon(painterResource(R.drawable.ic_arrow_upward), "Move ${l.name} up", tint = c.onSurfaceVariant)
                        }
                        Icon(painterResource(R.drawable.ic_edit), null, Modifier.padding(end = 12.dp).size(20.dp), tint = c.onSurfaceVariant)
                    }
                },
            ) { Text(l.name) }
            if (i < lists.lastIndex) Spacer(Modifier.height(3.dp))
        }
        item(key = "new") {
            SegmentedListItem(
                onClick = { creating = true },
                shapes = ListItemDefaults.shapes(shape = RoundedCornerShape(28.dp)),
                colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                modifier = Modifier.padding(top = 16.dp),
                leadingContent = { Icon(painterResource(R.drawable.ic_add), null, tint = c.primary) },
            ) { Text("New list", color = c.primary) }
        }
    }
    editing?.let { l -> ListSheet(l, open[l.id] ?: 0) { editing = null } }
    if (creating) ListSheet(null, 0) { creating = false }
}
