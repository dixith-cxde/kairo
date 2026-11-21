@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.widgets

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.provider.CalendarContract
import android.text.format.DateFormat
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ButtonDefaults
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import app.kairo.KairoApplication
import app.kairo.MainActivity
import app.kairo.R
import app.kairo.data.db.TxKind
import app.kairo.data.model.Task
import app.kairo.data.money.Money
import app.kairo.data.settings.ThemeMode
import app.kairo.ui.theme.KairoDarkColorTokens
import app.kairo.ui.theme.KairoLightColorTokens
import app.kairo.ui.theme.kairoScheme
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.glance.color.ColorProvider as DayNight

/*
 * Kairo's home-screen widgets (canvas "Widgets" board, docs/05-widgets.md), built with Jetpack Glance.
 *
 * - Colors: the app's own scheme (seed or wallpaper, same as in the app) through GlanceTheme;
 *   containers are Kairo's neutral grays (rule 3). One solid primary action per widget at most.
 * - Shapes: the real MaterialShapes polygons, drawn to a white bitmap and tinted by Glance, so the
 *   Progress cookie and Streak sunny are the library shapes, not approximations.
 * - Glance can't animate, so the app's springs and the slow cookie spin don't exist here.
 */

// ───────── shared ─────────

/** Theme for one render: GlanceTheme colors + Kairo's item / itemHigh grays, honoring the app's theme setting. */
private class WTheme(val colors: ColorProviders, val item: ColorProvider, val itemHigh: ColorProvider)

private suspend fun widgetTheme(context: Context): WTheme {
    val s = (context.applicationContext as KairoApplication).settings.settings.first()
    val light = kairoScheme(context, s, false)
    val dark = kairoScheme(context, s, true)
    val l = KairoLightColorTokens
    val d = KairoDarkColorTokens
    return when (s.theme) {
        ThemeMode.Dark -> WTheme(androidx.glance.material3.ColorProviders(dark, dark), ColorProvider(d.item), ColorProvider(d.itemHigh))
        ThemeMode.Light -> WTheme(androidx.glance.material3.ColorProviders(light, light), ColorProvider(l.item), ColorProvider(l.itemHigh))
        ThemeMode.System -> WTheme(
            androidx.glance.material3.ColorProviders(light, dark),
            DayNight(day = l.item, night = d.item), DayNight(day = l.itemHigh, night = d.itemHigh),
        )
    }
}

private fun openApp(context: Context, action: String? = null): Action =
    actionStartActivity(Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        action?.let { putExtra(MainActivity.EXTRA_ACTION, it) }
    })

private fun openTask(context: Context, id: Long) = openApp(context, MainActivity.taskAction(id))

/** Content widgets: a gray rectangle with the system widget radius (never a custom one). */
private fun GlanceModifier.card(t: WTheme) = fillMaxSize().appWidgetBackground().background(t.item)
    .cornerRadius(android.R.dimen.system_app_widget_background_radius)

private fun tint(c: ColorProvider) = ColorFilter.tint(c)

/** A MaterialShapes polygon as a white bitmap (tinted by Glance), fitted into a px × px square. */
private fun shapeBitmap(poly: RoundedPolygon, px: Int): Bitmap {
    val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val path = poly.toPath(Path())
    val bounds = RectF().also { path.computeBounds(it, true) }
    path.transform(Matrix().apply { setRectToRect(bounds, RectF(0f, 0f, px.toFloat(), px.toFloat()), Matrix.ScaleToFit.CENTER) })
    Canvas(b).drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE })
    return b
}

/**
 * The Progress ring, as on the canvas (radius 52, stroke 8 in a 171 box): a plain track circle,
 * or the wavy progress arc (r = 52 + 2·sin 10t) from 12 o'clock. White, tinted by Glance.
 */
private fun ringBitmap(px: Int, progress: Float, track: Boolean): Bitmap {
    val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val k = px / 171f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 8 * k; strokeCap = Paint.Cap.ROUND
        color = if (track) android.graphics.Color.argb(41, 255, 255, 255) else android.graphics.Color.WHITE // 16% track
    }
    val c = Canvas(b)
    val mid = px / 2f
    if (track) c.drawCircle(mid, mid, 52 * k, paint)
    else if (progress > 0f) {
        val path = Path()
        val steps = (360 * progress).toInt().coerceAtLeast(2)
        for (i in 0..steps) {
            val t = i / 360.0 * 2 * PI
            val r = (52 + 2 * sin(10 * t)) * k
            val x = (mid + r * cos(t - PI / 2)).toFloat()
            val y = (mid + r * sin(t - PI / 2)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        c.drawPath(path, paint)
    }
    return b
}

private fun px(context: Context, dp: Dp) = (dp.value * context.resources.displayMetrics.density).toInt().coerceIn(48, 720)

/** A library shape with an icon centered on it (the canvas's `.wsh`). */
@Composable
private fun ShapeIcon(shape: Bitmap, fill: ColorProvider, icon: Int, iconColor: ColorProvider, size: Dp, iconSize: Dp) {
    Box(GlanceModifier.size(size), contentAlignment = Alignment.Center) {
        Image(ImageProvider(shape), null, GlanceModifier.fillMaxSize(), colorFilter = tint(fill))
        Image(ImageProvider(icon), null, GlanceModifier.size(iconSize), colorFilter = tint(iconColor))
    }
}

private fun timeText(context: Context, t: LocalTime, ampm: Boolean): String =
    t.format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "H:mm" else if (ampm) "h:mm a" else "h:mm", Locale.getDefault()))

/** Refresh every Kairo widget (called when tasks, money or settings change). */
suspend fun updateAllWidgets(context: Context) {
    TodayWidget().updateAll(context)
    ProgressWidget().updateAll(context)
    MoneyWidget().updateAll(context)
    QuickAddWidget().updateAll(context)
    NextUpWidget().updateAll(context)
    StreakWidget().updateAll(context)
    StatsWidget().updateAll(context)
}

/** Each widget loads its data once, then renders it for the home screen and for the picker preview. */
private abstract class KairoWidget<D> : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    abstract suspend fun load(context: Context): D
    @Composable abstract fun Content(context: Context, d: D, t: WTheme)

    override suspend fun provideGlance(context: Context, id: GlanceId) = render(context)
    override suspend fun providePreview(context: Context, widgetCategory: Int) = render(context)

    private suspend fun render(context: Context) {
        val t = widgetTheme(context)
        val d = load(context)
        provideContent { GlanceTheme(colors = t.colors) { Content(context, d, t) } }
    }
}

// ───────── Today · list with working checkboxes (4×2; 2×2 → 4×4) ─────────

private class TodayData(val open: List<Task>, val done: Int, val total: Int, val cookie: Bitmap)

private class TodayWidget : KairoWidget<TodayData>() {
    override suspend fun load(context: Context): TodayData {
        val app = context.applicationContext as KairoApplication
        val today = LocalDate.now()
        val all = app.repository.tasks.first().filter { it.due != null && !it.due.isAfter(today) && !it.archived }
        val open = all.filter { !it.done }.sortedWith(compareBy<Task>({ it.due }, { it.time ?: LocalTime.MAX }))
        val done = all.count { it.done && it.due == today }
        return TodayData(open, done, done + open.size, shapeBitmap(MaterialShapes.Cookie4Sided, px(context, 40.dp)))
    }

    @Composable
    override fun Content(context: Context, d: TodayData, t: WTheme) {
        val size = LocalSize.current
        val small = size.width < 220.dp
        // Extra height adds rows (2 at 2×2, 3 at 4×2, up to 7 at 4×4), never empty space.
        val rows = ((size.height.value - 24 - 48 - 8 + 2) / 38).toInt().coerceIn(1, 7)
        val c = GlanceTheme.colors
        Column(GlanceModifier.card(t).padding(12.dp)) {
            Row(GlanceModifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                ShapeIcon(d.cookie, c.primaryContainer, R.drawable.ic_today, c.onPrimaryContainer, if (small) 36.dp else 40.dp, if (small) 18.dp else 20.dp)
                Spacer(GlanceModifier.width(10.dp))
                Column(GlanceModifier.defaultWeight().clickable(openApp(context))) {
                    if (small) Text("${d.open.size} left", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = c.onSurface), maxLines = 1)
                    else {
                        Text("Today", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = c.onSurface))
                        Text("${d.done} of ${d.total} done · ${d.open.size} left", style = TextStyle(fontSize = 12.sp, color = c.onSurfaceVariant), maxLines = 1)
                    }
                }
                // The one solid primary action (rule 4: solid primary, bold plus).
                CircleIconButton(
                    imageProvider = ImageProvider(R.drawable.ic_add_bold), contentDescription = "Add task",
                    onClick = openApp(context, MainActivity.ACTION_QUICK_ADD),
                    modifier = GlanceModifier.size(if (small) 40.dp else 48.dp),
                    backgroundColor = c.primary, contentColor = c.onPrimary,
                )
            }
            Spacer(GlanceModifier.height(8.dp))
            if (d.open.isEmpty()) Text("All done for today", style = TextStyle(fontSize = 14.sp, color = c.onSurfaceVariant), modifier = GlanceModifier.padding(start = 6.dp))
            val shown = d.open.take(rows)
            shown.forEachIndexed { i, task ->
                val seg = when {
                    shown.size == 1 -> R.drawable.wg_seg_single
                    i == 0 -> R.drawable.wg_seg_top
                    i == shown.lastIndex -> R.drawable.wg_seg_bottom
                    else -> R.drawable.wg_seg_mid
                }
                Row(
                    GlanceModifier.fillMaxWidth().height(36.dp)
                        .background(ImageProvider(seg), ContentScale.FillBounds, tint(t.itemHigh))
                        .padding(end = 12.dp).clickable(openTask(context, task.id)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CheckBox(
                        checked = false,
                        onCheckedChange = actionRunCallback<CompleteTask>(actionParametersOf(TaskKey to task.id)),
                        text = "",
                        colors = CheckboxDefaults.colors(checkedColor = c.primary, uncheckedColor = c.onSurfaceVariant),
                    )
                    Text(task.title, style = TextStyle(fontSize = 14.sp, color = c.onSurface), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                    if (!small && task.time != null) Text(timeText(context, task.time, ampm = false), style = TextStyle(fontSize = 12.sp, color = c.onSurfaceVariant))
                }
                if (i < shown.lastIndex) Spacer(GlanceModifier.height(2.dp))
            }
        }
    }
}

private val TaskKey = ActionParameters.Key<Long>("taskId")

class CompleteTask : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = context.applicationContext as KairoApplication
        val id = parameters[TaskKey] ?: return
        app.repository.task(id).first()?.let { app.repository.setDone(it, true) }
        updateAllWidgets(context)
    }
}

// ───────── Progress · shape widget (2×2; 1×1) ─────────

private class ProgressData(val done: Int, val total: Int)

private class ProgressWidget : KairoWidget<ProgressData>() {
    override suspend fun load(context: Context): ProgressData {
        val t = (context.applicationContext as KairoApplication).repository.tasks.first().filter { it.due == LocalDate.now() && !it.archived }
        return ProgressData(t.count { it.done }, t.size)
    }

    @Composable
    override fun Content(context: Context, d: ProgressData, t: WTheme) {
        val c = GlanceTheme.colors
        val size = LocalSize.current
        val side = minOf(size.width, size.height)       // fills the cell on one axis
        val p = px(context, side)
        val progress = if (d.total == 0) 0f else d.done / d.total.toFloat()
        val left = d.total - d.done
        val k = side.value / 171f
        Box(GlanceModifier.fillMaxSize().clickable(openApp(context)), contentAlignment = Alignment.Center) {
            Box(GlanceModifier.size(side), contentAlignment = Alignment.Center) {
                Image(ImageProvider(shapeBitmap(MaterialShapes.Cookie9Sided, p)), null, GlanceModifier.fillMaxSize(), colorFilter = tint(c.primaryContainer))
                Image(ImageProvider(ringBitmap(p, progress, track = true)), null, GlanceModifier.fillMaxSize(), colorFilter = tint(c.onPrimaryContainer))
                Image(ImageProvider(ringBitmap(p, progress, track = false)), "${d.done} of ${d.total} tasks done", GlanceModifier.fillMaxSize(), colorFilter = tint(c.primary))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (d.total > 0 && left == 0) "✓" else "$left",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = (32 * k).coerceAtLeast(16f).sp, color = c.onPrimaryContainer))
                    if (side >= 90.dp) Text(if (d.total > 0 && left == 0) "all done" else "left today",
                        style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, color = c.onPrimaryContainer))
                }
            }
        }
    }
}

// ───────── Money · text + action (2×2; 4×2) ─────────

/** [hidden]: app lock is on, so amounts stay off the home screen. */
private class MoneyData(val spent: Long, val limit: Long?, val month: String, val hidden: Boolean)

private class MoneyWidget : KairoWidget<MoneyData>() {
    override suspend fun load(context: Context): MoneyData {
        val app = context.applicationContext as KairoApplication
        val ym = YearMonth.now()
        val spent = app.money.transactions.first().filter { it.kind == TxKind.Expense && YearMonth.from(it.date) == ym }.sumOf { it.amountPaise }
        val limit = app.money.budgets.first().firstOrNull { it.overall }?.limitPaise
        val hidden = app.settings.settings.first().appLock
        return MoneyData(spent, limit, ym.month.getDisplayName(JTextStyle.SHORT, Locale.getDefault()), hidden)
    }

    @Composable
    override fun Content(context: Context, d: MoneyData, t: WTheme) {
        val c = GlanceTheme.colors
        val width = LocalSize.current.width
        Column(GlanceModifier.card(t).padding(12.dp)) {
            Row(GlanceModifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_account_balance_wallet), null, GlanceModifier.size(18.dp), colorFilter = tint(c.onSurfaceVariant))
                Spacer(GlanceModifier.width(4.dp))
                Text("Spent · ${d.month}", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = c.onSurfaceVariant))
            }
            Spacer(GlanceModifier.height(6.dp))
            Text(if (d.hidden) "₹ •••••" else Money.format(d.spent), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.onSurface), maxLines = 1,
                modifier = GlanceModifier.padding(start = 4.dp))
            if (d.hidden) {
                Text("Hidden · app lock is on", style = TextStyle(fontSize = 11.sp, color = c.onSurfaceVariant), modifier = GlanceModifier.padding(start = 4.dp))
            } else if (d.limit != null && d.limit > 0) {
                val frac = (d.spent.toFloat() / d.limit).coerceIn(0f, 1f)
                val over = d.spent > d.limit
                // Two-part bar like the canvas: used (primary, error when over) · 3dp gap · rest (secondary container).
                val barW = width - 24.dp - 8.dp
                val used = if (over) barW else ((barW.value - 3) * frac).coerceAtLeast(8f).dp
                Spacer(GlanceModifier.height(6.dp))
                Row(GlanceModifier.padding(horizontal = 4.dp).height(8.dp)) {
                    Box(GlanceModifier.width(used).height(8.dp).background(if (over) c.error else c.primary).cornerRadius(4.dp)) {}
                    if (!over && frac < 1f) {
                        Spacer(GlanceModifier.width(3.dp))
                        Box(GlanceModifier.defaultWeight().height(8.dp).background(c.secondaryContainer).cornerRadius(4.dp)) {}
                    }
                }
                Spacer(GlanceModifier.height(6.dp))
                Text("${(d.spent * 100 / d.limit)}% of ${Money.format(d.limit)}", style = TextStyle(fontSize = 11.sp, color = if (over) c.error else c.onSurfaceVariant),
                    maxLines = 1, modifier = GlanceModifier.padding(start = 4.dp))
            } else {
                Text("No monthly budget", style = TextStyle(fontSize = 11.sp, color = c.onSurfaceVariant), modifier = GlanceModifier.padding(start = 4.dp))
            }
            Spacer(GlanceModifier.defaultWeight())
            FilledButton(
                text = "Expense", onClick = openApp(context, MainActivity.ACTION_ADD_EXPENSE),
                icon = ImageProvider(R.drawable.ic_add_bold),
                colors = ButtonDefaults.buttonColors(backgroundColor = c.primary, contentColor = c.onPrimary),
                modifier = GlanceModifier.fillMaxWidth().height(48.dp),
            )
        }
    }
}

// ───────── Quick add · toolbar (4×1; 2×1) ─────────

private class QuickAddWidget : KairoWidget<Unit>() {
    override suspend fun load(context: Context) = Unit

    @Composable
    override fun Content(context: Context, d: Unit, t: WTheme) {
        val c = GlanceTheme.colors
        val wide = LocalSize.current.width >= 220.dp
        Row(GlanceModifier.card(t).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Search-style field: tap to type, mic to speak.
            Row(
                GlanceModifier.defaultWeight().height(48.dp).background(t.itemHigh).cornerRadius(24.dp)
                    .clickable(openApp(context, MainActivity.ACTION_QUICK_ADD)).padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(ImageProvider(R.drawable.ic_add_task), null, GlanceModifier.size(24.dp), colorFilter = tint(c.primary))
                Spacer(GlanceModifier.width(10.dp))
                Text("Add a task", style = TextStyle(fontSize = 16.sp, color = c.onSurfaceVariant), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                Box(GlanceModifier.size(48.dp).clickable(openApp(context, MainActivity.ACTION_VOICE_ADD)), contentAlignment = Alignment.Center) {
                    Image(ImageProvider(R.drawable.ic_mic), "Speak a task", GlanceModifier.size(24.dp), colorFilter = tint(c.onSurfaceVariant))
                }
            }
            if (wide) {
                // The rest are tonal (only one solid primary per widget, and here it's none).
                Spacer(GlanceModifier.width(8.dp))
                CircleIconButton(ImageProvider(R.drawable.ic_currency_rupee), "Add expense", openApp(context, MainActivity.ACTION_ADD_EXPENSE),
                    modifier = GlanceModifier.size(48.dp), backgroundColor = c.secondaryContainer, contentColor = c.onSecondaryContainer)
                Spacer(GlanceModifier.width(8.dp))
                CircleIconButton(ImageProvider(R.drawable.ic_timer), "Start focus", openApp(context, MainActivity.ACTION_FOCUS),
                    modifier = GlanceModifier.size(48.dp), backgroundColor = c.secondaryContainer, contentColor = c.onSecondaryContainer)
            }
        }
    }
}

// ───────── Next up · text (2×1; 4×1) ─────────

/** The next thing today: a timed task or a calendar event (whichever comes first), else the first untimed task. */
private class NextData(val time: String?, val title: String?, val detail: String?, val action: Action?, val sunny: Bitmap)

private class NextUpWidget : KairoWidget<NextData>() {
    override suspend fun load(context: Context): NextData {
        val app = context.applicationContext as KairoApplication
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val sunny = shapeBitmap(MaterialShapes.Sunny, px(context, 52.dp))
        val open = app.repository.tasks.first().filter { !it.done && !it.archived && it.due == today }
        val task = open.filter { it.time != null && !it.time.isBefore(now.toLocalTime().withSecond(0).withNano(0)) }.minByOrNull { it.time!! }
        val event = if (app.phoneCalendar.hasPermission())
            runCatching { app.phoneCalendar.events(today, today.plusDays(1)) }.getOrDefault(emptyList())
                .filter { !it.allDay && it.end.isAfter(now) }.minByOrNull { it.start }
        else null
        val taskAt = task?.let { LocalDateTime.of(today, it.time) }
        return when {
            event != null && (taskAt == null || event.start.isBefore(taskAt)) -> NextData(
                if (event.start.isBefore(now)) "Now" else timeText(context, event.start.toLocalTime(), ampm = true),
                event.title.ifBlank { "Event" }, event.calendar,
                actionStartActivity(Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)),
                sunny,
            )
            task != null -> NextData(timeText(context, task.time!!, ampm = true), task.title, inText(now, taskAt!!), openTask(context, task.id), sunny)
            open.isNotEmpty() -> open.first().let { NextData("Today", it.title, "${open.size} open today", openTask(context, it.id), sunny) }
            else -> NextData(null, null, null, null, sunny)
        }
    }

    private fun inText(now: LocalDateTime, at: LocalDateTime): String {
        val min = java.time.Duration.between(now, at).toMinutes()
        return when { min < 1 -> "now"; min < 60 -> "in $min min"; else -> "in ${min / 60} h ${if (min % 60 > 0) "${min % 60} min" else ""}".trim() }
    }

    @Composable
    override fun Content(context: Context, d: NextData, t: WTheme) {
        val c = GlanceTheme.colors
        val wide = LocalSize.current.width >= 220.dp
        Row(
            GlanceModifier.card(t).padding(start = 10.dp, end = 12.dp).clickable(d.action ?: openApp(context)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShapeIcon(d.sunny, c.tertiary, R.drawable.ic_bolt, c.onTertiary, 52.dp, 22.dp)
            Spacer(GlanceModifier.width(10.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(d.time ?: "Next up", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = c.tertiary), maxLines = 1)
                Text(d.title ?: "Nothing else today", style = TextStyle(fontSize = 14.sp, color = c.onSurface), maxLines = 1)
                if (wide && d.detail != null) Text(d.detail, style = TextStyle(fontSize = 12.sp, color = c.onSurfaceVariant), maxLines = 1)
            }
        }
    }
}

// ───────── Streak · shape widget (1×1) ─────────

/** Days in a row with at least one task finished (today counts once something's done; same rule as Stats). */
private class StreakWidget : KairoWidget<Int>() {
    override suspend fun load(context: Context): Int {
        val zone = ZoneId.systemDefault()
        val days = (context.applicationContext as KairoApplication).repository.tasks.first()
            .mapNotNull { t -> t.completedAt?.takeIf { t.done }?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } }.toSet()
        val today = LocalDate.now()
        var d = if (today in days) today else today.minusDays(1)
        var n = 0
        while (d in days) { n++; d = d.minusDays(1) }
        return n
    }

    @Composable
    override fun Content(context: Context, d: Int, t: WTheme) {
        val c = GlanceTheme.colors
        val size = LocalSize.current
        val side = minOf(size.width, size.height)
        Box(GlanceModifier.fillMaxSize().clickable(openApp(context)), contentAlignment = Alignment.Center) {
            Box(GlanceModifier.size(side), contentAlignment = Alignment.Center) {
                Image(ImageProvider(shapeBitmap(MaterialShapes.Sunny, px(context, side))), "$d day streak", GlanceModifier.fillMaxSize(),
                    colorFilter = tint(c.tertiaryContainer))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(ImageProvider(R.drawable.ic_local_fire_department), null, GlanceModifier.size(20.dp), colorFilter = tint(c.onTertiaryContainer))
                    Text("$d", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = c.onTertiaryContainer))
                }
            }
        }
    }
}

// ───────── Stats · week at a glance (4×2; 2×2 → 4×4) ─────────

/** Same rules as the Stats tab (Week): tasks finished per day, change vs the 7 days before, streak, on time. */
private class StatsData(
    val total: Int, val change: Int?, val days: List<Pair<String, Int>>, val streak: Int, val onTime: Int?, val badge: Bitmap,
)

private class StatsWidget : KairoWidget<StatsData>() {
    override suspend fun load(context: Context): StatsData {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val start = today.minusDays(6)
        val done = (context.applicationContext as KairoApplication).repository.tasks.first()
            .filter { it.done && it.completedAt != null }
            .map { it to Instant.ofEpochMilli(it.completedAt!!).atZone(zone).toLocalDate() }
        val week = done.filter { !it.second.isBefore(start) && !it.second.isAfter(today) }
        val prev = done.count { !it.second.isBefore(start.minusDays(7)) && it.second.isBefore(start) }
        val days = (0..6).map { i -> start.plusDays(i.toLong()).let { d ->
            d.dayOfWeek.getDisplayName(JTextStyle.NARROW, Locale.getDefault()) to week.count { it.second == d } } }
        val doneDays = done.map { it.second }.toSet()
        var d = if (today in doneDays) today else today.minusDays(1)
        var streak = 0
        while (d in doneDays) { streak++; d = d.minusDays(1) }
        val dated = week.filter { it.first.due != null }
        val onTime = if (dated.isEmpty()) null else dated.count { !it.second.isAfter(it.first.due) } * 100 / dated.size
        return StatsData(
            week.size, if (prev == 0) null else (week.size - prev) * 100 / prev, days, streak, onTime,
            shapeBitmap(MaterialShapes.Cookie9Sided, px(context, 40.dp)), // the Stats badge's Week shape
        )
    }

    @Composable
    override fun Content(context: Context, d: StatsData, t: WTheme) {
        val c = GlanceTheme.colors
        val size = LocalSize.current
        val wide = size.width >= 220.dp
        val tall = size.height >= 150.dp
        Column(GlanceModifier.card(t).padding(12.dp).clickable(openApp(context, MainActivity.ACTION_STATS))) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ShapeIcon(d.badge, c.primaryContainer, R.drawable.ic_bar_chart, c.onPrimaryContainer, 40.dp, 20.dp)
                Spacer(GlanceModifier.width(10.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${d.total}", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = c.onSurface), maxLines = 1)
                        Spacer(GlanceModifier.width(6.dp))
                        Text("done", style = TextStyle(fontSize = 14.sp, color = c.onSurfaceVariant), maxLines = 1)
                    }
                    Text(
                        when { d.change == null -> "last 7 days"; d.change >= 0 -> "+${d.change}% vs last week"; else -> "${d.change}% vs last week" },
                        style = TextStyle(fontSize = 12.sp, color = if ((d.change ?: 0) < 0) c.error else c.onSurfaceVariant), maxLines = 1,
                    )
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            if (wide) {
                // Seven bars, oldest to today; today's in primary, the rest in primary container.
                val max = (d.days.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
                val barArea = (size.height.value - 24 - 40 - 8 - 16 - (if (tall) 28 else 0)).coerceAtLeast(16f)
                Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.Bottom) {
                    d.days.forEachIndexed { i, (label, n) ->
                        Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.Bottom) {
                            val h = if (n == 0) 4f else (barArea * n / max).coerceAtLeast(8f)
                            Box(
                                GlanceModifier.width(14.dp).height(h.dp).cornerRadius(7.dp)
                                    .background(if (n == 0) t.itemHigh else if (i == 6) c.primary else c.primaryContainer),
                            ) {}
                            Text(label, style = TextStyle(fontSize = 11.sp, fontWeight = if (i == 6) FontWeight.Bold else FontWeight.Normal,
                                color = if (i == 6) c.primary else c.onSurfaceVariant))
                        }
                    }
                }
            } else Spacer(GlanceModifier.defaultWeight())
            if (tall || !wide) {
                Spacer(GlanceModifier.height(6.dp))
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(ImageProvider(R.drawable.ic_local_fire_department), null, GlanceModifier.size(16.dp), colorFilter = tint(c.tertiary))
                    Spacer(GlanceModifier.width(4.dp))
                    Text("${d.streak} day streak", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = c.onSurface), maxLines = 1)
                    if (d.onTime != null && wide) {
                        Spacer(GlanceModifier.defaultWeight())
                        Text("${d.onTime}% on time", style = TextStyle(fontSize = 12.sp, color = c.onSurfaceVariant), maxLines = 1)
                    }
                }
            }
        }
    }
}

// ───────── receivers ─────────

class TodayWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = TodayWidget() }
class ProgressWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = ProgressWidget() }
class MoneyWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = MoneyWidget() }
class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = QuickAddWidget() }
class NextUpWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = NextUpWidget() }
class StreakWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = StreakWidget() }
class StatsWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = StatsWidget() }

/** Picker previews from real content (Android 15+). Rate-limited by the system, so once per app version. */
suspend fun publishWidgetPreviews(context: Context) {
    if (android.os.Build.VERSION.SDK_INT < 35) return
    val prefs = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)
    val version = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    if (prefs.getLong("previews", -1) == version) return
    val m = androidx.glance.appwidget.GlanceAppWidgetManager(context)
    val ok = listOf(
        TodayWidgetReceiver::class, ProgressWidgetReceiver::class, MoneyWidgetReceiver::class,
        QuickAddWidgetReceiver::class, NextUpWidgetReceiver::class, StreakWidgetReceiver::class, StatsWidgetReceiver::class,
    ).all { runCatching { m.setWidgetPreviews(it) }.getOrNull() == androidx.glance.appwidget.GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS }
    if (ok) prefs.edit().putLong("previews", version).apply()
}
