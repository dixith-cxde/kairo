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

            // The hero: a slowly turning Cookie12, wavy progress ring, big tabular time.
            val spin by slowSpin(60_000)
            val stroke = with(LocalDensity.current) { Stroke(10.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round) }
            Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin }.background(ringBg, MaterialShapes.Cookie12Sided.toShape()))
                CircularWavyProgressIndicator(
                    progress = { ring },
                    color = if (isBreak) c.tertiary else c.primary,
                    trackColor = onRing.copy(alpha = 0.16f),
                    stroke = stroke, trackStroke = stroke,
                    modifier = Modifier.size(256.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.semantics { contentDescription = "${left / 60_000} minutes ${left / 1000 % 60} seconds left" }) {
                    Text(
                        "%d:%02d".format(left / 60_000, left / 1000 % 60),
                        style = MaterialTheme.typography.displayLargeEmphasized.copy(fontSize = 72.sp, lineHeight = 76.sp),
                        color = onRing,
                    )
                    Text(
                        when (s.phase) {
                            Phase.Idle -> "${s.focusMin} min focus · ${s.sessions} sessions"
                            Phase.Focus -> "of ${s.focusMin} min · session ${s.session} of ${s.sessions}"
                            Phase.Break -> "Break · ${s.breakMin} min"
                        },
                        style = MaterialTheme.typography.labelLarge, color = onRing,
                    )
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

    if (tune) ModalBottomSheet(onDismissRequest = { tune = false }) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Focus settings", style = MaterialTheme.typography.titleLargeEmphasized)
            Choice("Focus length", listOf(15, 25, 45, 60), s.focusMin, { "$it min" }) { focus.configure(it, s.breakMin, s.sessions) }
            Choice("Break", listOf(5, 10, 15), s.breakMin, { "$it min" }) { focus.configure(s.focusMin, it, s.sessions) }
            Choice("Sessions", listOf(2, 4, 6), s.sessions, { "$it" }) { focus.configure(s.focusMin, s.breakMin, it) }
            Text("Changes apply to the next session.", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
    }
}

/** A labeled M3 Expressive connected button group. */
@Composable
private fun <T> Choice(label: String, options: List<T>, selected: T, text: (T) -> String, onPick: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            options.forEachIndexed { i, o ->
                ToggleButton(
                    checked = o == selected, onCheckedChange = { onPick(o) },
                    shapes = when (i) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                ) { Text(text(o)) }
            }
        }
    }
}
