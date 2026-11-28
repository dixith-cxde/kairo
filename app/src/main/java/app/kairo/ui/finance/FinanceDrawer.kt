@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.ui.theme.KairoTheme

/** What the Finance drawer can open (canvas `finDrawer`). */
enum class FinanceAction { Bills, Reports, AddAccount, Import, Export }

/**
 * Canvas "Finance drawer": Bills & subscriptions, Reports, Add account; then Data: import from and
 * export to an Ivy Wallet CSV.
 */
@Composable
fun FinanceDrawerContent(onAction: (FinanceAction) -> Unit) {
    val c = MaterialTheme.colorScheme
    ModalDrawerSheet(drawerContainerColor = KairoTheme.colors.bg, drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 16.dp)) {
            Text("Kairo", style = MaterialTheme.typography.headlineSmall, color = c.onSurface, modifier = Modifier.padding(start = 12.dp))
            Text("Finance", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Item(R.drawable.ic_event_repeat, "Bills & subscriptions", 0, 3) { onAction(FinanceAction.Bills) }
                Item(R.drawable.ic_bar_chart, "Reports", 1, 3) { onAction(FinanceAction.Reports) }
                Item(R.drawable.ic_add_card, "Add account", 2, 3) { onAction(FinanceAction.AddAccount) }
            }
            Text("Data", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = c.primary,
                modifier = Modifier.padding(start = 12.dp, top = 24.dp, bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Item(R.drawable.ic_download, "Import from Ivy Wallet", 0, 2) { onAction(FinanceAction.Import) }
                Item(R.drawable.ic_upload, "Export as Ivy Wallet CSV", 1, 2) { onAction(FinanceAction.Export) }
            }
        }
    }
}

@Composable
private fun Item(icon: Int, label: String, i: Int, n: Int, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.shapes(shape = seg(i, n), pressedShape = RoundedCornerShape(28.dp)),
        colors = ListItemDefaults.segmentedColors(containerColor = KairoTheme.colors.item),
        leadingContent = { Icon(painterResource(icon), null, tint = c.onSurfaceVariant) },
    ) { Text(label, color = c.onSurface) }
}
