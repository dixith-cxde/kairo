@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.data.db.AccountEntity
import app.kairo.data.db.CategoryEntity
import app.kairo.data.db.TransactionEntity
import app.kairo.data.db.TxKind
import app.kairo.data.money.Money
import app.kairo.ui.shapes.polygon
import app.kairo.ui.theme.KairoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Icons offered when creating a category (Material Symbols Rounded names). */
val CategoryIconChoices = listOf(
    "restaurant", "local_cafe", "fastfood", "shopping_cart", "shopping_bag", "directions_car", "local_gas_station",
    "receipt_long", "home", "medical_services", "fitness_center", "sports_esports", "movie", "celebration",
    "flight", "school", "pets", "redeem", "work", "payments", "savings", "category",
)

fun categoryIcon(name: String): Int = when (name) {
    "local_cafe" -> R.drawable.ic_local_cafe
    "fastfood" -> R.drawable.ic_fastfood
    "local_gas_station" -> R.drawable.ic_local_gas_station
    "home" -> R.drawable.ic_home
    "fitness_center" -> R.drawable.ic_fitness_center
    "movie" -> R.drawable.ic_movie
    "celebration" -> R.drawable.ic_celebration
    "flight" -> R.drawable.ic_flight
    "school" -> R.drawable.ic_school
    "pets" -> R.drawable.ic_pets
    "redeem" -> R.drawable.ic_redeem
    "work" -> R.drawable.ic_work
    "restaurant" -> R.drawable.ic_restaurant
    "shopping_cart" -> R.drawable.ic_shopping_cart
    "directions_car" -> R.drawable.ic_directions_car
    "shopping_bag" -> R.drawable.ic_shopping_bag
    "receipt_long" -> R.drawable.ic_receipt_long
    "medical_services" -> R.drawable.ic_medical_services
    "sports_esports" -> R.drawable.ic_sports_esports
    "payments" -> R.drawable.ic_payments
    "savings" -> R.drawable.ic_savings
    else -> R.drawable.ic_category
}

/** A category's own shape in its container color, with its icon (rule 7). */
@Composable
fun CategoryBadge(c: CategoryEntity?, size: Int = 40) {
    val k = KairoTheme.colors
    if (c == null) {
        Box(Modifier.size(size.dp).background(MaterialTheme.colorScheme.secondaryContainer, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_swap_vert), null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size((size / 2).dp))
        }
        return
    }
    Box(Modifier.size(size.dp).background(k.container(c.color), c.shape.polygon.toShape()), contentAlignment = Alignment.Center) {
        Icon(painterResource(categoryIcon(c.icon)), null, tint = k.onContainer(c.color), modifier = Modifier.size((size / 2).dp))
    }
}

/** Signed amount as shown: expense −₹, income +₹, transfer legs by sign. */
fun TransactionEntity.signedPaise(): Long = when (kind) {
    TxKind.Expense -> -amountPaise
    TxKind.Income -> amountPaise
    TxKind.Transfer -> amountPaise
}

fun accountIcon(t: app.kairo.data.db.AccountType) = when (t) {
    app.kairo.data.db.AccountType.Bank -> R.drawable.ic_account_balance
    app.kairo.data.db.AccountType.Cash -> R.drawable.ic_payments
    app.kairo.data.db.AccountType.Card -> R.drawable.ic_credit_card
    app.kairo.data.db.AccountType.Wallet -> R.drawable.ic_savings
}

@Composable
fun TxRow(
    tx: TransactionEntity, cat: CategoryEntity?, account: AccountEntity?, index: Int, count: Int,
    selected: Boolean = false, selecting: Boolean = false, onLongClick: (() -> Unit)? = null, onClick: () -> Unit = {},
) {
    val c = MaterialTheme.colorScheme
    val signed = tx.signedPaise()
    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = "Select",
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = app.kairo.ui.common.selectableRowColors(),
        leadingContent = { if (selecting) app.kairo.ui.common.SelectBadge(selected) else CategoryBadge(cat) },
        supportingContent = {
            Text(listOfNotNull(if (tx.kind == TxKind.Transfer) "Transfer" else cat?.name, account?.name).joinToString(" · ") +
                if (tx.tags.isNotBlank()) " · ${tx.tags}" else "")
        },
        trailingContent = {
            Text(
                Money.format(signed, sign = true),
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = if (signed > 0) c.tertiary else c.onSurface,
            )
        },
    ) { Text(tx.note.ifBlank { cat?.name ?: "Transaction" }, style = MaterialTheme.typography.bodyLarge) }
}

private val DayFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM")

fun dayLabel(d: LocalDate): String {
    val t = LocalDate.now()
    return when (d) { t -> "Today"; t.minusDays(1) -> "Yesterday"; else -> d.format(DayFmt) }
}

@Composable fun SegGap() = Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
