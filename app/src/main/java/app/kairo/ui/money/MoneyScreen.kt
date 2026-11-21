@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.db.TxKind
import app.kairo.data.money.Money
import app.kairo.data.money.MoneyRepository
import app.kairo.ui.theme.KairoTheme
import java.time.YearMonth

@Composable
fun MoneyScreen(
    money: MoneyRepository,
    onAccount: (Long) -> Unit,
    onBudgets: () -> Unit,
    onInsights: () -> Unit,
    onRecurring: () -> Unit,
) {
    val balances by money.balances.collectAsStateWithLifecycle(initialValue = emptyList())
    val txs by money.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val cats by money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val budgets by money.budgets.collectAsStateWithLifecycle(initialValue = emptyList())
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val month = YearMonth.now()
    val thisMonth = remember(txs) { txs.filter { YearMonth.from(it.date) == month } }
    val income = thisMonth.filter { it.kind == TxKind.Income }.sumOf { it.amountPaise }
    val spent = thisMonth.filter { it.kind == TxKind.Expense }.sumOf { it.amountPaise }
    val overall = budgets.firstOrNull { it.categoryId == null }
    val catMap = remember(cats) { cats.associateBy { it.id } }
    val accMap = remember(balances) { balances.associate { it.account.id to it.account } }
    val recent = remember(txs) { txs.filter { !(it.kind == TxKind.Transfer && it.amountPaise > 0) }.take(20).groupBy { it.date } }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = k.bg,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Money") },
                subtitle = { Text("Total ${Money.format(balances.sumOf { it.balancePaise })}") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = k.bg, scrolledContainerColor = k.bg),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 120.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // M3 carousel of accounts.
            if (balances.isNotEmpty()) item(key = "accounts") {
                val state = rememberCarouselState { balances.size }
                HorizontalMultiBrowseCarousel(
                    state = state,
                    preferredItemWidth = 220.dp,
                    itemSpacing = 8.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                ) { i ->
                    val b = balances[i]
                    Column(
                        Modifier
                            .fillMaxSize()
                            .maskClip(RoundedCornerShape(28.dp))
                            .background(k.container(b.account.color))
                            .clickable(role = Role.Button) { onAccount(b.account.id) }
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(b.account.name, style = MaterialTheme.typography.titleMediumEmphasized, color = k.onContainer(b.account.color), maxLines = 1)
                        Text(
                            Money.format(b.balancePaise),
                            style = MaterialTheme.typography.headlineSmallEmphasized,
                            color = k.onContainer(b.account.color), maxLines = 1, overflow = TextOverflow.Clip,
                        )
                    }
                }
            }
            item(key = "tiles") {
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tile("Income", Money.format(income), c.tertiary, Modifier.weight(1f))
                    Tile("Spent", Money.format(spent), c.onSurface, Modifier.weight(1f))
                }
            }
            overall?.let { b ->
                item(key = "budget") {
                    val left = b.limitPaise - spent
                    Column(
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp)).background(k.item)
                            .clickable(role = Role.Button, onClick = onBudgets).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            if (left >= 0) "${Money.format(left)} left of ${Money.format(b.limitPaise)}"
                            else "${Money.format(-left)} over ${Money.format(b.limitPaise)}",
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            color = if (left >= 0) c.onSurface else c.error,
                        )
                        LinearWavyProgressIndicator(
                            progress = { (spent.toFloat() / b.limitPaise).coerceIn(0f, 1f) },
                            color = if (left >= 0) c.primary else c.error,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item(key = "links") {
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinkButton("Budgets", R.drawable.ic_savings, onBudgets, Modifier.weight(1f))
                    LinkButton("Insights", R.drawable.ic_pie_chart, onInsights, Modifier.weight(1f))
                    LinkButton("Bills", R.drawable.ic_repeat, onRecurring, Modifier.weight(1f))
                }
            }
            recent.forEach { (date, list) ->
                item(key = "d$date") {
                    Text(
                        dayLabel(date), style = MaterialTheme.typography.titleSmallEmphasized, color = c.primary,
                        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
                    )
                }
                list.forEachIndexed { i, tx ->
                    item(key = "t${tx.id}") {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            TxRow(tx, tx.categoryId?.let(catMap::get), accMap[tx.accountId], i, list.size) { onAccount(tx.accountId) }
                            if (i < list.lastIndex) SegGap()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(28.dp)).background(KairoTheme.colors.item).padding(16.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLargeEmphasized, color = color, maxLines = 1)
    }
}

@Composable
private fun LinkButton(label: String, icon: Int, onClick: () -> Unit, modifier: Modifier) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(icon), null, Modifier.size(18.dp))
        Text(" $label", maxLines = 1)
    }
}

