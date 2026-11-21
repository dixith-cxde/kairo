@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.db.RecurringEntity
import app.kairo.data.db.RecurringKind
import app.kairo.data.money.Money
import app.kairo.data.money.MoneyRepository
import app.kairo.ui.common.DateChoice
import app.kairo.ui.common.DateSheet
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Add or edit a recurring bill, subscription or income (monthly; v1 keeps rules simple).
 * Same pieces as the add-transaction drawer: kind group, big amount, category and account pickers.
 */
@Composable
fun BillSheet(money: MoneyRepository, bill: RecurringEntity?, onDismiss: () -> Unit, onDelete: (RecurringEntity) -> Unit) {
    val accounts by money.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by money.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val history by money.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val usage = remember(history) { history.mapNotNull { it.categoryId }.groupingBy { it }.eachCount() }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors

    var kind by rememberSaveable { mutableStateOf(bill?.kind ?: RecurringKind.Bill) }
    var name by rememberSaveable { mutableStateOf(bill?.name ?: "") }
    var amount by rememberSaveable { mutableStateOf(bill?.let { (it.amountPaise / 100).toString() } ?: "") }
    var categoryId by rememberSaveable { mutableStateOf(bill?.categoryId) }
    var accountId by rememberSaveable { mutableStateOf(bill?.accountId) }
    var due by rememberSaveable { mutableStateOf((bill?.nextDue ?: LocalDate.now().plusMonths(1)).toString()) }
    var reminder by rememberSaveable { mutableStateOf(bill?.reminder ?: true) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var newAccount by rememberSaveable { mutableStateOf(false) }
    var newCategory by rememberSaveable { mutableStateOf(false) }
    if (accountId == null && accounts.isNotEmpty()) accountId = accounts.first().id
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (bill == null) focus.requestFocus() }

    val paise = Money.parsePaise(amount)
    val income = kind == RecurringKind.Income
    val cats = categories.filter { it.income == income }
    val canSave = name.isNotBlank() && paise > 0 && accountId != null
    val missing = when { name.isBlank() -> "Name it"; paise <= 0 -> "Enter an amount"; accountId == null -> "Add an account"; else -> null }

    fun save() {
        if (!canSave) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
        scope.launch {
            val d = LocalDate.parse(due)
            if (bill == null) money.addRecurring(name, kind, paise, accountId!!, categoryId, d, reminder)
            else money.updateRecurring(bill.copy(name = name, kind = kind, amountPaise = paise, accountId = accountId!!,
                categoryId = categoryId, nextDue = d, reminder = reminder))
            sheet.hide(); onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (bill == null) "New recurring" else "Edit recurring", style = MaterialTheme.typography.headlineSmallEmphasized, modifier = Modifier.weight(1f))
                if (bill != null) FilledTonalIconButton(onClick = { onDelete(bill) }, shapes = IconButtonDefaults.shapes()) {
                    Icon(painterResource(R.drawable.ic_delete), "Delete")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                RecurringKind.entries.forEachIndexed { i, kd ->
                    ToggleButton(
                        checked = kd == kind,
                        onCheckedChange = { if (kd != kind) { kind = kd; categoryId = null } },
                        shapes = when (i) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            RecurringKind.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                    ) { Text(kd.name) }
                }
            }
            KairoField(name, { name = it }, when (kind) { RecurringKind.Bill -> "Name · e.g. Rent"; RecurringKind.Subscription -> "Name · e.g. Netflix"
                RecurringKind.Income -> "Name · e.g. Salary" }, KeyboardCapitalization.Words, ImeAction.Next) {}
            BasicTextField(
                value = amount,
                onValueChange = { v -> cleanAmount(v)?.let { amount = it } },
                textStyle = MaterialTheme.typography.displayMediumEmphasized.copy(color = c.onSurface),
                cursorBrush = SolidColor(c.primary),
                singleLine = true,
                visualTransformation = RupeeGrouping,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (amount.isEmpty()) Text("₹0 a month", style = MaterialTheme.typography.displayMediumEmphasized, color = c.onSurfaceVariant)
                        inner()
                    }
                },
            )
            // Next due date: a card that opens the date drawer (no dialogs, rule 12).
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(k.itemHigh)
                    .clickable(role = Role.Button) { pickDate = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(painterResource(R.drawable.ic_event), null, tint = c.primary)
                Column(Modifier.weight(1f)) {
                    Text("Next due · then monthly", style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                    Text(LocalDate.parse(due).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")), style = MaterialTheme.typography.titleMediumEmphasized)
                }
                Icon(painterResource(R.drawable.ic_expand_more), null, tint = c.onSurfaceVariant)
            }
            CategoryPicker(
                if (income) "Category (optional)" else "Category", cats, categoryId, usage, optional = true,
                onCreate = { n -> scope.launch { categoryId = money.addCategory(n, "category", autoColor(n), income) } },
                onFullCreate = { newCategory = true },
            ) { categoryId = it }
            AccountSelect(if (income) "Into account" else "Paid from", accounts, accountId, onNew = { newAccount = true }) { accountId = it }
            Button(onClick = ::save, enabled = canSave, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(missing ?: if (bill == null) "Add ${kind.name.lowercase()}" else "Save", style = MaterialTheme.typography.titleMediumEmphasized)
            }
            Spacer(Modifier.size(4.dp))
        }
    }
    if (pickDate) DateSheet(initial = DateChoice(LocalDate.parse(due), null, false), title = "Next due date", onDismiss = { pickDate = false }) { ch ->
        ch.date?.let { due = it.toString() }; pickDate = false
    }
    if (newAccount) AccountSheet(money, null, canDelete = false, onDismiss = { newAccount = false }, onSaved = { accountId = it })
    if (newCategory) NewCategorySheet(income = income, onDismiss = { newCategory = false }) { n, icon, color ->
        scope.launch { categoryId = money.addCategory(n, icon, color, income) }
        newCategory = false
    }
}
