@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.kairo.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kairo.R
import app.kairo.data.db.AccountType
import app.kairo.data.model.ListColor
import app.kairo.data.money.Money
import app.kairo.data.money.MoneyRepository
import app.kairo.ui.money.accountIcon
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Canvas "New account" sheet: name, type (Bank · UPI · Cash · Card · Wallet), balance now; Cancel / Add. */
@Composable
fun NewAccountSheet(money: MoneyRepository, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(AccountType.Bank) }
    var balance by rememberSaveable { mutableStateOf("") }
    val types = listOf(AccountType.Bank, AccountType.UPI, AccountType.Cash, AccountType.Card, AccountType.Wallet)
    fun close() = scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = k.bg) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).imePadding()) {
            Text("New account", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp, bottom = 16.dp))
            BasicTextField(
                name, { if (it.length <= 40) name = it }, singleLine = true, cursorBrush = SolidColor(c.primary),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.onSurface),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth().background(k.item, RoundedCornerShape(16.dp)).padding(horizontal = 20.dp, vertical = 18.dp),
                decorationBox = { inner -> Box { if (name.isEmpty()) Text("Name, e.g. SBI Savings", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant); inner() } },
            )
            FlowRow(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { t ->
                    FilterChip(
                        selected = t == type, onClick = { type = t }, label = { Text(t.name) },
                        leadingIcon = { Icon(painterResource(accountIcon(t)), null, Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = c.primary, selectedLabelColor = c.onPrimary, selectedLeadingIconColor = c.onPrimary),
                    )
                }
            }
            Row(Modifier.padding(top = 16.dp).fillMaxWidth().background(k.item, RoundedCornerShape(16.dp)).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("₹", style = MaterialTheme.typography.headlineSmall, color = c.onSurfaceVariant)
                BasicTextField(
                    balance, { v -> if (v.count { it == '.' } <= 1 && v.all { it.isDigit() || it == '.' } && v.length <= 12) balance = v }, singleLine = true,
                    cursorBrush = SolidColor(c.primary), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(color = c.onSurface, fontSize = 24.sp, fontFeatureSettings = "tnum"),
                    modifier = Modifier.padding(start = 12.dp).weight(1f),
                    decorationBox = { inner -> Box { if (balance.isEmpty()) Text("Balance now", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant); inner() } },
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.Reject); close() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).height(56.dp)) { Text("Cancel") }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
                        val n = name.trim()
                        scope.launch {
                            val count = money.accounts.first().size
                            money.addAccount(n, type, ListColor.entries[count % ListColor.entries.size], Money.parsePaise(balance.ifBlank { "0" }))
                            sheet.hide(); onDone("Account added")
                        }
                    },
                    enabled = name.isNotBlank(), shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1.4f).height(56.dp),
                ) { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp)); Text("  Add") }
            }
        }
    }
}
