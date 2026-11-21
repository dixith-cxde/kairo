@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.money

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import app.kairo.data.model.ListColor
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import app.kairo.data.db.RecurringKind
import androidx.compose.material3.FilterChip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    initialKind: TxKind = TxKind.Expense,
    initialAccount: Long? = null,
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val c = MaterialTheme.colorScheme

    var kind by rememberSaveable { mutableStateOf(initialKind) }
    var amount by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var accountId by rememberSaveable { mutableStateOf(initialAccount ?: accounts.firstOrNull()?.id) }
    var toAccountId by rememberSaveable { mutableStateOf(accounts.firstOrNull { it.id != accountId }?.id) }
    var note by rememberSaveable { mutableStateOf("") }   // title (optional)
    var tags by rememberSaveable { mutableStateOf(listOf<String>()) }
    var repeat by rememberSaveable { mutableStateOf(false) } // also make it a monthly bill / income
    val history by money.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val usage = remember(history) { history.mapNotNull { it.categoryId }.groupingBy { it }.eachCount() }
    val recentTags = remember(history) {
        history.flatMap { t -> t.tags.split(' ').map(::normalizeTag).filter(String::isNotEmpty) }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }.take(10)
    }
    var newCategory by rememberSaveable { mutableStateOf(false) }
    var newAccountFor by rememberSaveable { mutableStateOf<String?>(null) } // "from" | "to" | null
    val amountFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { amountFocus.requestFocus() } // number keyboard straight away
    val success = remember { Animatable(0f) }
    val fastSpatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()

    val paise = Money.parsePaise(amount)
    val cats = categories.filter { it.income == (kind == TxKind.Income) }
    // Income (e.g. salary) needs no category; expenses do, so budgets and insights stay meaningful.
    val canSave = paise > 0 && accountId != null && when (kind) {
        TxKind.Transfer -> toAccountId != null && toAccountId != accountId
        TxKind.Expense -> categoryId != null
        TxKind.Income -> true
    }
    val missing = when {
        paise <= 0 -> "Enter an amount"
        kind == TxKind.Expense && categoryId == null -> "Pick a category"
        kind == TxKind.Transfer && toAccountId == accountId -> "Pick two different accounts"
        else -> null
    }

    fun save() {
        if (!canSave) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
        scope.launch {
            val tagText = tags.joinToString(" ") { "#$it" }
            val title = note.trim()
            if (kind == TxKind.Transfer) money.transfer(paise, accountId!!, toAccountId!!, LocalDate.now(), title)
            else money.add(kind, paise, accountId!!, categoryId, LocalDate.now(), title, tagText)
            // "Repeats monthly": a bill (or income) due again on this day next month.
            if (repeat && kind != TxKind.Transfer) money.addRecurring(
                name = title.ifBlank { categories.firstOrNull { it.id == categoryId }?.name ?: kind.name },
                kind = if (kind == TxKind.Income) RecurringKind.Income else RecurringKind.Bill,
                amountPaise = paise, accountId = accountId!!, categoryId = categoryId,
                nextDue = LocalDate.now().plusMonths(1),
            )
            // Scalloped success (Cookie12) blooms, then the drawer closes.
            success.snapTo(0f)
            success.animateTo(1f, fastSpatial)
            sheet.hide(); onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Box {
            Column(
                Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                app.kairo.ui.common.ConnectedGroup(TxKind.entries, kind, { it.name }) { kind = it; categoryId = null }
                // Big amount: system number keyboard, live ₹ Indian grouping while you type.
                BasicTextField(
                    value = amount,
                    onValueChange = { v -> cleanAmount(v)?.let { amount = it } },
                    textStyle = MaterialTheme.typography.displayMediumEmphasized.copy(color = c.onSurface),
                    cursorBrush = SolidColor(c.primary),
                    singleLine = true,
                    visualTransformation = RupeeGrouping,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().focusRequester(amountFocus),
                    decorationBox = { inner ->
                        Box {
                            if (amount.isEmpty()) Text("₹0", style = MaterialTheme.typography.displayMediumEmphasized, color = c.onSurfaceVariant)
                            inner()
                        }
                    },
                )

                if (kind == TxKind.Transfer) {
                    // Transfer: From card, swap, To card — each a dropdown of accounts with balances.
                    AccountSelect("From", accounts, accountId, onNew = { newAccountFor = "from" }) { accountId = it }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        FilledTonalIconButton(onClick = { val a = accountId; accountId = toAccountId; toAccountId = a }, shapes = IconButtonDefaults.shapes()) {
                            Icon(painterResource(R.drawable.ic_swap_vert), "Swap accounts")
                        }
                    }
                    AccountSelect("To", accounts, toAccountId, onNew = { newAccountFor = "to" }) { toAccountId = it }
                } else {
                    // Category, Stash style: likely ones in a connected group, New inline, All in a drawer.
                    CategoryPicker(
                        if (kind == TxKind.Income) "Category (optional)" else "Category",
                        cats, categoryId, usage, optional = kind == TxKind.Income,
                        onCreate = { name -> scope.launch { categoryId = money.addCategory(name, "category", autoColor(name), kind == TxKind.Income) } },
                        onFullCreate = { newCategory = true },
                    ) { categoryId = it }
                    AccountSelect("Account", accounts, accountId, onNew = { newAccountFor = "from" }) { accountId = it }
                }
                // Normal keyboard for text. The keyboard's ✓ on the last field saves.
                KairoField(note, { note = it }, "Title (optional)", KeyboardCapitalization.Sentences, ImeAction.Done) { save() }
                if (kind != TxKind.Transfer) {
                    TagPicker(tags, recentTags) { tags = it }
                    FilterChip(
                        selected = repeat, onClick = { repeat = !repeat },
                        label = { Text(if (kind == TxKind.Income) "Repeats monthly" else "Repeats monthly · add as a bill") },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_repeat), null, Modifier.size(18.dp)) },
                    )
                }

                Button(
                    onClick = ::save,
                    enabled = canSave,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text(missing ?: "Save ${kind.name.lowercase()}", style = MaterialTheme.typography.titleMediumEmphasized) }
            }
            if (success.value > 0f) Box(
                Modifier.align(Alignment.Center).size(160.dp)
                    .graphicsLayer { scaleX = 0.4f + success.value * 0.6f; scaleY = scaleX; rotationZ = success.value * 45f }
                    .background(c.tertiaryContainer, MaterialShapes.Cookie12Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_check), "Saved", tint = c.onTertiaryContainer, modifier = Modifier.size(56.dp)) }
        }
    }
    if (newCategory) NewCategorySheet(income = kind == TxKind.Income, onDismiss = { newCategory = false }) { name, icon, color ->
        scope.launch { categoryId = money.addCategory(name, icon, color, kind == TxKind.Income) }
        newCategory = false
    }
    newAccountFor?.let { slot ->
        AccountSheet(money, null, canDelete = false, onDismiss = { newAccountFor = null },
            onSaved = { id -> if (slot == "to") toAccountId = id else accountId = id })
    }
}

/** The "+ New …" item at the bottom of a grouped menu. */
@Composable
private fun NewItem(label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        shape = MenuDefaults.standaloneItemShape, // the only item in its group
        leadingIcon = { Icon(painterResource(R.drawable.ic_add), null, Modifier.size(20.dp)) },
    )
}

/** Account as a card-like field that opens an Expressive grouped menu (name + balance). */
@Composable
internal fun AccountSelect(label: String, accounts: List<AccountEntity>, selected: Long?, onNew: () -> Unit, onPick: (Long) -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val acc = accounts.firstOrNull { it.id == selected }
    Box {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(k.itemHigh)
                .clickable(role = Role.Button) { open = true }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (acc != null) Box(Modifier.size(36.dp).background(k.container(acc.color), MaterialShapes.Cookie4Sided.toShape()), contentAlignment = Alignment.Center) {
                Icon(painterResource(accountIcon(acc.type)), null, tint = k.onContainer(acc.color), modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                Text(acc?.name ?: "Choose account", style = MaterialTheme.typography.titleMediumEmphasized)
            }
            Icon(painterResource(R.drawable.ic_expand_more), null, tint = c.onSurfaceVariant)
        }
        DropdownMenuPopup(expanded = open, onDismissRequest = { open = false }, properties = app.kairo.ui.common.KeepKeyboard) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 2)) {
                accounts.forEach { a ->
                    SelectableDropdownMenuItem(
                        selected = a.id == selected,
                        onClick = { onPick(a.id); open = false },
                        text = { Text(a.name) },
                        shapes = MenuDefaults.itemShapes(),
                        leadingIcon = { Icon(painterResource(accountIcon(a.type)), null, tint = k.accent(a.color), modifier = Modifier.size(20.dp)) },
                    )
                }
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(1, 2)) {
                NewItem("New account") { open = false; onNew() }
            }
        }
    }
}

/** Make a category: name, icon, color. Becomes a chip you can pick from then on. */
@Composable
internal fun NewCategorySheet(income: Boolean, onDismiss: () -> Unit, onCreate: (String, String, ListColor) -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf(CategoryIconChoices.first()) }
    var color by rememberSaveable { mutableStateOf(ListColor.Coral) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (income) "New income category" else "New category", style = MaterialTheme.typography.headlineSmallEmphasized)
            TextField(
                value = name, onValueChange = { name = it }, label = { Text("Name · e.g. Food & drinks") }, singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onCreate(name, icon, color) }),
                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Text("Icon", style = MaterialTheme.typography.titleSmallEmphasized, color = c.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryIconChoices.forEach { ic ->
                    val on = ic == icon
                    Box(
                        Modifier.size(44.dp).clip(if (on) MaterialShapes.Cookie9Sided.toShape() else CircleShape)
                            .background(if (on) k.container(color) else k.itemHigh)
                            .clickable(role = Role.RadioButton) { icon = ic },
                        contentAlignment = Alignment.Center,
                    ) { Icon(painterResource(categoryIcon(ic)), ic, tint = if (on) k.onContainer(color) else c.onSurfaceVariant, modifier = Modifier.size(22.dp)) }
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
            Button(onClick = { onCreate(name, icon, color) }, enabled = name.isNotBlank(), shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Create category", style = MaterialTheme.typography.titleMediumEmphasized) }
        }
    }
}

/** Text field in Kairo's style: filled itemHigh, 12dp corners, no indicator line. */
@Composable
internal fun KairoField(
    value: String, onChange: (String) -> Unit, label: String, caps: KeyboardCapitalization, ime: ImeAction,
    keyboardType: KeyboardType = KeyboardType.Text, transformation: VisualTransformation = VisualTransformation.None,
    onDone: () -> Unit,
) {
    TextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        shape = RoundedCornerShape(12.dp),
        visualTransformation = transformation,
        keyboardOptions = KeyboardOptions(capitalization = caps, imeAction = ime, keyboardType = keyboardType),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = KairoTheme.colors.itemHigh, unfocusedContainerColor = KairoTheme.colors.itemHigh,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Shows raw "123456.5" as "₹1,23,456.5" (Indian grouping, rule 11) while the field keeps plain
 * digits. The offset map moves the cursor across the added ₹ and commas.
 */
internal object RupeeGrouping : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val intPart = raw.substringBefore('.')
        val frac = if ('.' in raw) "." + raw.substringAfter('.') else ""
        val grouped = if (intPart.length <= 3) intPart else {
            val last3 = intPart.takeLast(3)
            val rest = intPart.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
            "$rest,$last3"
        }
        val out = if (raw.isEmpty()) "" else "₹$grouped$frac"
        // original index -> transformed index
        val o2t = IntArray(raw.length + 1)
        var t = if (raw.isEmpty()) 0 else 1
        var oi = 0
        for (ch in grouped) { if (ch == ',') { t++; continue }; o2t[oi] = t; oi++; t++ }
        o2t[oi] = t
        for (i in frac.indices) { o2t[intPart.length + i] = t + i; o2t[intPart.length + i + 1] = t + i + 1 }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = o2t[offset.coerceIn(0, raw.length)].coerceAtMost(out.length)
            override fun transformedToOriginal(offset: Int): Int {
                var best = 0
                for (i in 0..raw.length) if (o2t[i] <= offset) best = i
                return best
            }
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

/** A color for a category made inline (no picker shown): stable per name. */
internal fun autoColor(name: String) = ListColor.entries[(name.lowercase().hashCode() and 0x7fffffff) % ListColor.entries.size]
