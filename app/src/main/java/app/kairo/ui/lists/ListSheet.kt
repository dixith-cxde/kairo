@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package app.kairo.ui.lists

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import app.kairo.data.model.TaskList
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch

/**
 * Canvas "new list" / "edit lists" drawer: a preview row, Name, Colour, then Cancel / Create (or
 * Save, plus Delete when editing). Inbox can't be renamed or deleted (it isn't a real list row).
 * Deleting moves the list's tasks to Inbox first; the database would otherwise delete them too.
 */
@Composable
fun ListSheet(list: TaskList?, openCount: Int, onDismiss: () -> Unit) {
    val app = LocalKairoApp.current
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val lists = app.repository.lists
    var name by rememberSaveable { mutableStateOf(list?.name ?: "") }
    var color by rememberSaveable { mutableStateOf(list?.color ?: ListColor.Teal) }
    var confirm by remember { mutableStateOf(false) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (list == null) focus.requestFocus() }
    fun close() = scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = k.bg) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.padding(horizontal = 8.dp)) {
                Text(if (list == null) "New list" else "Edit list", style = MaterialTheme.typography.headlineSmall)
                Text("Shows in the list row above your tasks", style = MaterialTheme.typography.labelLarge, color = c.primary)
            }
            // Preview row, as on the canvas: the tag in the list colour, the name, its open count.
            Row(
                Modifier.fillMaxWidth().background(k.item, RoundedCornerShape(28.dp)).padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(painterResource(R.drawable.ic_label_filled), null, tint = k.accent(color))
                Text(name.ifBlank { "List name" }, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
                    color = if (name.isBlank()) c.onSurfaceVariant else c.onSurface)
                Text("$openCount", style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
            }
            Text("Name", style = MaterialTheme.typography.titleSmall, color = c.primary, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
            TextField(
                value = name, onValueChange = { name = it }, singleLine = true, placeholder = { Text("List name") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = k.item, unfocusedContainerColor = k.item,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Text("Colour", style = MaterialTheme.typography.titleSmall, color = c.primary, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ListColor.entries.forEach { co ->
                    val on = co == color
                    val size by animateDpAsState(if (on) 44.dp else 40.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "co")
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.size(size).background(k.accent(co), if (on) RoundedCornerShape(12.dp) else CircleShape)
                                .clickable(role = Role.RadioButton) { color = co }
                                .semantics { contentDescription = co.name; selected = on },
                            contentAlignment = Alignment.Center,
                        ) { if (on) Icon(painterResource(R.drawable.ic_check), null, tint = HabitOn(), modifier = Modifier.size(20.dp)) }
                    }
                }
            }
            if (list != null) TextButton(
                onClick = { confirm = true }, shapes = ButtonDefaults.shapes(), modifier = Modifier.padding(start = 0.dp),
            ) { Text(if (confirm) "Tap Delete again: its $openCount open tasks move to Inbox" else "Delete list", color = c.error) }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { close() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).height(56.dp)) { Text("Cancel") }
                if (confirm && list != null) Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: delete
                        scope.launch { app.repository.deleteListKeepTasks(list) }; close()
                    },
                    shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1.4f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.errorContainer, contentColor = c.onErrorContainer),
                ) { Text("Delete") }
                else Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save
                        scope.launch {
                            if (list == null) {
                                val taken = app.repository.listsNow().map { it.shape }.toSet()
                                val shape = ListShape.choices.firstOrNull { it !in taken } ?: ListShape.choices.first()
                                app.repository.addList(TaskList(0, name.trim(), shape, color))
                            } else app.repository.updateList(list.copy(name = name.trim(), color = color))
                        }
                        close()
                    },
                    enabled = name.isNotBlank(), shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1.4f).height(56.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp))
                    Text(if (list == null) "  Create" else "  Save")
                }
            }
        }
    }
}

/** Mark colour on a filled swatch (canvas --hon). */
@Composable
private fun HabitOn(): Color = app.kairo.ui.habits.HabitLook.onColor()
