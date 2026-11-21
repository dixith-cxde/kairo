@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * M3 Expressive connected button group with the real ButtonGroup: pressing a button springs it
 * wider and squeezes its neighbours (animateWidth), the "surrounding elements react" motion the
 * owner asked for. One choice at a time (radio semantics).
 */
@Composable
fun <T> ConnectedGroup(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    icon: ((T) -> Int)? = null,
    enabled: Boolean = true,
    onPick: (T) -> Unit,
) {
    val many = options.size >= 4
    ButtonGroup(
        overflowIndicator = {},
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { i, o ->
            customItem(
                buttonGroupContent = {
                    val interaction = remember { MutableInteractionSource() }
                    ToggleButton(
                        checked = o == selected,
                        onCheckedChange = { onPick(o) },
                        enabled = enabled,
                        interactionSource = interaction,
                        shapes = when {
                            options.size == 1 -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            i == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            i == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        // 4+ options (Today's filters): share the width by label length so "Upcoming"
                        // fits beside "All", and trim the side padding.
                        contentPadding = if (many) PaddingValues(horizontal = 6.dp) else PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.weight(if (many) label(o).length + 4f else 1f).animateWidth(interaction).semantics { role = Role.RadioButton },
                    ) {
                        icon?.let { Icon(painterResource(it(o)), null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)) }
                        Text(label(o), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                menuContent = {},
            )
        }
    }
}
