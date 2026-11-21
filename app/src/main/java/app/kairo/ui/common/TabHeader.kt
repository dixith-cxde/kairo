@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Tab header (owner, 2026-11-21: the collapsing app bar felt like old Android). A big emphasized
 * title in primary over a soft wash of primaryContainer that fades into the neutral screen, actions
 * as round tonal buttons on the right, and an optional row underneath (chips, groups). It is the
 * first item of the tab's list, so it scrolls away with the content and leaves room to breathe.
 */
@Composable
fun TabHeader(
    title: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    below: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(c.primaryContainer.copy(alpha = 0.42f), Color.Transparent)))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.displaySmallEmphasized, color = c.primary, maxLines = 1)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.titleSmall, color = c.onSurfaceVariant)
            } else Row(Modifier.weight(1f)) {}
            actions()
        }
        below?.invoke(this)
    }
}

/** Lets a header inside a padded LazyColumn reach the screen edges (undoes [h] of side padding). */
fun Modifier.fullBleed(h: androidx.compose.ui.unit.Dp = 16.dp) = layout { m, cs ->
    val extra = h.roundToPx() * 2
    val p = m.measure(cs.copy(minWidth = cs.maxWidth + extra, maxWidth = cs.maxWidth + extra))
    layout(cs.maxWidth, p.height) { p.place(-extra / 2, 0) }
}
