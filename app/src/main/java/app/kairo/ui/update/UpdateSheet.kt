@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.KairoApplication
import app.kairo.update.Release
import app.kairo.update.UpdateState
import java.util.Locale

/** The release an update state is about, if any. */
val UpdateState.release: Release?
    get() = when (this) {
        is UpdateState.Available -> release
        is UpdateState.Downloading -> release
        is UpdateState.Installing -> release
        is UpdateState.Failed -> release
        else -> null
    }

/**
 * App-wide: when a newer release is found, a drawer offers it (rule 12: drawers, not dialogs).
 * "Later" hides it for that version; Settings → App updates brings it back.
 */
@Composable
fun UpdatePrompt() {
    val updater = (LocalContext.current.applicationContext as KairoApplication).updater
    val state by updater.state.collectAsStateWithLifecycle()
    val dismissed by updater.dismissed.collectAsStateWithLifecycle()
    val release = state.release ?: return
    if (release.version == dismissed) return

    val context = LocalContext.current
    val scope = (context.applicationContext as KairoApplication).appScope // download outlives the sheet
    val haptic = LocalHapticFeedback.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // "Install unknown apps" for Kairo: once allowed in Android's settings, carry straight on.
    val allow = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (updater.canInstall()) updater.downloadAndInstall(scope, release)
    }
    fun later() { updater.dismissed.value = release.version }
    fun update() {
        haptic.performHapticFeedback(HapticFeedbackType.Confirm) // rule 9: save on a sheet
        if (updater.canInstall()) updater.downloadAndInstall(scope, release) else allow.launch(updater.installPermissionIntent())
    }

    ModalBottomSheet(onDismissRequest = ::later, sheetState = sheet) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column {
                Text("Kairo ${release.version} is ready", style = MaterialTheme.typography.headlineSmallEmphasized)
                Text(
                    "You have ${updater.currentVersion}" + if (release.apkSize > 0) " · ${mb(release.apkSize)}" else "",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (release.notes.isNotBlank()) Text(
                release.notes,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
            )
            AnimatedContent(state, contentKey = { it::class }, label = "updateStep") { s ->
                when (s) {
                    is UpdateState.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val p = s.progress
                        if (p != null) LinearWavyProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                        else LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                        Text(
                            if (p != null) "Downloading · ${(p * 100).toInt()}%" else "Downloading…",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is UpdateState.Installing -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LoadingIndicator(Modifier.size(40.dp))
                        Text("Confirm on Android's screen · Kairo restarts when done", style = MaterialTheme.typography.bodyMedium)
                    }
                    else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (s is UpdateState.Failed) Text(s.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(::later, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).height(56.dp)) {
                                Text("Later", style = MaterialTheme.typography.titleMediumEmphasized)
                            }
                            Button(::update, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).height(56.dp)) {
                                Text(if (s is UpdateState.Failed) "Try again" else "Update", style = MaterialTheme.typography.titleMediumEmphasized)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.size(4.dp))
        }
    }
}

fun mb(bytes: Long) = String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
