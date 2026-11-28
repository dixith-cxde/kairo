@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.db.TxKind
import app.kairo.data.money.Money
import app.kairo.data.money.MoneyRepository
import app.kairo.ui.money.categoryIcon
import app.kairo.ui.theme.KairoTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * v2 Reports (canvas v103 "report filters" then "finance reports"): pick what to show first, then the
 * report. The summary row at the top goes back to the filters.
 */
@Composable
fun ReportScreen(money: MoneyRepository, onBack: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    var q by remember { mutableStateOf(ReportQuery()) }
    var filtering by remember { mutableStateOf(true) }
    var picked by remember { mutableIntStateOf(-1) }
    val txs by money.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by money.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val cats by money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val balances by money.balances.collectAsStateWithLifecycle(initialValue = emptyList())
    val kindCats = cats.filter { it.income == q.income }
    val allTags = txs.flatMap { t -> t.tags.split(' ').filter { it.isNotBlank() }.map { it.removePrefix("#") } }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }.take(12)

    Column(Modifier.fillMaxSize().background(k.bg).statusBarsPadding()) {
        Row(Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (!filtering) filtering = true else onBack() }, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = c.onSurface) }
            Text(if (filtering) "New report" else "Report", style = MaterialTheme.typography.titleLarge, color = c.onSurface)
        }
        if (filtering) {
            Box(Modifier.weight(1f)) {
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)) {
                    item { ChipCard("Show", 0, 5) { ShowGroup(q.income) { q = q.copy(income = it, categories = emptySet()) } } }
                    item {
                        ChipCard("Period", 1, 5) {
                            Period.entries.forEach { p -> Pick(p.label, q.period == p) { q = q.copy(period = p) } }
                            if (q.period == Period.Custom) Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                MonthStep("From", q.from) { m -> q = q.copy(from = m, to = maxOf(m, q.to)) }
                                MonthStep("To", q.to) { m -> q = q.copy(to = m, from = minOf(m, q.from)) }
                            }
                        }
                    }
                    item { ChipCard("Accounts", 2, 5) {
                        Pick("All", q.accounts.isEmpty()) { q = q.copy(accounts = emptySet()) }
                        accounts.forEach { a -> Pick(a.name, a.id in q.accounts, k.accent(a.color)) { q = q.copy(accounts = q.accounts.flip(a.id, accounts.size)) } }
                    } }
                    item { ChipCard("Categories", 3, 5) {
                        Pick("All", q.categories.isEmpty()) { q = q.copy(categories = emptySet()) }
                        kindCats.forEach { ct -> Pick(ct.name, ct.id in q.categories, k.accent(ct.color)) { q = q.copy(categories = q.categories.flip(ct.id, kindCats.size)) } }
                    } }
                    item { ChipCard("Tags", 4, 5) {
                        Pick("All", q.tags.isEmpty()) { q = q.copy(tags = emptySet()) }
                        allTags.forEach { t -> Pick("#$t", t in q.tags) { q = q.copy(tags = q.tags.flip(t, allTags.size)) } }
                    } }
                }
            }
            Button(onClick = { picked = -1; filtering = false }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(56.dp)) {
                Icon(painterResource(R.drawable.ic_bar_chart), null, Modifier.size(20.dp)); Text("  Show report")
            }
            return@Column
        }

        val (start, end) = q.range()
        val kind = if (q.income) TxKind.Income else TxKind.Expense
        val rows = txs.filter { t ->
            t.kind == kind && !t.date.isBefore(start) && !t.date.isAfter(end) &&
                (q.accounts.isEmpty() || t.accountId in q.accounts) && (q.categories.isEmpty() || t.categoryId in q.categories) &&
                (q.tags.isEmpty() || q.tags.any { tg -> "#$tg" in t.tags.split(' ') })
        }
        val total = rows.sumOf { it.amountPaise }
        val days = ChronoUnit.DAYS.between(start, end) + 1
        val unit = when { days <= 31 -> "day"; days <= 92 -> "week"; else -> "month" }
        val buckets = buckets(start, end, unit) { a, b -> rows.filter { !it.date.isBefore(a) && !it.date.isAfter(b) }.sumOf { it.amountPaise } }
        val avg = if (buckets.isEmpty()) 0 else total / buckets.size
        val pick = if (picked in buckets.indices) picked else buckets.lastIndex
        val scope = listOf(
            if (q.accounts.isEmpty()) "All accounts" else accounts.filter { it.id in q.accounts }.joinToString { it.name },
            if (q.categories.isEmpty()) "all categories" else "${q.categories.size} categor" + if (q.categories.size == 1) "y" else "ies",
        ).joinToString(" · ")
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.secondaryContainer).clickable { filtering = true }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).background(k.bg, CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_tune), null, tint = c.onSecondaryContainer) }
                    Column(Modifier.weight(1f)) {
                        Text(q.title(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = c.onSecondaryContainer)
                        Text(scope, style = MaterialTheme.typography.bodyMedium, color = c.onSecondaryContainer.copy(alpha = 0.8f), maxLines = 1)
                    }
                    Icon(painterResource(R.drawable.ic_edit), "Edit report", tint = c.onSecondaryContainer)
                }
            }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(k.item).padding(20.dp)) {
                    Text((if (q.income) "Earned" else "Spent") + " · ${rows.size} transaction" + if (rows.size == 1) "" else "s", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                    Text(Money.format(total), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"), color = c.onSurface)
                    Text("Average ${short(avg)} a $unit", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                    if (buckets.isNotEmpty()) {
                        Bars(buckets, avg, q.income, pick) { picked = it }
                        Text("${buckets[pick].full} · ${Money.format(buckets[pick].paise)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                            color = c.onSurface, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
            item {
                val shown = balances.filter { q.accounts.isEmpty() || it.account.id in q.accounts }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(k.item).padding(20.dp)) {
                    Text((if (q.accounts.isEmpty()) "All accounts" else shown.joinToString { it.account.name }) + " · balance now", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                    Text(Money.format(shown.sumOf { it.balancePaise }), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"), color = c.onSurface)
                }
            }
            val byCat = rows.groupBy { it.categoryId }.map { (id, l) -> cats.firstOrNull { it.id == id } to l.sumOf { it.amountPaise } }.sortedByDescending { it.second }
            if (byCat.isNotEmpty()) item { Text("By category", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = c.primary, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) }
            items(byCat.withIndex().toList(), key = { it.index }) { (i, p) ->
                val (cat, amt) = p
                val share = if (total == 0L) 0f else amt.toFloat() / total
                val color = cat?.let { k.accent(it.color) } ?: c.secondary
                Row(Modifier.fillMaxWidth().background(k.item, seg(i, byCat.size)).padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundIcon(cat?.let { categoryIcon(it.icon) } ?: R.drawable.ic_category, color)
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(cat?.name ?: "Other", style = MaterialTheme.typography.bodyLarge, color = c.onSurface, modifier = Modifier.weight(1f))
                            Text(Money.format(amt), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"), color = c.onSurface)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearWavyProgressIndicator(progress = { share }, color = color, modifier = Modifier.weight(1f).padding(end = 12.dp))
                            Text("${(share * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), color = c.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** Day, week (Monday) or month buckets across the range, summed by [sum]. */
private fun buckets(start: LocalDate, end: LocalDate, unit: String, sum: (LocalDate, LocalDate) -> Long): List<Bucket> = when (unit) {
    "day" -> generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }
        .map { d -> Bucket("${d.dayOfMonth}", d.format(DateTimeFormatter.ofPattern("EEE d MMM")), sum(d, d)) }.toList()
    "week" -> generateSequence(start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) { it.plusWeeks(1) }.takeWhile { !it.isAfter(end) }
        .map { w -> Bucket(w.format(DateTimeFormatter.ofPattern("d/M")), "Week of " + w.format(DateTimeFormatter.ofPattern("d MMM")), sum(maxOf(w, start), minOf(w.plusDays(6), end))) }.toList()
    else -> generateSequence(YearMonth.from(start)) { it.plusMonths(1) }.takeWhile { !it.isAfter(YearMonth.from(end)) }
        .map { m -> Bucket(m.format(DateTimeFormatter.ofPattern("MMM")), m.format(DateTimeFormatter.ofPattern("MMM yyyy")), sum(maxOf(m.atDay(1), start), minOf(m.atEndOfMonth(), end))) }.toList()
}

@Composable
private fun MonthStep(label: String, m: YearMonth, onChange: (YearMonth) -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = c.onSurface, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(m.minusMonths(1)) }, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_chevron_left), "Earlier", tint = c.onSurface) }
        Text(m.format(DateTimeFormatter.ofPattern("MMM yyyy")), style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"), color = c.onSurface)
        IconButton(onClick = { onChange(m.plusMonths(1)) }, enabled = m < YearMonth.now(), shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_chevron_right), "Later", tint = c.onSurface) }
    }
}
