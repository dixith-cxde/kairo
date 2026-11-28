@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package app.kairo.ui.calendar

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.calendar.PhoneCalendar
import app.kairo.ui.common.DateChoice
import app.kairo.ui.common.DateSheet
import app.kairo.ui.common.LocalAppSnackbar
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** The day the Calendar tab is showing, so + → Task / Event lands on it (owner, 2026-11-21). */
object CalendarFocus {
    var day by mutableStateOf<LocalDate?>(null)
}

private val Reminders = listOf<Pair<Int?, String>>(null to "None", 0 to "At start", 10 to "10 min", 30 to "30 min", 60 to "1 hr", 1440 to "1 day")
private val Repeats = listOf<Pair<String?, String>>(null to "Never", "FREQ=DAILY" to "Daily", "FREQ=WEEKLY" to "Weekly", "FREQ=MONTHLY" to "Monthly", "FREQ=YEARLY" to "Yearly")
private val Lengths = listOf(30L to "30 min", 60L to "1 hr", 120L to "2 hr", 1440L to "1 day")

/**
 * v2 New event drawer (canvas "new event" board), saving to a phone calendar through the Calendar
 * Provider (no sign-in; Google uploads it on its next sync). Title; From → To with Birthday, All day
 * and length chips; the calendar, reminder and repeat; location and meeting link; a note.
 */
@Composable
fun NewEventSheet(calendar: PhoneCalendar, initialDay: LocalDate, onDismiss: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val snackbar = LocalAppSnackbar.current
    var granted by remember { mutableStateOf(calendar.hasPermission() && calendar.canWrite()) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r -> granted = r.values.all { it } }
    val cals by produceState(emptyList(), granted) { value = calendar.writableCalendars() }

    val start0 = initialDay.atTime(if (initialDay == LocalDate.now()) LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0) else LocalTime.of(9, 0))
    var title by remember { mutableStateOf("") }
    var from by remember { mutableStateOf(start0) }
    var to by remember { mutableStateOf(start0.plusHours(1)) }
    var allDay by remember { mutableStateOf(false) }
    var bday by remember { mutableStateOf(false) }
    var calId by remember { mutableStateOf<Long?>(null) }
    var remind by remember { mutableStateOf<Int?>(10) }
    var repeat by remember { mutableStateOf<String?>(null) }
    var loc by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var pick by remember { mutableStateOf<String?>(null) }
    var calMenu by remember { mutableStateOf(false) }
    val cal = cals.firstOrNull { it.id == calId } ?: cals.firstOrNull()
    val whole = allDay || bday

    // Clash line (canvas .evclash): another timed event overlapping this one.
    val clash by produceState<String?>(null, from, to, whole, granted) {
        value = if (!granted || whole) null else calendar.events(from.toLocalDate(), to.toLocalDate().plusDays(1))
            .firstOrNull { !it.allDay && it.start < to && it.end > from }?.title
    }

    fun close() = scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
    fun save() {
        val target = cal ?: return
        if (title.isBlank()) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
        val name = title.trim()
        val desc = listOf(link.trim(), note.trim()).filter { it.isNotEmpty() }.joinToString("\n\n")
        val s = if (whole) from.toLocalDate().atStartOfDay() else from
        val e = if (whole) to.toLocalDate().atStartOfDay().let { if (it < s) s else it } else to.coerceAtLeast(from.plusMinutes(15))
        scope.launch {
            val id = calendar.addEvent(target.id, name, s, e, whole, loc, desc, if (bday) "FREQ=YEARLY" else repeat, if (whole && remind == null) null else remind)
            calendar.requestSync()
            snackbar.showSnackbar(if (id != null) "Added “$name” to ${target.name}" else "Couldn't add the event")
        }
        close()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = k.bg) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).imePadding()) {
            Text("New event", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp, bottom = 16.dp))
            if (!granted) {
                Text("Events go straight into this phone's calendars, Google included. No sign-in.", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp))
                Button(onClick = { ask.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) }, shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp)) { Text("Allow calendar access") }
                return@Column
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                BasicTextField(
                    title, { title = it }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.onSurface), cursorBrush = SolidColor(c.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).background(k.item, RoundedCornerShape(16.dp)).padding(horizontal = 20.dp, vertical = 18.dp),
                    decorationBox = { inner -> Box { if (title.isEmpty()) Text("Event title", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant); inner() } },
                )
                GroupItem(0, 2) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WhenColumn("From", from.format(ShortDay), if (whole) null else from.format(Clock), Modifier.weight(1f), { pick = "fromDate" }, { pick = "fromTime" })
                        Icon(painterResource(R.drawable.ic_arrow_forward), null, Modifier.padding(top = 20.dp).size(18.dp), tint = c.onSurfaceVariant)
                        WhenColumn("To", to.format(ShortDay), if (whole) null else to.format(Clock), Modifier.weight(1f), { pick = "toDate" }, { pick = "toTime" })
                    }
                    clash?.let { Text("Overlaps $it", style = MaterialTheme.typography.labelLarge, color = c.error, modifier = Modifier.padding(top = 8.dp)) }
                }
                GroupItem(1, 2, Modifier.padding(bottom = 12.dp)) {
                    SwitchLine("Birthday", bday, R.drawable.ic_cake_filled) { bday = it }
                    if (!bday) SwitchLine("All day", allDay) { allDay = it }
                    if (!whole) {
                        val dur = ChronoUnit.MINUTES.between(from, to)
                        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                        ChipLine(null, Lengths, dur) { m -> to = from.plusMinutes(m) }
                    }
                }
                GroupItem(0, 3) {
                    Box {
                        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Calendar", style = MaterialTheme.typography.bodyLarge, color = c.onSurface, modifier = Modifier.weight(1f))
                            Box(Modifier.size(8.dp).background(cal?.let { Color(it.color) } ?: c.tertiary, CircleShape))
                            FilledTonalButton(onClick = { calMenu = true }, enabled = cals.size > 1, shapes = ButtonDefaults.shapes(), modifier = Modifier.padding(start = 8.dp)) {
                                Text(cal?.name ?: "No calendar", maxLines = 1)
                                if (cals.size > 1) Icon(painterResource(R.drawable.ic_expand_more), null, Modifier.size(18.dp))
                            }
                        }
                        DropdownMenuPopup(expanded = calMenu, onDismissRequest = { calMenu = false }) {
                            DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1)) {
                                cals.forEach { pc ->
                                    SelectableDropdownMenuItem(
                                        selected = pc.id == cal?.id, onClick = { calId = pc.id; calMenu = false },
                                        text = { Text(pc.name) }, supportingText = { Text(pc.account) }, shapes = MenuDefaults.itemShapes(),
                                        leadingIcon = { Box(Modifier.size(14.dp).background(Color(pc.color), CircleShape)) },
                                    )
                                }
                            }
                        }
                    }
                }
                GroupItem(1, 3) { ChipLine("Reminder", Reminders, remind) { remind = it } }
                GroupItem(2, 3, Modifier.padding(bottom = 12.dp)) { if (!bday) ChipLine("Repeat", Repeats, repeat) { repeat = it } else Text("Repeats yearly", color = c.onSurfaceVariant) }
                GroupItem(0, 2) { FieldLine(R.drawable.ic_location_on, loc, "Location or maps link") { loc = it } }
                GroupItem(1, 2, Modifier.padding(bottom = 12.dp)) { FieldLine(R.drawable.ic_video_call, link, "Meeting link", url = true) { link = it } }
                BasicTextField(
                    note, { note = it }, textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.onSurface), cursorBrush = SolidColor(c.primary), minLines = 3,
                    modifier = Modifier.fillMaxWidth().background(k.item, RoundedCornerShape(16.dp)).padding(horizontal = 20.dp, vertical = 16.dp),
                    decorationBox = { inner -> Box { if (note.isEmpty()) Text("Note", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant); inner() } },
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { close() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).height(56.dp)) { Text("Cancel") }
                Button(onClick = ::save, enabled = title.isNotBlank() && cal != null, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1.4f).height(56.dp)) {
                    Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp)); Text("  Add")
                }
            }
        }
    }
    when (pick) {
        "fromDate", "toDate" -> DateSheet(
            initial = DateChoice(if (pick == "fromDate") from.toLocalDate() else to.toLocalDate(), null, false), title = if (pick == "fromDate") "From" else "To",
            onDismiss = { pick = null },
        ) { ch ->
            ch.date?.let { d ->
                if (pick == "fromDate") { val len = ChronoUnit.MINUTES.between(from, to); from = d.atTime(from.toLocalTime()); to = from.plusMinutes(len) }
                else to = d.atTime(to.toLocalTime()).let { if (it < from) from.plusHours(1) else it }
            }
            pick = null
        }
        "fromTime" -> TimeSheet(from.toLocalTime(), onDismiss = { pick = null }) { t -> val len = ChronoUnit.MINUTES.between(from, to); from = from.toLocalDate().atTime(t); to = from.plusMinutes(len) }
        "toTime" -> TimeSheet(to.toLocalTime(), onDismiss = { pick = null }) { t -> to = to.toLocalDate().atTime(t).let { if (it <= from) it.plusDays(1) else it } }
    }
}

private fun LocalDateTime.coerceAtLeast(o: LocalDateTime) = if (this < o) o else this
