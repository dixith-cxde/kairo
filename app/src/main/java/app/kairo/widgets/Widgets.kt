package app.kairo.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.kairo.KairoApplication
import app.kairo.MainActivity
import app.kairo.R
import app.kairo.data.db.TxKind
import app.kairo.data.model.Task
import app.kairo.data.money.Money
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth

// Kairo's gray containers (rule 3), day/night. Accents come from GlanceTheme (dynamic color).
private val ItemBg = ColorProvider(day = Color(0xFFEEEEEE), night = Color(0xFF0E0E0E))
private val RowBg = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1F1F1F))

private fun openApp(context: Context, action: String? = null) =
    actionStartActivity(Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        action?.let { putExtra(MainActivity.EXTRA_ACTION, it) }
    })

/** Refresh every Kairo widget (called when tasks or money change). */
suspend fun updateAllWidgets(context: Context) {
    TodayWidget().updateAll(context)
    ProgressWidget().updateAll(context)
    MoneyWidget().updateAll(context)
}

// ───────── Today: list with working checkboxes ─────────

class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KairoApplication
        val today = LocalDate.now()
        val all = app.repository.tasks.first().filter { it.due != null && !it.due.isAfter(today) }
        val open = all.filter { !it.done }.sortedWith(compareBy(nullsLast()) { it.time })
        val done = all.count { it.done && it.due == today }
        provideContent { GlanceTheme { TodayContent(context, open, done, done + open.count { it.due == today }) } }
    }

    @Composable
    private fun TodayContent(context: Context, open: List<Task>, done: Int, total: Int) {
        val rows = when { LocalSize.current.height < 140.dp -> 2; LocalSize.current.height < 260.dp -> 3; else -> 7 }
        Column(GlanceModifier.fillMaxSize().background(ItemBg).cornerRadius(28.dp).padding(12.dp)) {
            Row(GlanceModifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight().clickable(openApp(context))) {
                    Text("Today", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GlanceTheme.colors.onSurface))
                    Text("$done of $total done", style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant))
                }
                // The one solid primary action.
                CircleIconButton(
                    imageProvider = ImageProvider(R.drawable.ic_add_bold),
                    contentDescription = "Add task",
                    onClick = openApp(context, MainActivity.ACTION_QUICK_ADD),
                    backgroundColor = GlanceTheme.colors.primary,
                    contentColor = GlanceTheme.colors.onPrimary,
                )
            }
            if (open.isEmpty()) Text("All done for today", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant), modifier = GlanceModifier.padding(8.dp))
            open.take(rows).forEach { t ->
                Row(
                    GlanceModifier.fillMaxWidth().background(RowBg).cornerRadius(12.dp).padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CheckBox(
                        checked = false,
                        onCheckedChange = actionRunCallback<CompleteTask>(actionParametersOf(TaskKey to t.id)),
                        text = t.title,
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
                        maxLines = 1,
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
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

// ───────── Progress ─────────

class ProgressWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KairoApplication
        val today = LocalDate.now()
        val t = app.repository.tasks.first().filter { it.due == today }
        val done = t.count { it.done }
        provideContent {
            GlanceTheme {
                Column(
                    GlanceModifier.fillMaxSize().background(GlanceTheme.colors.primaryContainer).cornerRadius(28.dp)
                        .padding(16.dp).clickable(openApp(context)),
                    verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("$done/${t.size}", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, color = GlanceTheme.colors.onPrimaryContainer))
                    Text(if (t.size - done == 0) "All done" else "${t.size - done} left today",
                        style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onPrimaryContainer))
                    Spacer(GlanceModifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = if (t.isEmpty()) 0f else done / t.size.toFloat(),
                        modifier = GlanceModifier.fillMaxWidth(),
                        color = GlanceTheme.colors.primary, backgroundColor = GlanceTheme.colors.surface,
                    )
                }
            }
        }
    }
}

// ───────── Money ─────────

class MoneyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KairoApplication
        val ym = YearMonth.now()
        val spent = app.money.transactions.first().filter { it.kind == TxKind.Expense && YearMonth.from(it.date) == ym }.sumOf { it.amountPaise }
        val limit = app.money.budgets.first().firstOrNull { it.categoryId == null }?.limitPaise
        provideContent {
            GlanceTheme {
                Column(GlanceModifier.fillMaxSize().background(ItemBg).cornerRadius(28.dp).padding(16.dp)) {
                    Text("Spent · ${ym.month.name.take(3).lowercase().replaceFirstChar(Char::uppercase)}",
                        style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant))
                    Text(Money.format(spent), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = GlanceTheme.colors.onSurface), maxLines = 1)
                    if (limit != null) {
                        Spacer(GlanceModifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = (spent.toFloat() / limit).coerceIn(0f, 1f), modifier = GlanceModifier.fillMaxWidth(),
                            color = if (spent > limit) GlanceTheme.colors.error else GlanceTheme.colors.primary,
                            backgroundColor = GlanceTheme.colors.surfaceVariant,
                        )
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    CircleIconButton(
                        imageProvider = ImageProvider(R.drawable.ic_add_bold), contentDescription = "Add expense",
                        onClick = openApp(context, MainActivity.ACTION_ADD_EXPENSE),
                        backgroundColor = GlanceTheme.colors.primary, contentColor = GlanceTheme.colors.onPrimary,
                    )
                }
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = TodayWidget() }
class ProgressWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = ProgressWidget() }
class MoneyWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = MoneyWidget() }
