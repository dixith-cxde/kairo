@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Kairo's Expressive time picker (owner, 2026-11-28): the M3 time-picker layout (hour and minute
 * boxes, AM/PM, a dial), but the dial is a Cookie-12 and the hand is a wavy line, like the wavy
 * progress indicators. M3's TimePicker can't change its dial shape, so this one is drawn here;
 * colours and springs come from the theme. Tap or drag to pick; picking an hour moves to minutes.
 */
@Composable
fun ExpressiveTimePicker(hour: Int, minute: Int, onChange: (Int, Int) -> Unit, modifier: Modifier = Modifier, is24: Boolean = false) {
    var minutes by remember { mutableStateOf(false) }
    val change by rememberUpdatedState(onChange)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TimeBox(if (is24) "%02d".format(hour) else "%02d".format((hour + 11) % 12 + 1), !minutes, "Hour") { minutes = false }
            Text(":", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurface)
            TimeBox("%02d".format(minute), minutes, "Minute") { minutes = true }
            if (!is24) Column(Modifier.padding(start = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Half("AM", hour < 12, top = true) { if (hour >= 12) change(hour - 12, minute) }
                Half("PM", hour >= 12, top = false) { if (hour < 12) change(hour + 12, minute) }
            }
        }
        Dial(hour, minute, minutes, is24, onPick = { h, m, done -> change(h, m); if (done && !minutes) minutes = true })
    }
}

@Composable
private fun TimeBox(text: String, on: Boolean, label: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (on) c.primaryContainer else c.surfaceContainerHighest, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "box")
    Box(
        Modifier.size(width = 96.dp, height = 80.dp).clip(RoundedCornerShape(if (on) 24.dp else 12.dp)).background(bg)
            .clickable(role = Role.Button, onClickLabel = "Pick $label".lowercase(), onClick = onClick)
            .semantics { contentDescription = "$label $text" },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"), color = if (on) c.onPrimaryContainer else c.onSurface)
    }
}

@Composable
private fun Half(text: String, on: Boolean, top: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val shape = if (top) RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    else RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    Box(
        Modifier.size(width = 52.dp, height = 39.dp).clip(shape).background(if (on) c.tertiaryContainer else c.surfaceContainerHighest)
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = if (on) c.onTertiaryContainer else c.onSurfaceVariant) }
}

/** The Cookie-12 dial with numbers, the selection circle and the wavy hand. */
@Composable
private fun Dial(hour: Int, minute: Int, minutes: Boolean, is24: Boolean, onPick: (Int, Int, Boolean) -> Unit) {
    val c = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val scope = rememberCoroutineScope()
    val inner = is24 && !minutes && (hour == 0 || hour > 12)
    val target = if (minutes) minute * 6f else (hour % 12) * 30f
    val angle = remember { Animatable(target) }
    val radiusF = remember { Animatable(if (inner) 0.25f else 0.36f) }
    LaunchedEffect(target, minutes) {
        // Turn the short way round.
        var d = target - angle.value % 360f
        if (d > 180f) d -= 360f; if (d < -180f) d += 360f
        angle.animateTo(angle.value + d, motion.fastSpatialSpec())
    }
    LaunchedEffect(inner) { radiusF.animateTo(if (inner) 0.25f else 0.36f, motion.fastSpatialSpec()) }
    val pick by rememberUpdatedState(onPick)
    fun valueAt(p: Offset, size: Float, done: Boolean) {
        val cx = size / 2; val dx = p.x - cx; val dy = p.y - cx
        val deg = ((atan2(dy, dx) * 180 / PI + 90 + 360) % 360).toFloat()
        if (minutes) pick(hour, (deg / 6f).roundToInt() % 60, done)
        else {
            val pos = (deg / 30f).roundToInt() % 12
            val h = if (is24) { val innerRing = hypot(dx, dy) < size * 0.30f; if (innerRing) (if (pos == 0) 0 else pos + 12) else (if (pos == 0) 12 else pos) }
            else (if (pos == 0) 0 else pos) + if (hour >= 12) 12 else 0
            pick(h, minute, done)
        }
    }
    Box(
        Modifier.size(256.dp)
            .pointerInput(minutes, is24) { detectTapGestures { p -> valueAt(p, size.width.toFloat(), true) } }
            .pointerInput(minutes, is24) {
                detectDragGestures(onDragEnd = { pick(hour, minute, true) }) { ch, _ -> valueAt(ch.position, size.width.toFloat(), false) }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().background(c.surfaceContainerHighest, MaterialShapes.Cookie12Sided.toShape()))
        val wave = c.primary
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val r = size.width * radiusF.value
            val a = (angle.value - 90f) * PI.toFloat() / 180f
            val end = Offset(center.x + r * cos(a), center.y + r * sin(a))
            val len = r - 24.dp.toPx()
            val amp = 3.dp.toPx(); val lambda = 14.dp.toPx()
            val path = Path()
            val nx = -sin(a); val ny = cos(a)
            var t = 0f
            while (t <= len) {
                val w = amp * sin(2 * PI.toFloat() * t / lambda) * sin(PI.toFloat() * t / len) // calm at both ends
                val x = center.x + t * cos(a) + nx * w; val y = center.y + t * sin(a) + ny * w
                if (t == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                t += 1f
            }
            drawPath(path, wave, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(wave, 4.dp.toPx(), center)
            drawCircle(wave, 24.dp.toPx(), end)
        }
        Numbers(minutes, is24, hour, minute, radiusF.value, angle.value)
    }
}

/** Labels around the dial; the one under the selection circle turns onPrimary. */
@Composable
private fun Numbers(minutes: Boolean, is24: Boolean, hour: Int, minute: Int, handR: Float, handDeg: Float) {
    val c = MaterialTheme.colorScheme
    val outer = (0 until 12).map { i -> if (minutes) "%02d".format(i * 5) else "${if (i == 0) 12 else i}" }
    val innerL = if (is24 && !minutes) (0 until 12).map { i -> if (i == 0) "00" else "${i + 12}" } else emptyList()
    Box(Modifier.size(256.dp)) {
        listOf(outer to 0.36f, innerL to 0.25f).forEach { (labels, rf) ->
            labels.forEachIndexed { i, s ->
                val deg = i * 30f
                val a = (deg - 90f) * PI.toFloat() / 180f
                val under = kotlin.math.abs(((handDeg - deg) % 360f + 540f) % 360f - 180f) < 8f && kotlin.math.abs(handR - rf) < 0.04f
                Box(
                    Modifier.size(48.dp).align(Alignment.Center)
                        .offset((256f * rf * cos(a)).dp, (256f * rf * sin(a)).dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(s, fontSize = if (rf < 0.3f) 13.sp else 16.sp, style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                        color = if (under) c.onPrimary else c.onSurface)
                }
            }
        }
    }
}

