@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package app.kairo.ui.finance

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.money.IvyCsv
import app.kairo.data.money.IvyImport
import app.kairo.data.money.IvyRow
import app.kairo.data.money.MoneyRepository
import app.kairo.ui.calendar.GroupItem
import app.kairo.ui.calendar.SwitchLine
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class IvyFile(val name: String, val rows: List<IvyRow>, val columns: Int, val error: String?)

/**
 * Canvas "ivy import" board. Picks the CSV first, then shows what it holds before anything is
 * written: transactions, transfers, planned payments (→ bills, a switch), new accounts and
 * categories, and rows already in Kairo (skipped). Cancel / Import.
 */
@Composable
fun IvyImportSheet(money: MoneyRepository, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var file by remember { mutableStateOf<IvyFile?>(null) }
    var uri by remember { mutableStateOf<Uri?>(null) }
    var bills by remember { mutableStateOf(true) }
    var preview by remember { mutableStateOf<IvyImport.Preview?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) uri = picked else if (uri == null) onDismiss()
    }
    LaunchedEffect(Unit) { if (uri == null) picker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) }
    LaunchedEffect(uri) {
        val u = uri ?: return@LaunchedEffect
        file = withContext(Dispatchers.IO) {
            val name = runCatching {
                context.contentResolver.query(u, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
            }.getOrNull() ?: "Ivy Wallet CSV"
            runCatching {
                val (rows, cols) = IvyCsv.parse(IvyCsv.decode(context.contentResolver.openInputStream(u)!!.use { it.readBytes() }))
                IvyFile(name, rows, cols, null)
            }.getOrElse { IvyFile(name, emptyList(), 0, it.message ?: "Couldn't read this file") }
        }
    }
    LaunchedEffect(file, bills) { preview = file?.takeIf { it.error == null }?.let { IvyImport.preview(money, it.rows, it.columns, bills) } }

    val f = file ?: return
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun close() = scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = k.bg) {
        Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 16.dp)) {
            Text("Import from Ivy Wallet", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp, bottom = 16.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth().background(k.item, RoundedCornerShape(28.dp)).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_description), null, tint = c.onSurfaceVariant)
                    Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        Text(f.name, style = MaterialTheme.typography.bodyLarge, color = c.onSurface, maxLines = 1)
                        Text(f.error ?: "${f.rows.size} rows · ${f.columns} columns", style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            color = if (f.error != null) c.error else c.onSurfaceVariant)
                    }
                    TextButton(onClick = { picker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) }, shapes = ButtonDefaults.shapes()) { Text("Change") }
                }
                preview?.let { p ->
                    val rows = rows(p, bills)
                    Column(Modifier.padding(top = 12.dp, bottom = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        rows.forEachIndexed { i, (icon, t, s) -> CountRow(icon, t, s, i, rows.size) }
                    }
                    if (p.planned > 0) GroupItem(0, 1) { SwitchLine("Add planned payments as bills", bills) { bills = it } }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.Reject); close() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).height(56.dp)) { Text("Cancel") }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
                        scope.launch {
                            val r = IvyImport.import(money, f.rows, bills)
                            sheet.hide(); onDone(resultText(r))
                        }
                    },
                    enabled = preview != null, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1.4f).height(56.dp),
                ) { Icon(painterResource(R.drawable.ic_download), null, Modifier.size(18.dp)); Text("  Import") }
            }
        }
    }
}

private fun plural(n: Int, one: String, many: String = one + "s") = "$n " + if (n == 1) one else many

/** The six lines of the board: what · what happens to it. */
private fun rows(p: IvyImport.Preview, bills: Boolean): List<Triple<Int, String, String>> = listOf(
    Triple(R.drawable.ic_receipt_long, if (p.expenses + p.incomes == 0) "None" else listOfNotNull(
        plural(p.expenses, "expense").takeIf { p.expenses > 0 }, plural(p.incomes, "income", "incomes").takeIf { p.incomes > 0 }).joinToString(" · "), "Transactions"),
    Triple(R.drawable.ic_swap_horiz, if (p.transfers == 0) "None" else plural(p.transfers, "transfer"), "Transfers"),
    Triple(R.drawable.ic_event_repeat, if (p.planned == 0) "None" else plural(p.planned, "planned payment"), if (p.planned == 0) "Planned payments" else if (bills) "Becomes a bill" else "Skipped"),
    Triple(R.drawable.ic_account_balance_wallet, if (p.newAccounts.isEmpty()) "None new" else p.newAccounts.joinToString(", "),
        if (p.newAccounts.isEmpty()) "Accounts" else plural(p.newAccounts.size, "new account")),
    Triple(R.drawable.ic_category, plural(p.categories, "category", "categories"),
        when { p.categories == 0 -> "Categories"; p.newCategories == p.categories -> "All new"; p.newCategories == 0 -> "Already in Kairo"; else -> "${p.newCategories} new" }),
    Triple(R.drawable.ic_content_copy, "${p.duplicates} already in Kairo", "Skipped"),
)

private fun resultText(r: IvyImport.Result): String = when {
    r.added == 0 && r.bills == 0 -> "Nothing new to import"
    else -> "Imported " + listOfNotNull(plural(r.added, "transaction").takeIf { r.added > 0 }, plural(r.bills, "bill").takeIf { r.bills > 0 }).joinToString(" and ")
}

/** Canvas .crw: a round icon, the count, and what happens to it underneath. */
@Composable
private fun CountRow(icon: Int, title: String, sub: String, i: Int, n: Int) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().background(KairoTheme.colors.item, seg(i, n)).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(KairoTheme.colors.chip, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(20.dp), tint = c.onSurfaceVariant)
        }
        Column(Modifier.padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"), color = c.onSurface)
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
        }
    }
}

/**
 * Drawer → Export as Ivy Wallet CSV: the system "Save as" screen, then every transaction written in
 * Ivy's 15 columns. Stays composed until the file is written, so leaving can't cancel the write.
 */
@Composable
fun IvyExport(money: MoneyRepository, onDone: (String?) -> Unit) {
    val context = LocalContext.current
    var target by remember { mutableStateOf<Uri?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> if (uri == null) onDone(null) else target = uri }
    LaunchedEffect(Unit) { save.launch("kairo-ivy-${java.time.LocalDate.now()}.csv") }
    LaunchedEffect(target) {
        val uri = target ?: return@LaunchedEffect
        val msg = runCatching { withContext(Dispatchers.IO) {
            val (csv, rows) = IvyImport.export(money)
            context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(csv) }
            "Exported " + plural(rows, "transaction")
        } }.getOrElse { "Couldn't export: ${it.message}" }
        onDone(msg)
    }
}
