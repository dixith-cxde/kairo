@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.focus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.material3.toPath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.KairoApplication
import app.kairo.R
import app.kairo.focus.Phase
import app.kairo.data.model.Task
import app.kairo.ui.common.slowSpin
import app.kairo.ui.common.selectableRowColors
import app.kairo.ui.theme.KairoTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

/** Focus (canvas Focus board): task chip, big ring in a turning Cookie12, session dots, Stop / Pause / +5. */
@Composable
fun FocusScreen(app: KairoApplication, onBack: () -> Unit) {
    val focus = app.focus
    val s by focus.state.collectAsStateWithLifecycle()
    val tasks by app.repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val since = remember { LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val todayMin by app.database.focus().minutesSince(since).collectAsStateWithLifecycle(initialValue = 0)
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val haptic = LocalHapticFeedback.current
    var pickTask by remember { mutableStateOf(false) }
    var tune by remember { mutableStateOf(false) }

    // Tick once a second while running; when time's up, move to the next phase.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s.running, s.endAt) {
        while (s.running) {
            now = System.currentTimeMillis()
            if (now >= s.endAt) { haptic.performHapticFeedback(HapticFeedbackType.Confirm); focus.finishPhase(); break }
            delay(1000 - now % 1000)
        }
    }
    val left = s.leftMs(now)
    val progress = 1f - left / s.phaseMs.toFloat().coerceAtLeast(1f)
    val ring by animateFloatAsState(progress.coerceIn(0f, 1f), MaterialTheme.motionScheme.slowEffectsSpec(), label = "ring")
    val task = tasks.firstOrNull { it.id == s.taskId }
    val isBreak = s.phase == Phase.Break
    val ringBg by animateColorAsState(if (isBreak) c.tertiaryContainer else c.primaryContainer, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "ringBg")
    val onRing = if (isBreak) c.onTertiaryContainer else c.onPrimaryContainer

    Scaffold(
        containerColor = k.bg,
        topBar = {
            TopAppBar(
                title = { Text("Focus") },
                navigationIcon = { IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") } },
                actions = { IconButton(onClick = { tune = true }, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(R.drawable.ic_tune), "Focus settings") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = k.bg),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AssistChip(
                onClick = { pickTask = true },
                label = { Text(task?.title ?: "Pick a task (optional)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_task_alt), null, Modifier.size(18.dp)) },
                shape = CircleShape,
                colors = AssistChipDefaults.assistChipColors(containerColor = c.secondaryContainer, labelColor = c.onSecondaryContainer, leadingIconContentColor = c.onSecondaryContainer),
                border = null,
            )

            // The clock, in one of three views. Tap it while paused/reset to change the timing.
            val prefs = remember { app.getSharedPreferences("focus", android.content.Context.MODE_PRIVATE) }
            var view by remember { mutableStateOf(ClockView.valueOf(prefs.getString("view", ClockView.Ring.name)!!)) }
            val timeText = "%d:%02d".format(left / 60_000, left / 1000 % 60)
            val caption = when (s.phase) {
                Phase.Idle -> "${s.focusMin} min focus · ${s.sessions} sessions"
                Phase.Focus -> "of ${s.focusMin} min · session ${s.session} of ${s.sessions}"
                Phase.Break -> "Break · ${s.breakMin} min"
            }
            val clockMod = Modifier
                .clickable(enabled = !s.running, role = Role.Button, onClickLabel = "Change timer") { tune = true }
                .semantics { contentDescription = "${left / 60_000} minutes ${left / 1000 % 60} seconds left" }
            val viewSpring = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
            AnimatedContent(
                view,
                transitionSpec = {
                    (scaleIn(viewSpring, initialScale = 0.85f) + fadeIn()) togetherWith
                        (scaleOut(targetScale = 0.85f) + fadeOut())
                },
                label = "clockView",
            ) { v ->
                when (v) {
                    ClockView.Ring -> RingClock(ring, timeText, caption, ringBg, onRing, if (isBreak) c.tertiary else c.primary, clockMod)
                    ClockView.Liquid -> LiquidClock(ring, timeText, caption, ringBg, onRing, if (isBreak) c.tertiary else c.primary, clockMod)
                    ClockView.Minimal -> MinimalClock(ring, timeText, caption, if (isBreak) c.tertiary else c.primary, clockMod)
                }
            }
            // View switcher: M3 Expressive connected icon group.
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                ClockView.entries.forEachIndexed { i, v ->
                    ToggleButton(
                        checked = v == view,
                        onCheckedChange = { view = v; prefs.edit().putString("view", v.name).apply() },
                        shapes = when (i) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            ClockView.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier.semantics { role = Role.RadioButton },
                    ) { Text(v.label, style = MaterialTheme.typography.labelLarge) }
                }
            }

            // Session dots: done, current (ringed), to go.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.semantics { contentDescription = "Session ${s.session} of ${s.sessions}" }) {
                (1..s.sessions).forEach { i ->
                    val done = i < s.session || (i == s.session && isBreak)
                    val current = i == s.session && s.phase == Phase.Focus
                    val size by androidx.compose.animation.core.animateDpAsState(if (current) 16.dp else 12.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "dot")
                    Box(Modifier.size(size).background(if (done || current) c.primary else c.surfaceContainerHighest, CircleShape))
                }
            }

            // Controls: Stop · Start/Pause (large, morphing) · +5
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalIconButton(
                    onClick = { focus.stop() }, enabled = s.phase != Phase.Idle,
                    shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(64.dp),
                ) { Icon(painterResource(R.drawable.ic_stop_filled), "Stop", Modifier.size(32.dp)) }
                Button(
                    onClick = { if (s.running) focus.pause() else focus.start() },
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = PaddingValues(horizontal = 28.dp),
                    modifier = Modifier.height(80.dp),
                ) {
                    val pop = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
                    AnimatedContent(s.running, transitionSpec = {
                        (scaleIn(pop) + fadeIn()) togetherWith (scaleOut() + fadeOut())
                    }, label = "play") { running ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(if (running) R.drawable.ic_pause_filled else R.drawable.ic_play_arrow_filled), null, Modifier.size(32.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(if (running) "Pause" else if (s.phase == Phase.Idle) "Start" else "Resume", style = MaterialTheme.typography.titleLargeEmphasized)
                        }
                    }
                }
                FilledTonalIconButton(
                    onClick = { focus.addFive() }, enabled = s.phase != Phase.Idle,
                    shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(64.dp),
                ) { Text("+5", style = MaterialTheme.typography.labelLargeEmphasized) }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(painterResource(R.drawable.ic_schedule), null, Modifier.size(18.dp), tint = c.onSurfaceVariant)
                Text("Focused today: ", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                Text(if (todayMin >= 60) "${todayMin / 60} h ${todayMin % 60} min" else "$todayMin min",
                    style = MaterialTheme.typography.labelLargeEmphasized, color = c.onSurface)
            }
        }
    }

    if (pickTask) ModalBottomSheet(onDismissRequest = { pickTask = false }) {
        val open = tasks.filter { !it.done }.take(20)
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Focus on…", style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
            val rows = listOf<Task?>(null) + open
            rows.forEachIndexed { i, t ->
                SegmentedListItem(
                    selected = t?.id == s.taskId,
                    onClick = { focus.setTask(t?.id); pickTask = false },
                    shapes = ListItemDefaults.segmentedShapes(i, rows.size),
                    colors = selectableRowColors(),
                ) { Text(t?.title ?: "No task", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                if (i < rows.lastIndex) Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
            }
        }
    }

    if (tune) FocusSettingsSheet(s.focusMin, s.breakMin, s.sessions, onDismiss = { tune = false }) { f, b, n -> focus.configure(f, b, n) }
}

enum class ClockView(val label: String) { Ring("Ring"), Liquid("Liquid"), Minimal("Minimal") }

/** Ring: turning Cookie12 + thick wavy progress ring (canvas Focus board). */
@Composable
private fun RingClock(p: Float, time: String, caption: String, bg: androidx.compose.ui.graphics.Color, on: androidx.compose.ui.graphics.Color,
                      accent: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val spin by slowSpin(60_000)
    val stroke = with(LocalDensity.current) { Stroke(10.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round) }
    Box(modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin }.background(bg, MaterialShapes.Cookie12Sided.toShape()))
        CircularWavyProgressIndicator(progress = { p }, color = accent, trackColor = on.copy(alpha = 0.16f),
            stroke = stroke, trackStroke = stroke, modifier = Modifier.size(256.dp))
        ClockText(time, caption, on)
    }
}

/** Liquid: the Cookie12 fills up as time passes, with a gently moving wave on top. */
@Composable
private fun LiquidClock(p: Float, time: String, caption: String, bg: androidx.compose.ui.graphics.Color, on: androidx.compose.ui.graphics.Color,
                        accent: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val phase by slowSpin(4_000) // wave drift (ambient loop)
    val shape = MaterialShapes.Cookie12Sided.toPath()
    Box(modifier.size(300.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val path = androidx.compose.ui.graphics.Path().apply {
                addPath(shape); transform(androidx.compose.ui.graphics.Matrix().apply { scale(size.width, size.height) })
            }
            drawPath(path, bg)
            clipPath(path) {
                val top = size.height * (1f - p)
                val wave = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, top)
                    for (x in 0..48) {
                        val fx = size.width * x / 48
                        lineTo(fx, top + kotlin.math.sin(x / 48f * 4 * Math.PI + phase / 180 * Math.PI).toFloat() * size.height * 0.025f)
                    }
                    lineTo(size.width, size.height); lineTo(0f, size.height); close()
                }
                drawPath(wave, accent.copy(alpha = 0.55f))
            }
        }
        ClockText(time, caption, on)
    }
}

/** Minimal: huge rounded digits and a wavy line. */
@Composable
private fun MinimalClock(p: Float, time: String, caption: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier.height(300.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(time, style = MaterialTheme.typography.displayLargeEmphasized.copy(fontSize = 96.sp, lineHeight = 100.sp),
            maxLines = 1, softWrap = false)
        Text(caption, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.material3.LinearWavyProgressIndicator(progress = { p }, color = accent,
            modifier = Modifier.padding(top = 24.dp).fillMaxWidth(0.8f).height(12.dp))
    }
}

@Composable
private fun ClockText(time: String, caption: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(time, style = MaterialTheme.typography.displayLargeEmphasized.copy(fontSize = 72.sp, lineHeight = 76.sp), color = color)
        Text(caption, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/**
 * Timer settings drawer: presets as a connected group, then sliders for any custom timing.
 * Changes apply right away when the timer isn't running, else from the next phase.
 */
@Composable
private fun FocusSettingsSheet(focusMin: Int, breakMin: Int, sessions: Int, onDismiss: () -> Unit, onChange: (Int, Int, Int) -> Unit) {
    val c = MaterialTheme.colorScheme
    val presets = listOf(Triple("Short", 15, 3), Triple("Pomodoro", 25, 5), Triple("Deep", 50, 10))
    var f by remember { mutableStateOf(focusMin.toFloat()) }
    var b by remember { mutableStateOf(breakMin.toFloat()) }
    var n by remember { mutableStateOf(sessions.toFloat()) }
    fun push() = onChange(f.toInt(), b.toInt(), n.toInt())
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Timer", style = MaterialTheme.typography.headlineSmallEmphasized)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                presets.forEachIndexed { i, (label, pf, pb) ->
                    ToggleButton(
                        checked = f.toInt() == pf && b.toInt() == pb,
                        onCheckedChange = { f = pf.toFloat(); b = pb.toFloat(); push() },
                        shapes = when (i) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            presets.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, style = MaterialTheme.typography.labelLargeEmphasized)
                            Text("$pf · $pb", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            SliderRow("Focus", f, 5f..120f, 22, "${f.toInt()} min") { f = it; push() }
            SliderRow("Break", b, 1f..30f, 28, "${b.toInt()} min") { b = it; push() }
            SliderRow("Sessions", n, 1f..8f, 6, "${n.toInt()}") { n = it; push() }
            Text("Tip: tap the clock while it's paused to come back here.", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, valueText: String, onChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.headlineSmallEmphasized)
        }
        androidx.compose.material3.Slider(
            value = value, onValueChange = { onChange(kotlin.math.round(it)) },
            valueRange = range, steps = steps,
        )
    }
}

