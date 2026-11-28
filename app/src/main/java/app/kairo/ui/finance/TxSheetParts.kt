@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package app.kairo.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.db.TxKind
import app.kairo.ui.money.normalizeTag
import app.kairo.ui.theme.KairoTheme

/** Expense / Income / Transfer as connected toggle buttons with arrows (canvas type group). */
@Composable
internal fun TypeGroup(kind: TxKind, onPick: (TxKind) -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        TxKind.entries.forEachIndexed { i, t ->
            ToggleButton(
                checked = t == kind, onCheckedChange = { onPick(t) },
                shapes = when (i) { 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(); 2 -> ButtonGroupDefaults.connectedTrailingButtonShapes(); else -> ButtonGroupDefaults.connectedMiddleButtonShapes() },
                colors = ToggleButtonDefaults.colors(containerColor = k.item, contentColor = c.onSurfaceVariant, checkedContainerColor = c.primary, checkedContentColor = c.onPrimary),
                contentPadding = PaddingValues(horizontal = 6.dp),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Icon(painterResource(if (t == TxKind.Transfer) R.drawable.ic_swap_vert else R.drawable.ic_arrow_forward), null,
                    Modifier.size(18.dp).rotate(when (t) { TxKind.Expense -> -45f; TxKind.Income -> 135f; TxKind.Transfer -> 90f }))
                Spacer(Modifier.width(6.dp))
                Text(t.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
            }
        }
    }
}

/** One choice row in a group (canvas "Category ▾ Food"): label left, the pick on the right, an Expressive grouped menu. */
@Composable
internal fun <T> PickRow(
    label: String, value: String, icon: Int?, tint: Color, shape: RoundedCornerShape,
    options: List<T>, selected: T?, name: (T) -> String, optIcon: (T) -> Int?, optTint: (T) -> Color, onPick: (T) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(bottom = 3.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(KairoTheme.colors.item).clickable(role = Role.DropdownList) { open = true }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = c.onSurface, modifier = Modifier.weight(1f))
            if (icon != null) Icon(painterResource(icon), null, Modifier.size(20.dp), tint = tint)
            Text("  $value", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = c.onSurface, maxLines = 1)
            Icon(painterResource(R.drawable.ic_expand_more), null, Modifier.padding(start = 6.dp).size(20.dp), tint = c.onSurfaceVariant)
        }
        DropdownMenuPopup(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.align(Alignment.TopEnd)) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1)) {
                options.forEach { o ->
                    SelectableDropdownMenuItem(
                        selected = o == selected, onClick = { onPick(o); open = false }, text = { Text(name(o)) }, shapes = MenuDefaults.itemShapes(),
                        leadingIcon = optIcon(o)?.let { ic -> { Icon(painterResource(ic), null, Modifier.size(20.dp), tint = optTint(o)) } },
                    )
                }
            }
        }
    }
}

/** Canvas tags row: picked tags as chips with ✕, then "Add tag" (Enter adds it). */
@Composable
internal fun TagLine(tags: List<String>, onChange: (List<String>) -> Unit) {
    val c = MaterialTheme.colorScheme
    var draft by remember { mutableStateOf("") }
    fun add() { val t = normalizeTag(draft); if (t.isNotEmpty() && t !in tags) onChange(tags + t); draft = "" }
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(28.dp)).background(KairoTheme.colors.item).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center, itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        tags.forEach { t ->
            InputChip(selected = false, onClick = { onChange(tags - t) }, label = { Text("#$t") },
                trailingIcon = { Icon(painterResource(R.drawable.ic_close), "Remove #$t", Modifier.size(16.dp)) })
        }
        BasicTextField(
            draft, { draft = it }, singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.onSurface), cursorBrush = SolidColor(c.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { add() }),
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp).width(140.dp),
            decorationBox = { inner -> Box { if (draft.isEmpty()) Text("Add tag", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant); inner() } },
        )
    }
}
