package app.kairo.ui.finance

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kairo.R
import app.kairo.data.money.Money
import app.kairo.ui.theme.KairoTheme

/** One bar: its label under it, the full label when picked, and the amount. */
class Bucket(val label: String, val full: String, val paise: Long)

/**
 * Canvas v103–v105 pill bars: each bar fills its column (2dp gaps), full-round; green when the period
 * beats the average (spent less, or earned more), blue otherwise. A green bar tall enough carries a
 * Cookie-12 check inside its top cap. A dashed average line with its value; the picked label goes bold.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun Bars(buckets: List<Bucket>, avg: Long, income: Boolean, picked: Int, onPick: (Int) -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val dark = c.background.red < 0.5f
    val max = maxOf(buckets.maxOfOrNull { it.paise } ?: 0L, avg, 1L).toFloat()
    val chartH = 140.dp
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(chartH)) {
            Row(Modifier.fillMaxSize().padding(end = 36.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                buckets.forEachIndexed { i, b ->
                    val good = if (income) b.paise >= avg else b.paise <= avg
                    val h by animateFloatAsState((b.paise / max).coerceIn(0.04f, 1f), MaterialTheme.motionScheme.slowSpatialSpec(), label = "bar")
                    Box(
                        Modifier.weight(1f).fillMaxHeight(h).background(if (b.paise == 0L) c.outlineVariant else if (good) k.chartGreen else k.chartBlue, CircleShape).clickable { onPick(i) }
                            .semantics { contentDescription = "${b.full}: ${Money.format(b.paise)}" },
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (good && b.paise > 0 && chartH * h > 48.dp) Box(
                            Modifier.padding(top = 4.dp).size(28.dp).background(if (dark) Color(0xFFC4EED0) else Color(0xFF6DD58C), MaterialShapes.Cookie12Sided.toShape()),
                            contentAlignment = Alignment.Center,
                        ) { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp), tint = Color(0xFF0F5223)) }
                    }
                }
            }
            val y = chartH * (1 - avg / max)
            val line = c.onSurfaceVariant.copy(alpha = 0.6f)
            Canvas(Modifier.fillMaxWidth().height(1.dp).offset(y = y)) {
                drawLine(line, Offset(0f, 0f), Offset(size.width - 34.dp.toPx(), 0f), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            }
            Text(short(avg), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontFeatureSettings = "tnum"), color = c.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopEnd).offset(y = y - 7.dp))
        }
        Row(Modifier.fillMaxWidth().padding(end = 36.dp, top = 6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val every = ((buckets.size + 6) / 7).coerceAtLeast(1) // at most ~7 labels
            buckets.forEachIndexed { i, b ->
                Text(if (i % every == 0 || i == picked) b.label else "", style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (i == picked) FontWeight.Bold else FontWeight.Normal),
                    color = if (i == picked) c.onSurface else c.onSurfaceVariant, maxLines = 1, softWrap = false,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

/** "₹35k", "₹1.2L" for the average line. */
internal fun short(paise: Long): String {
    val r = paise / 100.0
    return when { r >= 1_00_000 -> "₹%.1fL".format(r / 1_00_000); r >= 1000 -> "₹%.0fk".format(r / 1000); else -> "₹%.0f".format(r) }
}
