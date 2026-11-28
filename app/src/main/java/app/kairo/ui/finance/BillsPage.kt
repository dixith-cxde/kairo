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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.db.RecurringEntity
import app.kairo.data.db.RecurringKind
import app.kairo.data.money.Money
import app.kairo.data.money.MoneyRepository
import app.kairo.ui.common.LocalAppSnackbar
import app.kairo.ui.money.BillSheet
import app.kairo.ui.money.categoryIcon
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Canvas "finance bills" board: every bill by due date, with Skip / Pay (Got it for income), and a
 * New bill FAB in the bottom-right corner (v107–v108). Tap a bill to edit it.
 */
@Composable
fun BillsPage(money: MoneyRepository, onBack: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val scope = rememberCoroutineScope()
    val snackbar = LocalAppSnackbar.current
    val haptic = LocalHapticFeedback.current
    val bills by money.recurring.collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by money.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val cats by money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    var editing by remember { mutableStateOf<RecurringEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val monthly = bills.filter { it.kind != RecurringKind.Income }.sumOf { it.amountPaise }

    Box(Modifier.fillMaxSize().background(k.bg)) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)) {
            item {
                IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = c.onSurface) }
                Text("Bills", style = MaterialTheme.typography.headlineMedium, color = c.onSurface, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
                Text(Money.format(monthly) + " a month · ${bills.size} bill" + if (bills.size == 1) "" else "s",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), color = c.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            }
            itemsIndexed(bills, key = { _, b -> b.id }) { i, b ->
                val cat = cats.firstOrNull { it.id == b.categoryId }
                val acc = accounts.firstOrNull { it.id == b.accountId }
                val late = b.nextDue.isBefore(today)
                val income = b.kind == RecurringKind.Income
                Column(
                    Modifier.animateItem().fillMaxWidth().padding(bottom = 3.dp).background(k.item, seg(i, bills.size)).clickable { editing = b }
                        .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RoundIcon(cat?.let { categoryIcon(it.icon) } ?: R.drawable.ic_receipt_long, cat?.let { k.accent(it.color) } ?: c.secondary)
                        Column(Modifier.weight(1f)) {
                            Text(b.name, style = MaterialTheme.typography.bodyLarge, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text((if (late) "Overdue" else if (b.nextDue == today) "Due today" else "Due " + b.nextDue.format(Due)) + (acc?.let { " · " + it.name } ?: ""),
                                style = MaterialTheme.typography.bodyMedium, color = if (late) c.error else c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(Money.format(if (income) b.amountPaise else b.amountPaise, sign = income), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                            color = if (income) k.chartGreen else c.onSurface)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        FilledTonalButton(onClick = { scope.launch { money.skip(b); snackbar.showSnackbar("Skipped ${b.name} this month") } }, shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = k.chip, contentColor = c.onSurface)) { Text("Skip") }
                        Button(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            scope.launch { money.pay(b); snackbar.showSnackbar(if (income) "Got ${b.name}" else "Paid ${b.name}") }
                        }, shapes = ButtonDefaults.shapes(), colors = ButtonDefaults.buttonColors(containerColor = c.primaryContainer, contentColor = c.onPrimaryContainer)) {
                            Text(if (income) "Got it" else "Pay")
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { adding = true }, shape = RoundedCornerShape(20.dp), containerColor = c.primary, contentColor = c.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 20.dp).height(64.dp),
            icon = { Icon(painterResource(R.drawable.ic_add_bold), null) }, text = { Text("New bill", style = MaterialTheme.typography.titleMedium) },
        )
    }
    if (adding) BillSheet(money, null, onDismiss = { adding = false }, onDelete = {})
    editing?.let { b ->
        BillSheet(money, b, onDismiss = { editing = null }, onDelete = { r ->
            haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: delete
            scope.launch {
                money.deleteRecurring(r.id)
                if (snackbar.showSnackbar("Deleted ${r.name}", "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) money.restoreRecurring(r)
            }
            editing = null
        })
    }
}

private val Due = DateTimeFormatter.ofPattern("EEE d MMM")
