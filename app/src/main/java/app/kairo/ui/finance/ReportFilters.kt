@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package app.kairo.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.kairo.ui.calendar.GroupItem
import app.kairo.ui.theme.KairoTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** Period presets (canvas report filters). Custom uses its own From / To months. */
enum class Period(val label: String) { ThisMonth("This month"), LastMonth("Last month"), Last3("Last 3 months"), Last6("Last 6 months"), ThisYear("This year"), Custom("Custom") }

/** What a report covers. Empty sets mean "All". */
data class ReportQuery(
    val income: Boolean = false, val period: Period = Period.Last6,
    val from: YearMonth = YearMonth.now().minusMonths(5), val to: YearMonth = YearMonth.now(),
    val accounts: Set<Long> = emptySet(), val categories: Set<Long> = emptySet(), val tags: Set<String> = emptySet(),
) {
    /** First and last day the report covers. */
    fun range(today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> {
        val now = YearMonth.from(today)
        return when (period) {
            Period.ThisMonth -> now.atDay(1) to today
            Period.LastMonth -> now.minusMonths(1).let { it.atDay(1) to it.atEndOfMonth() }
            Period.Last3 -> now.minusMonths(2).atDay(1) to today
            Period.Last6 -> now.minusMonths(5).atDay(1) to today
            Period.ThisYear -> today.withDayOfYear(1) to today
            Period.Custom -> from.atDay(1) to minOf(to.atEndOfMonth(), today)
        }
    }
    fun title(): String = if (period != Period.Custom) period.label
        else if (from == to) from.format(MonthYear) else from.format(Mon) + " – " + to.format(MonthYear)
}

private val Mon = DateTimeFormatter.ofPattern("MMM")
private val MonthYear = DateTimeFormatter.ofPattern("MMM yyyy")

/** Show: Spending | Income (real connected toggle buttons). */
@Composable
internal fun ShowGroup(income: Boolean, onPick: (Boolean) -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        listOf(false to "Spending", true to "Income").forEachIndexed { i, (v, t) ->
            ToggleButton(
                checked = income == v, onCheckedChange = { onPick(v) },
                shapes = if (i == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                colors = ToggleButtonDefaults.colors(containerColor = k.chip, contentColor = c.onSurfaceVariant, checkedContainerColor = c.primary, checkedContentColor = c.onPrimary),
                modifier = Modifier.weight(1f).height(44.dp),
            ) { Text(t, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** A titled card of filter chips; [all] is the "All" chip state when [allLabel] is set. */
@Composable
internal fun ChipCard(title: String, i: Int, n: Int, content: @Composable () -> Unit) {
    GroupItem(i, n) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) { content() }
    }
}

/** One filter chip, with a colour dot when [dot] is set (canvas: accounts and categories). */
@Composable
internal fun Pick(label: String, on: Boolean, dot: Color? = null, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    FilterChip(
        selected = on, onClick = onClick, label = { Text(label) },
        leadingIcon = dot?.let { { Box(Modifier.size(8.dp).background(it, CircleShape)) } },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = c.secondaryContainer, selectedLabelColor = c.onSecondaryContainer),
    )
}

/** Toggle one id in a set; picking every one means All (an empty set). */
internal fun <T> Set<T>.flip(x: T, all: Int): Set<T> = (if (x in this) this - x else this + x).let { if (it.size >= all) emptySet() else it }

@Composable
internal fun ColumnGap() = Column(Modifier.height(8.dp)) {}
