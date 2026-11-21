@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.money

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors

    var kind by rememberSaveable { mutableStateOf(initialKind) }
    var amount by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var accountId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id) }
    var toAccountId by rememberSaveable { mutableStateOf(accounts.getOrNull(1)?.id) }
    var note by rememberSaveable { mutableStateOf("") }   // title (optional)
    var tags by rememberSaveable { mutableStateOf("") }
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

    fun save() {
        if (!canSave) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
        scope.launch {
            val tagText = Regex("""\w+""").findAll(tags).joinToString(" ") { "#" + it.value }
            val cleanNote = note.trim()
            if (kind == TxKind.Transfer) money.transfer(paise, accountId!!, toAccountId!!, LocalDate.now(), cleanNote)
            else money.add(kind, paise, accountId!!, categoryId, LocalDate.now(), cleanNote, tagText)
            // Scalloped success (Cookie12) blooms, then the sheet closes.
            success.snapTo(0f)
            success.animateTo(1f, fastSpatial)
            sheet.hide(); onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Box {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                // Big amount: system number keyboard, live ₹ Indian grouping while you type.
                BasicTextField(
                    value = amount,
                    onValueChange = { v ->
                        val clean = v.filter { it.isDigit() || it == '.' }
                        val parts = clean.split('.')
                        if (parts.size <= 2 && parts[0].length <= 9 && (parts.getOrNull(1)?.length ?: 0) <= 2) amount = clean
                    },
                    textStyle = MaterialTheme.typography.displayMediumEmphasized.copy(color = c.onSurface),
                    cursorBrush = SolidColor(c.primary),
                    singleLine = true,
                    visualTransformation = RupeeGrouping,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).focusRequester(amountFocus),
                    decorationBox = { inner ->
                        Box {
                            if (amount.isEmpty()) Text("₹0", style = MaterialTheme.typography.displayMediumEmphasized, color = c.onSurfaceVariant)
                            inner()
                        }
                    },
                )

                if (kind == TxKind.Transfer) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccountPicker("From", accounts, accountId, Modifier.weight(1f)) { accountId = it }
                        FilledTonalIconButton(onClick = { val a = accountId; accountId = toAccountId; toAccountId = a }, shapes = IconButtonDefaults.shapes()) {
                            Icon(painterResource(R.drawable.ic_swap_vert), "Swap accounts")
                        }
                        AccountPicker("To", accounts, toAccountId, Modifier.weight(1f)) { toAccountId = it }
                    }
                } else {
                    // Categories: the picked one morphs from a circle into its own shape. Tap again to clear.
                    Text(if (kind == TxKind.Income) "Category (optional)" else "Category",
                        style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        cats.forEach { cat -> CategoryPick(cat, cat.id == categoryId) { categoryId = if (categoryId == cat.id) null else cat.id } }
                    }
                    AccountPicker("Account", accounts, accountId, Modifier.fillMaxWidth()) { accountId = it }
                }
                // Normal keyboard for text. Filled fields, no underline (Expressive).
                KairoField(note, { note = it }, "Title (optional)", KeyboardCapitalization.Sentences)
                KairoField(tags, { tags = it }, "Tags (optional) · e.g. trip food", KeyboardCapitalization.None)
                Button(
                    onClick = ::save,
                    enabled = canSave,
                    shapes = ButtonDefaults.shapes(),
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


/** Text field in Kairo's style: filled itemHigh, 12dp corners, no indicator line. */
@Composable
private fun KairoField(value: String, onChange: (String) -> Unit, label: String, caps: KeyboardCapitalization) {
    TextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(capitalization = caps, imeAction = ImeAction.Next),
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
private object RupeeGrouping : VisualTransformation {
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
