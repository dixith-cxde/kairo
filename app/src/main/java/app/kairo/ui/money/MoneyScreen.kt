@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.money

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.db.TxKind
import app.kairo.data.money.AccountBalance
import app.kairo.data.money.Money
import app.kairo.data.money.MoneyRepository
import app.kairo.ui.common.EmptyState
import app.kairo.ui.common.LocalOpenSearch
import app.kairo.ui.common.MoneySelectionToolbar
import app.kairo.ui.common.SearchPill
import app.kairo.ui.common.SelectionTopBar
import app.kairo.ui.common.TabHeader
import app.kairo.ui.common.countUp
import app.kairo.ui.common.rememberEntrance
import app.kairo.ui.common.rememberSelection
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Money tab (owner, 2026-11-21: "too much at once, let's have some space to breathe").
 * Shared tab header with the total balance and a connected month summary, then a connected
 * Activity / Accounts / Plan switcher. Only the chosen view is shown below it.
 */
@Composable
fun MoneyScreen(
    money: MoneyRepository,
    onAccount: (Long) -> Unit,
    onBudgets: () -> Unit,
    onInsights: () -> Unit,
    onRecurring: () -> Unit,
) {
    // null = still loading, so a fresh install's empty state doesn't flash for users with data.
    val balancesOrNull by money.balances.collectAsStateWithLifecycle(initialValue = null as List<AccountBalance>?)
    val balances = balancesOrNull.orEmpty()
    val txs by money.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val cats by money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val budgets by money.budgets.collectAsStateWithLifecycle(initialValue = emptyList())
    val recurring by money.recurring.collectAsStateWithLifecycle(initialValue = emptyList())
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val month = YearMonth.now()
    val thisMonth = remember(txs) { txs.filter { YearMonth.from(it.date) == month } }
    val income = thisMonth.filter { it.kind == TxKind.Income }.sumOf { it.amountPaise }
    val spent = thisMonth.filter { it.kind == TxKind.Expense }.sumOf { it.amountPaise }
    val total = balances.sumOf { it.balancePaise }
    val overall = budgets.firstOrNull { it.overall }
    val catMap = remember(cats) { cats.associateBy { it.id } }
    val accMap = remember(balances) { balances.associate { it.account.id to it.account } }
    // A transfer shows once (its outgoing leg).
    val days = remember(txs) { txs.filter { !(it.kind == TxKind.Transfer && it.amountPaise > 0) }.groupBy { it.date } }
    val selection = rememberSelection()
    val entrance = rememberEntrance()
    var addAccount by rememberSaveable { mutableStateOf(false) }

    // Chosen view survives rotation and process death. Switching slides the new view in from the
    // side it sits on (default spatial spring) while lazy rows fade (effects specs, see springItem).
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tab = MoneyTab.entries[tabIndex]
    val slide = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val select: (MoneyTab) -> Unit = { t ->
        if (t != tab) {
            val dir = if (t.ordinal > tab.ordinal) 1f else -1f
            selection.clear()
            tabIndex = t.ordinal
            scope.launch(start = CoroutineStart.UNDISPATCHED) { slide.snapTo(dir); slide.animateTo(0f, spatial) }
        }
    }
    val slideIn = Modifier.graphicsLayer { translationX = slide.value * 48.dp.toPx() }

    Scaffold(
        containerColor = k.bg,
        bottomBar = { MoneySelectionToolbar(selection, txs) },
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 140.dp), // the floating nav overlays the bottom
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "header") {
                    TabHeader(
                        title = null,
                        actions = { SearchPill(LocalOpenSearch.current) },
                        below = if (balancesOrNull.isNullOrEmpty()) null else {
                            {
                                Column(Modifier.padding(end = 4.dp)) {
                                    Text("Total balance", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                                    val shown by countUp((total / 100).toInt()) // whole rupees roll smoothly
                                    Text(
                                        Money.format(shown * 100L),
                                        style = MaterialTheme.typography.displaySmallEmphasized,
                                        color = if (total < 0) c.error else c.onSurface,
                                        maxLines = 1,
                                    )
                                }
                                val inc by countUp((income / 100).toInt())
                                val sp by countUp((spent / 100).toInt())
                                val budget = overall?.let { b ->
                                    val left = b.limitPaise - spent
                                    if (left >= 0) Summary("Left", Money.format(left), c.primary, onBudgets)
                                    else Summary("Over", Money.format(-left), c.error, onBudgets)
                                } ?: Summary("Budget", "Set up", c.primary, onBudgets)
                                SummaryGroup(
                                    listOf(
                                        Summary("Income", Money.format(inc * 100L), c.tertiary),
                                        Summary("Spent", Money.format(sp * 100L), c.onSurface),
                                        budget,
                                    ),
                                    Modifier.padding(end = 4.dp, bottom = 4.dp),
                                )
                            }
                        },
                    )
                }

                val loaded = balancesOrNull ?: return@LazyColumn
                if (loaded.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            icon = R.drawable.ic_account_balance_wallet,
                            title = "Start with an account",
                            body = "Add a bank account, card, wallet or cash, then track what comes in and goes out.",
                            shape = MaterialShapes.Cookie12Sided,
                            size = 200.dp,
                            modifier = springItem().padding(top = 24.dp),
                            action = {
                                Button(onClick = { addAccount = true }, shapes = ButtonDefaults.shapes()) {
                                    Icon(painterResource(R.drawable.ic_add_bold), null, Modifier.size(ButtonDefaults.IconSize))
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text("Add account")
                                }
                            },
                        )
                    }
                    return@LazyColumn
                }

                item(key = "switcher") {
                    MoneyTabSwitcher(tab, select, Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp))
                }

                when (tab) {
                    MoneyTab.Activity -> {
                        if (days.isEmpty()) item(key = "a-empty") {
                            EmptyState(
                                icon = R.drawable.ic_receipt_long,
                                title = "No activity yet",
                                body = "Spending and income you add with + show up here, by day.",
                                size = 160.dp,
                                modifier = springItem().then(slideIn),
                            )
                        }
                        days.forEach { (date, list) ->
                            item(key = "d$date") {
                                Text(
                                    dayLabel(date), style = MaterialTheme.typography.titleSmallEmphasized, color = c.primary,
                                    modifier = springItem().then(slideIn).padding(start = 20.dp, top = 24.dp, bottom = 8.dp),
                                )
                            }
                            list.forEachIndexed { i, tx ->
                                item(key = "t${tx.id}") {
                                    Column(springItem().then(slideIn).then(with(entrance) { Modifier.enter("t${tx.id}", i + 2) }).padding(horizontal = 16.dp)) {
                                        TxRow(
                                            tx, tx.categoryId?.let(catMap::get), accMap[tx.accountId], i, list.size,
                                            selected = tx.id in selection,
                                            selecting = selection.active,
                                            onLongClick = { selection.toggle(tx.id) },
                                        ) { if (selection.active) selection.toggle(tx.id) else onAccount(tx.accountId) }
                                        if (i < list.lastIndex) SegGap()
                                    }
                                }
                            }
                        }
                    }

                    MoneyTab.Accounts -> {
                        item(key = "acc-label") {
                            SectionLabel(
                                if (loaded.size == 1) "1 account" else "${loaded.size} accounts",
                                springItem().then(slideIn).padding(top = 16.dp, bottom = 8.dp),
                                trailing = Money.format(total),
                            )
                        }
                        loaded.forEachIndexed { i, b ->
                            item(key = "acc${b.account.id}") {
                                val acc = b.account
                                Column(springItem().then(slideIn).padding(horizontal = 16.dp)) {
                                    SegmentedListItem(
                                        onClick = { onAccount(acc.id) },
                                        shapes = ListItemDefaults.segmentedShapes(i, loaded.size),
                                        colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                                        leadingContent = {
                                            Box(
                                                Modifier.size(44.dp).background(k.container(acc.color), MaterialShapes.Cookie4Sided.toShape()),
                                                contentAlignment = Alignment.Center,
                                            ) { Icon(painterResource(accountIcon(acc.type)), null, tint = k.onContainer(acc.color), modifier = Modifier.size(22.dp)) }
                                        },
                                        supportingContent = { Text(acc.type.name) },
                                        trailingContent = {
                                            Text(
                                                Money.format(b.balancePaise),
                                                style = MaterialTheme.typography.titleMediumEmphasized,
                                                color = if (b.balancePaise < 0) c.error else c.onSurface,
                                            )
                                        },
                                    ) { Text(acc.name, style = MaterialTheme.typography.bodyLarge) }
                                    if (i < loaded.lastIndex) SegGap()
                                }
                            }
                        }
                        item(key = "acc-add") {
                            Box(springItem().then(slideIn).fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                                FilledTonalButton(onClick = { addAccount = true }, shapes = ButtonDefaults.shapes()) {
                                    Icon(painterResource(R.drawable.ic_add_bold), null, Modifier.size(ButtonDefaults.IconSize))
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text("Add account")
                                }
                            }
                        }
                    }

                    MoneyTab.Plan -> {
                        item(key = "plan-budget") {
                            PlanCard(
                                "Budgets", R.drawable.ic_savings, MaterialShapes.Cookie4Sided, onBudgets,
                                springItem().then(slideIn).padding(start = 16.dp, end = 16.dp, top = 16.dp),
                            ) {
                                if (overall == null) {
                                    Text("Set a monthly limit to see how much is left.", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant)
                                } else {
                                    val left = overall.limitPaise - spent
                                    Text(
                                        if (left >= 0) "${Money.format(left)} left of ${Money.format(overall.limitPaise)}"
                                        else "${Money.format(-left)} over ${Money.format(overall.limitPaise)}",
                                        style = MaterialTheme.typography.titleSmallEmphasized,
                                        color = if (left >= 0) c.onSurface else c.error,
                                    )
                                    LinearWavyProgressIndicator(
                                        progress = { (spent.toFloat() / overall.limitPaise).coerceIn(0f, 1f) },
                                        color = if (left >= 0) c.primary else c.error,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                val others = budgets.count { !it.overall }
                                if (others > 0) Text(
                                    if (others == 1) "1 category or account budget" else "$others category and account budgets",
                                    style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant,
                                )
                            }
                        }
                        item(key = "plan-bills") {
                            val upcoming = recurring.sortedBy { it.nextDue }.take(3)
                            val fmt = remember { DateTimeFormatter.ofPattern("d MMM") }
                            PlanCard(
                                "Bills & subscriptions", R.drawable.ic_repeat, MaterialShapes.Clover4Leaf, onRecurring,
                                springItem().then(slideIn).padding(start = 16.dp, end = 16.dp, top = 16.dp),
                            ) {
                                if (upcoming.isEmpty()) {
                                    Text("Add rent, subscriptions or salary so nothing sneaks up on you.", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant)
                                } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    upcoming.forEach { r ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CategoryBadge(r.categoryId?.let(catMap::get), size = 32)
                                            Spacer(Modifier.size(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(r.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                                Text("due ${r.nextDue.format(fmt)}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                                            }
                                            Text(Money.format(r.amountPaise), style = MaterialTheme.typography.titleSmallEmphasized)
                                        }
                                    }
                                }
                            }
                        }
                        item(key = "plan-insights") {
                            val top = remember(thisMonth) {
                                thisMonth.filter { it.kind == TxKind.Expense }.groupBy { it.categoryId }
                                    .mapValues { (_, l) -> l.sumOf { it.amountPaise } }.maxByOrNull { it.value }
                            }
                            PlanCard(
                                "Insights", R.drawable.ic_pie_chart, MaterialShapes.Cookie7Sided, onInsights,
                                springItem().then(slideIn).padding(start = 16.dp, end = 16.dp, top = 16.dp),
                            ) {
                                if (top == null || spent == 0L) {
                                    Text("Where your money goes, once you add some spending this month.", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant)
                                } else {
                                    val cat = top.key?.let(catMap::get)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CategoryBadge(cat, size = 32)
                                        Spacer(Modifier.size(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text("Most this month: ${cat?.name ?: "Other"}", style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                            Text("${top.value * 100 / spent}% of spending", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                                        }
                                        Text(Money.format(top.value), style = MaterialTheme.typography.titleSmallEmphasized)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            SelectionTopBar(selection, txs.map { it.id }, Modifier.align(Alignment.TopCenter))
        }
    }
    if (addAccount) AccountSheet(money, null, canDelete = false, onDismiss = { addAccount = false })
}
