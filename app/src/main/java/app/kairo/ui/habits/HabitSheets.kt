@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package app.kairo.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.habits.Goal
import app.kairo.habits.Habit
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Shared drawer frame: title, body, Cancel + one main action (drawers, not dialogs: rule 12). */
@Composable
private fun Drawer(title: String, onDismiss: () -> Unit, action: String, danger: Boolean = false, onAction: () -> Unit, body: @Composable () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val c = MaterialTheme.colorScheme
    fun close(then: () -> Unit = {}) = scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss(); then() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = KairoTheme.colors.bg) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp))
            body()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { close() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).height(56.dp)) { Text("Cancel") }
                Button(
                    onClick = { close(onAction) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1.4f).height(56.dp),
                    colors = if (danger) ButtonDefaults.buttonColors(containerColor = c.errorContainer, contentColor = c.onErrorContainer) else ButtonDefaults.buttonColors(),
                ) { Text(action) }
            }
        }
    }
}

/** Log drawer for a count or time habit: the day's amount with − / + by one step, and a wavy bar. */
@Composable
fun LogSheet(h: Habit, day: LocalDate, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    val c = MaterialTheme.colorScheme
    val step = if (h.goal == Goal.Time) 1.0 else h.e.step
    var v by rememberSaveable { mutableStateOf(h.count(day)) }
    val unit = if (h.goal == Goal.Time) "min" else h.e.unit
    Drawer(if (day == LocalDate.now()) "Today" else day.toString(), onDismiss, "Save", onAction = { onSave(v) }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledTonalIconButton(onClick = { v = (v - step).coerceAtLeast(0.0) }, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(56.dp)) {
                Icon(painterResource(R.drawable.ic_remove), "Less")
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(num(v), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"), color = c.onSurface)
                Text("of ${num(h.target)} $unit".trim(), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            }
            FilledTonalIconButton(onClick = { v = (v + step).coerceAtMost(h.target * 3) }, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(56.dp)) {
                Icon(painterResource(R.drawable.ic_add), "More")
            }
        }
        LinearWavyProgressIndicator(progress = { (v / h.target).toFloat().coerceIn(0f, 1f) }, color = KairoTheme.colors.accent(h.color), modifier = Modifier.fillMaxWidth())
    }
}

/** Pause drawer: rest days for a while; the streak is kept and reminders stop. */
@Composable
fun PauseSheet(onDismiss: () -> Unit, onPause: (Int?) -> Unit) {
    val choices = listOf<Pair<String, Int?>>("3 days" to 3, "1 week" to 7, "2 weeks" to 14, "Until I resume" to null)
    var pick by rememberSaveable { mutableStateOf(1) }
    Drawer("Pause habit", onDismiss, "Pause", onAction = { onPause(choices[pick].second) }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            choices.forEachIndexed { i, (label, _) ->
                ToggleButton(
                    checked = pick == i, onCheckedChange = { pick = i },
                    shapes = when (i) { 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(); choices.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes(); else -> ButtonGroupDefaults.connectedMiddleButtonShapes() },
                    modifier = Modifier.weight(if (i == 3) 1.6f else 1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                ) { Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

/** H7: confirm before deleting a habit, like "Empty trash?". Undo still follows. */
@Composable
fun DeleteHabitSheet(h: Habit, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val n = h.total()
    Drawer("Delete ${h.name}?", onDismiss, "Delete", danger = true, onAction = onDelete) {
        Text("Its $n check-in" + (if (n == 1) "" else "s") + " go with it.", style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
    }
}
