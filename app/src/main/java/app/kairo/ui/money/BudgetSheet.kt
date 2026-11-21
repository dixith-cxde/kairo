@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.db.AccountEntity
import app.kairo.data.db.BudgetEntity
import app.kairo.data.db.CategoryEntity
import app.kairo.data.money.Money
import app.kairo.data.money.MoneyRepository
import kotlinx.coroutines.launch

private enum class BudgetScope(val label: String) { Overall("Monthly"), Category("Category"), Account("Account") }

/**
 * New / edit budget drawer (rule 12). A budget is a monthly limit for everything, one category or
 * one account. Editing keeps the scope and offers Delete (with Undo on the screen).
 */
@Composable
fun BudgetSheet(
    money: MoneyRepository,
    budget: BudgetEntity?,
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onDelete: (BudgetEntity) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialScope = when {
        budget == null -> if (budgets.none { it.overall }) BudgetScope.Overall else BudgetScope.Category
        budget.categoryId != null -> BudgetScope.Category
        budget.accountId != null -> BudgetScope.Account
        else -> BudgetScope.Overall
    }
    var kind by rememberSaveable { mutableStateOf(initialScope) }
    var categoryId by rememberSaveable { mutableStateOf(budget?.categoryId) }
    var accountId by rememberSaveable { mutableStateOf(budget?.accountId) }
    var amount by rememberSaveable { mutableStateOf(budget?.limitPaise?.let { (it / 100).toString() } ?: "") }
    val paise = Money.parsePaise(amount)
    val ready = paise > 0 && when (kind) {
        BudgetScope.Overall -> true
        BudgetScope.Category -> categoryId != null
        BudgetScope.Account -> accountId != null
    }
    // Saving a scope that already has a budget updates that one; say so before it happens.
    val replaces = budget == null && ready && budgets.any {
        when (kind) {
            BudgetScope.Overall -> it.overall
            BudgetScope.Category -> it.categoryId == categoryId
            BudgetScope.Account -> it.accountId == accountId
        }
    }

    fun save() {
        if (!ready) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
        val b = BudgetEntity(
            id = budget?.id ?: 0,
            categoryId = categoryId.takeIf { kind == BudgetScope.Category },
            accountId = accountId.takeIf { kind == BudgetScope.Account },
            limitPaise = paise,
        )
        scope.launch { money.saveBudget(budgets, b); sheet.hide(); onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).background(c.primaryContainer, MaterialShapes.Cookie9Sided.toShape()), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_savings), null, tint = c.onPrimaryContainer, modifier = Modifier.size(24.dp))
                }
                Text(if (budget == null) "New budget" else "Edit budget", style = MaterialTheme.typography.headlineSmallEmphasized)
            }
            if (budget == null) {
                Text("For", style = MaterialTheme.typography.titleSmallEmphasized, color = c.primary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    BudgetScope.entries.forEachIndexed { i, s ->
                        ToggleButton(
                            checked = s == kind,
                            onCheckedChange = { kind = s },
                            shapes = when (i) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                BudgetScope.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                        ) { Text(s.label, maxLines = 1, softWrap = false) }
                    }
                }
            }
            when (kind) {
                BudgetScope.Overall -> Text(
                    "All spending this month, from every account.",
                    style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant,
                )
                BudgetScope.Category -> Picker(categories, categoryId, enabled = budget == null, label = { it.name }, id = { it.id },
                    badge = { CategoryBadge(it, 24) }) { categoryId = it }
                BudgetScope.Account -> Picker(accounts, accountId, enabled = budget == null, label = { it.name }, id = { it.id },
                    badge = { AccountBadge(it, 24) }) { accountId = it }
            }
            KairoField(
                amount, { v -> cleanAmount(v)?.let { amount = it } }, "Limit per month",
                KeyboardCapitalization.None, ImeAction.Done, onDone = ::save,
                keyboardType = KeyboardType.Decimal, transformation = RupeeGrouping,
            )
            if (replaces) Text(
                "This replaces the budget that's already set for it.",
                style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant,
            )
            Button(onClick = ::save, enabled = ready, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(if (budget == null) "Add budget" else "Save", style = MaterialTheme.typography.titleMediumEmphasized)
            }
            if (budget != null) TextButton(
                onClick = { onDelete(budget) }, shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.textButtonColors(contentColor = c.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(painterResource(R.drawable.ic_delete), null, Modifier.size(18.dp))
                Text("Delete budget", Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** Chips to pick one category or account; when editing, only the chosen one shows. */
@Composable
private fun <T> Picker(
    items: List<T>, selected: Long?, enabled: Boolean, label: (T) -> String, id: (T) -> Long,
    badge: @Composable (T) -> Unit, onPick: (Long) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.filter { enabled || id(it) == selected }.forEach { item ->
            FilterChip(
                selected = id(item) == selected,
                onClick = { if (enabled) onPick(id(item)) },
                label = { Text(label(item)) },
                leadingIcon = { badge(item) },
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}
