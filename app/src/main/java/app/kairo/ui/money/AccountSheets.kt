@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import app.kairo.R
import app.kairo.data.db.AccountEntity
import app.kairo.data.db.AccountType
import app.kairo.data.model.ListColor
import app.kairo.data.money.Money
import app.kairo.data.money.MoneyRepository
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch

/**
 * New / edit account drawer: name, type, color, starting balance. Editing also offers Delete,
 * which asks in a second drawer (rule 12: drawers, not dialogs).
 * [canDelete] is false for the last account, since money needs somewhere to live.
 */
@Composable
fun AccountSheet(
    money: MoneyRepository,
    account: AccountEntity?,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSaved: (Long) -> Unit = {},
    onDeleted: () -> Unit = {},
) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable { mutableStateOf(account?.name ?: "") }
    var type by rememberSaveable { mutableStateOf(account?.type ?: AccountType.Bank) }
    var color by rememberSaveable { mutableStateOf(account?.color ?: ListColor.Indigo) }
    var opening by rememberSaveable { mutableStateOf(account?.openingPaise?.let(::paiseToText) ?: "") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (account == null) focus.requestFocus() }

    fun save() {
        if (name.isBlank()) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
        scope.launch {
            val paise = Money.parsePaise(opening)
            val id = if (account == null) money.addAccount(name, type, color, paise)
            else account.id.also { money.updateAccount(account.copy(name = name, type = type, color = color, openingPaise = paise)) }
            sheet.hide(); onSaved(id); onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // The account's mark, live: its type icon on its color.
                Box(Modifier.size(44.dp).background(k.container(color), MaterialShapes.Cookie4Sided.toShape()), contentAlignment = Alignment.Center) {
                    Icon(painterResource(accountIcon(type)), null, tint = k.onContainer(color), modifier = Modifier.size(24.dp))
                }
                Text(if (account == null) "New account" else "Edit account", style = MaterialTheme.typography.headlineSmallEmphasized)
            }
            Box(Modifier.focusRequester(focus)) {
                KairoField(name, { name = it }, "Name · e.g. HDFC savings", KeyboardCapitalization.Words, ImeAction.Next) {}
            }
            Text("Type", style = MaterialTheme.typography.titleSmallEmphasized, color = c.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                AccountType.entries.forEachIndexed { i, t ->
                    ToggleButton(
                        checked = t == type,
                        onCheckedChange = { type = t },
                        shapes = when (i) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            AccountType.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                    ) { Text(t.name, maxLines = 1, softWrap = false) }
                }
            }
            Text("Color", style = MaterialTheme.typography.titleSmallEmphasized, color = c.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ListColor.entries.forEach { co ->
                    Box(Modifier.size(36.dp).clip(CircleShape).background(k.accent(co)).clickable(role = Role.RadioButton) { color = co },
                        contentAlignment = Alignment.Center) {
                        if (co == color) Icon(painterResource(R.drawable.ic_check), null, tint = c.surface, modifier = Modifier.size(18.dp))
                    }
                }
            }
            KairoField(
                opening, { v -> cleanAmount(v)?.let { opening = it } },
                if (account == null) "Starting balance (optional)" else "Starting balance",
                KeyboardCapitalization.None, ImeAction.Done, onDone = ::save,
                keyboardType = KeyboardType.Decimal, transformation = RupeeGrouping,
            )
            if (account != null) Text(
                "The balance is this plus everything recorded in the account.",
                style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant,
            )
            Button(onClick = ::save, enabled = name.isNotBlank(), shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(if (account == null) "Create account" else "Save", style = MaterialTheme.typography.titleMediumEmphasized)
            }
            if (account != null && canDelete) TextButton(
                onClick = { confirmDelete = true }, shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.textButtonColors(contentColor = c.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(painterResource(R.drawable.ic_delete), null, Modifier.size(18.dp))
                Text("Delete account", Modifier.padding(start = 8.dp))
            }
        }
    }

    if (confirmDelete && account != null) DeleteAccountSheet(money, account, onDismiss = { confirmDelete = false }) {
        confirmDelete = false
        scope.launch { sheet.hide(); onDismiss(); onDeleted() }
    }
}

/** "Delete HDFC and its 23 transactions?" — says exactly what goes. */
@Composable
private fun DeleteAccountSheet(money: MoneyRepository, account: AccountEntity, onDismiss: () -> Unit, onDeleted: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var count by remember { mutableIntStateOf(-1) }
    LaunchedEffect(account.id) { count = money.transactionCount(account.id) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Delete ${account.name}?", style = MaterialTheme.typography.headlineSmallEmphasized)
            Text(
                when (count) {
                    -1 -> " "
                    0 -> "It has no transactions. Its bills, if any, go too. This can't be undone."
                    1 -> "Its 1 transaction and any bills on it go too. This can't be undone."
                    else -> "Its $count transactions and any bills on it go too. Transfers lose both sides. This can't be undone."
                },
                style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onDismiss, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).height(56.dp)) { Text("Keep") }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: delete
                        scope.launch { money.deleteAccount(account.id); onDeleted() }
                    },
                    enabled = count >= 0,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(containerColor = c.error, contentColor = c.onError),
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Text("Delete") }
            }
        }
    }
}

/** Keep amount input to digits, one dot, ≤ 9 rupee digits and ≤ 2 paise digits; null = reject. */
internal fun cleanAmount(v: String): String? {
    val clean = v.filter { it.isDigit() || it == '.' }
    val parts = clean.split('.')
    return clean.takeIf { parts.size <= 2 && parts[0].length <= 9 && (parts.getOrNull(1)?.length ?: 0) <= 2 }
}

private fun paiseToText(p: Long): String = when {
    p == 0L -> ""
    p % 100 == 0L -> (p / 100).toString()
    else -> "%d.%02d".format(p / 100, p % 100)
}
