@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.ui.theme.KairoTheme

/** One thing Kairo uses or borrows from, with who made it and under which license. */
private data class Credit(val name: String, val by: String, val license: String, val url: String, val licenseFile: String?)

private const val APACHE = "licenses/apache-2.0.txt"

private val Credits = listOf(
    Credit("Google Sans Flex", "The Google Sans Flex Authors · Google", "SIL Open Font License 1.1",
        "https://github.com/googlefonts/googlesans-flex", "licenses/ofl-google-sans-flex.txt"),
    Credit("Material Symbols (Rounded)", "Google", "Apache License 2.0",
        "https://github.com/google/material-design-icons", APACHE),
    Credit("Jetpack Compose & Material 3", "The Android Open Source Project", "Apache License 2.0",
        "https://developer.android.com/jetpack/androidx/releases/compose-material3", APACHE),
    Credit("AndroidX: Glance, Room, Navigation 3, Lifecycle, DataStore, Biometric, Graphics Shapes, Core, Activity",
        "The Android Open Source Project", "Apache License 2.0", "https://developer.android.com/jetpack/androidx", APACHE),
    Credit("Kotlin & kotlinx.coroutines", "JetBrains s.r.o. and Kotlin contributors", "Apache License 2.0",
        "https://github.com/JetBrains/kotlin", APACHE),
    Credit("material-color-utilities (SchemeTonalSpot)", "Google", "Apache License 2.0",
        "https://github.com/material-foundation/material-color-utilities", APACHE),
    Credit("Material 3 Expressive design guidance", "Google · m3.material.io", "Used as design reference",
        "https://m3.material.io", null),
    Credit("Stash (palette and floating nav)", "dixith-cxde", "CC BY-NC-SA 4.0",
        "https://github.com/dixith-cxde/stash", null),
)

/** Settings rows: Kairo's own license, then the credits drawer. */
@Composable
fun AboutSection() {
    val k = KairoTheme.colors
    val context = LocalContext.current
    var credits by rememberSaveable { mutableStateOf(false) }
    var reading by rememberSaveable { mutableStateOf<String?>(null) } // asset path of the license being read

    SegmentedListItem(
        onClick = { credits = true },
        shapes = ListItemDefaults.segmentedShapes(0, 2),
        colors = ListItemDefaults.segmentedColors(containerColor = k.item),
        leadingContent = { SettingsRoundIcon(R.drawable.ic_info) },
        supportingContent = { Text("Fonts, icons and libraries Kairo is built with") },
    ) { Text("Credits & licenses") }
    Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
    SegmentedListItem(
        onClick = { reading = "licenses/kairo-cc-by-nc-sa-4.0.txt" },
        shapes = ListItemDefaults.segmentedShapes(1, 2),
        colors = ListItemDefaults.segmentedColors(containerColor = k.item),
        leadingContent = { SettingsRoundIcon(R.drawable.ic_license, tertiary = true) },
        supportingContent = { Text("CC BY-NC-SA 4.0 · © 2026 dixith-cxde") },
    ) { Text("Kairo's license") }

    if (credits) ModalBottomSheet(onDismissRequest = { credits = false }) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Credits & licenses", style = MaterialTheme.typography.headlineSmallEmphasized, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
            Text("Tap one to read its license, or open its page.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp, bottom = 16.dp))
            Credits.forEachIndexed { i, cr ->
                SegmentedListItem(
                    onClick = {
                        if (cr.licenseFile != null) reading = cr.licenseFile
                        else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cr.url)))
                    },
                    shapes = ListItemDefaults.segmentedShapes(i, Credits.size),
                    colors = ListItemDefaults.segmentedColors(containerColor = k.itemHigh),
                    supportingContent = { Text("${cr.by}\n${cr.license}") },
                    trailingContent = if (cr.licenseFile == null) {
                        { Icon(painterResource(R.drawable.ic_open_in_new), "Open page", Modifier.size(20.dp)) }
                    } else null,
                ) { Text(cr.name) }
                if (i < Credits.lastIndex) Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
            }
        }
    }

    reading?.let { path -> LicenseSheet(path) { reading = null } }
}

/** Full license text from assets, monospaced so the legal layout keeps its shape. */
@Composable
private fun LicenseSheet(assetPath: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = remember(assetPath) { runCatching { context.assets.open(assetPath).bufferedReader().use { it.readText() } }.getOrDefault("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
        )
    }
}
