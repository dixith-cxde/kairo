@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package app.kairo.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.db.RecurringKind
import app.kairo.data.db.TransactionEntity
import app.kairo.data.db.TxKind
import app.kairo.data.money.Money
import app.kairo.data.money.MoneyRepository
import app.kairo.ui.calendar.GroupItem
import app.kairo.ui.calendar.SwitchLine
import app.kairo.ui.calendar.TimeSheet
import app.kairo.ui.common.DateChoice
import app.kairo.ui.common.DateSheet
import app.kairo.ui.money.accountIcon
import app.kairo.ui.money.categoryIcon
import app.kairo.ui.money.dayLabel
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * v2 add / edit drawer (canvas "add expense" board): type group, the big ₹ amount, title, Category
 * and Account menus (From / To with swap for a transfer), date & time, Repeats monthly (adds a
 * bill), tags, description; Cancel / Save. [edit] = the transaction to change (a transfer's "from" leg).
 */
@Composable
fun TxSheet(money: MoneyRepository, initialKind: TxKind, edit: TransactionEntity? = null, onDismiss: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val accounts by money.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val cats by money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val txs by money.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val peer = edit?.transferPeerId?.let { id -> txs.firstOrNull { it.id == id } } ?: edit?.let { e -> txs.firstOrNull { it.transferPeerId == e.id } }
    var kind by remember { mutableStateOf(edit?.kind ?: initialKind) }
    var amount by remember { mutableStateOf(edit?.let { Money.format(kotlin.math.abs(it.amountPaise)).filter { ch -> ch.isDigit() || ch == '.' } } ?: "") }
    var title by remember { mutableStateOf(edit?.note ?: "") }
    var catId by remember { mutableStateOf(edit?.categoryId) }
    var accId by remember { mutableStateOf(edit?.accountId) }
    var toId by remember { mutableStateOf(peer?.accountId) }
    var date by remember { mutableStateOf(edit?.date ?: LocalDate.now()) }
    var time by remember { mutableStateOf(edit?.time ?: if (edit == null) LocalTime.now().withSecond(0).withNano(0) else null) }
    var monthly by remember { mutableStateOf(false) }
    var tags by remember { mutableStateOf(edit?.tags?.split(' ')?.map { it.removePrefix("#") }?.filter { it.isNotBlank() } ?: emptyList()) }
    var desc by remember { mutableStateOf(edit?.description ?: "") }
    var pick by remember { mutableStateOf<String?>(null) }
    val kindCats = cats.filter { it.income == (kind == TxKind.Income) }
    val cat = cats.firstOrNull { it.id == catId }?.takeIf { it.income == (kind == TxKind.Income) } ?: kindCats.firstOrNull()
    val acc = accounts.firstOrNull { it.id == accId } ?: accounts.firstOrNull()
    val to = accounts.firstOrNull { it.id == toId && it.id != acc?.id } ?: accounts.firstOrNull { it.id != acc?.id }
    val paise = runCatching { Money.parsePaise(amount) }.getOrDefault(0L)
    val ok = paise > 0 && acc != null && (kind != TxKind.Transfer || to != null)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (edit == null) focus.requestFocus() }
    fun close() = scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
    fun save() {
        if (!ok) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
        val t = title.trim().ifBlank { if (kind == TxKind.Transfer) "Transfer" else cat?.name ?: "" }
        scope.launch {
            money.save(edit?.id, kind, paise, acc!!.id, to?.id, if (kind == TxKind.Transfer) null else cat?.id, date, time, t, desc.trim(), tags.joinToString(" ") { "#$it" })
            if (monthly && kind != TxKind.Transfer) money.addRecurring(t, if (kind == TxKind.Income) RecurringKind.Income else RecurringKind.Bill, paise, acc.id, cat?.id, date.plusMonths(1))
        }
        close()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = k.bg) {
        Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 16.dp).imePadding()) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                TypeGroup(kind) { kind = it }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("₹", style = MaterialTheme.typography.displaySmall, color = c.onSurfaceVariant)
                    BasicTextField(
                        amount, { v -> if (v.count { it == '.' } <= 1 && v.all { it.isDigit() || it == '.' } && v.substringAfter('.', "").length <= 2) amount = v },
                        singleLine = true, cursorBrush = SolidColor(c.primary),
                        textStyle = MaterialTheme.typography.displayMedium.copy(fontSize = 48.sp, fontWeight = FontWeight.Medium, color = c.onSurface, fontFeatureSettings = "tnum"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.padding(start = 12.dp).weight(1f).focusRequester(focus),
                        decorationBox = { inner -> Box { if (amount.isEmpty()) Text("0", style = MaterialTheme.typography.displayMedium.copy(fontSize = 48.sp), color = c.onSurfaceVariant); inner() } },
                    )
                }
                HorizontalDivider(Modifier.padding(top = 4.dp, bottom = 12.dp), thickness = 2.dp, color = c.primary)
                BasicTextField(
                    title, { title = it }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.onSurface), cursorBrush = SolidColor(c.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).background(k.item, RoundedCornerShape(16.dp)).padding(horizontal = 20.dp, vertical = 18.dp),
                    decorationBox = { inner -> Box { if (title.isEmpty()) Text("Title", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant); inner() } },
                )
                if (kind != TxKind.Transfer) {
                    PickRow("Category", cat?.name ?: "None", cat?.let { categoryIcon(it.icon) }, cat?.let { k.accent(it.color) } ?: c.onSurfaceVariant, seg(0, 9),
                        kindCats, cat, { it.name }, { categoryIcon(it.icon) }, { k.accent(it.color) }) { catId = it.id }
                    PickRow("Account", acc?.name ?: "None", acc?.let { accountIcon(it.type) }, acc?.let { k.accent(it.color) } ?: c.onSurfaceVariant, seg(1, 4),
                        accounts, acc, { it.name }, { accountIcon(it.type) }, { k.accent(it.color) }) { accId = it.id }
                } else {
                    Box {
                        Column {
                            PickRow("From", acc?.name ?: "None", acc?.let { accountIcon(it.type) }, acc?.let { k.accent(it.color) } ?: c.onSurfaceVariant, seg(0, 4),
                                accounts, acc, { it.name }, { accountIcon(it.type) }, { k.accent(it.color) }) { accId = it.id }
                            PickRow("To", to?.name ?: "None", to?.let { accountIcon(it.type) }, to?.let { k.accent(it.color) } ?: c.onSurfaceVariant, seg(1, 4),
                                accounts.filter { it.id != acc?.id }, to, { it.name }, { accountIcon(it.type) }, { k.accent(it.color) }) { toId = it.id }
                        }
                        IconButton(onClick = { val a = acc?.id; accId = to?.id; toId = a }, shapes = IconButtonDefaults.shapes(), modifier = Modifier.align(Alignment.CenterStart).padding(start = 72.dp)) {
                            Icon(painterResource(R.drawable.ic_swap_vert), "Swap accounts", tint = c.primary)
                        }
                    }
                }
                val repeatRow = kind != TxKind.Transfer && edit == null
                Row(
                    Modifier.fillMaxWidth().padding(bottom = if (repeatRow) 3.dp else 12.dp).heightIn(min = 56.dp).background(k.item, seg(2, if (repeatRow) 4 else 3)).clickable { pick = "date" }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_schedule), null, tint = c.onSurfaceVariant)
                    Text("  " + dayLabel(date).let { if (it.contains(",")) date.format(DateTimeFormatter.ofPattern("EEE d MMM")) else it } + (time?.let { " · " + it.format(DateTimeFormatter.ofPattern("h:mm a")) } ?: ""),
                        style = MaterialTheme.typography.bodyLarge, color = c.onSurface, modifier = Modifier.weight(1f))
                    Icon(painterResource(R.drawable.ic_chevron_right), null, tint = c.onSurfaceVariant)
                }
                if (repeatRow) GroupItem(3, 4, Modifier.padding(bottom = 9.dp)) { SwitchLine("Repeats monthly", monthly, R.drawable.ic_event_repeat) { monthly = it } }
                TagLine(tags) { tags = it }
                BasicTextField(
                    desc, { desc = it }, textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.onSurface), cursorBrush = SolidColor(c.primary), minLines = 3,
                    modifier = Modifier.fillMaxWidth().background(k.item, RoundedCornerShape(28.dp)).padding(horizontal = 20.dp, vertical = 16.dp),
                    decorationBox = { inner -> Box { if (desc.isEmpty()) Text("Description", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant); inner() } },
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { close() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).height(56.dp)) { Text("Cancel") }
                Button(onClick = ::save, enabled = ok, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1.4f).height(56.dp)) {
                    Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp)); Text("  Save " + kind.name.lowercase())
                }
            }
        }
    }
    when (pick) {
        "date" -> DateSheet(initial = DateChoice(date, null, false), title = "Date", onDismiss = { pick = null }) { ch -> ch.date?.let { date = it }; pick = "time" }
        "time" -> TimeSheet(time ?: LocalTime.now(), onDismiss = { pick = null }) { time = it; pick = null }
    }
}
