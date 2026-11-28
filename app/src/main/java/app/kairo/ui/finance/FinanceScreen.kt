@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.finance

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.db.TxKind
import app.kairo.data.money.MoneyRepository
import app.kairo.data.settings.AppSettings
import app.kairo.ui.common.EmptyState
import app.kairo.ui.common.ExpressiveSwipe
import app.kairo.ui.common.LocalAppSnackbar
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.common.MoneySelectionToolbar
import app.kairo.ui.common.SelectionTopBar
import app.kairo.ui.common.TabSearchBar
import app.kairo.ui.common.rememberSelection
import app.kairo.ui.money.dayLabel
import app.kairo.ui.money.signedPaise
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * v2 Finance tab (canvas v102–v112). Activity: the month's spent card with the budget, upcoming
 * bills, a type filter and transactions by day. Accounts: the counted total and a tile per account.
 * ‹ › move by month. Tap an account tile to see its activity.
 */
@Composable
fun FinanceScreen(money: MoneyRepository, onSearch: () -> Unit, onVoice: () -> Unit, onAccount: () -> Unit, onReports: () -> Unit, onOpenTx: (Long) -> Unit = {}, onBills: () -> Unit = {}) {
    val app = LocalKairoApp.current
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val scope = rememberCoroutineScope()
    val snackbar = LocalAppSnackbar.current
    val haptic = LocalHapticFeedback.current
    val today = LocalDate.now()
    var accountsView by rememberSaveable { mutableStateOf(false) }
    var monthOff by rememberSaveable { mutableStateOf(0) }
    var kindName by rememberSaveable { mutableStateOf<String?>(null) }
    var accFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var billsOpen by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSelection()
    val kind = kindName?.let(TxKind::valueOf)
    val month = YearMonth.from(today).plusMonths(monthOff.toLong())

    val txs by money.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by money.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val cats by money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val balances by money.balances.collectAsStateWithLifecycle(initialValue = emptyList())
    val budgets by money.budgets.collectAsStateWithLifecycle(initialValue = emptyList())
    val bills by money.recurring.collectAsStateWithLifecycle(initialValue = emptyList())
    val prefs by app.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val accMap = accounts.associateBy { it.id }
    val catMap = cats.associateBy { it.id }
    val byId = txs.associateBy { it.id }
    val inMonth = txs.filter { YearMonth.from(it.date) == month }
    val scoped = inMonth.filter { accFilter == null || it.accountId == accFilter }
    val spent = scoped.filter { it.kind == TxKind.Expense }.sumOf { it.amountPaise }
    val income = scoped.filter { it.kind == TxKind.Income }.sumOf { it.amountPaise }
    val budget = budgets.firstOrNull { it.overall }?.limitPaise?.takeIf { accFilter == null }
    // A transfer shows once (its "from" leg).
    val shown = scoped.filter { (kind == null || it.kind == kind) && !(it.kind == TxKind.Transfer && it.amountPaise > 0 && it.transferPeerId != null) }
    val days = shown.groupBy { it.date }.toSortedMap(compareByDescending { it })
    val monthName = month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())

    fun delete(ids: List<Long>) {
        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: delete
        scope.launch {
            val rows = money.deleteAll(ids)
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar("Deleted", "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm); money.restore(rows)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(k.bg)) {
        Column(Modifier.fillMaxSize()) {
            TabSearchBar("Search in finance", onSearch = onSearch, onVoice = onVoice, onMenu = null, onAccount = onAccount)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 168.dp)) {
                item(key = "head") {
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(monthName, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Medium), color = c.onSurface)
                        Text("${month.year}", style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), color = c.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp).weight(1f))
                        IconButton(onClick = onReports, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_bar_chart), "Reports", tint = c.onSurfaceVariant) }
                    }
                }
                item(key = "views") { Views(accountsView, { accountsView = it }, { monthOff-- }, { if (monthOff < 0) monthOff++ }) }
                if (accountsView) {
                    item(key = "total") { TotalCard(balances, prefs.finExcluded) }
                    val rows = balances.chunked(2)
                    itemsIndexed(rows, key = { _, r -> "acc" + r.first().account.id }) { _, pair ->
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { b ->
                                val mine = inMonth.filter { it.accountId == b.account.id }
                                AccountTile(
                                    b, mine.filter { it.signedPaise() > 0 }.sumOf { it.signedPaise() }, mine.filter { it.signedPaise() < 0 }.sumOf { -it.signedPaise() },
                                    b.account.id.toString() !in prefs.finExcluded, Modifier.weight(1f),
                                    onCount = { on -> scope.launch { app.settings.setCounted(b.account.id, on) } },
                                    onOpen = { accFilter = b.account.id; accountsView = false },
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                } else {
                    item(key = "spent") { SpentCard(monthName, spent, income, budget) }
                    if (monthOff == 0 && accFilter == null) item(key = "bills") {
                        BillsBlock(bills, today, billsOpen, onToggle = { billsOpen = !billsOpen },
                            onPay = { b -> haptic.performHapticFeedback(HapticFeedbackType.ToggleOn); scope.launch { money.pay(b); snackbar.showSnackbar("Paid ${b.name}") } },
                            onSkip = { b -> scope.launch { money.skip(b); snackbar.showSnackbar("Skipped ${b.name} this month") } }, onAll = onBills)
                    }
                    accFilter?.let { id -> item(key = "accf") {
                        InputChip(selected = true, onClick = { accFilter = null }, label = { Text(accMap[id]?.name ?: "Account") }, modifier = Modifier.padding(top = 12.dp),
                            trailingIcon = { Icon(painterResource(R.drawable.ic_close), "Show all accounts", Modifier.size(18.dp)) })
                    } }
                    item(key = "filter") { TypeFilter(kind) { kindName = it?.name } }
                    if (days.isEmpty()) item(key = "empty") {
                        EmptyState("No transactions", "", size = 120.dp, color = c.tertiaryContainer, icon = R.drawable.ic_receipt_long, modifier = Modifier.padding(top = 16.dp))
                    }
                    days.forEach { (d, list) ->
                        item(key = "d$d") { DayHeader(dayLabel(d), list.sumOf { if (it.kind == TxKind.Transfer) 0 else it.signedPaise() }) }
                        itemsIndexed(list, key = { _, t -> "t${t.id}" }) { i, t ->
                            Column(Modifier.animateItem()) {
                                ExpressiveSwipe(
                                    enabled = !selection.active, rightIcon = R.drawable.ic_check, rightLabel = "", leftIcon = R.drawable.ic_delete, leftLabel = "Delete",
                                    onRight = {}, onLeft = { delete(listOf(t.id)) }, rightEnabled = false,
                                ) {
                                    TxRowV2(
                                        t, t.categoryId?.let(catMap::get), accMap[t.accountId], t.transferPeerId?.let { byId[it] }?.let { accMap[it.accountId] }
                                            ?: txs.firstOrNull { it.transferPeerId == t.id }?.let { accMap[it.accountId] },
                                        i, list.size, t.id in selection, selection.active,
                                        onClick = { if (selection.active) selection.toggle(t.id) else onOpenTx(t.id) },
                                        onLongClick = { selection.toggle(t.id) },
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                            }
                        }
                    }
                }
            }
        }
        SelectionTopBar(selection, shown.map { it.id }, Modifier.align(Alignment.TopCenter))
        MoneySelectionToolbar(selection, txs, Modifier.align(Alignment.BottomCenter))
    }
}

/** Activity | Accounts (connected toggle buttons with icons), then ‹ › for the month. */
@Composable
private fun Views(accounts: Boolean, onPick: (Boolean) -> Unit, onPrev: () -> Unit, onNext: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).padding(end = 4.dp), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            listOf(false to ("Activity" to R.drawable.ic_receipt_long), true to ("Accounts" to R.drawable.ic_account_balance_wallet)).forEachIndexed { i, (v, lt) ->
                ToggleButton(
                    checked = accounts == v, onCheckedChange = { onPick(v) },
                    shapes = if (i == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    colors = ToggleButtonDefaults.colors(containerColor = k.chip, contentColor = c.onSurfaceVariant, checkedContainerColor = c.primary, checkedContentColor = c.onPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f).height(40.dp),
                ) {
                    Icon(painterResource(lt.second), null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(lt.first, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                }
            }
        }
        IconButton(onClick = onPrev, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_chevron_left), "Previous month", tint = c.onSurface) }
        IconButton(onClick = onNext, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_chevron_right), "Next month", tint = c.onSurface) }
    }
}
