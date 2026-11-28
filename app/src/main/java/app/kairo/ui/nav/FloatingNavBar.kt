@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.nav

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.navigation.Tab
import app.kairo.ui.theme.KairoTheme

// Sizes from the v2 canvas (.dock / .nav / .item / .ind / .fab).
private val BarHeight = 64.dp
private val IndHeight = 40.dp
private val Fab = 64.dp          // matches the bar
private val FabCorner = 20.dp
private val FabBig = 80.dp       // alone while the bar is tucked away (M3 medium FAB)
private val FabBigCorner = 24.dp
private val FabMenu = 56.dp      // round × while the create menu is open

/**
 * v2 floating nav (rule 5): a grey pill sized to its tabs and the + beside it, centred together,
 * 8dp apart. Icons only (owner, 2026-11-28: the label pushed the group to the screen edge on a
 * 360dp phone); the selected tab is a solid primary pill, its name is read out by TalkBack.
 * Scrolling down tucks the pill away and grows the + (80dp); opening the menu turns + into ×.
 */
@Composable
fun FloatingNavBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    menuOpen: Boolean,
    onMenu: (Boolean) -> Unit,
    collapsed: Boolean,
    modifier: Modifier = Modifier,
    menu: @Composable FloatingActionButtonMenuScope.() -> Unit,
) {
    val k = KairoTheme.colors
    val motion = MaterialTheme.motionScheme
    val hide by animateFloatAsState(if (collapsed || menuOpen) 1f else 0f, motion.fastSpatialSpec(), label = "barHide")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
            // Sized to its items and centred; on narrow phones with a long label the group may run
            // into the 16dp side margins evenly rather than squeeze the + (canvas .dock behaves the same).
            .wrapContentWidth(Alignment.CenterHorizontally, unbounded = true),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val barShape = RoundedCornerShape(BarHeight / 2)
        Row(
            Modifier
                .graphicsLayer { translationY = hide * 120.dp.toPx(); alpha = (1f - hide * 1.4f).coerceIn(0f, 1f) }
                .height(BarHeight)
                .floatShadow(barShape, k.floatShadow)
                .background(k.navBg, barShape)
                .padding(horizontal = 8.dp)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab -> NavItem(tab, tab == selected, enabled = hide < 0.5f) { onSelect(tab) } }
        }
        CreateButton(open = menuOpen, big = collapsed && !menuOpen, onClick = { onMenu(!menuOpen) }, menu = menu)
    }
}

@Composable
private fun NavItem(tab: Tab, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val bg by animateColorAsState(if (selected) c.primary else Color.Transparent, motion.defaultEffectsSpec(), label = "navBg")
    val fg by animateColorAsState(if (selected) c.onPrimary else c.onSurfaceVariant, motion.defaultEffectsSpec(), label = "navFg")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Pressed: 20dp → 8dp corners (canvas .item:active .ind), on the fast spatial spring.
    val corner by animateDpAsState(if (pressed) 8.dp else IndHeight / 2, motion.fastSpatialSpec(), label = "navCorner")
    // The icon pops when it becomes selected.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(selected) { if (selected) { pop.snapTo(0.8f); pop.animateTo(1f, motion.fastSpatialSpec()) } }
    Box(
        Modifier
            .height(BarHeight)
            .selectable(
                selected = selected, enabled = enabled, onClick = onClick, role = Role.Tab,
                interactionSource = interaction, indication = ripple(bounded = false, radius = 24.dp),
            )
            .semantics { contentDescription = tab.label },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .heightIn(min = IndHeight)
                .widthIn(min = 48.dp)
                .clip(RoundedCornerShape(corner))
                .background(bg)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painterResource(if (selected) tab.selectedIcon else tab.icon), null, tint = fg,
                modifier = Modifier.size(24.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value },
            )
        }
    }
}

/**
 * Rule 4: solid primary, bold plus. It is the button of the real M3 Expressive FAB menu, so the menu
 * items rise from it. The menu reports only the button's size to the row, so opening it doesn't
 * shift the centred group.
 */
@Composable
private fun CreateButton(open: Boolean, big: Boolean, onClick: () -> Unit, menu: @Composable FloatingActionButtonMenuScope.() -> Unit) {
    val c = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val rest = Fab
    val side by animateDpAsState(when { open -> FabMenu; big -> FabBig; else -> rest }, motion.fastSpatialSpec(), label = "fabSide")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        when { open -> FabMenu / 2; pressed -> 12.dp; big -> FabBigCorner; else -> FabCorner },
        motion.fastSpatialSpec(), label = "fabCorner",
    )
    val turn by animateFloatAsState(if (open) 90f else 0f, motion.defaultSpatialSpec(), label = "fabTurn")
    val slot = rest // the row always reserves the resting size
    Box(
        Modifier.layout { m, _ ->
            val p = m.measure(Constraints())
            val s = slot.roundToPx()
            // Bottom-right of the menu (where its button sits) lines up with the slot's bottom-right,
            // centred vertically on the 64dp bar.
            // FloatingActionButtonMenu pads its button by 16dp at the end and bottom; cancel that.
            val extra = (side.roundToPx() - s) / 2 + 16.dp.roundToPx()
            layout(s, s) { p.place(s - p.width + extra, s - p.height + extra) }
        },
    ) {
        FloatingActionButtonMenu(
            expanded = open,
            button = {
                val shape = RoundedCornerShape(corner)
                FloatingActionButton(
                    onClick = onClick,
                    shape = shape,
                    containerColor = c.primary,
                    contentColor = c.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    interactionSource = interaction,
                    modifier = Modifier.size(side).floatShadow(shape, KairoTheme.colors.floatShadow),
                ) {
                    Icon(
                        painterResource(if (open) R.drawable.ic_close else R.drawable.ic_add_bold),
                        contentDescription = if (open) "Close create menu" else "Create",
                        modifier = Modifier.size(if (open) 20.dp else if (big) 32.dp else 28.dp).graphicsLayer { rotationZ = if (open) turn - 90f else turn },
                    )
                }
            },
            content = menu,
        )
    }
}

/** Canvas shadow under floating things. */
internal fun Modifier.floatShadow(shape: Shape, color: Color): Modifier =
    dropShadow(shape, Shadow(radius = 8.dp, color = color, offset = DpOffset(0.dp, 3.dp)))
