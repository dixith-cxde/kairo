@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.money

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import app.kairo.R
import app.kairo.data.db.AccountEntity
import app.kairo.data.db.CategoryEntity
import app.kairo.data.db.TxKind
import app.kairo.data.money.Money
import app.kairo.data.money.MoneyRepository
import app.kairo.ui.shapes.MorphShape
import app.kairo.ui.shapes.polygon
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Add expense / income / transfer (canvas AddTransaction + Transfer boards). */
@Composable
fun AddTransactionSheet(
    money: MoneyRepository,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors

    var kind by rememberSaveable { mutableStateOf(TxKind.Expense) }
    var amount by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var accountId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id) }
    var toAccountId by rememberSaveable { mutableStateOf(accounts.getOrNull(1)?.id) }
    var note by rememberSaveable { mutableStateOf("") }
    val success = remember { Animatable(0f) }
    val fastSpatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()

    val paise = Money.parsePaise(amount)
    val cats = categories.filter { it.income == (kind == TxKind.Income) }
    val canSave = paise > 0 && accountId != null &&
        (if (kind == TxKind.Transfer) toAccountId != null && toAccountId != accountId else categoryId != null)

    fun press(key: String) {
        amount = when (key) {
            "⌫" -> amount.dropLast(1)
            "." -> if ("." in amount) amount else (amount.ifEmpty { "0" } + ".")
            else -> if (amount.substringAfter('.', "").length >= 2 && "." in amount) amount
                    else if (amount.length >= 9) amount else (if (amount == "0") key else amount + key)
        }
    }

    fun save() {
        if (!canSave) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
        scope.launch {
            val tags = Regex("""#\w+""").findAll(note).joinToString(" ") { it.value }
            val cleanNote = note.replace(Regex("""#\w+"""), "").trim()
            if (kind == TxKind.Transfer) money.transfer(paise, accountId!!, toAccountId!!, LocalDate.now(), cleanNote)
            else money.add(kind, paise, accountId!!, categoryId, LocalDate.now(), cleanNote, tags)
            // Scalloped success (Cookie12) blooms, then the sheet closes.
            success.snapTo(0f)
            success.animateTo(1f, fastSpatial)
            sheet.hide(); onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Box {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    TxKind.entries.forEachIndexed { i, kd ->
                        ToggleButton(
                            checked = kd == kind,
                            onCheckedChange = { kind = kd; categoryId = null },
                            shapes = when (i) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                TxKind.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                        ) { Text(kd.name) }
                    }
                }
                // Big amount with live ₹ grouping.
                Text(
                    if (amount.isEmpty()) Money.format(0) else Money.format(paise).let { if (amount.endsWith(".")) "$it." else it },
                    style = MaterialTheme.typography.displayMediumEmphasized,
                    color = if (paise == 0L) c.onSurfaceVariant else c.onSurface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )

                if (kind == TxKind.Transfer) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccountPicker("From", accounts, accountId, Modifier.weight(1f)) { accountId = it }
                        FilledTonalIconButton(onClick = { val a = accountId; accountId = toAccountId; toAccountId = a }) {
                            Icon(painterResource(R.drawable.ic_swap_vert), "Swap accounts")
                        }
                        AccountPicker("To", accounts, toAccountId, Modifier.weight(1f)) { toAccountId = it }
                    }
                } else {
                    // Categories: the picked one morphs from a circle into its own shape.
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        cats.forEach { cat -> CategoryPick(cat, cat.id == categoryId) { categoryId = cat.id } }
                    }
                    AccountPicker("Account", accounts, accountId, Modifier.fillMaxWidth()) { accountId = it }
                }
                OutlinedTextField(note, { note = it }, label = { Text("Note  #tags") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Keypad(::press)
                Button(
                    onClick = ::save,
                    enabled = canSave,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Save ${kind.name.lowercase()}", style = MaterialTheme.typography.titleMediumEmphasized) }
            }
            if (success.value > 0f) Box(
                Modifier.align(Alignment.Center).size(160.dp)
                    .graphicsLayer { scaleX = 0.4f + success.value * 0.6f; scaleY = scaleX; rotationZ = success.value * 45f }
                    .background(c.tertiaryContainer, MaterialShapes.Cookie12Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_check), "Saved", tint = c.onTertiaryContainer, modifier = Modifier.size(56.dp)) }
        }
    }
}

@Composable
private fun CategoryPick(cat: CategoryEntity, selected: Boolean, onClick: () -> Unit) {
    val k = KairoTheme.colors
    val progress by animateFloatAsState(if (selected) 1f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "cat")
    val morph = remember(cat.shape) { Morph(MaterialShapes.Circle, cat.shape.polygon) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(52.dp)
                .background(if (selected) k.accent(cat.color) else k.container(cat.color), MorphShape(morph, progress))
                .clickable(role = Role.RadioButton, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(categoryIcon(cat.icon)), cat.name,
                tint = if (selected) MaterialTheme.colorScheme.surface else k.onContainer(cat.color), modifier = Modifier.size(24.dp))
        }
        Text(cat.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun AccountPicker(label: String, accounts: List<AccountEntity>, selected: Long?, modifier: Modifier, onPick: (Long) -> Unit) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            accounts.forEach { a -> FilterChip(selected = a.id == selected, onClick = { onPick(a.id) }, label = { Text(a.name) }) }
        }
    }
}

/** Custom keypad: big rounded keys, tabular digits. */
@Composable
private fun Keypad(onKey: (String) -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    TextButton(
                        onClick = { onKey(key) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.textButtonColors(containerColor = KairoTheme.colors.itemHigh),
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        if (key == "⌫") Icon(painterResource(R.drawable.ic_backspace), "Delete digit")
                        else Text(key, style = MaterialTheme.typography.titleLargeEmphasized)
                    }
                }
            }
        }
    }
}
