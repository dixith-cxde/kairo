@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.finance

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kairo.R
import app.kairo.data.money.AccountBalance
import app.kairo.data.money.Money
import app.kairo.ui.money.accountIcon
import app.kairo.ui.theme.KairoTheme

/**
 * Canvas v103 total card: "Total balance · All accounts", the counted total, and a share bar of
 * pill segments (3dp gaps) in each counted account's colour.
 */
@Composable
internal fun TotalCard(balances: List<AccountBalance>, excluded: Set<String>) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val counted = balances.filter { it.account.id.toString() !in excluded }
    val total = counted.sumOf { it.balancePaise }
    val scope = if (counted.size == balances.size) "All accounts" else "${counted.size} of ${balances.size} accounts"
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(28.dp)).background(k.item).padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Total balance · $scope", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
        Text(Money.format(total), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"), color = c.onSurface)
        val parts = counted.filter { it.balancePaise > 0 }
        val sum = parts.sumOf { it.balancePaise }.toFloat()
        if (parts.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(top = 8.dp).height(12.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            parts.forEach { b ->
                Box(Modifier.weight((b.balancePaise / sum).coerceAtLeast(0.03f)).height(12.dp).background(k.accent(b.account.color), CircleShape))
            }
        }
    }
}

/** Canvas account tile: tinted icon, a corner check (counted in the total), name, balance, +in −out this month. */
@Composable
internal fun AccountTile(b: AccountBalance, inPaise: Long, outPaise: Long, counted: Boolean, modifier: Modifier, onCount: (Boolean) -> Unit, onOpen: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val k = KairoTheme.colors
    val color = k.accent(b.account.color)
    Column(
        modifier.clip(RoundedCornerShape(28.dp)).background(k.item).clickable(onClickLabel = "Show ${b.account.name}", onClick = onOpen).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            RoundIcon(accountIcon(b.account.type), color)
            Spacer(Modifier.weight(1f))
            val fill by animateColorAsState(if (counted) c.primary else Color.Transparent, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "count")
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Checkbox) { onCount(!counted) }
                    .semantics { contentDescription = if (counted) "Counted in total" else "Left out of total" },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(22.dp).background(fill, CircleShape).then(if (counted) Modifier else Modifier.border(2.dp, c.outline, CircleShape)), contentAlignment = Alignment.Center) {
                    if (counted) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp), tint = c.onPrimary)
                }
            }
        }
        Text(b.account.name, style = MaterialTheme.typography.bodyLarge, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 16.dp))
        Text(Money.format(b.balancePaise), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"), color = c.onSurface, maxLines = 1, softWrap = false,
            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(14.sp, 22.sp))
        androidx.compose.foundation.layout.FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Money.format(inPaise, sign = true), style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"), color = k.chartBlue, maxLines = 1, softWrap = false)
            Text(Money.format(-outPaise, sign = true), style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"), color = c.onSurfaceVariant, maxLines = 1, softWrap = false)
        }
    }
}
