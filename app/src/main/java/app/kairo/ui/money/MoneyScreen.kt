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
import app.kairo.ui.common.MoneySelectionBar
import app.kairo.ui.common.countUp
import app.kairo.ui.common.rememberEntrance
import app.kairo.ui.common.rememberSelection
import app.kairo.ui.common.slowSpin
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
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
    val selection = rememberSelection()
    val entrance = rememberEntrance()
    val spin by slowSpin(50_000)

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = k.bg,
        topBar = {
          Box {
            LargeFlexibleTopAppBar(
                title = { Text("Money") },
                subtitle = { Text("Total ${Money.format(balances.sumOf { it.balancePaise })}") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = k.bg, scrolledContainerColor = k.bg),
                scrollBehavior = scroll,
            )
            MoneySelectionBar(selection, txs)
          }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 120.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // M3 carousel of accounts.
            // Always present (even before data loads), so the list doesn't anchor below it.
            item(key = "accounts") {
                if (balances.isEmpty()) { androidx.compose.foundation.layout.Spacer(Modifier.height(150.dp)); return@item }
                val state = rememberCarouselState { balances.size }
                HorizontalMultiBrowseCarousel(
                    state = state,
                    preferredItemWidth = 220.dp,
                    itemSpacing = 8.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                ) { i ->
                    val b = balances[i]
                    val acc = b.account
                    val info = carouselItemDrawInfo
                    // How "open" this card is (small edge card = 0, full card = 1). Text fades with it
                    // instead of being cut off; content follows the visible mask's left edge.
                    val open = ((info.size - info.minSize) / (info.maxSize - info.minSize).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    val on = k.onContainer(acc.color)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .maskClip(RoundedCornerShape(28.dp))
                            .background(k.container(acc.color))
                            .clickable(role = Role.Button) { onAccount(acc.id) },
                    ) {
                        // Big soft shape in the corner, turning slowly: the account's "mark".
                        Box(
                            Modifier.align(Alignment.BottomEnd)
                                .graphicsLayer { translationX = 40.dp.toPx() + info.maskRect.left; translationY = 50.dp.toPx(); rotationZ = spin }
                                .size(150.dp)
                                .background(on.copy(alpha = 0.08f), MaterialShapes.Cookie9Sided.toShape()),
                        )
                        Column(
                            Modifier.fillMaxSize().graphicsLayer { translationX = info.maskRect.left }.padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Box(
                                Modifier.size(40.dp).background(on.copy(alpha = 0.14f), MaterialShapes.Cookie4Sided.toShape()),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(painterResource(accountIcon(acc.type)), null, tint = on, modifier = Modifier.size(22.dp))
                            }
                            Column(Modifier.graphicsLayer { alpha = ((open - 0.5f) * 2f).coerceIn(0f, 1f) }) {
                                Text(acc.name, style = MaterialTheme.typography.labelLarge, color = on.copy(alpha = 0.8f), maxLines = 1)
                                Text(
                                    Money.format(b.balancePaise),
                                    style = MaterialTheme.typography.headlineSmallEmphasized,
                                    color = on, maxLines = 1, softWrap = false,
                                )
                            }
                        }
                    }
                }
            }
            item(key = "tiles") {
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Amounts count up to their value (in whole rupees, so the digits roll smoothly).
                    val inc by countUp((income / 100).toInt())
                    val sp by countUp((spent / 100).toInt())
                    Tile("Income", Money.format(inc * 100L), c.tertiary, with(entrance) { Modifier.enter("income", 1) }.weight(1f))
                    Tile("Spent", Money.format(sp * 100L), c.onSurface, with(entrance) { Modifier.enter("spent", 2) }.weight(1f))
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
                        Column(with(entrance) { Modifier.enter("t${tx.id}", i + 4) }.animateItem().padding(horizontal = 16.dp)) {
                            TxRow(
                                tx, tx.categoryId?.let(catMap::get), accMap[tx.accountId], i, list.size,
                                selected = tx.id in selection,
                                onLongClick = { selection.toggle(tx.id) },
                            ) { if (selection.active) selection.toggle(tx.id) else onAccount(tx.accountId) }
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
    FilledTonalButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 8.dp)) {
        Icon(painterResource(icon), null, Modifier.size(18.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        Text(label, maxLines = 1, softWrap = false)
    }
}

