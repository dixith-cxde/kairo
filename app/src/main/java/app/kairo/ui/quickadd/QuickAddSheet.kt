@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.quickadd

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import app.kairo.ui.shapes.polygon
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
    onAdd: (Task) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors

    var text by remember { mutableStateOf("") }
    var pickedList by remember { mutableStateOf<TaskList?>(null) }
    var priorityOverride by remember { mutableStateOf<Priority?>(null) }
    var reminder by remember { mutableStateOf(false) }
    var listMenu by remember { mutableStateOf(false) }

    val parsed = remember(text) { QuickAddParser.parse(text) }
    val tokenList = parsed.listName?.let { n -> lists.firstOrNull { it.name.equals(n, ignoreCase = true) } }
    val list = pickedList ?: tokenList ?: lists.firstOrNull { it.name == "Personal" } ?: lists.firstOrNull()
    val due = parsed.due ?: defaultDue
    val priority = priorityOverride ?: parsed.priority ?: Priority.None

    fun close() = scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    fun submit() {
        if (parsed.title.isBlank() || list == null) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
        onAdd(Task(0, parsed.title, list.id, due, parsed.time, priority, reminder = reminder))
        text = ""; priorityOverride = null; reminder = false // stay open for the next one
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
                "New task in ${list?.name ?: "…"}",
                style = MaterialTheme.typography.labelMediumEmphasized,
                color = c.primary,
            )
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = MaterialTheme.typography.titleLarge.copy(color = c.onSurface),
                cursorBrush = SolidColor(c.primary),
                visualTransformation = highlight,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).focusRequester(focus),
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
        }

        // Chips mirror what the parser understood; tapping changes it.
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = due != null,
                onClick = { /* Date picker: step 7 */ },
                label = { Text(describe(due, parsed.time)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_event), null, Modifier.size(18.dp)) },
            )
            FilterChip(
                selected = priority != Priority.None,
                onClick = { priorityOverride = Priority.entries[(priority.ordinal + 1) % Priority.entries.size] },
                label = { Text(if (priority == Priority.None) "Priority" else priority.name) },
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_flag_filled), null, Modifier.size(18.dp),
                        tint = when (priority) {
                            Priority.High -> k.prioHigh; Priority.Medium -> k.prioMed
                            Priority.Low -> k.prioLow; Priority.None -> c.onSurfaceVariant
                        },
                    )
                },
            )
            FilterChip(
                selected = reminder,
                onClick = { reminder = !reminder },
                label = { Text("Remind") },
                leadingIcon = { Icon(painterResource(R.drawable.ic_notifications), null, Modifier.size(18.dp)) },
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                IconButton(onClick = { listMenu = true }) {
                    Icon(painterResource(R.drawable.ic_drive_file_move), contentDescription = "Choose list")
                }
                DropdownMenu(expanded = listMenu, onDismissRequest = { listMenu = false }) {
                    lists.forEach { l ->
                        DropdownMenuItem(
                            text = { Text(l.name) },
                            leadingIcon = { Box(Modifier.size(16.dp).background(k.accent(l.color), l.shape.polygon.toShape())) },
                            onClick = { pickedList = l; listMenu = false },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            // Solid primary send (rule 4 spirit): 72x56, 16dp corners.
            FilledIconButton(
                onClick = ::submit,
                enabled = parsed.title.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = c.primary, contentColor = c.onPrimary),
                modifier = Modifier.size(width = 72.dp, height = 56.dp),
            ) {
                Icon(painterResource(R.drawable.ic_arrow_upward), contentDescription = "Add task")
            }
        }
    }
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
