@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.quickadd

import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.model.Priority
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import app.kairo.reminders.rememberAskNotifications
import app.kairo.ui.shapes.polygon
import app.kairo.ui.common.ListPickerMenu
import app.kairo.ui.common.DateChoice
import app.kairo.ui.common.DateSheet
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Quick add (canvas QuickAdd board): one line of natural language, live-highlighted tokens,
 * chips that mirror what was understood, solid primary send button.
 */
@Composable
fun QuickAddSheet(
    lists: List<TaskList>,
    defaultDue: LocalDate?,
    defaultListId: Long? = null,
    initialText: String = "",
    onAdd: (Task) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val askNotifications = rememberAskNotifications()

    var text by remember { mutableStateOf(initialText) }
    var pickedList by remember { mutableStateOf<TaskList?>(null) }
    var priorityOverride by remember { mutableStateOf<Priority?>(null) }
    var reminder by remember { mutableStateOf(true) } // on by default (owner, 2026-11-21)
    var listMenu by remember { mutableStateOf(false) }
    var dateSheet by remember { mutableStateOf(false) }
    var dateOverride by remember { mutableStateOf<DateChoice?>(null) }
    var repeat by remember { mutableStateOf<app.kairo.data.model.Repeat?>(null) }
    var repeatSheet by remember { mutableStateOf(false) }

    val parsed = remember(text) { QuickAddParser.parse(text) }
    val tokenList = parsed.listName?.let { n -> lists.firstOrNull { it.name.equals(n, ignoreCase = true) } }
    // No list picked = Inbox (no default lists, owner 2026-11-21).
    val list = (pickedList ?: tokenList ?: lists.firstOrNull { it.id == defaultListId })?.takeIf { it.id > 0 }
    val due = dateOverride?.let { it.date } ?: parsed.due ?: defaultDue
    val time = dateOverride?.let { it.time } ?: parsed.time
    val priority = priorityOverride ?: parsed.priority ?: Priority.None

    fun close() = scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    fun submit() {
        if (parsed.title.isBlank()) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
        onAdd(Task(0, parsed.title, list?.id, due ?: repeat?.next(null), time, priority, reminder = reminder, repeat = repeat))
        close() // done → the drawer closes (owner, 2026-11-21)
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val dateBg = c.secondaryContainer
    val prioBg = k.container(app.kairo.data.model.ListColor.Coral)
    val listBg = tokenList?.let { k.container(it.color) } ?: c.surfaceVariant
    val highlight = remember(parsed, dateBg, prioBg, listBg) {
        VisualTransformation { s ->
            val styled = buildAnnotatedString {
                append(s.text)
                parsed.tokens.forEach { t ->
                    if (t.range.last < s.text.length) addStyle(
                        SpanStyle(background = when (t.kind) {
                            Token.Kind.Date -> dateBg
                            Token.Kind.Priority -> prioBg
                            Token.Kind.List -> listBg
                        }),
                        t.range.first, t.range.last + 1,
                    )
                }
            }
            TransformedText(styled, OffsetMapping.Identity)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                "New task in ${list?.name ?: "Inbox"}",
                style = MaterialTheme.typography.labelMediumEmphasized,
                color = c.primary,
            )
          Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = text,
                singleLine = true, // so the keyboard's ✓ saves instead of adding a new line
                onValueChange = { text = it },
                textStyle = MaterialTheme.typography.titleLarge.copy(color = c.onSurface),
                cursorBrush = SolidColor(c.primary),
                visualTransformation = highlight,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f).padding(top = 6.dp).focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) Text(
                            "e.g. Call Mom tomorrow 7pm !high #Family",
                            style = MaterialTheme.typography.titleLarge,
                            color = c.onSurfaceVariant,
                        )
                        inner()
                    }
                },
            )
            // Priority right in the input: a small flag that opens a compact Expressive menu.
            Box {
                var prioMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { prioMenu = true }, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        painterResource(if (priority == Priority.None) R.drawable.ic_flag else R.drawable.ic_flag_filled),
                        contentDescription = "Priority: ${priority.name}",
                        tint = prioColor(priority),
                    )
                }
                DropdownMenuPopup(expanded = prioMenu, onDismissRequest = { prioMenu = false }, properties = app.kairo.ui.common.KeepKeyboard) {
                    DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1)) {
                        listOf(Priority.High, Priority.Medium, Priority.Low, Priority.None).forEach { p ->
                            SelectableDropdownMenuItem(
                                selected = p == priority,
                                onClick = { priorityOverride = p; prioMenu = false },
                                text = { Text(if (p == Priority.None) "No priority" else p.name) },
                                shapes = MenuDefaults.itemShapes(),
                                leadingIcon = {
                                    Icon(painterResource(if (p == Priority.None) R.drawable.ic_flag else R.drawable.ic_flag_filled), null,
                                        tint = prioColor(p), modifier = Modifier.size(20.dp))
                                },
                            )
                        }
                    }
                }
            }
          }
        }

        // Chips mirror what the parser understood; tapping changes it.
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = due != null,
                onClick = { dateSheet = true },
                label = { Text(describe(due, time)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_event), null, Modifier.size(18.dp)) },
            )
            FilterChip(
                selected = reminder,
                onClick = { if (!reminder) askNotifications(); reminder = !reminder },
                label = { Text("Remind") },
                leadingIcon = { Icon(painterResource(R.drawable.ic_notifications), null, Modifier.size(18.dp)) },
            )
            FilterChip(
                selected = repeat != null,
                onClick = { repeatSheet = true },
                label = { Text(repeat?.label() ?: "Repeat") },
                leadingIcon = { Icon(painterResource(R.drawable.ic_repeat), null, Modifier.size(18.dp)) },
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { listMenu = true }) {
                    Icon(painterResource(R.drawable.ic_drive_file_move), contentDescription = "Choose list")
                }
                ListPickerMenu(listMenu, lists, list?.id, onDismiss = { listMenu = false }) { pickedList = it }
            }
            Spacer(Modifier.weight(1f))
            // Solid primary send (rule 4 spirit): 72x56, 16dp corners.
            FilledIconButton(
                onClick = ::submit,
                enabled = parsed.title.isNotBlank(),
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = c.primary, contentColor = c.onPrimary),
                modifier = Modifier.size(width = 72.dp, height = 56.dp),
            ) {
                Icon(painterResource(R.drawable.ic_arrow_upward), contentDescription = "Add task")
            }
        }
    }
    if (repeatSheet) app.kairo.ui.common.RepeatSheet(repeat, due, onDismiss = { repeatSheet = false }) { repeat = it; repeatSheet = false }
    if (dateSheet) DateSheet(
        initial = DateChoice(due, time, reminder),
        onDismiss = { dateSheet = false },
    ) { ch -> dateOverride = ch; reminder = ch.reminder; if (ch.reminder) askNotifications(); dateSheet = false }
}

@Composable
private fun prioColor(p: Priority) = when (p) {
    Priority.High -> KairoTheme.colors.prioHigh
    Priority.Medium -> KairoTheme.colors.prioMed
    Priority.Low -> KairoTheme.colors.prioLow
    Priority.None -> MaterialTheme.colorScheme.onSurfaceVariant
}

private val DayFmt = DateTimeFormatter.ofPattern("EEE, d MMM")
private val TimeFmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

private fun describe(due: LocalDate?, time: java.time.LocalTime?): String {
    if (due == null) return "No date"
    val today = LocalDate.now()
    val day = when (due) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> due.format(DayFmt)
    }
    return if (time != null) "$day, ${time.format(TimeFmt)}" else day
}
