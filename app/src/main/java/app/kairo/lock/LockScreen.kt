package app.kairo.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.ui.common.slowSpin
import app.kairo.ui.theme.KairoTheme

/** Covers the whole app while locked. The system prompt opens on its own; the button re-opens it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LockScreen(locked: Boolean, onUnlock: () -> Unit) {
    val motion = MaterialTheme.motionScheme
    AnimatedVisibility(
        visible = locked,
        enter = fadeIn(motion.fastEffectsSpec()),
        exit = fadeOut(motion.defaultEffectsSpec()) + scaleOut(motion.defaultSpatialSpec(), targetScale = 1.05f),
    ) {
        val c = MaterialTheme.colorScheme
        val spin by slowSpin(30_000)
        Box(
            Modifier.fillMaxSize().background(KairoTheme.colors.bg)
                // swallow touches so nothing underneath can be used
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .systemBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)) {
                Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(112.dp).graphicsLayer { rotationZ = spin }.background(c.primaryContainer, MaterialShapes.Cookie9Sided.toShape()))
                    Icon(painterResource(R.drawable.ic_lock), null, tint = c.onPrimaryContainer, modifier = Modifier.size(44.dp))
                }
                Text("Kairo is locked", style = MaterialTheme.typography.headlineMediumEmphasized)
                Text("Use your fingerprint, face or screen lock to open it.",
                    style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
                Button(onClick = onUnlock, shapes = ButtonDefaults.shapes(), contentPadding = PaddingValues(horizontal = 32.dp),
                    modifier = Modifier.padding(top = 8.dp).height(56.dp)) { Text("Unlock", style = MaterialTheme.typography.titleMediumEmphasized) }
            }
        }
    }
}
