@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.model.Repeat
import app.kairo.data.model.long
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * Repeat drawer (rule 12: drawers, not dialogs). Quick picks save and close in one tap; "Custom"
 * opens an interval stepper, a Day/Week/Month/Year connected group and, for weeks, weekday toggles.
 * [onPick] gets null for "Doesn't repeat".
 */
@Composable
fun RepeatSheet(current: Repeat?, due: LocalDate?, onDismiss: () -> Unit, onPick: (Repeat?) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val day = (due ?: LocalDate.now()).dayOfWeek
    fun close(r: Repeat?) = scope.launch { sheet.hide() }.invokeOnCompletion { onPick(r) }

    val quick: List<Pair<String, Repeat?>> = listOf(
        "Doesn't repeat" to null,
        "Every day" to Repeat(Repeat.Unit.Day),
        "Weekdays" to Repeat(Repeat.Unit.Week, 1, Repeat.WEEKDAYS),
        "Every ${day.long()}" to Repeat(Repeat.Unit.Week, 1, setOf(day)),
        "Every month on day ${(due ?: LocalDate.now()).dayOfMonth}" to Repeat(Repeat.Unit.Month),
        "Every year" to Repeat(Repeat.Unit.Year),
    )
    val isQuick = quick.any { it.second == current }
    var custom by rememberSaveable { mutableStateOf(!isQuick) }
    var unit by rememberSaveable { mutableStateOf(current?.unit ?: Repeat.Unit.Week) }
    var interval by rememberSaveable { mutableIntStateOf(current?.interval ?: 1) }
    var days by rememberSaveable { mutableStateOf(current?.days?.map { it.value }?.toSet() ?: setOf(day.value)) }
    val built = Repeat(unit, interval, if (unit == Repeat.Unit.Week) days.map(DayOfWeek::of).toSet() else emptySet())

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 20.dp),
        ) {
            Text("Repeat", style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            val count = quick.size + 1
            quick.forEachIndexed { i, (label, r) ->
                SegmentedListItem(
                    selected = !custom && r == current,
                    onClick = { close(r) },
                    shapes = ListItemDefaults.segmentedShapes(i, count),
                    colors = selectableRowColors(),
                ) { Text(label) }
                Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
            }
            SegmentedListItem(
                selected = custom,
                onClick = { custom = true },
                shapes = ListItemDefaults.segmentedShapes(quick.size, count),
                colors = selectableRowColors(),
                supportingContent = if (custom) {{ Text(built.label()) }} else null,
            ) { Text("Custom") }

            AnimatedVisibility(custom) {
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Interval stepper: "Every [−] 2 [+]".
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Every", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp).weight(1f))
                        FilledTonalIconButton(onClick = { if (interval > 1) interval-- }, enabled = interval > 1, shapes = IconButtonDefaults.shapes()) {
                            Icon(painterResource(R.drawable.ic_remove), "Fewer")
                        }
                        Text(
                            "$interval", style = MaterialTheme.typography.titleLargeEmphasized.copy(fontFeatureSettings = "tnum"), // rule 8
                            textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 48.dp),
                        )
                        FilledTonalIconButton(onClick = { if (interval < 99) interval++ }, shapes = IconButtonDefaults.shapes()) {
                            Icon(painterResource(R.drawable.ic_add), "More")
                        }
                    }
                    // Unit: connected button group.
                    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                        Repeat.Unit.entries.forEachIndexed { i, u ->
                            val word = when (u) { Repeat.Unit.Day -> "Day"; Repeat.Unit.Week -> "Week"; Repeat.Unit.Month -> "Month"; Repeat.Unit.Year -> "Year" }
                            ToggleButton(
                                checked = unit == u,
                                onCheckedChange = { unit = u },
                                shapes = when (i) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    Repeat.Unit.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                            ) { Text(if (interval == 1) word else word + "s") }
                        }
                    }
                    // Weekdays (weeks only): seven connected toggles, several can be on.
                    AnimatedVisibility(unit == Repeat.Unit.Week) {
                        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                            DayOfWeek.entries.forEachIndexed { i, d ->
                                ToggleButton(
                                    checked = d.value in days,
                                    // Keep at least one day picked.
                                    onCheckedChange = { on -> days = if (on) days + d.value else (days - d.value).ifEmpty { days } },
                                    shapes = when (i) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        6 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.weight(1f).semantics { contentDescription = d.long() },
                                ) { Text(d.long().take(1)) }
                            }
                        }
                    }
                    Row {
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
                                close(built)
                            },
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = PaddingValues(horizontal = 32.dp),
                        ) { Text("Done") }
                    }
                }
            }
        }
    }
}
