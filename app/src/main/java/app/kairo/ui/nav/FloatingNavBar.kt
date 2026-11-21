package app.kairo.ui.nav

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import app.kairo.R
import app.kairo.navigation.Tab
import app.kairo.ui.theme.KairoTheme

// Sizes from Stash FloatingNavBar.tsx and the canvas (m3e.css .fnav / .ffab).
private val BarHeight = 64.dp
private val OuterCorner = 32.dp        // fully round outer ends
private val InnerCorner = 8.dp         // where bar and FAB meet (connected button group inner corner)
private val FabPressedCorner = 16.dp   // FAB outer corners while pressed
private val PillHeight = 48.dp

/**
 * Kairo's floating nav: a pill bar and the create FAB as one connected group,
 * 16dp from the sides and 20dp above the system navigation bar.
 * Icon-only (owner, 2026-11-21): the selected tab shows a pill and a filled icon, no label;
 * the label is still announced by TalkBack. Deliberate departure from the docked M3 nav bar (rule 5).
 */
@Composable
fun FloatingNavBar(
    selected: Tab,
    position: () -> Float,
    onSelect: (Tab) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    menuOpen: Boolean = false,
) {
    val surfaces = KairoTheme.colors
    val motion = MaterialTheme.motionScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val barShape = RoundedCornerShape(
            topStart = OuterCorner, bottomStart = OuterCorner,
            topEnd = InnerCorner, bottomEnd = InnerCorner,
        )
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(BarHeight)
                .floatShadow(barShape, surfaces.floatShadow)
                .background(surfaces.navBg, barShape)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val gap = 4.dp
            val count = Tab.entries.size
            val slot = (maxWidth - gap * (count - 1)) / count
            // One pill that rides the pager: it follows your finger while you swipe, and the pager's
            // spatial spring (tap a tab) carries it with a little overshoot. Between two tabs it
            // stretches toward the next one and shrinks back as it lands (the liquid feel).
            Box(
                Modifier
                    .layout { measurable, constraints ->
                        val pos = position()
                        val step = (slot + gap).toPx()
                        val stretch = step * 0.5f * sin(PI.toFloat() * (pos - floor(pos)))
                        val w = (slot.toPx() + stretch).roundToInt().coerceAtLeast(PillHeight.roundToPx())
                        val p = measurable.measure(Constraints.fixed(w, PillHeight.roundToPx()))
                        layout(p.width, p.height) { p.place((step * pos - stretch / 2).roundToInt(), 0) }
                    }
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(PillHeight / 2)),
            )
            Row(
                Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tab.entries.forEach { tab ->
                    NavSlot(tab = tab, selected = tab == selected, onClick = { if (tab != selected) onSelect(tab) })
                }
            }
        }
        CreateFab(onClick = onCreate, open = menuOpen)
    }
}

@Composable
private fun RowScope.NavSlot(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val motion = MaterialTheme.motionScheme
    val colors = MaterialTheme.colorScheme
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        animationSpec = motion.defaultEffectsSpec(),
        label = "contentColor",
    )
    // The icon pops when it becomes selected (fast spatial spring overshoot).
    val pop = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) { pop.snapTo(0.8f); pop.animateTo(1f, motion.fastSpatialSpec()) }
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(PillHeight)
            .clip(RoundedCornerShape(PillHeight / 2))
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
            )
            .semantics { contentDescription = tab.label },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = selected, animationSpec = motion.defaultEffectsSpec(), label = "icon") { on ->
            Icon(
                painter = painterResource(if (on) tab.selectedIcon else tab.icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value },
            )
        }
    }
}

/** Solid primary, bold plus (rule 4). Outer corners tighten from 32 to 16dp while pressed. */
@Composable
private fun CreateFab(onClick: () -> Unit, open: Boolean) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val outer by animateDpAsState(
        targetValue = if (pressed) FabPressedCorner else OuterCorner,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "fabCorner",
    )
    val shape = RoundedCornerShape(
        topStart = InnerCorner, bottomStart = InnerCorner,
        topEnd = outer, bottomEnd = outer,
    )
    FloatingActionButton(
        onClick = onClick,
        shape = shape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        // Shadow drawn by floatShadow so bar and FAB match the canvas exactly.
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
        interactionSource = interaction,
        modifier = Modifier
            .size(BarHeight)
            .floatShadow(shape, KairoTheme.colors.floatShadow),
    ) {
        // + turns into × while the create menu is open.
        val turn by animateFloatAsState(if (open) 45f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "fabTurn")
        Icon(
            painter = painterResource(R.drawable.ic_add_bold),
            contentDescription = if (open) "Close create menu" else "Create",
            modifier = Modifier.size(30.dp).graphicsLayer { rotationZ = turn },
        )
    }
}

/** Canvas shadow: 0 6px 20px. */
private fun Modifier.floatShadow(shape: Shape, color: Color): Modifier =
    dropShadow(shape, Shadow(radius = 20.dp, color = color, offset = DpOffset(0.dp, 6.dp)))
