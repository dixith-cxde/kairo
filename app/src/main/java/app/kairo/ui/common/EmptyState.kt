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

/** Bumped by the app each time a tab is shown, so empty states pick new shapes on every visit. */
val LocalTabVisit = androidx.compose.runtime.compositionLocalOf { 0 }

/**
 * Shapes for empty states (owner, 2026-11-21): one shape only, always a cookie, clover, Gem or
 * Pentagon. Never Puffy / Puffy Diamond / Heart (rule 6).
 */
private val EmptyShapes = listOf(
    MaterialShapes.Cookie4Sided, MaterialShapes.Cookie6Sided, MaterialShapes.Cookie7Sided,
    MaterialShapes.Cookie9Sided, MaterialShapes.Cookie12Sided,
    MaterialShapes.Clover4Leaf, MaterialShapes.Clover8Leaf,
    MaterialShapes.Gem, MaterialShapes.Pentagon,
)

/**
 * Empty screens: a single library shape, no icon. It's picked at random each time the tab is
 * shown, pops in on the slow spatial spring, then turns slowly.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    action: (@Composable () -> Unit)? = null,
) {
    val visit = LocalTabVisit.current
    val shape = remember(visit) { EmptyShapes.random() }
    val spin by slowSpin(48_000)
    val pop = remember(visit) { Animatable(0.6f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(visit) { pop.animateTo(1f, spec) }
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(size)
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationZ = spin }
                .background(color, shape.toShape()),
        )
        Text(title, style = MaterialTheme.typography.headlineSmallEmphasized, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) Box(Modifier.padding(top = 4.dp)) { action() }
    }
}
