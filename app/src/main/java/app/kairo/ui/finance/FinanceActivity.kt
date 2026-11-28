@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.finance

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kairo.R
import app.kairo.data.db.AccountEntity
import app.kairo.data.db.CategoryEntity
import app.kairo.data.db.RecurringEntity
import app.kairo.data.db.RecurringKind
import app.kairo.data.db.TransactionEntity
import app.kairo.data.db.TxKind
import app.kairo.data.money.Money
import app.kairo.ui.money.categoryIcon
import app.kairo.ui.money.signedPaise
import app.kairo.ui.theme.KairoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Canvas spent card: "Spent in November", the big amount, a wavy budget bar (error when over), Income and Net chips. */
@Composable
internal fun SpentCard(month: String, spent: Long, income: Long, budget: Long?) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val over = budget != null && spent > budget
    val p by animateFloatAsState(if (budget == null || budget == 0L) 0f else (spent.toFloat() / budget).coerceIn(0f, 1f), MaterialTheme.motionScheme.slowSpatialSpec(), label = "budget")
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(28.dp)).background(k.item).padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Spent in $month", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
        Text(Money.format(spent), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"), color = c.onSurface)
        if (budget != null) {
            LinearWavyProgressIndicator(progress = { p }, color = if (over) c.error else c.primary, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            Text(if (over) Money.format(spent - budget) + " over " + Money.format(budget) else Money.format(budget - spent) + " left of " + Money.format(budget),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), color = if (over) c.error else c.onSurfaceVariant)
        }
        androidx.compose.foundation.layout.FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(R.drawable.ic_arrow_forward, "Income", Money.format(income, sign = true), rotate = 135f)
            Chip(R.drawable.ic_swap_vert, "Net", Money.format(income - spent, sign = true))
        }
    }
}

@Composable
private fun Chip(icon: Int, label: String, value: String, rotate: Float = 0f) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(KairoTheme.colors.chip).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), null, Modifier.size(16.dp).rotate(rotate), tint = c.onSurfaceVariant)
        Text("  $label ", style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant, maxLines = 1, softWrap = false)
        Text(value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), color = c.onSurface, maxLines = 1, softWrap = false)
    }
}

/** Canvas v103: "Upcoming bills · 3 due · Spotify is overdue", collapsed by default; open shows the next three with Skip / Pay. */
@Composable
internal fun BillsBlock(bills: List<RecurringEntity>, today: LocalDate, open: Boolean, onToggle: () -> Unit, onPay: (RecurringEntity) -> Unit, onSkip: (RecurringEntity) -> Unit, onAll: () -> Unit = {}) {
    if (bills.isEmpty()) return
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val due = bills.filter { !it.nextDue.isAfter(today.plusDays(7)) }
    val late = bills.firstOrNull { it.nextDue.isBefore(today) }
    val turn by animateFloatAsState(if (open) 180f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "chev")
    val shown = bills.take(3)
    val n = if (open) shown.size + 2 else 1
    Column(Modifier.padding(top = 16.dp)) {
        SegmentedListItem(
            onClick = onToggle,
            shapes = ListItemDefaults.shapes(shape = seg(0, n), pressedShape = RoundedCornerShape(28.dp)),
            colors = ListItemDefaults.segmentedColors(containerColor = k.item),
            leadingContent = { RoundIcon(R.drawable.ic_event_repeat, c.primary) },
            supportingContent = { Text(listOfNotNull("${due.size} due", late?.let { "${it.name} is overdue" }).joinToString(" · "), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingContent = { Icon(painterResource(R.drawable.ic_expand_more), if (open) "Hide bills" else "Show bills", Modifier.rotate(turn), tint = c.onSurfaceVariant) },
        ) { Text("Upcoming bills") }
        if (open) shown.forEachIndexed { i, b ->
            val days = ChronoUnit.DAYS.between(today, b.nextDue)
            SegmentedListItem(
                onClick = {},
                shapes = ListItemDefaults.shapes(shape = seg(i + 1, n)),
                colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                modifier = Modifier.padding(top = 3.dp),
                supportingContent = {
                    Text((if (days < 0) "Overdue · " else if (days == 0L) "Due today · " else "Due " + b.nextDue.format(Short) + " · ") + Money.format(b.amountPaise),
                        color = if (days < 0) c.error else c.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"))
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onSkip(b) }, shapes = ButtonDefaults.shapes()) { Text("Skip") }
                        FilledTonalButton(onClick = { onPay(b) }, shapes = ButtonDefaults.shapes(), contentPadding = PaddingValues(horizontal = 16.dp)) {
                            Text(if (b.kind == RecurringKind.Income) "Got it" else "Pay")
                        }
                    }
                },
            ) { Text(b.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        if (open) SegmentedListItem(
            onClick = onAll, shapes = ListItemDefaults.shapes(shape = seg(n - 1, n), pressedShape = RoundedCornerShape(28.dp)),
            colors = ListItemDefaults.segmentedColors(containerColor = k.item), modifier = Modifier.padding(top = 3.dp),
            trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), null, tint = c.onSurfaceVariant) },
        ) { Text("All bills", color = c.primary) }
    }
}

/** All / Expense / Income / Transfer, the real connected toggle buttons. */
@Composable
internal fun TypeFilter(kind: TxKind?, onPick: (TxKind?) -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val opts = listOf<TxKind?>(null) + TxKind.entries
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        opts.forEachIndexed { i, o ->
            ToggleButton(
                checked = o == kind, onCheckedChange = { onPick(o) },
                shapes = when (i) { 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(); opts.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes(); else -> ButtonGroupDefaults.connectedMiddleButtonShapes() },
                colors = ToggleButtonDefaults.colors(containerColor = k.chip, contentColor = c.onSurfaceVariant, checkedContainerColor = c.primary, checkedContentColor = c.onPrimary),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.weight((o?.name?.length ?: 3) + 4f).height(40.dp),
            ) { Text(o?.name ?: "All", style = MaterialTheme.typography.labelLarge, maxLines = 1) }
        }
    }
}

/** Day header: "Today" on the left, the day's net on the right. */
@Composable
internal fun DayHeader(label: String, net: Long) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 20.dp, bottom = 8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = c.primary, modifier = Modifier.weight(1f))
        Text(Money.format(net, sign = true), style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"), color = c.onSurfaceVariant)
    }
}

/** Canvas .crw for a transaction: round tinted category icon, title, "Category · Account", signed amount. */
@Composable
internal fun TxRowV2(
    tx: TransactionEntity, cat: CategoryEntity?, account: AccountEntity?, other: AccountEntity?, i: Int, n: Int,
    selected: Boolean, selecting: Boolean, onClick: () -> Unit, onLongClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val signed = tx.signedPaise()
    val color = cat?.let { k.accent(it.color) } ?: c.secondary
    SegmentedListItem(
        selected = selected, onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Select",
        shapes = ListItemDefaults.shapes(shape = seg(i, n), selectedShape = RoundedCornerShape(28.dp), pressedShape = RoundedCornerShape(28.dp)),
        colors = app.kairo.ui.common.selectableRowColors(),
        contentPadding = PaddingValues(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        modifier = Modifier.heightIn(min = 64.dp),
        leadingContent = {
            if (selecting) app.kairo.ui.common.SelectBadge(selected)
            else RoundIcon(if (tx.kind == TxKind.Transfer) R.drawable.ic_swap_vert else cat?.let { categoryIcon(it.icon) } ?: R.drawable.ic_category, color)
        },
        supportingContent = {
            val meta = if (tx.kind == TxKind.Transfer) listOfNotNull(if (signed < 0) account?.name else other?.name, if (signed < 0) other?.name else account?.name).joinToString(" → ")
                else listOfNotNull(cat?.name, account?.name).joinToString(" · ")
            Text(meta + if (tx.tags.isNotBlank()) " · ${tx.tags}" else "", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            Text(Money.format(signed, sign = true), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = if (tx.kind == TxKind.Income) k.chartGreen else c.onSurface)
        },
    ) { Text(tx.note.ifBlank { if (tx.kind == TxKind.Transfer) "Transfer" else cat?.name ?: "Transaction" }, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

/** Canvas .fic: a 40dp circle tinted 22% with the colour, icon filled in the colour. */
@Composable
internal fun RoundIcon(icon: Int, color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.size(40.dp).background(color.copy(alpha = 0.22f).compositeOver(KairoTheme.colors.item), CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), null, Modifier.size(20.dp), tint = color)
    }
}

internal fun seg(i: Int, n: Int): RoundedCornerShape {
    val big = 28.dp; val small = 8.dp
    return RoundedCornerShape(
        topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
        bottomStart = if (i == n - 1) big else small, bottomEnd = if (i == n - 1) big else small,
    )
}

private val Short = DateTimeFormatter.ofPattern("d MMM")
