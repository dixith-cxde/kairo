@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package app.kairo.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.ui.common.ExpressiveTimePicker
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Canvas .hgrp: items of one card, 3dp apart, 28dp outer / 8dp inner corners. */
@Composable
internal fun GroupItem(i: Int, n: Int, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val big = 28.dp; val small = 8.dp
    val shape = RoundedCornerShape(
        topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
        bottomStart = if (i == n - 1) big else small, bottomEnd = if (i == n - 1) big else small,
    )
    Column(modifier.fillMaxWidth().padding(bottom = 3.dp).clip(shape).background(KairoTheme.colors.item).padding(horizontal = 16.dp, vertical = 12.dp), content = content)
}

/** One switch line inside a group item (canvas .hgsw). */
@Composable
internal fun SwitchLine(label: String, on: Boolean, icon: Int? = null, onChange: (Boolean) -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clickable(role = Role.Switch) { onChange(!on) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(painterResource(icon), null, Modifier.padding(end = 16.dp), tint = c.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = c.onSurface, modifier = Modifier.weight(1f))
        Switch(on, onChange)
    }
}

/** A small label over single-choice chips (canvas .hgl + .pchips small): the real M3 FilterChip. */
@Composable
internal fun <T> ChipLine(label: String?, options: List<Pair<T, String>>, picked: T, onPick: (T) -> Unit) {
    val c = MaterialTheme.colorScheme
    if (label != null) Text(label, style = MaterialTheme.typography.labelLarge, color = c.primary, modifier = Modifier.padding(bottom = 4.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (v, t) ->
            FilterChip(
                selected = v == picked, onClick = { onPick(v) }, label = { Text(t) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = c.secondaryContainer, selectedLabelColor = c.onSecondaryContainer),
            )
        }
    }
}

/** From / To column: a label, a date pill and (when timed) a time pill. */
@Composable
internal fun WhenColumn(label: String, date: String, time: String?, modifier: Modifier, onDate: () -> Unit, onTime: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = c.primary)
        Pill(date, onDate)
        if (time != null) Pill(time, onTime)
    }
}

@Composable
private fun Pill(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(KairoTheme.colors.bg).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.onSurface, maxLines = 1) }
}

/** A text line with a leading icon (canvas .evf: Location, Meeting link). */
@Composable
internal fun FieldLine(icon: Int, value: String, hint: String, url: Boolean = false, onChange: (String) -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), null, Modifier.padding(end = 16.dp), tint = c.onSurfaceVariant)
        BasicTextField(
            value, onChange, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.onSurface),
            cursorBrush = SolidColor(c.primary), modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = if (url) KeyboardType.Uri else KeyboardType.Text, capitalization = if (url) KeyboardCapitalization.None else KeyboardCapitalization.Sentences),
            decorationBox = { inner -> Box { if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant); inner() } },
        )
    }
}

/** A drawer with the Expressive time picker (rule 12: drawers, not dialogs). */
@Composable
internal fun TimeSheet(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var h by remember { mutableIntStateOf(initial.hour) }
    var m by remember { mutableIntStateOf(initial.minute) }
    fun close(then: () -> Unit = {}) = scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss(); then() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = KairoTheme.colors.bg) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ExpressiveTimePicker(h, m, { hh, mm -> h = hh; m = mm })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { close() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).height(56.dp)) { Text("Cancel") }
                Button(onClick = { close { onPick(LocalTime.of(h, m)) } }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1.4f).height(56.dp)) {
                    Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp)); Text("  Set time")
                }
            }
        }
    }
}
