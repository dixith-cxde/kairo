@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.kairo.habits.Goal
import app.kairo.habits.Habit
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Today's amount for the page, with a live timer for time habits. */
class HabitTimer(val running: Boolean, val value: Double, val clock: String, val start: () -> Unit, val pause: () -> Unit)

/**
 * Time habits (canvas): Start timer counts up from today's minutes; at the goal it marks the day
 * done; Pause logs what you did. It runs while the page is open and saves when you leave.
 * Not yet a background timer with a notification (that is the Focus tab's job).
 */
@Composable
fun rememberHabitTimer(h: Habit): HabitTimer {
    val app = LocalKairoApp.current
    val today = LocalDate.now()
    val habit by rememberUpdatedState(h)
    var startAt by rememberSaveable(h.id) { mutableStateOf<Long?>(null) }
    var base by rememberSaveable(h.id) { mutableStateOf(0.0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val running = startAt != null
    val value = if (running) base + (now - startAt!!) / 60_000.0 else h.count(today)
    fun save(v: Double) { app.appScope.launch { app.habits.log(habit.id, today, minOf(v, habit.target)) } }
    LaunchedEffect(running) {
        while (startAt != null) {
            now = System.currentTimeMillis()
            if (base + (now - startAt!!) / 60_000.0 >= habit.target) { save(habit.target); startAt = null }
            delay(1000)
        }
    }
    DisposableEffect(h.id) {
        onDispose { startAt?.let { save(base + (System.currentTimeMillis() - it) / 60_000.0) } }
    }
    val secs = (value * 60).toLong()
    return HabitTimer(
        running = running, value = if (h.goal == Goal.Time) value else h.count(today),
        clock = "${secs / 60}:" + "%02d".format(secs % 60),
        start = { base = h.count(today); now = System.currentTimeMillis(); startAt = now },
        pause = { startAt?.let { save(base + (System.currentTimeMillis() - it) / 60_000.0) }; startAt = null },
    )
}

/** History view: Month | Year, the real connected toggle buttons. */
@Composable
fun ViewToggle(year: Boolean, onPick: (Boolean) -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        listOf(false to "Month", true to "Year").forEachIndexed { i, (v, label) ->
            ToggleButton(
                checked = year == v, onCheckedChange = { onPick(v) },
                shapes = if (i == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                colors = ToggleButtonDefaults.colors(containerColor = k.chip, contentColor = c.onSurfaceVariant, checkedContainerColor = c.primary, checkedContentColor = c.onPrimary),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.weight(1f).height(40.dp),
            ) { Text(label, style = MaterialTheme.typography.labelLarge) }
        }
    }
}
