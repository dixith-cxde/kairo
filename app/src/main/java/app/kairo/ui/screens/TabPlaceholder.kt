package app.kairo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.kairo.navigation.Tab
import app.kairo.ui.theme.KairoTheme

/** Empty tab until its real screen is built. Title style = screen title rule (displaySmall emphasized). */
@Composable
fun TabPlaceholder(tab: Tab, comingIn: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KairoTheme.colors.bg)
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 64.dp),
    ) {
        Text(
            text = tab.label,
            style = MaterialTheme.typography.displaySmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = comingIn,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Preview(name = "Dark")
@Composable
private fun TabPlaceholderDark() {
    KairoTheme(darkTheme = true) { TabPlaceholder(Tab.Lists, "Coming in step 8") }
}

@Preview(name = "Light")
@Composable
private fun TabPlaceholderLight() {
    KairoTheme(darkTheme = false) { TabPlaceholder(Tab.Lists, "Coming in step 8") }
}
