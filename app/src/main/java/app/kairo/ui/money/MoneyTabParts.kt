@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import app.kairo.R
import app.kairo.ui.theme.KairoTheme

/** The three views inside Money. Saved by ordinal. */
internal enum class MoneyTab(val label: String) { Activity("Activity"), Accounts("Accounts"), Plan("Plan") }

/**
 * Lazy row placement from the motion scheme: new rows fade in on the default effects spring,
 * neighbors spring into place on the default spatial spring, removed rows fade out fast.
 */
@Composable
internal fun LazyItemScope.springItem(): Modifier {
    val m = MaterialTheme.motionScheme
    return Modifier.animateItem(
        fadeInSpec = m.defaultEffectsSpec(),
        placementSpec = m.defaultSpatialSpec(),
        fadeOutSpec = m.fastEffectsSpec(),
    )
}

/** One tile of the month summary. [progress] (0..1) draws a wavy bar under the value (budget). */
internal data class Summary(
    val label: String, val value: String, val color: Color,
    val onClick: (() -> Unit)? = null, val progress: Float? = null,
)

/**
 * Month summary (owner, 2026-11-21): Income and Spent side by side, the budget full width under
 * them, and the three cut as one piece: only the block's four outer corners are large, every
 * inner corner is small, 2dp gaps (the M3 Expressive connected-group geometry).
 */
@Composable
internal fun SummaryGroup(income: Summary, spent: Summary, budget: Summary, modifier: Modifier = Modifier) {
    val big = 28.dp
    val small = 6.dp
    val gap = ButtonGroupDefaults.ConnectedSpaceBetween
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            SummaryTile(income, RoundedCornerShape(topStart = big, topEnd = small, bottomStart = small, bottomEnd = small), Modifier.weight(1f))
            SummaryTile(spent, RoundedCornerShape(topStart = small, topEnd = big, bottomStart = small, bottomEnd = small), Modifier.weight(1f))
        }
        SummaryTile(budget, RoundedCornerShape(topStart = small, topEnd = small, bottomStart = big, bottomEnd = big), Modifier.fillMaxWidth())
    }
}

@Composable
private fun SummaryTile(s: Summary, shape: Shape, modifier: Modifier) {
    Column(
        modifier.heightIn(min = 72.dp).clip(shape).background(KairoTheme.colors.item)
            .then(if (s.onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = "Open ${s.label}", onClick = s.onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(s.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(
            s.value, style = MaterialTheme.typography.titleLargeEmphasized, color = s.color,
            maxLines = 1, softWrap = false, autoSize = TextAutoSize.StepBased(12.sp, 22.sp),
        )
        if (s.progress != null) androidx.compose.material3.LinearWavyProgressIndicator(
            progress = { s.progress.coerceIn(0f, 1f) },
            color = s.color,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

/** Activity / Accounts / Plan as a connected segmented control (ToggleButtons in a group). */
@Composable
internal fun MoneyTabSwitcher(tab: MoneyTab, onSelect: (MoneyTab) -> Unit, modifier: Modifier = Modifier) {
    val tabs = MoneyTab.entries
    app.kairo.ui.common.ConnectedGroup(tabs, tab, { it.label }, modifier) { onSelect(it) }
}

/** Roomy Plan card: a meaningful shape badge, a title, a chevron, then its own content. */
@Composable
internal fun PlanCard(
    title: String,
    icon: Int,
    shape: RoundedPolygon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(KairoTheme.colors.item)
            .clickable(role = Role.Button, onClickLabel = "Open $title", onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(c.secondaryContainer, shape.toShape()), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), null, tint = c.onSecondaryContainer, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMediumEmphasized, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = c.onSurfaceVariant)
        }
        content()
    }
}

/** Small section label used above groups (primary, like the day labels). */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
