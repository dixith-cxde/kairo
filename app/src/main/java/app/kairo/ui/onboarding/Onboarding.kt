@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import app.kairo.R
import app.kairo.ui.common.slowSpin
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.launch

// ───────── Welcome (Stash welcome.tsx pattern: pages of shape + title + body + three points) ─────────

private class Page(val shape: RoundedPolygon, val icon: Int, val title: String, val body: String, val points: List<Pair<Int, String>>)

private val Pages = listOf(
    Page(MaterialShapes.Cookie9Sided, R.drawable.ic_today_filled, "Your day,\nin one place",
        "Tasks, reminders and your phone's calendar, sorted by time on Today.",
        listOf(R.drawable.ic_task_alt to "Type naturally: \"Call mom tomorrow 6pm\"",
            R.drawable.ic_notifications to "Reminders that fire on time",
            R.drawable.ic_calendar_month to "Month, week and day views")),
    Page(MaterialShapes.Clover4Leaf, R.drawable.ic_account_balance_wallet_filled, "Money,\nwithout the noise",
        "Expenses, income, transfers, budgets and bills in ₹ with Indian grouping.",
        listOf(R.drawable.ic_payments to "Add an expense in two taps",
            R.drawable.ic_repeat to "Bills and subscriptions that repeat",
            R.drawable.ic_pie_chart to "See where it went")),
    Page(MaterialShapes.Sunny, R.drawable.ic_lock, "Yours,\nand only yours",
        "Everything stays on this phone. No account, no sign-in, no tracking.",
        listOf(R.drawable.ic_lock to "Optional app lock",
            R.drawable.ic_bolt to "Widgets for your home screen",
            R.drawable.ic_timer to "A focus timer when you need it")),
)

@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val pager = rememberPagerState { Pages.size }
    val scope = rememberCoroutineScope()
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val last = pager.currentPage == Pages.lastIndex
    BackHandler(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1, animationSpec = spatial) } }
    // No Surface here, so set the text color ourselves (it defaulted to black on the dark screen).
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides c.onSurface) {
    Column(Modifier.fillMaxSize().background(k.bg).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            if (!last) TextButton(onClick = onDone, shapes = ButtonDefaults.shapes()) { Text("Skip") }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { i -> WelcomePage(Pages[i]) }
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Pages.indices.forEach { i ->
                val w by animateDpAsState(if (i == pager.currentPage) 24.dp else 8.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "dot")
                Box(Modifier.padding(end = 6.dp).size(w, 8.dp).background(if (i == pager.currentPage) c.primary else c.outlineVariant, CircleShape))
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1, animationSpec = spatial) } },
                shapes = ButtonDefaults.shapes(), modifier = Modifier.height(56.dp),
            ) {
                val fx = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                AnimatedContent(last, transitionSpec = { fadeIn(fx) togetherWith fadeOut(fx) }, label = "cta") {
                    Text(if (it) "Get started" else "Next", style = MaterialTheme.typography.titleMediumEmphasized)
                }
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(R.drawable.ic_chevron_right), null)
            }
        }
    }
}
}

@Composable
private fun WelcomePage(p: Page) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val spin by slowSpin(40_000)
    val pop = remember { Animatable(0.6f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(Unit) { pop.animateTo(1f, spec) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(168.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }, contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin }.background(c.primaryContainer, p.shape.toShape()))
            Icon(painterResource(p.icon), null, tint = c.onPrimaryContainer, modifier = Modifier.size(64.dp))
        }
        Text(p.title, style = MaterialTheme.typography.displaySmallEmphasized, modifier = Modifier.padding(top = 12.dp))
        Text(p.body, style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant)
        Column(Modifier.fillMaxWidth().background(k.item, RoundedCornerShape(24.dp)).padding(8.dp)) {
            p.points.forEach { (icon, text) ->
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).background(c.secondaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(icon), null, tint = c.onSecondaryContainer, modifier = Modifier.size(20.dp))
                    }
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ───────── Tour (Stash HomeTour: spotlight hole + primary ring + tooltip card with step counter) ─────────

/** Where the tour's targets are on screen (window coordinates), recorded by [tourTarget]. */
object TourTargets {
    val rects = mutableStateMapOf<String, Pair<Rect, Float>>() // rect, corner radius px
}

object TourTargetScope {
    @Composable fun Modifier.target(id: String, cornerRadius: androidx.compose.ui.unit.Dp) = tourTarget(id, cornerRadius)
}

/** Records this element's bounds for the tour, only while it's actually on screen (pager pages aren't). */
@Composable
fun Modifier.tourTarget(id: String, cornerRadius: androidx.compose.ui.unit.Dp): Modifier {
    val width = LocalWindowInfo.current.containerSize.width
    val r = with(LocalDensity.current) { cornerRadius.toPx() }
    return onGloballyPositioned { lc ->
        val b = lc.boundsInWindow()
        if (b.width > 0 && b.left >= 0f && b.right <= width + 1f) TourTargets.rects[id] = b to r
    }
}

private class Step(val id: String, val icon: Int, val title: String, val body: String)

private val Steps = listOf(
    Step("hero", R.drawable.ic_today, "Your day at a glance", "The ring fills as you finish today's tasks. When it closes, you're done for the day."),
    Step("filters", R.drawable.ic_tune, "Slice the day", "Overdue, today, the week ahead or what you've done. Tap a chip to switch."),
    Step("search", R.drawable.ic_search, "Search everything", "Tasks, lists and money, from any tab."),
    Step("focus", R.drawable.ic_timer, "Focus timer", "Start a focus session for a task, up to 5 hours, with a live notification."),
    Step("settings", R.drawable.ic_settings, "Make it yours", "Theme, colors, app lock, widgets and importing from Ivy Wallet live in Settings."),
    Step("add", R.drawable.ic_add_bold, "Create anything", "Tap + for a task, expense, income, transfer or list. Type naturally: \"gym tmrw 7am\". It stays put when the bar tucks away."),
    Step("nav", R.drawable.ic_checklist, "Five tabs", "Today, Lists, Calendar, Money and Stats. On any task row, swipe left to complete or right to delete, or tap ⋮ for more."),
)


@Composable
fun TourOverlay(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val s = Steps[step]
    val c = MaterialTheme.colorScheme
    val target = TourTargets.rects[s.id] ?: run { LaunchedEffect(s.id) { if (step < Steps.lastIndex) step++ else onDone() }; return }
    val pad = with(LocalDensity.current) { 6.dp.toPx() }
    val rect = target.first.inflate(pad)
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    // The hole glides between targets on the spatial spring.
    val l by animateFloatAsState(rect.left, spec, label = "l"); val t by animateFloatAsState(rect.top, spec, label = "t")
    val r by animateFloatAsState(rect.right, spec, label = "r"); val b by animateFloatAsState(rect.bottom, spec, label = "b")
    val rad by animateFloatAsState(target.second + pad, spec, label = "rad")
    BackHandler { onDone() }
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides c.onSurface) {
    BoxWithConstraints(
        Modifier.fillMaxSize()
            // Blocks taps on the app underneath while the tour is up.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        val scrim = Color.Black.copy(alpha = 0.6f)
        val ring = c.primary
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(scrim)
            drawRoundRect(Color.Transparent, Offset(l, t), Size(r - l, b - t), CornerRadius(rad), blendMode = BlendMode.Clear)
            drawRoundRect(ring, Offset(l, t), Size(r - l, b - t), CornerRadius(rad), style = Stroke(2.dp.toPx()))
        }
        val density = LocalDensity.current
        val screenH = with(density) { maxHeight.toPx() }
        val below = (rect.center.y < screenH / 2)
        val y = with(density) { (if (below) rect.bottom + 16.dp.toPx() else rect.top - 16.dp.toPx()).toDp() }
        val fxIn = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val fxOut = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        AnimatedContent(step, transitionSpec = { fadeIn(fxIn) togetherWith fadeOut(fxOut) },
            label = "tip", modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .offset { IntOffset(0, y.roundToPx()) }
                .graphicsLayer { if (!below) translationY = -size.height }) { i ->
            val st = Steps[i]
            Column(
                Modifier.fillMaxWidth().background(c.surfaceContainerHigh, RoundedCornerShape(28.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).background(c.primaryContainer, MaterialShapes.Cookie6Sided.toShape()), contentAlignment = Alignment.Center) {
                        Icon(painterResource(st.icon), null, tint = c.onPrimaryContainer, modifier = Modifier.size(22.dp))
                    }
                    Text(st.title, style = MaterialTheme.typography.titleMediumEmphasized, modifier = Modifier.weight(1f))
                    Text("${i + 1} of ${Steps.size}", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                }
                Text(st.body, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (i < Steps.lastIndex) TextButton(onClick = onDone, shapes = ButtonDefaults.shapes()) { Text("Skip tour") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { if (i < Steps.lastIndex) step++ else onDone() }, shapes = ButtonDefaults.shapes()) {
                        Text(if (i < Steps.lastIndex) "Next" else "Done")
                    }
                }
            }
        }
    }
}
}
