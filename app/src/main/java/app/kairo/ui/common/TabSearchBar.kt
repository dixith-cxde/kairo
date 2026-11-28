@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package app.kairo.ui.common

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kairo.R
import app.kairo.ui.theme.KairoTheme
/** Canvas .top, on every tab: a 64dp search bar (menu · hint · mic) and the account button on a Cookie-9. */
@Composable
fun TabSearchBar(hint: String, onSearch: () -> Unit, onVoice: () -> Unit, onMenu: (() -> Unit)?, onAccount: () -> Unit) {
    val k = KairoTheme.colors
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 12.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.weight(1f).height(64.dp).clip(CircleShape).background(k.searchBg)
                .clickable(role = Role.Button, onClickLabel = "Search", onClick = onSearch),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                hint, style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 56.dp),
            )
            if (onMenu != null) IconButton(onClick = onMenu, shapes = IconButtonDefaults.shapes(), modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                Icon(painterResource(R.drawable.ic_menu), "Lists", tint = c.onSurface)
            }
            IconButton(onClick = onVoice, shapes = IconButtonDefaults.shapes(), modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                Icon(painterResource(R.drawable.ic_mic), "Add by voice", tint = c.onSurface)
            }
        }
        AccountButton(onAccount)
    }
}

@Composable
fun AccountButton(onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.size(52.dp).clip(CircleShape)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(bounded = false, radius = 26.dp),
                role = Role.Button, onClickLabel = "Your account and stats", onClick = onClick)
            .semantics { contentDescription = "Your account and stats" },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(44.dp).background(c.tertiaryContainer, MaterialShapes.Cookie9Sided.toShape()))
        Icon(painterResource(R.drawable.ic_person_filled), null, tint = c.onTertiaryContainer)
    }
}

// ---------------------------------------------------------------- views, hero, list filter

