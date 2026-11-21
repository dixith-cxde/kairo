@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.money

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.db.RecurringKind
import app.kairo.data.db.TxKind
import app.kairo.data.money.Money
import app.kairo.data.money.MoneyRepository
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
private fun DetailScaffold(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        containerColor = KairoTheme.colors.bg,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLargeEmphasized) },
                navigationIcon = { IconButton(shapes = IconButtonDefaults.shapes(), onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KairoTheme.colors.bg),
            )
        },
        content = content,
    )
}

// ───────── Account ─────────

@Composable
fun AccountScreen(money: MoneyRepository, accountId: Long, onBack: () -> Unit) {
    val balances by money.balances.collectAsStateWithLifecycle(initialValue = emptyList())
    val txs by money.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val cats by money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val b = balances.firstOrNull { it.account.id == accountId } ?: return
    val k = KairoTheme.colors
    val catMap = remember(cats) { cats.associateBy { it.id } }
    val mine = remember(txs) { txs.filter { it.accountId == accountId }.groupBy { it.date } }
    DetailScaffold(b.account.name, onBack) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 48.dp)) {
            item(key = "hero") {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(k.container(b.account.color)).padding(20.dp),
                ) {
                    Text("Balance", style = MaterialTheme.typography.labelLarge, color = k.onContainer(b.account.color))
                    Text(Money.format(b.balancePaise), style = MaterialTheme.typography.displaySmallEmphasized, color = k.onContainer(b.account.color))
                    Text(b.account.type.name, style = MaterialTheme.typography.bodyMedium, color = k.onContainer(b.account.color))
                }
            }
            mine.forEach { (date, list) ->
                item(key = "d$date") {
                    Text(dayLabel(date), style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp))
                }
                list.forEachIndexed { i, tx ->
                    item(key = "t${tx.id}") {
                        Column {
                            TxRow(tx, tx.categoryId?.let(catMap::get), b.account, i, list.size)
                            if (i < list.lastIndex) SegGap()
                        }
                    }
                }
            }
        }
    }
}

// ───────── Budgets ─────────

@Composable
fun BudgetsScreen(money: MoneyRepository, onBack: () -> Unit) {
    val txs by money.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val cats by money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val budgets by money.budgets.collectAsStateWithLifecycle(initialValue = emptyList())
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val ym = YearMonth.now()
    val spentBy = remember(txs) {
        txs.filter { it.kind == TxKind.Expense && YearMonth.from(it.date) == ym }.groupBy { it.categoryId }
            .mapValues { (_, l) -> l.sumOf { it.amountPaise } }
    }
    val spent = spentBy.values.sum()
    val overall = budgets.firstOrNull { it.categoryId == null }
    val daysLeft = ym.lengthOfMonth() - LocalDate.now().dayOfMonth + 1
    DetailScaffold("Budgets", onBack) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 48.dp)) {
            overall?.let { b ->
                item(key = "jar") {
                    val left = b.limitPaise - spent
                    val fill = (left.toFloat() / b.limitPaise).coerceIn(0f, 1f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        LiquidJar(fill, Modifier.size(200.dp))
                        Text(
                            if (left >= 0) "${Money.format(left)} left of ${Money.format(b.limitPaise)}"
                            else "${Money.format(-left)} over ${Money.format(b.limitPaise)}",
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = if (left >= 0) c.onSurface else c.error,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        if (left > 0) Text(
                            "$daysLeft days to go · ≈ ${Money.format(left / daysLeft / 100 * 100)} a day",
                            style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant,
                        )
                    }
                }
            }
            val rows = budgets.filter { it.categoryId != null }
            rows.forEachIndexed { i, b ->
                val cat = cats.firstOrNull { it.id == b.categoryId } ?: return@forEachIndexed
                val s = spentBy[b.categoryId] ?: 0
                val over = s > b.limitPaise
                item(key = "b${b.id}") {
                    Column {
                        SegmentedListItem(
                            shapes = ListItemDefaults.segmentedShapes(i, rows.size),
                            colors = ListItemDefaults.segmentedColors(containerColor = if (over) c.errorContainer else k.item),
                            leadingContent = { CategoryBadge(cat) },
                            supportingContent = {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${Money.format(s)} of ${Money.format(b.limitPaise)}",
                                        color = if (over) c.onErrorContainer else c.onSurfaceVariant)
                                    LinearWavyProgressIndicator(
                                        progress = { (s.toFloat() / b.limitPaise).coerceIn(0f, 1f) },
                                        color = if (over) c.error else k.accent(cat.color),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            },
                        ) { Text(cat.name, color = if (over) c.onErrorContainer else c.onSurface) }
                        if (i < rows.lastIndex) SegGap()
                    }
                }
            }
        }
    }
}

/** Liquid jar: a Cookie9 shape filled to `fill` with a wavy surface (hero, canvas Budgets). */
@Composable
private fun LiquidJar(fill: Float, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    val level by animateFloatAsState(fill, MaterialTheme.motionScheme.slowSpatialSpec(), label = "jar")
    val jar = MaterialShapes.Cookie9Sided.toPath() // composable; remembers internally
    Canvas(modifier.semantics { contentDescription = "${(fill * 100).roundToInt()}% of budget left" }) {
        val p = Path().apply { addPath(jar); transform(Matrix().apply { scale(size.width, size.height) }) }
        drawPath(p, c.primaryContainer)
        clipPath(p) {
            val top = size.height * (1f - level)
            val wave = Path().apply {
                moveTo(0f, top)
                val steps = 48
                for (i in 0..steps) {
                    val x = size.width * i / steps
                    lineTo(x, top + sin(i / steps.toFloat() * 4 * PI).toFloat() * size.height * 0.02f)
                }
                lineTo(size.width, size.height); lineTo(0f, size.height); close()
            }
            drawPath(wave, c.primary)
        }
    }
}

// ───────── Insights ─────────

@Composable
fun InsightsScreen(money: MoneyRepository, onBack: () -> Unit) {
    val txs by money.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val cats by money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val ym = YearMonth.now()
    val expenses = remember(txs) { txs.filter { it.kind == TxKind.Expense } }
    val month = expenses.filter { YearMonth.from(it.date) == ym }
    val last = expenses.filter { YearMonth.from(it.date) == ym.minusMonths(1) }.sumOf { it.amountPaise }
    val total = month.sumOf { it.amountPaise }
    val slices = remember(month, cats) {
        month.groupBy { it.categoryId }.mapNotNull { (id, l) -> cats.firstOrNull { it.id == id }?.let { it to l.sumOf { t -> t.amountPaise } } }
            .sortedByDescending { it.second }
    }
    var picked by rememberSaveable { mutableStateOf<Long?>(null) }
    DetailScaffold("Insights", onBack) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 48.dp)) {
            item(key = "donut") {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                        Donut(slices.map { k.accent(it.first.color) to it.second }, slices.indexOfFirst { it.first.id == picked }, Modifier.fillMaxSize())
                        val sel = slices.firstOrNull { it.first.id == picked }
                        Box(
                            Modifier.size(110.dp).background(sel?.let { k.container(it.first.color) } ?: c.secondaryContainer, MaterialShapes.Cookie9Sided.toShape()),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(sel?.first?.name ?: ym.month.name.lowercase().replaceFirstChar(Char::uppercase),
                                    style = MaterialTheme.typography.labelMedium)
                                Text(Money.format(sel?.second ?: total), style = MaterialTheme.typography.titleMediumEmphasized)
                            }
                        }
                    }
                    if (last > 0) {
                        val pct = ((total - last) * 100f / last).roundToInt()
                        Text(
                            if (pct <= 0) "${-pct}% less than last month" else "$pct% more than last month",
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            color = if (pct <= 0) c.onTertiaryContainer else c.onErrorContainer,
                            modifier = Modifier.padding(top = 12.dp)
                                .background(if (pct <= 0) c.tertiaryContainer else c.errorContainer, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            slices.forEachIndexed { i, (cat, amt) ->
                item(key = "s${cat.id}") {
                    Column(Modifier.padding(top = if (i == 0) 20.dp else 0.dp)) {
                        SegmentedListItem(
                            onClick = { picked = if (picked == cat.id) null else cat.id },
                            shapes = ListItemDefaults.segmentedShapes(i, slices.size),
                            colors = ListItemDefaults.segmentedColors(containerColor = if (picked == cat.id) k.itemHigh else k.item),
                            leadingContent = { CategoryBadge(cat) },
                            supportingContent = { Text("${(amt * 100f / total.coerceAtLeast(1)).roundToInt()}%") },
                            trailingContent = { Text(Money.format(amt), style = MaterialTheme.typography.titleSmallEmphasized) },
                        ) { Text(cat.name) }
                        if (i < slices.lastIndex) SegGap()
                    }
                }
            }
        }
    }
}

/** Rounded, gapped slices; the picked one pulls out, the rest dim. */
@Composable
private fun Donut(slices: List<Pair<Color, Long>>, picked: Int, modifier: Modifier) {
    val total = slices.sumOf { it.second }.coerceAtLeast(1).toFloat()
    Canvas(modifier) {
        val stroke = size.minDimension * 0.12f
        val gap = 6f
        var start = -90f
        slices.forEachIndexed { i, (color, v) ->
            val sweep = v / total * 360f
            val out = if (i == picked) stroke * 0.35f else 0f
            val mid = Math.toRadians((start + sweep / 2).toDouble())
            val shift = Offset((kotlin.math.cos(mid) * out).toFloat(), (kotlin.math.sin(mid) * out).toFloat())
            val inset = stroke / 2 + stroke * 0.4f
            drawArc(
                color = color.copy(alpha = if (picked >= 0 && i != picked) 0.35f else 1f),
                startAngle = start + gap / 2, sweepAngle = (sweep - gap).coerceAtLeast(0.5f), useCenter = false,
                topLeft = Offset(inset, inset) + shift, size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            start += sweep
        }
    }
}

// ───────── Recurring & bills ─────────

@Composable
fun RecurringScreen(money: MoneyRepository, onBack: () -> Unit) {
    val items by money.recurring.collectAsStateWithLifecycle(initialValue = emptyList())
    val cats by money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val fmt = DateTimeFormatter.ofPattern("d MMM")
    DetailScaffold("Bills & subscriptions", onBack) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 48.dp)) {
            item(key = "total") {
                Column(Modifier.padding(bottom = 16.dp, start = 4.dp)) {
                    Text(Money.format(items.filter { it.kind != RecurringKind.Income }.sumOf { it.amountPaise }),
                        style = MaterialTheme.typography.displaySmallEmphasized)
                    Text("${items.count { it.kind == RecurringKind.Bill }} bills · ${items.count { it.kind == RecurringKind.Subscription }} subscriptions a month",
                        style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                }
            }
            items.forEachIndexed { i, r ->
                item(key = "r${r.id}") {
                    Column {
                        SegmentedListItem(
                            shapes = ListItemDefaults.segmentedShapes(i, items.size),
                            colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                            leadingContent = { CategoryBadge(cats.firstOrNull { it.id == r.categoryId }) },
                            supportingContent = { Text("${r.kind.name} · due ${r.nextDue.format(fmt)}") },
                            trailingContent = {
                                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { scope.launch { money.pay(r) } }) { Text("Pay ${Money.format(r.amountPaise)}") }
                            },
                        ) { Text(r.name) }
                        if (i < items.lastIndex) SegGap()
                    }
                }
            }
        }
    }
}

