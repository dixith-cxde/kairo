@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.KairoApplication
import app.kairo.R
import app.kairo.data.model.Task
import app.kairo.ui.common.rememberEntrance
import app.kairo.ui.lists.ListBadge
import app.kairo.ui.money.TxRow
import app.kairo.ui.theme.KairoTheme
import app.kairo.ui.today.TaskRow
import kotlinx.coroutines.launch

/** One search for everything: tasks (title, notes), lists, and money (title, tags, category). */
@Composable
fun SearchScreen(app: KairoApplication, onBack: () -> Unit, onOpenTask: (Task) -> Unit, onOpenList: (Long) -> Unit, onOpenAccount: (Long) -> Unit) {
    val query = rememberTextFieldState()
    val bar = rememberSearchBarState(initialValue = SearchBarValue.Expanded)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val scope = rememberCoroutineScope()
    val tasks by app.repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val lists by app.repository.lists.collectAsStateWithLifecycle(initialValue = emptyList())
    val txs by app.money.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val cats by app.money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by app.money.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val entrance = rememberEntrance()

    val q = query.text.toString().trim()
    val listMap = lists.associateBy { it.id }
    val catMap = cats.associateBy { it.id }
    val accMap = accounts.associateBy { it.id }
    val foundTasks = if (q.isEmpty()) emptyList() else tasks.filter { it.title.contains(q, true) || it.notes.contains(q, true) }.take(30)
    val foundLists = if (q.isEmpty()) emptyList() else lists.filter { it.name.contains(q, true) }
    val foundTx = if (q.isEmpty()) emptyList() else txs.filter { t ->
        t.note.contains(q, true) || t.tags.contains(q, true) || (t.categoryId?.let { catMap[it]?.name?.contains(q, true) } == true)
    }.take(30)

    Column(Modifier.fillMaxSize().padding(top = 8.dp).statusBarsPadding()) {
        // M3 SearchBar with its input field; back arrow leaves search.
        SearchBar(
            state = bar,
            inputField = {
                SearchBarDefaults.InputField(
                    textFieldState = query,
                    searchBarState = bar,
                    onSearch = {},
                    placeholder = { Text("Search tasks, lists, money") },
                    leadingIcon = {
                        IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") }
                    },
                    trailingIcon = if (q.isNotEmpty()) {
                        { IconButton(onClick = { query.edit { replace(0, length, "") } }, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_close), "Clear") } }
                    } else null,
                    modifier = Modifier.focusRequester(focus),
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp)) {
            if (q.isNotEmpty() && foundTasks.isEmpty() && foundLists.isEmpty() && foundTx.isEmpty()) item {
                Text("Nothing matches “$q”.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp))
            }
            if (foundLists.isNotEmpty()) {
                item { Header("Lists") }
                itemsIndexed(foundLists, key = { _, l -> "l${l.id}" }) { i, l ->
                    Column(with(entrance) { Modifier.enter("l${l.id}", i) }) {
                        SegmentedListItem(
                            onClick = { onOpenList(l.id) },
                            shapes = ListItemDefaults.segmentedShapes(i, foundLists.size),
                            colors = ListItemDefaults.segmentedColors(containerColor = KairoTheme.colors.item),
                            leadingContent = { ListBadge(l) },
                        ) { Text(l.name) }
                        if (i < foundLists.lastIndex) Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
                    }
                }
            }
            if (foundTasks.isNotEmpty()) {
                item { Header("Tasks") }
                itemsIndexed(foundTasks, key = { _, t -> "t${t.id}" }) { i, t ->
                    Column(with(entrance) { Modifier.enter("t${t.id}", i) }) {
                        (listMap[t.listId] ?: app.kairo.data.model.TaskList.None).let { l ->
                            TaskRow(t, l, i, foundTasks.size, onToggle = { scope.launch { app.repository.setDone(it, !it.done) } }, onOpen = onOpenTask)
                        }
                        if (i < foundTasks.lastIndex) Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
                    }
                }
            }
            if (foundTx.isNotEmpty()) {
                item { Header("Money") }
                itemsIndexed(foundTx, key = { _, t -> "x${t.id}" }) { i, t ->
                    Column(with(entrance) { Modifier.enter("x${t.id}", i) }) {
                        TxRow(t, t.categoryId?.let(catMap::get), accMap[t.accountId], i, foundTx.size) { onOpenAccount(t.accountId) }
                        if (i < foundTx.lastIndex) Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(text: String) = Text(
    text, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
)
