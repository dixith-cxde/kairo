@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.lists

import app.kairo.ui.common.TabHeader
import app.kairo.ui.common.fullBleed
import app.kairo.ui.common.LocalOpenSearch
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import app.kairo.ui.shapes.polygon
import app.kairo.ui.theme.KairoTheme
import app.kairo.ui.today.TaskRow
import app.kairo.ui.common.SelectionTopBar
import app.kairo.ui.common.TaskSelectionToolbar
import app.kairo.ui.common.countUp
import app.kairo.ui.common.rememberEntrance
import app.kairo.ui.common.rememberSelection

// ───────────────────────── Lists tab ─────────────────────────

@Composable
fun ListsScreen(vm: ListsViewModel, onOpenList: (Long) -> Unit, newListSignal: Int = 0) {
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var creating by rememberSaveable { mutableStateOf(false) }
    val entrance = rememberEntrance()
    // The create menu's "List" item opens the dialog here.
    androidx.compose.runtime.LaunchedEffect(newListSignal) { if (newListSignal > 0) creating = true }

    Scaffold(
        containerColor = k.bg,
    ) { _ ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") {
                TabHeader("Lists", Modifier.fullBleed())
            }
            // Smart lists (owner, 2026-11-21): 2×2 tiles with Archived full width under them, cut as one
            // piece. Only the block's four outer corners are large; every inner corner is small, 2dp gaps.
            item(key = "smart") {
                val big = 28.dp
                val small = 6.dp
                val gap = ListItemDefaults.SegmentedGap
                fun corners(tl: Boolean = false, tr: Boolean = false, bl: Boolean = false, br: Boolean = false) = RoundedCornerShape(
                    topStart = if (tl) big else small, topEnd = if (tr) big else small,
                    bottomStart = if (bl) big else small, bottomEnd = if (br) big else small,
                )
                val rows = listOf(
                    listOf(SmartList.Inbox to corners(tl = true), SmartList.Today to corners(tr = true)),
                    listOf(SmartList.Next7 to corners(), SmartList.Completed to corners()),
                    listOf(SmartList.Archived to corners(bl = true, br = true)),
                )
                Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
                    var n = 0
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            row.forEach { (sl, shape) ->
                                SmartTile(sl, s.smartCounts[sl] ?: 0, shape, with(entrance) { Modifier.enter("tile-$sl", n++) }.weight(1f)) { onOpenList(sl.id) }
                            }
                        }
                    }
                }
            }
            // "My lists" header with the New list action right beside it (owner, 2026-11-21).
            item(key = "h-mine") {
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("My lists", style = MaterialTheme.typography.titleSmallEmphasized, color = c.primary, modifier = Modifier.weight(1f))
                    androidx.compose.material3.FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { creating = true }) {
                        Icon(painterResource(R.drawable.ic_add), null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("New list")
                    }
                }
            }
            val count = s.lists.size
            itemsIndexed(s.lists, key = { _, l -> l.id }) { i, l ->
                Column(with(entrance) { Modifier.enter("list-${l.id}", i + 5) }) {
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
        }
    }
    if (creating) NewListDialog(onDismiss = { creating = false }, onCreate = { n, sh, co -> vm.create(n, sh, co); creating = false })
}

/** One smart-list tile: Cookie4 icon, big tabular count, label. */
@Composable
private fun SmartTile(sl: SmartList, count: Int, shape: androidx.compose.ui.graphics.Shape, modifier: Modifier, onClick: () -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    Column(
        modifier.clip(shape).background(k.item).clickable(role = Role.Button, onClickLabel = "Open ${sl.label}", onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(c.secondaryContainer, MaterialShapes.Cookie4Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(sl.icon), null, tint = c.onSecondaryContainer, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.weight(1f))
            val n by countUp(count)
            Text("$n", style = MaterialTheme.typography.headlineMediumEmphasized, maxLines = 1)
        }
        Text(sl.label, style = MaterialTheme.typography.titleSmallEmphasized, color = c.onSurfaceVariant, maxLines = 1, softWrap = false,
            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(12.sp, 14.sp))
    }
}

/** The list's own shape in its container color — "each list owns a shape" (rule 7). */
@Composable
fun ListBadge(l: TaskList, size: Int = 40) {
    val k = KairoTheme.colors
    Box(Modifier.size(size.dp).background(k.container(l.color), l.shape.polygon.toShape()), contentAlignment = Alignment.Center) {
        Text(l.name.take(1), style = MaterialTheme.typography.titleSmallEmphasized, color = k.onContainer(l.color))
    }
}

/**
 * New list drawer: live preview of the list's badge, name, the 8 list shapes (picked one morphs
 * in its color), the 7 colors, Create. A bottom sheet, not a dialog (owner, 2026-11-21).
 */
@Composable
private fun NewListDialog(onDismiss: () -> Unit, onCreate: (String, ListShape, ListColor) -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    var name by rememberSaveable { mutableStateOf("") }
    var shape by rememberSaveable { mutableStateOf(ListShape.Hexagon) }
    var color by rememberSaveable { mutableStateOf(ListColor.Indigo) }
    val sheet = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) { focus.requestFocus() }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Live preview: the badge morphs as you pick a shape.
                androidx.compose.animation.AnimatedContent(shape to color, label = "preview") { (sh, co) ->
                    Box(Modifier.size(56.dp).background(k.container(co), sh.polygon.toShape()), contentAlignment = Alignment.Center) {
                        Text(name.take(1).uppercase().ifEmpty { "A" }, style = MaterialTheme.typography.titleLargeEmphasized, color = k.onContainer(co))
                    }
                }
                Text("New list", style = MaterialTheme.typography.headlineSmallEmphasized)
            }
            androidx.compose.material3.TextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { if (name.isNotBlank()) onCreate(name, shape, color) }),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Text("Shape", style = MaterialTheme.typography.titleSmallEmphasized, color = c.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ListShape.choices.forEach { sh ->
                    val on = sh == shape
                    val scale by androidx.compose.animation.core.animateFloatAsState(if (on) 1.15f else 1f, motion.fastSpatialSpec(), label = "sh")
                    Box(
                        Modifier.size(36.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                            .background(if (on) k.accent(color) else c.surfaceContainerHighest, sh.polygon.toShape())
                            .clickable(role = Role.RadioButton) { shape = sh }
                            .semantics { contentDescription = sh.name; selected = on },
                    )
                }
            }
            Text("Color", style = MaterialTheme.typography.titleSmallEmphasized, color = c.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ListColor.entries.forEach { co ->
                    val on = co == color
                    val size by androidx.compose.animation.core.animateDpAsState(if (on) 36.dp else 28.dp, motion.fastSpatialSpec(), label = "co")
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.size(size).background(k.accent(co), CircleShape)
                                .clickable(role = Role.RadioButton) { color = co }
                                .semantics { contentDescription = co.name; selected = on },
                            contentAlignment = Alignment.Center,
                        ) { if (on) Icon(painterResource(R.drawable.ic_check), null, tint = c.surface, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
            androidx.compose.material3.Button(
                onClick = { onCreate(name, shape, color) },
                enabled = name.isNotBlank(),
                shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Create list", style = MaterialTheme.typography.titleMediumEmphasized) }
        }
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
