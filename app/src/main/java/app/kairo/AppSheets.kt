@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo

import app.kairo.data.db.TxKind
import app.kairo.data.model.TaskList
import app.kairo.navigation.Route
import app.kairo.navigation.Tab
import app.kairo.ui.calendar.CalendarFocus
import app.kairo.ui.calendar.NewEventSheet
import app.kairo.ui.common.LocalAppSnackbar
import app.kairo.ui.finance.IvyExport
import app.kairo.ui.finance.IvyImportSheet
import app.kairo.ui.finance.TxSheet
import app.kairo.ui.finance.NewAccountSheet
import app.kairo.ui.lists.ListSheet
import app.kairo.ui.lists.SmartList
import app.kairo.ui.money.AddTransactionSheet
import app.kairo.ui.quickadd.QuickAddSheet


import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.time.LocalDate


/** The create drawers opened from the + menu, the widgets and the mic: task, event, money. */
@Composable
internal fun CreateSheets(
    sheet: String?, top: Route, onTab: Boolean, currentTab: Tab, lists: List<TaskList>,
    quickAddText: String, onClose: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as KairoApplication
    val scope = rememberCoroutineScope()
    val snackbar = LocalAppSnackbar.current
    when (val sh = sheet) {
        null, "habit" -> {}
        "list" -> ListSheet(null, 0, onDismiss = onClose)
        "task" -> {
          // + adds where you are (owner, 2026-11-21): Today → today, Calendar → the day you're
          // looking at, inside a list → that list (smart Today → today).
          val inList = (top as? Route.ListRoute)?.listId
          val smart = inList?.let { SmartList.of(it) }
          QuickAddSheet(
            lists = lists,
            defaultDue = when {
                smart == SmartList.Today -> LocalDate.now()
                inList != null -> null
                !onTab -> null
                currentTab == Tab.Tasks -> LocalDate.now()
                currentTab == Tab.Calendar -> CalendarFocus.day
                else -> null
            },
            defaultListId = inList?.takeIf { it > 0 },
            initialText = quickAddText,
            onAdd = { task ->
                scope.launch {
                    app.repository.add(task)
                    // Say where it went: a task for another day isn't on Today's main view.
                    val today = LocalDate.now()
                    val where = when (task.due) {
                        null -> "Added to Inbox"
                        today -> "Added for today"
                        today.plusDays(1) -> "Added for tomorrow"
                        else -> "Added for " + task.due.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM"))
                    }
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(where, duration = androidx.compose.material3.SnackbarDuration.Short)
                }
            },
            onDismiss = onClose,
          )
        }
        "event" -> NewEventSheet(
            calendar = app.phoneCalendar,
            initialDay = if (onTab && currentTab == Tab.Calendar) CalendarFocus.day ?: LocalDate.now() else LocalDate.now(),
            onDismiss = onClose,
        )
        // Finance drawer: New account, Ivy import preview, Ivy export.
        "account" -> NewAccountSheet(app.money, onDismiss = onClose) { msg -> onClose(); scope.launch { snackbar.showSnackbar(msg) } }
        "ivy" -> IvyImportSheet(app.money, onDismiss = onClose) { msg -> onClose(); scope.launch { snackbar.showSnackbar(msg) } }
        "export" -> IvyExport(app.money) { msg -> onClose(); if (msg != null) scope.launch { snackbar.showSnackbar(msg) } }
        else -> TxSheet(app.money, TxKind.valueOf(sh), onDismiss = onClose)
    }
}

/** The search bar's mic: the system speech recognizer, then Quick Add with the words (or empty if none). */
@Composable
internal fun rememberVoiceAdd(onWords: (String) -> Unit): () -> Unit {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
    ) { r -> onWords(r.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()) }
    return {
        val ask = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Add a task")
        runCatching { launcher.launch(ask) }.onFailure { onWords("") } // no recognizer: just type
    }
}

