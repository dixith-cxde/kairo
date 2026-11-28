package app.kairo.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Canvas habit page "Note for today": one short note a day, saved as you type (a moment after you
 * stop), then the three latest earlier notes. A blank note removes the day's note.
 */
@Composable
fun HabitNotes(habitId: Long, today: LocalDate) {
    val app = LocalKairoApp.current
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val notes by remember(habitId) { app.habits.notes(habitId) }.collectAsStateWithLifecycle(initialValue = null)
    val saved = notes?.firstOrNull { it.date == today }?.text
    var text by rememberSaveable(habitId) { mutableStateOf<String?>(null) }
    var touched by remember { mutableStateOf(false) }
    if (text == null && notes != null) text = saved.orEmpty()
    LaunchedEffect(text) {
        val t = text ?: return@LaunchedEffect
        if (!touched || t.trim() == saved.orEmpty()) return@LaunchedEffect
        delay(600)
        app.habits.setNote(habitId, today, t)
    }
    Column {
        Text("Note for today", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = c.primary,
            modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 8.dp))
        BasicTextField(
            text.orEmpty(), { if (it.length <= 280) { touched = true; text = it } }, minLines = 2, cursorBrush = SolidColor(c.primary),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.onSurface),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().background(k.item, androidx.compose.foundation.shape.RoundedCornerShape(28.dp)).padding(horizontal = 20.dp, vertical = 16.dp),
            decorationBox = { inner -> Box { if (text.isNullOrEmpty()) Text("How did it go? (optional)", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant); inner() } },
        )
        val recent = notes.orEmpty().filter { it.date.isBefore(today) }.take(3)
        if (recent.isNotEmpty()) Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            recent.forEachIndexed { i, n ->
                Column(Modifier.fillMaxWidth().background(k.item, seg(i, recent.size)).padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(if (n.date == today.minusDays(1)) "Yesterday" else n.date.format(Day), style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                    Text(n.text, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
                }
            }
        }
    }
}

private val Day = DateTimeFormatter.ofPattern("EEE d MMM")

private fun seg(i: Int, n: Int): androidx.compose.foundation.shape.RoundedCornerShape {
    val big = 28.dp; val small = 8.dp
    return androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
        bottomStart = if (i == n - 1) big else small, bottomEnd = if (i == n - 1) big else small,
    )
}
