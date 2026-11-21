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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon

/**
 * Empty screens (owner, 2026-11-21): a large soft library shape turning slowly behind a smaller
 * one with the icon, a title and one line of help, and an optional action. The pair pops in on a
 * fast spatial spring. Never Puffy / Heart (rule 6).
 */
@Composable
fun EmptyState(
    icon: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    shape: RoundedPolygon = MaterialShapes.Cookie9Sided,
    size: Dp = 200.dp,
    back: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    front: Color = MaterialTheme.colorScheme.secondaryContainer,
    onFront: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    action: (@Composable () -> Unit)? = null,
) {
    val spin by slowSpin(48_000)
    val pop = remember { Animatable(0.6f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(Unit) { pop.animateTo(1f, spec) }
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(size).graphicsLayer { scaleX = pop.value; scaleY = pop.value }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(size).graphicsLayer { rotationZ = spin }.background(back, shape.toShape()))
            Box(
                Modifier.size(size * 0.46f).graphicsLayer { rotationZ = -spin * 2 }.background(front, MaterialShapes.Cookie6Sided.toShape()),
            )
            Icon(painterResource(icon), null, tint = onFront, modifier = Modifier.size(size * 0.2f))
        }
        Text(title, style = MaterialTheme.typography.headlineSmallEmphasized, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) Box(Modifier.padding(top = 4.dp)) { action() }
    }
}
