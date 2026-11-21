@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import kotlin.random.Random

/** Bumped by the app each time a tab is shown, so empty states pick new shapes on every visit. */
val LocalTabVisit = androidx.compose.runtime.compositionLocalOf { 0 }

/**
 * Shapes for empty states (owner, 2026-11-21): mostly the chubby Triangle, Pill and Oval, now and
 * then a Bun, Arch or Cookie4. Never Puffy / Puffy Diamond / Heart (rule 6).
 */
private val Mostly = listOf(MaterialShapes.Triangle, MaterialShapes.Pill, MaterialShapes.Oval)
private val Sometimes = listOf(MaterialShapes.Bun, MaterialShapes.Arch, MaterialShapes.Cookie4Sided)

private fun pickShape(avoid: RoundedPolygon? = null): RoundedPolygon {
    val pool = if (Random.nextFloat() < 0.8f) Mostly else Sometimes
    return pool.filter { it !== avoid }.random()
}

/**
 * Empty screens: two nested library shapes, no icon (owner, 2026-11-21). A big soft one turns
 * slowly; a smaller one in front turns the other way. Both are picked at random each time the tab
 * is shown, then the pair pops in on the slow spatial spring.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    back: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    front: Color = MaterialTheme.colorScheme.secondaryContainer,
    action: (@Composable () -> Unit)? = null,
) {
    val visit = LocalTabVisit.current
    val (backShape, frontShape) = remember(visit) { pickShape().let { b -> b to pickShape(avoid = b) } }
    val spin by slowSpin(48_000)
    val pop = remember(visit) { Animatable(0.6f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(visit) { pop.animateTo(1f, spec) }
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(size).graphicsLayer { scaleX = pop.value; scaleY = pop.value }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(size).graphicsLayer { rotationZ = spin }.background(back, backShape.toShape()))
            Box(
                Modifier.size(size * 0.5f).graphicsLayer { rotationZ = -spin * 2 + 20f }.background(front, frontShape.toShape()),
            )
        }
        Text(title, style = MaterialTheme.typography.headlineSmallEmphasized, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) Box(Modifier.padding(top = 4.dp)) { action() }
    }
}
