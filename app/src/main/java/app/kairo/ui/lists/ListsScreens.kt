@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.lists

import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import app.kairo.ui.shapes.polygon
import app.kairo.ui.theme.KairoTheme
import app.kairo.ui.tasks.TaskRow
import app.kairo.ui.common.SelectionTopBar
import app.kairo.ui.common.TaskSelectionToolbar
import app.kairo.ui.common.rememberEntrance
import app.kairo.ui.common.rememberSelection

// ───────────────────────── Lists tab ─────────────────────────

/** The list's own shape in its container color — "each list owns a shape" (rule 7). */
@Composable
fun ListBadge(l: TaskList, size: Int = 40) {
    val k = KairoTheme.colors
    Box(Modifier.size(size.dp).background(k.container(l.color), l.shape.polygon.toShape()), contentAlignment = Alignment.Center) {
        Text(l.name.take(1), style = MaterialTheme.typography.titleSmallEmphasized, color = k.onContainer(l.color))
    }
}

// ───────────────────────── List detail ─────────────────────────

@Composable
fun ListDetailScreen(vm: ListDetailViewModel, onBack: () -> Unit, onOpenTask: (Task) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val s = state ?: return
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val selection = rememberSelection()
    val entrance = rememberEntrance()

    Scaffold(
        containerColor = k.bg,
        bottomBar = {
            TaskSelectionToolbar(
                selection, s.sections.flatMap { it.second }, s.lists.values.toList(), onOpenTask,
                archivedView = s.smart == SmartList.Archived,
            )
        },
        topBar = {
          Box {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        s.list?.let { ListBadge(it, 32) }
                        Text(s.title, style = MaterialTheme.typography.titleLargeEmphasized)
                    }
                },
                navigationIcon = { IconButton(shapes = IconButtonDefaults.shapes(), onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = k.bg),
            )
            SelectionTopBar(selection, s.sections.flatMap { it.second }.map { it.id })
          }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 120.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Same Expressive connected group as Stats and Money (owner, 2026-11-21).
            if (s.smart != SmartList.Completed && s.smart != SmartList.Archived) item(key = "filters") {
                app.kairo.ui.common.ConnectedGroup(ListFilter.entries, filter, { it.label }, Modifier.padding(top = 4.dp)) { vm.filter.value = it }
            }
            if (s.sections.isEmpty()) item(key = "empty") {
                val (title, body) = when {
                    s.smart == SmartList.Completed -> "Nothing finished yet" to "Tasks you tick off land here."
                    s.smart == SmartList.Archived -> "Nothing archived" to "Archive a task from its ⋮ menu to tuck it away here."
                    filter == ListFilter.Done -> "Nothing done yet" to "Tick something off and it shows here."
                    filter == ListFilter.Upcoming -> "Nothing coming up" to "Tasks with a date ahead show here."
                    filter == ListFilter.NoDate -> "Everything has a date" to "Tasks without a date show here."
                    else -> "All clear" to "Add a task with + and it lands in ${s.title}."
                }
                app.kairo.ui.common.EmptyState(title, body, Modifier.padding(top = 24.dp), size = 180.dp)
            }
            s.sections.forEach { (title, tasks) ->
                item(key = "h-$title") {
                    Text(
                        "$title · ${tasks.size}", style = MaterialTheme.typography.titleSmallEmphasized,
                        color = if (title == "Overdue") c.error else c.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
                    )
                }
                itemsIndexed(tasks, key = { _, t -> "$title-${t.id}" }) { i, t ->
                    Column(Modifier.animateItem(
                        fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                        placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                        fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                    ).then(with(entrance) { Modifier.enter("$title-${t.id}", i) })) {
                        TaskRow(t, s.lists[t.listId] ?: TaskList.None, i, tasks.size, vm::toggle, onOpenTask, overdue = title == "Overdue", selection = selection)
                        if (i < tasks.lastIndex) Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
                    }
                }
            }
        }
    }
}
