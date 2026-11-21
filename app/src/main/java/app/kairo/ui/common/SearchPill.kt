package app.kairo.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.ui.theme.KairoTheme

/** Opens the universal search screen (provided by the app shell). */
val LocalOpenSearch = androidx.compose.runtime.staticCompositionLocalOf<() -> Unit> { {} }

/** Universal search entry: a wide pill (icon + "Search") in every tab's app bar. */
@Composable
fun SearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .then(with(app.kairo.ui.onboarding.TourTargetScope) { Modifier.target("search", 20.dp) })
            .height(40.dp)
            .clip(CircleShape)
            .background(KairoTheme.colors.itemHigh)
            .clickable(role = Role.Button, onClickLabel = "Search", onClick = onClick)
            .padding(start = 12.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(R.drawable.ic_search), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Search", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
