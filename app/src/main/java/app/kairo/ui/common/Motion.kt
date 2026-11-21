package app.kairo.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Screen-level entrance: each item rises 24dp and fades in on the spatial spring, staggered by index,
 * the first time the screen shows. Items scrolled back into view don't replay.
 */
class Entrance internal constructor(private val played: MutableSet<String>) {
    fun Modifier.enter(key: Any, index: Int): Modifier = composed {
        val k = key.toString()
        val already = remember { k in played }
        val p = remember { Animatable(if (already) 1f else 0f) }
        val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
        LaunchedEffect(Unit) {
            if (!already) {
                played += k
                delay(min(index, 10) * 35L) // stagger: one beat per row, capped
                p.animateTo(1f, spatial)
            }
        }
        graphicsLayer {
            alpha = p.value.coerceIn(0f, 1f)
            translationY = (1f - p.value) * 24.dp.toPx()
        }
    }
}

@Composable
fun rememberEntrance(): Entrance {
    val played = rememberSaveable { mutableStateOf(HashSet<String>()) }
    return remember { Entrance(played.value) }
}

/** A number that counts up to its target (effects spring: no overshoot past the real value). */
@Composable
fun countUp(target: Int): State<Int> {
    val a = remember { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
    LaunchedEffect(target) { a.animateTo(target.toFloat(), spec) }
    return remember { derivedIntState(a) }
}

private fun derivedIntState(a: Animatable<Float, *>): State<Int> = object : State<Int> {
    override val value: Int get() = kotlin.math.round(a.value).toInt()
}

/**
 * Slow, endless turn for decorative hero shapes (canvas: 60s spin). The motion scheme has no
 * looping token, so this is the one place with a written duration (owner asked for ambient motion).
 */
@Composable
fun slowSpin(periodMillis: Int = 60_000): State<Float> =
    rememberInfiniteTransition(label = "spin").animateFloat(
        0f, 360f, infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart), label = "spin",
    )
