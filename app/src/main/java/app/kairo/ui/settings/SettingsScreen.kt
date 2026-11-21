@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.settings

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.R
import app.kairo.data.settings.Contrast
import app.kairo.data.settings.Seed
import app.kairo.data.settings.SettingsStore
import app.kairo.data.settings.ThemeMode
import app.kairo.ui.shapes.MorphShape
import app.kairo.ui.theme.KairoTheme
import app.kairo.ui.theme.SeedSchemes
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(store: SettingsStore, onBack: () -> Unit) {
    val s by store.settings.collectAsStateWithLifecycle(initialValue = null)
    val settings = s ?: return
    val scope = rememberCoroutineScope()
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val alarms = remember { context.getSystemService(AlarmManager::class.java) }

    Scaffold(
        containerColor = k.bg,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(shapes = IconButtonDefaults.shapes(), onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = k.bg),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 16.dp).padding(bottom = 104.dp),
        ) {
            // Top card: the cat mark, local-only promise.
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(k.item).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFD4D4D4)), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(96.dp))
                }
                Column {
                    Text("Kairo", style = MaterialTheme.typography.titleLargeEmphasized)
                    Text("Stored on this phone · no account", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                }
            }

            Header("Appearance")
            SegmentedListItem(
                onClick = { scope.launch { store.setWallpaper(!settings.wallpaperColor) } },
                shapes = ListItemDefaults.segmentedShapes(0, 3),
                colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                leadingContent = { RoundIcon(R.drawable.ic_wallpaper) },
                supportingContent = { Text("Dynamic color, Android 12+") },
                trailingContent = {
                    Switch(checked = settings.wallpaperColor, onCheckedChange = { scope.launch { store.setWallpaper(it) } })
                },
            ) { Text("Colors from wallpaper") }
            Gap()
            Column(
                Modifier.fillMaxWidth().background(k.item, RoundedCornerShape(4.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (settings.wallpaperColor) "Or pick a seed color (turns wallpaper colors off)"
                    else "Seed color · ${settings.seed.name}",
                    style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Seed.entries.forEach { seed ->
                        SeedSwatch(
                            seed = seed,
                            dark = k.bg.red < 0.5f,
                            selected = !settings.wallpaperColor && settings.seed == seed,
                            dimmed = settings.wallpaperColor,
                        ) { scope.launch { store.setSeed(seed) } }
                    }
                }
            }
            Gap()
            Column(
                Modifier.fillMaxWidth().background(k.item, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Theme", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                Connected(ThemeMode.entries, settings.theme, { it.name }) { scope.launch { store.setTheme(it) } }
                Text("Contrast", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                Connected(Contrast.entries, settings.contrast, { it.name }, enabled = !settings.wallpaperColor) {
                    scope.launch { store.setContrast(it) }
                }
            }

            Header("Privacy")
            val activity = context as androidx.fragment.app.FragmentActivity
            val snackbar = app.kairo.ui.common.LocalAppSnackbar.current
            SegmentedListItem(
                onClick = {},
                shapes = ListItemDefaults.segmentedShapes(0, if (settings.appLock) 3 else 2),
                colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                leadingContent = { RoundIcon(R.drawable.ic_lock) },
                supportingContent = { Text("Fingerprint, face or screen lock — handled by Android") },
                trailingContent = {
                    Switch(checked = settings.appLock, onCheckedChange = { on ->
                        if (!on) { scope.launch { store.setAppLock(false) }; return@Switch }
                        if (!app.kairo.lock.AppLock.canLock(activity)) {
                            scope.launch { snackbar.showSnackbar("Set a screen lock on your phone first") }
                            return@Switch
                        }
                        // Prove it's you before turning it on.
                        app.kairo.lock.AppLock.authenticate(activity, "Turn on app lock") { ok -> if (ok) scope.launch { store.setAppLock(true) } }
                    })
                },
            ) { Text("App lock") }
            androidx.compose.animation.AnimatedVisibility(settings.appLock) {
                Column {
                    Gap()
                    Column(
                        Modifier.fillMaxWidth().background(k.item, RoundedCornerShape(4.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Lock after", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                        Connected(listOf(0, 60, 300), settings.lockAfterSec, { when (it) { 0 -> "Now"; 60 -> "1 min"; else -> "5 min" } }) {
                            scope.launch { store.setLockAfter(it) }
                        }
                        Text(
                            "While locked, reminders hide task names on the lock screen and the Money widget hides amounts.",
                            style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant,
                        )
                    }
                }
            }
            Gap()
            SegmentedListItem(
                onClick = { scope.launch { store.setBlockScreenshots(!settings.blockScreenshots) } },
                shapes = ListItemDefaults.segmentedShapes(if (settings.appLock) 2 else 1, if (settings.appLock) 3 else 2),
                colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                leadingContent = { RoundIcon(R.drawable.ic_visibility_off) },
                supportingContent = { Text("No screenshots, screen recording or recents preview") },
                trailingContent = {
                    Switch(checked = settings.blockScreenshots, onCheckedChange = { scope.launch { store.setBlockScreenshots(it) } })
                },
            ) { Text("Block screenshots") }

            Header("Reminders")
            val exact = alarms.canScheduleExactAlarms()
            SegmentedListItem(
                onClick = {
                    if (!exact) context.startActivity(
                        Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                    )
                },
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                leadingContent = { RoundIcon(R.drawable.ic_alarm) },
                supportingContent = { Text(if (exact) "Allowed · reminders fire on time" else "Not allowed · tap to allow") },
                trailingContent = if (exact) {
                    { Icon(painterResource(R.drawable.ic_task_alt), null, tint = c.tertiary) }
                } else null,
            ) { Text("Exact alarms") }

            Header("Data")
            val kairo = context.applicationContext as app.kairo.KairoApplication
            var importing by remember { mutableStateOf<String?>(null) }
            val pick = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                importing = "Importing…"
                scope.launch {
                    importing = runCatching {
                        val text = context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                        val r = app.kairo.data.money.IvyImport.import(kairo.money, text)
                        "Added ${r.added} · skipped ${r.skipped}" +
                            (if (r.accountsCreated > 0) " · ${r.accountsCreated} new accounts" else "") +
                            (if (r.categoriesCreated > 0) " · ${r.categoriesCreated} new categories" else "")
                    }.getOrElse { "Couldn't import: ${it.message}" }
                }
            }
            SegmentedListItem(
                onClick = { pick.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "application/octet-stream")) },
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                leadingContent = { RoundIcon(R.drawable.ic_account_balance_wallet) },
                supportingContent = { Text(importing ?: "CSV from Ivy Wallet (Settings → Export to CSV). Safe to run twice.") },
            ) { Text("Import from Ivy Wallet") }

            Header("Calendar")
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                colors = ListItemDefaults.segmentedColors(containerColor = k.item),
                leadingContent = { RoundIcon(R.drawable.ic_event_available, tertiary = true) },
                supportingContent = { Text("Read from this phone's calendars; no account. Allow it on the Calendar tab.") },
            ) { Text("Calendar events") }

            Header("About")
            AboutSection()
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable private fun Header(text: String) = Text(
    text, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp),
)

@Composable private fun Gap() = Spacer(Modifier.height(ListItemDefaults.SegmentedGap))

@Composable internal fun SettingsRoundIcon(icon: Int, tertiary: Boolean = false) = RoundIcon(icon, tertiary)

/** Icons in colored circles (Android 16 style). */
@Composable
private fun RoundIcon(icon: Int, tertiary: Boolean = false) {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier.size(40.dp).background(if (tertiary) c.tertiaryContainer else c.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(painterResource(icon), null, tint = if (tertiary) c.onTertiaryContainer else c.onSecondaryContainer, modifier = Modifier.size(20.dp)) }
}

/** M3 Expressive connected button group for a small enum. */
@Composable
private fun <T> Connected(options: List<T>, selected: T, label: (T) -> String, enabled: Boolean = true, onPick: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        options.forEachIndexed { i, o ->
            ToggleButton(
                checked = o == selected,
                onCheckedChange = { onPick(o) },
                enabled = enabled,
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
            ) { Text(label(o), maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
        }
    }
}

private val Circle = RoundedPolygon.circle(numVertices = 12)
    .let { it.transformed { x, y -> androidx.graphics.shapes.TransformResult(x * 0.5f + 0.5f, y * 0.5f + 0.5f) } }

/**
 * Seed swatch: primary / secondary / tertiary quadrants clipped to a shape that morphs
 * circle → Cookie9 when selected (canvas Settings board).
 */
@Composable
private fun SeedSwatch(seed: Seed, dark: Boolean, selected: Boolean, dimmed: Boolean, onClick: () -> Unit) {
    val motion = MaterialTheme.motionScheme
    val progress by animateFloatAsState(if (selected) 1f else 0f, motion.defaultSpatialSpec(), label = "seedMorph")
    val alpha by animateFloatAsState(if (dimmed) 0.5f else 1f, motion.defaultEffectsSpec(), label = "seedAlpha")
    val morph = remember { Morph(Circle, MaterialShapes.Cookie9Sided) }
    val v = SeedSchemes.data.getValue(seed.name)[if (dark) 1 else 0][0]
    val idx = SeedSchemes.ROLES
    val p = Color(v[idx.indexOf("primary")]); val sc = Color(v[idx.indexOf("secondary")]); val t = Color(v[idx.indexOf("tertiaryContainer")])
    Box(
        Modifier
            .size(44.dp)
            .clip(MorphShape(morph, progress))
            .clickable(role = Role.RadioButton, onClick = onClick) // no haptic: rule 9
            .semantics { contentDescription = "${seed.name} seed color"; this.selected = selected },
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.weight(1f)) {
                Box(Modifier.weight(1f).fillMaxSize().background(p.copy(alpha = alpha)))
                Box(Modifier.weight(1f).fillMaxSize().background(sc.copy(alpha = alpha)))
            }
            Box(Modifier.weight(1f).fillMaxWidth().background(t.copy(alpha = alpha)))
        }
        if (selected) Box(
            Modifier.align(Alignment.Center).size(24.dp).background(Color.Black.copy(alpha = 0.38f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(painterResource(R.drawable.ic_check), null, tint = Color.White, modifier = Modifier.size(18.dp)) }
    }
}
