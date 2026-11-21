package app.kairo.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.navigation.Tab
import app.kairo.ui.theme.KairoTheme

// Sizes from Stash FloatingNavBar.tsx and the canvas (m3e.css .fnav / .ffab).
private val BarHeight = 64.dp
private val OuterCorner = 32.dp        // fully round outer ends
private val InnerCorner = 8.dp         // where bar and FAB meet (connected button group inner corner)
private val FabPressedCorner = 16.dp   // FAB outer corners while pressed
private val PillHeight = 48.dp
private const val ActiveWeight = 3f    // the active slot is 3x as wide, to fit its label

/**
 * Kairo's floating nav: a pill bar and the create FAB as one connected group,
 * 16dp from the sides and 20dp above the system navigation bar.
 * Only the selected tab shows its label. Deliberate departure from the docked M3 nav bar (rule 5).
 */
@Composable
fun FloatingNavBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaces = KairoTheme.surfaces
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
        Row(
            modifier = Modifier
                .weight(1f)
                .height(BarHeight)
                .floatShadow(barShape, surfaces.floatShadow)
                .background(surfaces.navBg, barShape)
                .selectableGroup()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab ->
                NavSlot(
                    tab = tab,
                    selected = tab == selected,
                    onClick = { if (tab != selected) onSelect(tab) },
                )
            }
        }
        CreateFab(onClick = onCreate)
    }
}

@Composable
private fun RowScope.NavSlot(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val motion = MaterialTheme.motionScheme
    val colors = MaterialTheme.colorScheme

    // Spatial: the slot widens. Effects: colors fade. Both from the expressive motion scheme.
    val weight by animateFloatAsState(
        targetValue = if (selected) ActiveWeight else 1f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "slotWeight",
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else Color.Transparent,
        animationSpec = motion.defaultEffectsSpec(),
        label = "pillColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        animationSpec = motion.defaultEffectsSpec(),
        label = "contentColor",
    )

    val pillShape = RoundedCornerShape(PillHeight / 2)
    Row(
        modifier = Modifier
            .weight(weight)
            .height(PillHeight)
            .clip(pillShape)
            .background(pillColor)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
            )
            .semantics { contentDescription = tab.label }
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Outlined -> filled when selected (Material Symbols FILL axis, as two vectors).
        Crossfade(targetState = selected, animationSpec = motion.defaultEffectsSpec(), label = "icon") { on ->
            Icon(
                painter = painterResource(if (on) tab.selectedIcon else tab.icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
        }
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
            exit = shrinkHorizontally(motion.defaultSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
        ) {
            Row {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.clearAndSetSemantics { }, // the slot already announces the label
                )
            }
        }
    }
}

/** Solid primary, bold plus (rule 4). Outer corners tighten from 32 to 16dp while pressed. */
@Composable
private fun CreateFab(onClick: () -> Unit) {
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
            .floatShadow(shape, KairoTheme.surfaces.floatShadow),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add_bold),
            contentDescription = "Create",
            modifier = Modifier.size(30.dp),
        )
    }
}

/** Canvas shadow: 0 6px 20px. */
private fun Modifier.floatShadow(shape: Shape, color: Color): Modifier =
    dropShadow(shape, Shadow(radius = 20.dp, color = color, offset = DpOffset(0.dp, 6.dp)))
