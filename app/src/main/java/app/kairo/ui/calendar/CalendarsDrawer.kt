@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.data.settings.AppSettings
import app.kairo.ui.common.LocalKairoApp
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch

/**
 * Canvas "calendar drawer" board: what the Calendar tab shows. Kairo: Tasks, Inbox and each list;
 * On this phone: each calendar (Google accounts too). Untick to hide it; the choice is kept.
 */
@Composable
fun CalendarsDrawerContent() {
    val app = LocalKairoApp.current
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val scope = rememberCoroutineScope()
    val prefs by app.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val lists by app.repository.lists.collectAsStateWithLifecycle(initialValue = emptyList())
    val phone by produceState(emptyList<app.kairo.data.calendar.PhoneCal>()) { value = app.phoneCalendar.calendars() }
    val hidden = prefs.calHidden
    fun set(key: String, on: Boolean) { scope.launch { app.settings.setCalShown(key, on) } }

    val kairo = listOf(Triple("tasks", "Tasks", c.primary), Triple("list:0", "Inbox", c.onSurfaceVariant)) +
        lists.sortedBy { it.position }.map { Triple("list:${it.id}", it.name, k.accent(it.color)) }
    ModalDrawerSheet(drawerContainerColor = k.bg, drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) {
        Column(Modifier.statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 16.dp)) {
            Text("Kairo", style = MaterialTheme.typography.headlineSmall, color = c.onSurface, modifier = Modifier.padding(start = 12.dp))
            Text("Calendars", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
            Section("Kairo")
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                kairo.forEachIndexed { i, (key, name, color) ->
                    // Lists only matter while Tasks is shown.
                    CheckRow(name, color, key !in hidden, i, kairo.size, enabled = key == "tasks" || "tasks" !in hidden) { set(key, it) }
                }
            }
            if (phone.isNotEmpty()) {
                Section("On this phone")
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    phone.forEachIndexed { i, pc ->
                        val label = if (pc.account.isNotBlank() && pc.account != pc.name && "@" in pc.account) "Google · ${pc.name}" else pc.name
                        CheckRow(label, Color(pc.color), "phone:${pc.id}" !in hidden, i, phone.size) { set("phone:${pc.id}", it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(t: String) = Text(t, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 12.dp, top = 24.dp, bottom = 8.dp))

@Composable
private fun CheckRow(label: String, color: Color, on: Boolean, i: Int, n: Int, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val big = 28.dp; val small = 8.dp
    SegmentedListItem(
        onClick = { onChange(!on) }, enabled = enabled,
        shapes = ListItemDefaults.shapes(
            shape = RoundedCornerShape(
                topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
                bottomStart = if (i == n - 1) big else small, bottomEnd = if (i == n - 1) big else small,
            ),
            pressedShape = RoundedCornerShape(big),
        ),
        colors = ListItemDefaults.segmentedColors(containerColor = KairoTheme.colors.item),
        leadingContent = {
            Checkbox(on, onCheckedChange = null, enabled = enabled,
                colors = CheckboxDefaults.colors(checkedColor = color, uncheckedColor = color, checkmarkColor = MaterialTheme.colorScheme.surface))
        },
    ) { Text(label) }
}
