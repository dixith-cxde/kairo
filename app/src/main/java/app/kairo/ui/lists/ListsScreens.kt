@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import app.kairo.ui.shapes.polygon
import app.kairo.ui.theme.KairoTheme
import app.kairo.ui.today.TaskRow

// ───────────────────────── Lists tab ─────────────────────────

@Composable
fun ListsScreen(vm: ListsViewModel, onOpenList: (Long) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var creating by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = k.bg,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Lists") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = k.bg, scrolledContainerColor = k.bg),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 120.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Smart lists: 2×2 tiles, Cookie4 icons, big tabular counts.
            item(key = "smart") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartList.entries.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { sl ->
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(k.item)
                                        .clickable(role = Role.Button) { onOpenList(sl.id) }
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(40.dp).background(c.secondaryContainer, MaterialShapes.Cookie4Sided.toShape()),
                                            contentAlignment = Alignment.Center,
                                        ) { Icon(painterResource(sl.icon), null, tint = c.onSecondaryContainer, modifier = Modifier.size(20.dp)) }
                                        Spacer(Modifier.weight(1f))
                                        Text("${s.smartCounts[sl] ?: 0}", style = MaterialTheme.typography.headlineMediumEmphasized)
                                    }
                                    Text(sl.label, style = MaterialTheme.typography.titleSmallEmphasized, color = c.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            item(key = "h-mine") {
                Text(
                    "My lists", style = MaterialTheme.typography.titleSmallEmphasized, color = c.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp),
                )
            }
            val count = s.lists.size + 1
            itemsIndexed(s.lists, key = { _, l -> l.id }) { i, l ->
                Column {
                    SegmentedListItem(
                        onClick = { onOpenList(l.id) },
                        shapes = ListItemDefaults.segmentedShapes(i, count),
                        colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                        leadingContent = { ListBadge(l) },
                        trailingContent = {
                            Text("${s.openCounts[l.id] ?: 0}", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                        },
                    ) { Text(l.name, style = MaterialTheme.typography.bodyLarge) }
                    Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
                }
            }
            item(key = "new") {
                SegmentedListItem(
                    onClick = { creating = true },
                    shapes = ListItemDefaults.segmentedShapes(count - 1, count),
                    colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                    leadingContent = {
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_add), null, tint = c.primary)
                        }
                    },
                ) { Text("New list", style = MaterialTheme.typography.bodyLarge, color = c.primary) }
            }
        }
    }
    if (creating) NewListDialog(onDismiss = { creating = false }, onCreate = { n, sh, co -> vm.create(n, sh, co); creating = false })
}

/** The list's own shape in its container color — "each list owns a shape" (rule 7). */
@Composable
fun ListBadge(l: TaskList, size: Int = 40) {
    val k = KairoTheme.colors
    Box(Modifier.size(size.dp).background(k.container(l.color), l.shape.polygon.toShape()), contentAlignment = Alignment.Center) {
        Text(l.name.take(1), style = MaterialTheme.typography.titleSmallEmphasized, color = k.onContainer(l.color))
    }
}

@Composable
private fun NewListDialog(onDismiss: () -> Unit, onCreate: (String, ListShape, ListColor) -> Unit) {
    val k = KairoTheme.colors
    var name by rememberSaveable { mutableStateOf("") }
    var shape by rememberSaveable { mutableStateOf(ListShape.Cookie9) }
    var color by rememberSaveable { mutableStateOf(ListColor.Indigo) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New list") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListShape.entries.forEach { sh ->
                        val sel = sh == shape
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(if (sel) k.container(color) else MaterialTheme.colorScheme.surfaceContainerHighest, sh.polygon.toShape())
                                .clickable(role = Role.RadioButton) { shape = sh }
                                .semantics { contentDescription = sh.name },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListColor.entries.forEach { co ->
                        Box(
                            Modifier
                                .size(if (co == color) 32.dp else 28.dp)
                                .background(k.accent(co), CircleShape)
                                .clickable(role = Role.RadioButton) { color = co }
                                .semantics { contentDescription = co.name },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name, shape, color) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ───────────────────────── List detail ─────────────────────────

@Composable
fun ListDetailScreen(vm: ListDetailViewModel, onBack: () -> Unit, onOpenTask: (Task) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val s = state ?: return
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme

    Scaffold(
        containerColor = k.bg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        s.list?.let { ListBadge(it, 32) }
                        Text(s.title, style = MaterialTheme.typography.titleLargeEmphasized)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = k.bg),
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 120.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (s.smart != SmartList.Completed) item(key = "filters") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListFilter.entries.forEach { f ->
                        FilterChip(selected = f == filter, onClick = { vm.filter.value = f }, label = { Text(f.label) })
                    }
                }
            }
            if (s.sections.isEmpty()) item(key = "empty") {
                Text(
                    "Nothing here.", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 24.dp),
                )
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
                    )) {
                        TaskRow(t, s.lists.getValue(t.listId), i, tasks.size, vm::toggle, onOpenTask, overdue = title == "Overdue")
                        if (i < tasks.lastIndex) Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
                    }
                }
            }
        }
    }
}
