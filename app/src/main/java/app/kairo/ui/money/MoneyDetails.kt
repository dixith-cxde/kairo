@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.money

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
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
private fun DetailScaffold(
    title: String, onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = KairoTheme.colors.bg,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLargeEmphasized) },
                navigationIcon = { IconButton(shapes = IconButtonDefaults.shapes(), onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") } },
                actions = actions,
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
    val bills by money.recurring.collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by money.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    var editing by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf<String?>(null) }
    val b = balances.firstOrNull { it.account.id == accountId } ?: return
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val catMap = remember(cats) { cats.associateBy { it.id } }
    val all = remember(txs) { txs.filter { it.accountId == accountId } }
    val mine = remember(all) { all.groupBy { it.date } }
    val ym = YearMonth.now()
    val month = all.filter { YearMonth.from(it.date) == ym }
    val moneyIn = month.filter { it.kind == TxKind.Income || (it.kind == TxKind.Transfer && it.amountPaise > 0) }.sumOf { it.amountPaise }
    val moneyOut = month.filter { it.kind == TxKind.Expense }.sumOf { it.amountPaise } -
        month.filter { it.kind == TxKind.Transfer && it.amountPaise < 0 }.sumOf { it.amountPaise }
    val myBills = bills.filter { it.accountId == accountId }
    // Balance at the end of each of the last 30 days, for the sparkline.
    val trend = remember(all, b.balancePaise) {
        val today = LocalDate.now()
        fun signed(t: app.kairo.data.db.TransactionEntity) = when (t.kind) { TxKind.Income -> t.amountPaise; TxKind.Expense -> -t.amountPaise; TxKind.Transfer -> t.amountPaise }
        (29 downTo 0).map { d -> val day = today.minusDays(d.toLong()); b.balancePaise - all.filter { it.date.isAfter(day) }.sumOf(::signed) }
    }
    val selection = app.kairo.ui.common.rememberSelection()
    if (editing) AccountSheet(money, b.account, canDelete = true, onDismiss = { editing = false }, onDeleted = onBack)
    adding?.let { kind ->
        AddTransactionSheet(money, accounts, cats, initialKind = TxKind.valueOf(kind), initialAccount = accountId, onDismiss = { adding = null })
    }
    DetailScaffold(b.account.name, onBack, actions = {
        IconButton(shapes = IconButtonDefaults.shapes(), onClick = { editing = true }) { Icon(painterResource(R.drawable.ic_edit), "Edit account") }
    }) { padding ->
      Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 140.dp)) {
            item(key = "hero") {
                val on = k.onContainer(b.account.color)
                val spin by app.kairo.ui.common.slowSpin(60_000)
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(k.container(b.account.color))) {
                    // The account's Cookie4 mark, large and faint, turning behind the numbers.
                    Box(Modifier.matchParentSize()) {
                        Box(
                            Modifier.align(Alignment.TopEnd).wrapContentSize(Alignment.TopEnd, unbounded = true).offset(x = 48.dp, y = (-48).dp)
                                .size(200.dp).graphicsLayer { rotationZ = spin; alpha = 0.12f }
                                .background(on, MaterialShapes.Cookie4Sided.toShape()),
                        )
                    }
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AccountBadge(b.account, size = 36)
                            Text("${b.account.type.name} · balance", style = MaterialTheme.typography.labelLarge, color = on)
                        }
                        Text(Money.format(b.balancePaise), style = MaterialTheme.typography.displaySmallEmphasized, color = on,
                            modifier = Modifier.padding(top = 8.dp))
                        Sparkline(trend, on, Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp))
                        Text("Last 30 days", style = MaterialTheme.typography.labelSmall, color = on.copy(alpha = 0.7f))
                    }
                }
            }
            item(key = "stats") {
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("In · ${ym.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())}",
                        Money.format(moneyIn), R.drawable.ic_arrow_upward, k.container(app.kairo.data.model.ListColor.Green), Modifier.weight(1f), flip = true)
                    StatTile("Out · ${ym.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())}",
                        Money.format(moneyOut), R.drawable.ic_arrow_upward, k.container(app.kairo.data.model.ListColor.Coral), Modifier.weight(1f))
                }
            }
            item(key = "actions") {
                // Add to this account: connected button group (Expense is the solid one).
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    listOf(TxKind.Expense to R.drawable.ic_payments, TxKind.Income to R.drawable.ic_savings, TxKind.Transfer to R.drawable.ic_swap_vert)
                        .forEachIndexed { i, (kd, icon) ->
                            val shapes = when (i) {
                                0 -> androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes()
                                2 -> androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> androidx.compose.material3.ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                            androidx.compose.material3.ToggleButton(
                                checked = i == 0, onCheckedChange = { adding = kd.name }, shapes = shapes,
                                modifier = Modifier.weight(1f).height(56.dp),
                            ) {
                                Icon(painterResource(icon), null, Modifier.size(20.dp))
                                androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                                Text(kd.name)
                            }
                        }
                }
            }
            if (myBills.isNotEmpty()) {
                item(key = "h-bills") { SectionTitle("Bills from this account") }
                myBills.forEachIndexed { i, r ->
                    item(key = "bill${r.id}") {
                        Column {
                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(i, myBills.size),
                                colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                                leadingContent = { CategoryBadge(catMap[r.categoryId]) },
                                supportingContent = { Text("due ${r.nextDue.format(DateTimeFormatter.ofPattern("d MMM"))}") },
                                trailingContent = { Text(Money.format(r.amountPaise), style = MaterialTheme.typography.titleSmallEmphasized) },
                            ) { Text(r.name) }
                            if (i < myBills.lastIndex) SegGap()
                        }
                    }
                }
            }
            if (all.isEmpty()) item(key = "empty") {
                app.kairo.ui.common.EmptyState(
                    icon = accountIcon(b.account.type), title = "No transactions yet",
                    body = "Expenses, income and transfers for ${b.account.name} show up here.",
                    shape = MaterialShapes.Cookie4Sided, size = 160.dp,
                )
            }
            mine.forEach { (date, list) ->
                item(key = "d$date") {
                    Text(dayLabel(date), style = MaterialTheme.typography.titleSmallEmphasized, color = c.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp))
                }
                list.forEachIndexed { i, tx ->
                    item(key = "t${tx.id}") {
                        Column(Modifier.animateItem()) {
                            TxRow(tx, tx.categoryId?.let(catMap::get), b.account, i, list.size,
                                selected = tx.id in selection, selecting = selection.active,
                                onLongClick = { selection.toggle(tx.id) },
                            ) { if (selection.active) selection.toggle(tx.id) }
                            if (i < list.lastIndex) SegGap()
                        }
                    }
                }
            }
        }
        app.kairo.ui.common.SelectionTopBar(selection, all.map { it.id }, Modifier.align(Alignment.TopCenter))
        app.kairo.ui.common.MoneySelectionToolbar(selection, all, Modifier.align(Alignment.BottomCenter))
      }
    }
}

@Composable
private fun StatTile(label: String, value: String, icon: Int, tile: Color, modifier: Modifier, flip: Boolean = false) {
    val k = KairoTheme.colors
    Row(
        modifier.clip(RoundedCornerShape(24.dp)).background(k.item).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(36.dp).background(tile, MaterialShapes.Cookie6Sided.toShape()), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(18.dp).graphicsLayer { rotationZ = if (flip) 180f else 0f })
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 1)
        }
    }
}

/** Balance over time as a smooth line with a soft fill. */
@Composable
private fun Sparkline(points: List<Long>, color: Color, modifier: Modifier) {
    val grow by animateFloatAsState(1f, MaterialTheme.motionScheme.slowEffectsSpec(), label = "spark")
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val min = points.min().toFloat(); val max = points.max().toFloat()
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (points.size - 1)
        fun y(v: Long) = if (max == min) size.height / 2 else size.height - (v - min) / span * size.height * 0.9f - size.height * 0.05f
        val line = Path().apply {
            points.forEachIndexed { i, v -> if (i == 0) moveTo(0f, y(v)) else lineTo(i * stepX, y(v)) }
        }
        val fill = Path().apply { addPath(line); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        clipRect(right = size.width * grow) {
            drawPath(fill, color.copy(alpha = 0.12f))
            drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

// ───────── Budgets ─────────

@Composable
fun BudgetsScreen(money: MoneyRepository, onBack: () -> Unit) {
    val txs by money.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val cats by money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by money.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val budgets by money.budgets.collectAsStateWithLifecycle(initialValue = emptyList())
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val snackbar = app.kairo.ui.common.LocalAppSnackbar.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    // The drawer: null = closed; id 0 = new budget; otherwise the budget being edited.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val ym = YearMonth.now()
    val month = remember(txs) { txs.filter { it.kind == TxKind.Expense && YearMonth.from(it.date) == ym } }
    val spentByCat = remember(month) { month.groupBy { it.categoryId }.mapValues { (_, l) -> l.sumOf { it.amountPaise } } }
    val spentByAcc = remember(month) { month.groupBy { it.accountId }.mapValues { (_, l) -> l.sumOf { it.amountPaise } } }
    val spent = month.sumOf { it.amountPaise }
    val overall = budgets.firstOrNull { it.overall }
    val daysLeft = ym.lengthOfMonth() - LocalDate.now().dayOfMonth + 1

    fun delete(b: app.kairo.data.db.BudgetEntity) {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm) // rule 9: delete
        scope.launch {
            money.deleteBudget(b.id)
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar("Budget deleted", actionLabel = "Undo", duration = androidx.compose.material3.SnackbarDuration.Short)
            if (r == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm) // rule 9: undo
                money.restoreBudget(b)
            }
        }
    }

    editingId?.let { id ->
        BudgetSheet(
            money, budget = budgets.firstOrNull { it.id == id }, budgets = budgets,
            categories = cats.filter { !it.income }, accounts = accounts,
            onDismiss = { editingId = null }, onDelete = { editingId = null; delete(it) },
        )
    }

    DetailScaffold("Budgets", onBack, actions = {
        IconButton(shapes = IconButtonDefaults.shapes(), onClick = { editingId = 0L }) { Icon(painterResource(R.drawable.ic_add), "Add budget") }
    }) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 120.dp)) {
            item(key = "jar") {
                if (overall != null) {
                    val left = overall.limitPaise - spent
                    val fill = (left.toFloat() / overall.limitPaise).coerceIn(0f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
                            .clickable(onClickLabel = "Edit monthly budget") { editingId = overall.id }.padding(vertical = 12.dp),
                    ) {
                        LiquidJar(fill, Modifier.size(200.dp))
                        Text(
                            if (left >= 0) "${Money.format(left)} left of ${Money.format(overall.limitPaise)}"
                            else "${Money.format(-left)} over ${Money.format(overall.limitPaise)}",
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = if (left >= 0) c.onSurface else c.error,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        Text(
                            if (left > 0) "$daysLeft days to go · ≈ ${Money.format(left / daysLeft / 100 * 100)} a day · tap to edit" else "Tap to edit",
                            style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant,
                        )
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(k.item).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("No monthly budget", style = MaterialTheme.typography.titleMediumEmphasized)
                        Text("Spent ${Money.format(spent)} so far in ${ym.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())}.",
                            style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                        FilledTonalButton(onClick = { editingId = 0L }, shapes = ButtonDefaults.shapes()) { Text("Set a monthly budget") }
                    }
                }
            }
            @Composable
            fun BudgetRow(i: Int, n: Int, name: String, used: Long, b: app.kairo.data.db.BudgetEntity, accent: Color, badge: @Composable () -> Unit) {
                val over = used > b.limitPaise
                Column {
                    SegmentedListItem(
                        onClick = { editingId = b.id },
                        shapes = ListItemDefaults.segmentedShapes(i, n),
                        colors = ListItemDefaults.segmentedColors(containerColor = if (over) c.errorContainer else k.item),
                        leadingContent = badge,
                        supportingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    if (over) "${Money.format(used - b.limitPaise)} over · ${Money.format(used)} of ${Money.format(b.limitPaise)}"
                                    else "${Money.format(used)} of ${Money.format(b.limitPaise)} · ${Money.format(b.limitPaise - used)} left",
                                    color = if (over) c.onErrorContainer else c.onSurfaceVariant,
                                )
                                LinearWavyProgressIndicator(
                                    progress = { (used.toFloat() / b.limitPaise).coerceIn(0f, 1f) },
                                    color = if (over) c.error else accent,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        },
                    ) { Text(name, color = if (over) c.onErrorContainer else c.onSurface) }
                    if (i < n - 1) SegGap()
                }
            }
            val catRows = budgets.filter { it.categoryId != null }.mapNotNull { b -> cats.firstOrNull { it.id == b.categoryId }?.let { b to it } }
            if (catRows.isNotEmpty()) item(key = "h-cat") { SectionTitle("By category") }
            catRows.forEachIndexed { i, (b, cat) ->
                item(key = "b${b.id}") { BudgetRow(i, catRows.size, cat.name, spentByCat[cat.id] ?: 0, b, k.accent(cat.color)) { CategoryBadge(cat) } }
            }
            val accRows = budgets.filter { it.accountId != null }.mapNotNull { b -> accounts.firstOrNull { it.id == b.accountId }?.let { b to it } }
            if (accRows.isNotEmpty()) item(key = "h-acc") { SectionTitle("By account") }
            accRows.forEachIndexed { i, (b, acc) ->
                item(key = "b${b.id}") { BudgetRow(i, accRows.size, acc.name, spentByAcc[acc.id] ?: 0, b, k.accent(acc.color)) { AccountBadge(acc) } }
            }
            if (catRows.isEmpty() && accRows.isEmpty()) item(key = "hint") {
                Text("Add a budget for a category (Food, Shopping…) or for one account with +.",
                    style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 20.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) = Text(
    text, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
)

/** An account's mark: its type icon on its color, in Cookie4 (same as the account drawer). */
@Composable
internal fun AccountBadge(a: app.kairo.data.db.AccountEntity, size: Int = 40) {
    val k = KairoTheme.colors
    Box(Modifier.size(size.dp).background(k.container(a.color), MaterialShapes.Cookie4Sided.toShape()), contentAlignment = Alignment.Center) {
        Icon(painterResource(accountIcon(a.type)), null, tint = k.onContainer(a.color), modifier = Modifier.size((size / 2).dp))
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
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 120.dp)) {
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
    val snackbar = app.kairo.ui.common.LocalAppSnackbar.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val fmt = DateTimeFormatter.ofPattern("d MMM")
    // The drawer: null = closed, 0 = new, else the bill being edited.
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    editing?.let { id ->
        BillSheet(money, items.firstOrNull { it.id == id }, onDismiss = { editing = null }, onDelete = { r ->
            editing = null
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm) // rule 9: delete
            scope.launch {
                money.deleteRecurring(r.id)
                snackbar.currentSnackbarData?.dismiss()
                val res = snackbar.showSnackbar("${r.name} deleted", actionLabel = "Undo", duration = androidx.compose.material3.SnackbarDuration.Short)
                if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm) // rule 9: undo
                    money.restoreRecurring(r)
                }
            }
        })
    }
    DetailScaffold("Bills & subscriptions", onBack, actions = {
        IconButton(shapes = IconButtonDefaults.shapes(), onClick = { editing = 0L }) { Icon(painterResource(R.drawable.ic_add), "Add recurring") }
    }) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 120.dp)) {
            if (items.isEmpty()) item(key = "empty") {
                app.kairo.ui.common.EmptyState(
                    icon = R.drawable.ic_receipt_long, title = "No bills yet",
                    body = "Add rent, subscriptions or salary once and Kairo shows what's due each month.",
                    shape = MaterialShapes.Clover4Leaf,
                ) { FilledTonalButton(onClick = { editing = 0L }, shapes = ButtonDefaults.shapes()) { Text("Add a bill") } }
            } else item(key = "total") {
                Column(Modifier.padding(bottom = 16.dp, start = 4.dp)) {
                    Text(Money.format(items.filter { it.kind != RecurringKind.Income }.sumOf { it.amountPaise }),
                        style = MaterialTheme.typography.displaySmallEmphasized)
                    Text("${items.count { it.kind == RecurringKind.Bill }} bills · ${items.count { it.kind == RecurringKind.Subscription }} subscriptions a month",
                        style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                }
            }
            items.forEachIndexed { i, r ->
                item(key = "r${r.id}") {
                    val late = r.nextDue.isBefore(LocalDate.now())
                    Column {
                        SegmentedListItem(
                            onClick = { editing = r.id },
                            shapes = ListItemDefaults.segmentedShapes(i, items.size),
                            colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                            leadingContent = { CategoryBadge(cats.firstOrNull { it.id == r.categoryId }) },
                            supportingContent = { Text("${r.kind.name} · ${if (late) "overdue since" else "due"} ${r.nextDue.format(fmt)}",
                                color = if (late) c.error else c.onSurfaceVariant) },
                            trailingContent = {
                                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { scope.launch { money.pay(r) } }) {
                                    Text("${if (r.kind == RecurringKind.Income) "Got" else "Pay"} ${Money.format(r.amountPaise)}")
                                }
                            },
                        ) { Text(r.name) }
                        if (i < items.lastIndex) SegGap()
                    }
                }
            }
        }
    }
}
