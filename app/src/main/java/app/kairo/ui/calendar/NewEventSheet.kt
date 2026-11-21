@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.calendar

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.calendar.PhoneCalendar
import app.kairo.ui.common.ConnectedGroup
import app.kairo.ui.common.DateChoice
import app.kairo.ui.common.DateSheet
import app.kairo.ui.common.LocalAppSnackbar
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The day the Calendar tab is showing, so + → Task / Event lands on it (owner, 2026-11-21). */
object CalendarFocus {
    var day by mutableStateOf<LocalDate?>(null)
}

private enum class Length(val label: String, val minutes: Long?) {
    AllDay("All day", null), Half("30 min", 30), Hour("1 hr", 60), Two("2 hr", 120)
}

/**
 * New event drawer: a title, the date & time drawer, a length group, which phone calendar, Save.
 * Written through the Android Calendar Provider (WRITE_CALENDAR, no sign-in); Google Calendar
 * uploads it on its next sync (docs/06-data-and-calendar.md).
 */
@Composable
fun NewEventSheet(calendar: PhoneCalendar, initialDay: LocalDate, onDismiss: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val snackbar = LocalAppSnackbar.current

    var granted by remember { mutableStateOf(calendar.hasPermission() && calendar.canWrite()) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r -> granted = r.values.all { it } }
    val cals by produceState(emptyList(), granted) { value = calendar.writableCalendars() }

    var title by remember { mutableStateOf("") }
    var day by remember { mutableStateOf(initialDay) }
    var time by remember {
        // Next whole hour today, 9 AM on other days.
        mutableStateOf(if (initialDay == LocalDate.now()) LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0) else LocalTime.of(9, 0))
    }
    var length by remember { mutableStateOf(Length.Hour) }
    var calId by remember { mutableStateOf<Long?>(null) }
    var dateSheet by remember { mutableStateOf(false) }
    var calMenu by remember { mutableStateOf(false) }
    val cal = cals.firstOrNull { it.id == calId } ?: cals.firstOrNull()

    fun close() = scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
    fun save() {
        val target = cal ?: return
        if (title.isBlank()) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
        val allDay = length == Length.AllDay
        val start = day.atTime(if (allDay) LocalTime.MIDNIGHT else time)
        val end = if (allDay) start else start.plusMinutes(length.minutes!!)
        val name = title.trim()
        scope.launch {
            val id = calendar.addEvent(target.id, name, start, end, allDay)
            calendar.requestSync()
            snackbar.showSnackbar(if (id != null) "Added “$name” to ${target.name}" else "Couldn't add the event")
        }
        close()
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(granted) { if (granted) focus.requestFocus() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!granted) {
                Text("New event", style = MaterialTheme.typography.headlineSmallEmphasized)
                Text(
                    "Kairo adds events straight to the calendars on this phone, including Google Calendar. Nothing goes through Kairo; there's no sign-in.",
                    style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant,
                )
                Button(
                    onClick = { ask.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) },
                    shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) { Text("Allow calendar access") }
                return@Column
            }
            Text(
                "New event${cal?.let { " in ${it.name}" } ?: ""}",
                style = MaterialTheme.typography.labelMediumEmphasized, color = c.tertiary,
            )
            BasicTextField(
                value = title, onValueChange = { title = it }, singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(color = c.onSurface),
                cursorBrush = SolidColor(c.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) Text("Event title", style = MaterialTheme.typography.titleLarge, color = c.onSurfaceVariant)
                        inner()
                    }
                },
            )
            // When: one tonal pill opens the date & time drawer.
            FilledTonalButton(onClick = { dateSheet = true }, shapes = ButtonDefaults.shapes()) {
                Icon(painterResource(R.drawable.ic_event), null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    day.format(DateTimeFormatter.ofPattern("EEE, d MMM")) +
                        if (length == Length.AllDay) "" else " · ${time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))}",
                )
            }
            ConnectedGroup(Length.entries, length, { it.label }) { length = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    FilledTonalButton(onClick = { calMenu = true }, enabled = cals.size > 1, shapes = ButtonDefaults.shapes()) {
                        Box(Modifier.size(12.dp).background(cal?.let { Color(it.color) } ?: c.tertiary, CircleShape))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(cal?.name ?: "No calendar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (cals.size > 1) Icon(painterResource(R.drawable.ic_expand_more), null, Modifier.padding(start = 4.dp).size(ButtonDefaults.IconSize))
                    }
                    DropdownMenuPopup(expanded = calMenu, onDismissRequest = { calMenu = false }) {
                        DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1)) {
                            cals.forEach { pc ->
                                SelectableDropdownMenuItem(
                                    selected = pc.id == cal?.id,
                                    onClick = { calId = pc.id; calMenu = false },
                                    text = { Text(pc.name) },
                                    supportingText = { Text(pc.account) },
                                    shapes = MenuDefaults.itemShapes(),
                                    leadingIcon = { Box(Modifier.size(14.dp).background(Color(pc.color), CircleShape)) },
                                )
                            }
                        }
                    }
                }
                // Solid primary save, same as quick add (rule 4 spirit).
                FilledIconButton(
                    onClick = ::save,
                    enabled = title.isNotBlank() && cal != null,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = c.primary, contentColor = c.onPrimary),
                    modifier = Modifier.size(width = 72.dp, height = 56.dp),
                ) { Icon(painterResource(R.drawable.ic_arrow_upward), contentDescription = "Add event") }
            }
            if (cals.isEmpty()) Text(
                "No calendar on this phone accepts new events. Add a Google account in Settings → Accounts.",
                style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant,
            )
        }
    }
    if (dateSheet) DateSheet(
        initial = DateChoice(day, if (length == Length.AllDay) null else time, false),
        title = "Event date",
        onDismiss = { dateSheet = false },
    ) { ch ->
        ch.date?.let { day = it }
        if (ch.time != null) { time = ch.time; if (length == Length.AllDay) length = Length.Hour }
        dateSheet = false
    }
}
