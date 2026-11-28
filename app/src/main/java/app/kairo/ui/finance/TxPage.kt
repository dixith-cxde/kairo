@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package app.kairo.ui.finance

import app.kairo.data.db.TxKind
import app.kairo.data.money.Money
import app.kairo.data.money.MoneyRepository
import app.kairo.ui.common.LocalAppSnackbar
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.habits.HabitLook
import app.kairo.ui.theme.KairoTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.ui.money.accountIcon
import app.kairo.ui.money.categoryIcon
import app.kairo.ui.money.dayLabel
import app.kairo.ui.money.signedPaise
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/**
 * Canvas "transaction page" (v112 layout): category badge, the signed amount, title, category chip,
 * the account and date rows, tags, description; Duplicate and Delete in a floating toolbar beside Edit.
 */
@Composable
fun TxPage(money: MoneyRepository, id: Long, onBack: () -> Unit) {
    val app = LocalKairoApp.current
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val scope = rememberCoroutineScope()
    val snackbar = LocalAppSnackbar.current
    val haptic = LocalHapticFeedback.current
    val txs by money.transactions.collectAsStateWithLifecycle(initialValue = null)
    val accounts by money.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val cats by money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    var editing by remember { mutableStateOf(false) }
    val tx = txs?.firstOrNull { it.id == id } ?: return
    val peer = txs!!.firstOrNull { it.id == tx.transferPeerId } ?: txs!!.firstOrNull { it.transferPeerId == tx.id }
    val cat = cats.firstOrNull { it.id == tx.categoryId }
    val acc = accounts.firstOrNull { it.id == tx.accountId }
    val other = peer?.let { p -> accounts.firstOrNull { it.id == p.accountId } }
    val color = cat?.let { k.accent(it.color) } ?: c.secondary
    val signed = tx.signedPaise()
    Box(Modifier.fillMaxSize().background(k.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 168.dp)) {
            IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes(), modifier = Modifier.padding(top = 8.dp)) { Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = c.onSurface) }
            Column(Modifier.padding(start = 4.dp, top = 8.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(56.dp).background(color, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(if (tx.kind == TxKind.Transfer) R.drawable.ic_swap_vert else cat?.let { categoryIcon(it.icon) } ?: R.drawable.ic_category), null, Modifier.size(28.dp),
                        tint = HabitLook.onColor())
                }
                Text(Money.format(signed, sign = true), style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
                    color = if (tx.kind == TxKind.Income) k.chartGreen else c.onSurface)
                Text(tx.note.ifBlank { cat?.name ?: "Transaction" }, style = MaterialTheme.typography.titleLarge, color = c.onSurface)
                if (cat != null) Row(Modifier.height(32.dp).background(k.chip, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(color, CircleShape))
                    Text("  " + cat.name, style = MaterialTheme.typography.labelLarge, color = c.onSurface)
                }
            }
            val accLine = if (tx.kind == TxKind.Transfer) listOfNotNull(if (signed < 0) acc?.name else other?.name, if (signed < 0) other?.name else acc?.name).joinToString(" → ") else acc?.name ?: ""
            Row2(acc?.let { accountIcon(it.type) } ?: R.drawable.ic_account_balance, accLine, tx.kind.name, 0)
            Row2(R.drawable.ic_schedule, dayLabel(tx.date) + (tx.time?.let { " · " + it.format(DateTimeFormatter.ofPattern("h:mm a")) } ?: ""), null, 1)
            if (tx.tags.isNotBlank()) FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tx.tags.split(' ').filter { it.isNotBlank() }.forEach { t ->
                    Text(t, style = MaterialTheme.typography.labelLarge, color = c.onSurface, modifier = Modifier.background(k.chip, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
            if (tx.description.isNotBlank()) Text(tx.description, style = MaterialTheme.typography.bodyLarge, color = c.onSurface, modifier = Modifier.padding(start = 4.dp, top = 16.dp))
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalFloatingToolbar(expanded = true, colors = FloatingToolbarDefaults.standardFloatingToolbarColors(toolbarContainerColor = k.navBg), contentPadding = PaddingValues(horizontal = 4.dp)) {
                IconButton(onClick = { scope.launch { money.duplicate(tx); snackbar.showSnackbar("Duplicated for today") } }, shapes = IconButtonDefaults.shapes()) {
                    Icon(painterResource(R.drawable.ic_content_copy), "Duplicate")
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: delete
                    app.appScope.launch {
                        val rows = money.deleteAll(listOf(tx.id))
                        if (snackbar.showSnackbar("Deleted", "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) money.restore(rows)
                    }
                    onBack()
                }, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_delete), "Delete", tint = c.error) }
            }
            ExtendedFloatingActionButton(
                onClick = { editing = true }, shape = RoundedCornerShape(20.dp), containerColor = c.primary, contentColor = c.onPrimary, modifier = Modifier.height(64.dp),
                icon = { Icon(painterResource(R.drawable.ic_edit), null) }, text = { Text("Edit", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)) },
            )
        }
    }
    // A transfer is edited through its "from" leg.
    if (editing) TxSheet(money, tx.kind, edit = if (tx.kind == TxKind.Transfer && signed > 0 && peer != null) peer else tx) { editing = false }
}

@Composable
private fun Row2(icon: Int, title: String, sub: String?, i: Int) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(bottom = 3.dp).background(KairoTheme.colors.item, seg(i, 2)).heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(painterResource(icon), null, tint = c.onSurfaceVariant)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.onSurface)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
        }
    }
}
