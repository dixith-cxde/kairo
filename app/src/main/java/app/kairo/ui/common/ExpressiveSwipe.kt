@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * M3 Expressive swipe, as in Gmail and Android 16's shade: the row rounds into a pill as it moves,
 * and the action is its own full-round pill growing from the edge, 8dp clear of the row
 * (right on primaryContainer, left on errorContainer). Past the threshold the pill turns
 * solid and its icon springs bigger. Used by task rows and habit rows.
 */
@Composable
fun ExpressiveSwipe(
    enabled: Boolean,
    rightIcon: Int, rightLabel: String,
    leftIcon: Int, leftLabel: String,
    onRight: () -> Unit, onLeft: () -> Unit,
    rightEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = rememberSwipeToDismissBoxState()
    val c = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val density = androidx.compose.ui.platform.LocalDensity.current
    val gap = with(density) { 8.dp.toPx() }
    val roundBy = with(density) { 56.dp.toPx() } // fully round after 56dp of travel
    fun offset() = runCatching { state.requireOffset() }.getOrDefault(0f)
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = enabled && rightEnabled,
        enableDismissFromEndToStart = enabled,
        onDismiss = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onRight()
                SwipeToDismissBoxValue.EndToStart -> onLeft()
                else -> {}
            }
            scope.launch { state.reset() }
        },
        backgroundContent = {
            val dir = state.dismissDirection
            if (dir == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            val delete = dir == SwipeToDismissBoxValue.EndToStart
            val past = state.targetValue != SwipeToDismissBoxValue.Settled
            val grow by animateFloatAsState(if (past) 1.3f else 1f, motion.fastSpatialSpec(), label = "swipeIcon")
            val bg by androidx.compose.animation.animateColorAsState(
                when { delete && past -> c.error; delete -> c.errorContainer; past -> c.primary; else -> c.primaryContainer },
                motion.defaultEffectsSpec(), label = "swipeBg",
            )
            val fg by androidx.compose.animation.animateColorAsState(
                when { delete && past -> c.onError; delete -> c.onErrorContainer; past -> c.onPrimary; else -> c.onPrimaryContainer },
                motion.defaultEffectsSpec(), label = "swipeFg",
            )
            Box(Modifier.fillMaxSize(), contentAlignment = if (delete) Alignment.CenterEnd else Alignment.CenterStart) {
                // The action pill: as wide as the gap the row leaves (minus 4dp), always fully round.
                Box(
                    Modifier
                        .layout { m, cs ->
                            val w = (kotlin.math.abs(offset()) - gap).coerceIn(0f, cs.maxWidth.toFloat()).toInt()
                            val p = m.measure(cs.copy(minWidth = w, maxWidth = w))
                            layout(w, p.height) { p.place(0, 0) }
                        }
                        .fillMaxHeight()
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(bg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(if (delete) leftIcon else rightIcon),
                        if (delete) leftLabel else rightLabel,
                        tint = fg,
                        modifier = Modifier.size(24.dp).graphicsLayer { scaleX = grow; scaleY = grow },
                    )
                }
            }
        },
    ) {
        // The row itself rounds into a pill as it travels.
        Box(Modifier.graphicsLayer {
            val f = (kotlin.math.abs(offset()) / roundBy).coerceIn(0f, 1f)
            if (f > 0f) {
                clip = true
                shape = RoundedCornerShape(percent = (f * 50).toInt().coerceAtLeast(1))
            }
        }) { content() }
    }
}

