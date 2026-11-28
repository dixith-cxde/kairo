@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package app.kairo.ui.tasks

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kairo.R
import app.kairo.data.model.TaskList
import app.kairo.ui.common.countUp
import app.kairo.ui.common.fullBleed
import app.kairo.ui.common.slowSpin
import app.kairo.ui.shapes.polygon
import app.kairo.ui.theme.KairoTheme
/** Canvas .cg: real connected ToggleButtons, chip grey; the picked one is solid primary. */
@Composable
internal fun ViewGroup(view: TaskView, onPick: (TaskView) -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        TaskView.entries.forEachIndexed { i, v ->
            ToggleButton(
                checked = v == view,
                onCheckedChange = { onPick(v) },
                shapes = when (i) {
                    0 -> androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes()
                    TaskView.entries.lastIndex -> androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> androidx.compose.material3.ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.colors(
                    containerColor = k.chip, contentColor = c.onSurfaceVariant,
                    checkedContainerColor = c.primary, checkedContentColor = c.onPrimary,
                ),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.weight(v.name.length + 2f).height(40.dp).semantics { role = Role.RadioButton },
            ) { Text(v.name, maxLines = 1, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** Compact hero (canvas .hero): wavy ring on a slowly turning Cookie-12, title and the next task. */
@Composable
internal fun Hero(title: String, next: String, done: Int, total: Int, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val target = if (total == 0) 0f else done / total.toFloat()
    val ring = remember { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(target) { ring.animateTo(target, spec) }
    val shown by countUp(done)
    val spin by slowSpin(24_000)
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.primaryContainer)
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 20.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$done of $total tasks done today. $next" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().rotate(spin).alpha(0.1f).background(c.onPrimaryContainer, MaterialShapes.Cookie12Sided.toShape()))
            CircularWavyProgressIndicator(
                progress = { ring.value.coerceIn(0f, 1f) },
                color = c.onPrimaryContainer,
                trackColor = c.onPrimaryContainer.copy(alpha = 0.24f),
                modifier = Modifier.size(54.dp),
            )
            Text(
                buildAnnotatedString {
                    append("$shown")
                    withStyle(SpanStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.onPrimaryContainer.copy(alpha = 0.75f))) { append("/$total") }
                },
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = c.onPrimaryContainer,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = c.onPrimaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(next, style = MaterialTheme.typography.labelLarge, color = c.onPrimaryContainer.copy(alpha = 0.85f),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/**
 * List filter (canvas .lcg, owner option A): Inbox and every list in one scrolling connected row.
 * One tap picks a list (secondaryContainer pill with a check); tap again, or ✕, shows all lists.
 */
@Composable
internal fun ListRow(lists: List<TaskList>, picked: Long?, onPick: (Long?) -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val items = listOf<TaskList?>(null) + lists.sortedBy { it.position }
    LazyRow(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 0.dp).fullBleed(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(items, key = { _, l -> l?.id ?: 0L }) { i, l ->
            val id = l?.id ?: 0L
            val on = picked == id
            ToggleButton(
                checked = on,
                onCheckedChange = { onPick(if (on) null else id) },
                shapes = when (i) {
                    0 -> androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes()
                    items.lastIndex -> androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> androidx.compose.material3.ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.colors(
                    containerColor = k.chip, contentColor = c.onSurface,
                    checkedContainerColor = c.secondaryContainer, checkedContentColor = c.onSecondaryContainer,
                ),
                contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
                modifier = Modifier.height(40.dp).semantics { role = Role.RadioButton },
            ) {
                when {
                    on -> Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp))
                    l == null -> Icon(painterResource(R.drawable.ic_inbox), null, Modifier.size(18.dp), tint = c.onSurfaceVariant)
                    else -> Box(Modifier.size(12.dp).background(k.accent(l.color), l.shape.polygon.toShape()))
                }
                Spacer(Modifier.width(8.dp))
                Text(l?.name ?: "Inbox", style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
        if (picked != null) item(key = "clear") {
            IconButton(
                onClick = { onPick(null) }, shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.padding(start = 6.dp).size(40.dp).border(1.dp, c.outlineVariant, CircleShape),
            ) { Icon(painterResource(R.drawable.ic_close), "Clear list filter", Modifier.size(18.dp)) }
        }
    }
}

// ---------------------------------------------------------------- sections and rows
