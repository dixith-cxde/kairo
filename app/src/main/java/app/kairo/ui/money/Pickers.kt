@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package app.kairo.ui.money

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.db.CategoryEntity
import app.kairo.ui.shapes.polygon
import app.kairo.ui.theme.KairoTheme

/*
 * Category and tag pickers in Stash's pattern (capture.tsx CollectionRow / TagRow, owner 2026-11-21):
 * - a small primary section title with text actions on the right ("New", "All"),
 * - the likely choices right there (a connected button group / a row of filter chips),
 * - creating inline in a pill field, so the keyboard flow is never broken by a menu or dialog.
 * Chips and toggle buttons don't take focus, so tapping them keeps the keyboard where it is.
 */

@Composable
private fun SectionHeader(title: String, actions: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        actions()
    }
}

/** Stash's pill input (h48, fully round, surfaceContainerHigh) with an optional prefix and action. */
@Composable
private fun PillInput(
    value: String, onChange: (String) -> Unit, placeholder: String, action: String,
    prefix: String? = null, caps: KeyboardCapitalization = KeyboardCapitalization.Sentences, onSubmit: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.onSurface),
            cursorBrush = SolidColor(c.primary),
            keyboardOptions = KeyboardOptions(capitalization = caps, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.weight(1f).focusRequester(focus),
            decorationBox = { inner ->
                Row(
                    Modifier.height(48.dp).background(c.surfaceContainerHigh, RoundedCornerShape(24.dp)).padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (prefix != null) Text(prefix, style = MaterialTheme.typography.bodyLarge, color = c.primary, modifier = Modifier.padding(end = 2.dp))
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant)
                        inner()
                    }
                }
            },
        )
        Button(onClick = onSubmit, enabled = value.isNotBlank(), shapes = ButtonDefaults.shapes(), modifier = Modifier.height(48.dp)) { Text(action) }
    }
}

/** A category's shape badge (its shape, color and icon). */
@Composable
private fun MiniBadge(cat: CategoryEntity, size: Int) {
    val k = KairoTheme.colors
    Box(Modifier.size(size.dp).background(k.container(cat.color), cat.shape.polygon.toShape()), contentAlignment = Alignment.Center) {
        Icon(painterResource(categoryIcon(cat.icon)), null, tint = k.onContainer(cat.color), modifier = Modifier.size((size * 0.58f).dp))
    }
}

/**
 * Category: "Category  [New] [All]" then a connected button group with the three likely ones
 * (the chosen one first, then the most used). "New" opens an inline name field; "All" a drawer.
 */
@Composable
fun CategoryPicker(
    title: String, cats: List<CategoryEntity>, selected: Long?, usage: Map<Long, Int>, optional: Boolean,
    onCreate: (String) -> Unit, onFullCreate: () -> Unit, onPick: (Long?) -> Unit,
) {
    var naming by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var all by rememberSaveable { mutableStateOf(false) }
    val shown = remember(cats, selected, usage) {
        val sel = cats.firstOrNull { it.id == selected }
        (listOfNotNull(sel) + cats.filter { it.id != selected }.sortedByDescending { usage[it.id] ?: 0 }).take(3)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title) {
            TextButton(onClick = { naming = !naming; name = "" }, shapes = ButtonDefaults.shapes()) { Text(if (naming) "Cancel" else "New") }
            if (!naming && cats.size > 3) TextButton(onClick = { all = true }, shapes = ButtonDefaults.shapes()) { Text("All") }
        }
        AnimatedVisibility(naming) {
            PillInput(name, { name = it }, "Category name", "Create") {
                if (name.isNotBlank()) { onCreate(name.trim()); naming = false; name = "" }
            }
        }
        if (shown.isEmpty()) Text("No categories yet. Tap New.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        else Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            shown.forEachIndexed { i, ct ->
                val on = ct.id == selected
                ToggleButton(
                    checked = on,
                    onCheckedChange = { onPick(if (on && optional) null else ct.id) },
                    shapes = when {
                        shown.size == 1 -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        i == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        i == shown.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                    // The chosen one grows a little (Stash: flex 1 → 1.4).
                    modifier = Modifier.weight(if (on) 1.4f else 1f).height(48.dp).semantics { role = Role.RadioButton },
                ) {
                    MiniBadge(ct, 22)
                    Spacer(Modifier.width(6.dp))
                    Text(ct.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    if (all) CategoryListSheet(cats, selected, optional, onDismiss = { all = false }, onNew = { all = false; onFullCreate() }) { onPick(it); all = false }
}

/** Every category in a drawer (Stash's CollectionPicker): shape tile, name, check on the chosen one. */
@Composable
private fun CategoryListSheet(
    cats: List<CategoryEntity>, selected: Long?, optional: Boolean, onDismiss: () -> Unit, onNew: () -> Unit, onPick: (Long?) -> Unit,
) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Category", style = MaterialTheme.typography.headlineSmallEmphasized, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            val rows: List<CategoryEntity?> = (if (optional) listOf(null) else emptyList<CategoryEntity?>()) + cats
            rows.forEachIndexed { i, ct ->
                val on = ct?.id == selected
                SegmentedListItem(
                    onClick = { onPick(ct?.id) },
                    shapes = ListItemDefaults.segmentedShapes(i, rows.size),
                    colors = ListItemDefaults.segmentedColors(containerColor = if (on) c.secondaryContainer else k.item),
                    leadingContent = { if (ct != null) MiniBadge(ct, 40) else Box(Modifier.size(40.dp)) },
                    trailingContent = { if (on) Icon(painterResource(R.drawable.ic_check), "Selected", tint = c.primary) },
                ) { Text(ct?.name ?: "No category") }
                if (i < rows.lastIndex) Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
            }
            Button(onClick = onNew, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp)) {
                Icon(painterResource(R.drawable.ic_add), null); Spacer(Modifier.width(8.dp)); Text("New category with icon")
            }
        }
    }
}

/** Lowercase, no leading #, spaces → "-" (Stash's tag rule). */
fun normalizeTag(s: String) = s.trim().removePrefix("#").trim().replace(Regex("""\s+"""), "-").lowercase()

/**
 * Tags: "Tags" then filter chips "#name" (recent ones plus the chosen ones; tap toggles) and a
 * "+ Tag" chip that opens an inline "#" field. Done adds the tag and keeps the field for the next.
 */
@Composable
fun TagPicker(selected: List<String>, recent: List<String>, onChange: (List<String>) -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    val chips = (selected + recent.filter { it !in selected }).take(10 + selected.size)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Tags")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            chips.forEach { tag ->
                val on = tag in selected
                FilterChip(
                    selected = on,
                    onClick = { onChange(if (on) selected - tag else selected + tag) },
                    label = { Text("#$tag") },
                    leadingIcon = if (on) {
                        { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(FilterChipDefaults.IconSize)) }
                    } else null,
                )
            }
            AssistChip(
                onClick = { adding = !adding; draft = "" },
                label = { Text(if (adding) "Close" else "Tag") },
                leadingIcon = { Icon(painterResource(if (adding) R.drawable.ic_close else R.drawable.ic_add), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) },
            )
        }
        AnimatedVisibility(adding) {
            PillInput(draft, { draft = it }, "New tag", "Add", prefix = "#", caps = KeyboardCapitalization.None) {
                val t = normalizeTag(draft)
                if (t.isNotEmpty() && t !in selected) onChange(selected + t)
                draft = ""
            }
        }
    }
}
