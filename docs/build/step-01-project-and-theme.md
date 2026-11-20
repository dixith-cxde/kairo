# Step 1 · Project + theme

**Status:** done 2026-11-20. Claude wrote the project at the owner's request, built it, and ran it on the owner's phone (Android 12). Sections 1–3 describe what was set up; sections 4–5 are the code, with explanations.

**Goal:** Kairo installs on your phone, shows the cat icon, and opens on a dark gray screen that says "Kairo" in Google Sans Flex, using Kairo's real colors.

**New ideas:** `@Composable` and `@Preview` (explained below, at the point you use them).

## Versions (checked 2026-11-20 on Google's Maven)

| Thing | Version | Note |
|---|---|---|
| Android Studio | 2026.2.2 | already installed |
| Compose BOM | `2026.11.01` | brings in Material 3 `1.5.0` and Compose UI `1.13.0` |
| Material 3 | `1.5.0` (stable) | `MaterialExpressiveTheme`, `MotionScheme.expressive()` and emphasized type are **stable** here, so no `@OptIn` is needed for them. Some Expressive components still need it; we'll add it when we hit one. |
| compileSdk / targetSdk | 37.2 / 37 | Compose UI 1.13 requires compileSdk ≥ 37.1, so platform 37.2 was installed. compileSdk only sets which APIs we can call; targetSdk sets runtime behavior. |
| AGP / Gradle / Kotlin | 9.5.2 / 9.6.1 / 2.4.21 | AGP 9 compiles Kotlin itself; only the Compose compiler plugin is declared |
| minSdk | 31 (Android 12) | for dynamic color; was the open question, now set |

Don't upgrade to `material3 1.6.0-alpha01`. Stay on what the BOM gives you.

---

## 1. Create the project

1. Android Studio → **New Project** → **Empty Activity** (the Compose one).
2. Fill in:
   - Name: `Kairo`
   - Package name: `app.kairo` (change it if you own a domain, e.g. `com.yourname.kairo`, and use your choice everywhere below)
   - Save location: `~/coding/mobile-applications/current/kairo`, the folder that already has `docs/`. Studio will say the folder isn't empty; that's fine.
   - Minimum SDK: **API 31**
   - Build configuration language: Kotlin DSL
3. Let Gradle sync finish, then press **Run** with your phone selected. You should see the template's "Hello Android!" text.

This proves the toolchain works before we change anything.

Then open `gradle/libs.versions.toml` and check that `composeBom` is `2026.11.01`. If it's older, change it and click **Sync Now**. Also check that the `material3` library line has **no version** of its own; the BOM supplies it.

## 2. Add the font

1. Download the variable font from Google Fonts' repository (Open Font License):
   ```bash
   curl -L -o ~/Downloads/google_sans_flex.ttf "https://raw.githubusercontent.com/google/fonts/main/ofl/googlesansflex/GoogleSansFlex%5BROND%2Copsz%2Cslnt%2Cwdth%2Cwght%5D.ttf"
   ```
2. In Studio: right-click `app/src/main/res` → **New → Android Resource Directory** → type `font`.
3. Copy `google_sans_flex.ttf` into `app/src/main/res/font/`. Resource file names must be lowercase with underscores, which is why it's renamed.

It is one file of about 3.9 MB containing every weight. "Variable" means weight, roundness and so on are dials (axes) instead of separate files.

## 3. Add the icon

The traced icon files are ready in `kairo/design/icon/` (preview: `design/icon/preview.png`).

1. Copy `design/icon/ic_launcher_foreground.xml` and `design/icon/ic_launcher_background.xml` into `app/src/main/res/drawable/`, **replacing** the template's files with the same names.
2. Open `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` (in some templates it's `mipmap-anydpi/`). Make it exactly this, and do the same for `ic_launcher_round.xml`:
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
       <background android:drawable="@drawable/ic_launcher_background" />
       <foreground android:drawable="@drawable/ic_launcher_foreground" />
       <monochrome android:drawable="@drawable/ic_launcher_foreground" />
   </adaptive-icon>
   ```
   `monochrome` is the layer Android 13+ uses for themed icons. It only reads the shape, so the same cat drawing works.
3. You can delete the `mipmap-*dpi` folders containing `.webp` files. With minSdk 31 every device uses the adaptive XML. Leaving them does no harm either.

## 4. The theme

The template made `ui/theme/Color.kt`, `Type.kt` and `Theme.kt`. Replace each file's contents.

### Idea: `@Composable`

A composable is a Kotlin function, marked `@Composable`, that **describes** a piece of UI instead of building it step by step. You never call `setText()` or `findViewById()`. When the data a composable reads changes, Compose calls the function again and updates the screen. A theme is also a composable: it wraps your UI and makes colors, type and motion available to everything inside it.

### `Color.kt`

All values come from [03-design-system.md](../03-design-system.md): the TonalSpot scheme from seed `#79D0FD`, with every surface replaced by neutral gray. The "fixed" roles were computed with `material-color-utilities` from the same seed. **Every role is set on purpose.** Any role left out would fall back to Material's default purple.

```kotlin
package app.kairo.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Accents: SchemeTonalSpot (2021) from seed #79D0FD (same as Stash).
// Surfaces: neutral gray, chroma 0. Comment = M3 tone.

val KairoDarkColors = darkColorScheme(
    primary = Color(0xFF8ECFF2), onPrimary = Color(0xFF003549),
    primaryContainer = Color(0xFF004D68), onPrimaryContainer = Color(0xFFC2E8FF),
    inversePrimary = Color(0xFF1A6585),
    secondary = Color(0xFFB5C9D7), onSecondary = Color(0xFF20333D),
    secondaryContainer = Color(0xFF364954), onSecondaryContainer = Color(0xFFD1E5F3),
    tertiary = Color(0xFFC9C1EA), onTertiary = Color(0xFF312C4C),
    tertiaryContainer = Color(0xFF484264), onTertiaryContainer = Color(0xFFE5DEFF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8A9297), outlineVariant = Color(0xFF41484D),
    onBackground = Color(0xFFDFE3E7),
    onSurface = Color(0xFFDFE3E7), onSurfaceVariant = Color(0xFFC0C7CD),
    surfaceTint = Color(0xFF8ECFF2),
    scrim = Color(0xFF000000),
    // Neutral grays
    background = Color(0xFF0E0E0E),              // T4  · bg
    surface = Color(0xFF131313),                 // T6
    surfaceDim = Color(0xFF131313),              // T6
    surfaceBright = Color(0xFF393939),           // T24
    surfaceContainerLowest = Color(0xFF0E0E0E),  // T4
    surfaceContainerLow = Color(0xFF1B1B1B),     // T10
    surfaceContainer = Color(0xFF1F1F1F),        // T12 · item
    surfaceContainerHigh = Color(0xFF2A2A2A),    // T17 · itemHigh, navBg
    surfaceContainerHighest = Color(0xFF353535), // T22
    surfaceVariant = Color(0xFF353535),          // T22
    inverseSurface = Color(0xFFE2E2E2),          // T90
    inverseOnSurface = Color(0xFF303030),        // T20
    // Fixed roles (same in light and dark)
    primaryFixed = Color(0xFFC2E8FF), primaryFixedDim = Color(0xFF8ECFF2),
    onPrimaryFixed = Color(0xFF001E2C), onPrimaryFixedVariant = Color(0xFF004D68),
    secondaryFixed = Color(0xFFD1E5F3), secondaryFixedDim = Color(0xFFB5C9D7),
    onSecondaryFixed = Color(0xFF091E28), onSecondaryFixedVariant = Color(0xFF364954),
    tertiaryFixed = Color(0xFFE5DEFF), tertiaryFixedDim = Color(0xFFC9C1EA),
    onTertiaryFixed = Color(0xFF1C1736), onTertiaryFixedVariant = Color(0xFF484264),
)

val KairoLightColors = lightColorScheme(
    primary = Color(0xFF1A6585), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC2E8FF), onPrimaryContainer = Color(0xFF004D68),
    inversePrimary = Color(0xFF8ECFF2),
    secondary = Color(0xFF4E616D), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E5F3), onSecondaryContainer = Color(0xFF364954),
    tertiary = Color(0xFF605A7D), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE5DEFF), onTertiaryContainer = Color(0xFF484264),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF71787D), outlineVariant = Color(0xFFC0C7CD),
    onBackground = Color(0xFF171C1F),
    onSurface = Color(0xFF171C1F), onSurfaceVariant = Color(0xFF41484D),
    surfaceTint = Color(0xFF1A6585),
    scrim = Color(0xFF000000),
    // Neutral grays
    background = Color(0xFFEEEEEE),              // T94 · bg
    surface = Color(0xFFF9F9F9),                 // T98
    surfaceDim = Color(0xFFDADADA),              // T87
    surfaceBright = Color(0xFFF9F9F9),           // T98
    surfaceContainerLowest = Color(0xFFFFFFFF),  // T100 · item, navBg
    surfaceContainerLow = Color(0xFFF3F3F3),     // T96 · itemHigh
    surfaceContainer = Color(0xFFEEEEEE),        // T94
    surfaceContainerHigh = Color(0xFFE8E8E8),    // T92
    surfaceContainerHighest = Color(0xFFE2E2E2), // T90
    surfaceVariant = Color(0xFFE2E2E2),          // T90
    inverseSurface = Color(0xFF303030),          // T20
    inverseOnSurface = Color(0xFFF1F1F1),        // T95
    // Fixed roles (same in light and dark)
    primaryFixed = Color(0xFFC2E8FF), primaryFixedDim = Color(0xFF8ECFF2),
    onPrimaryFixed = Color(0xFF001E2C), onPrimaryFixedVariant = Color(0xFF004D68),
    secondaryFixed = Color(0xFFD1E5F3), secondaryFixedDim = Color(0xFFB5C9D7),
    onSecondaryFixed = Color(0xFF091E28), onSecondaryFixedVariant = Color(0xFF364954),
    tertiaryFixed = Color(0xFFE5DEFF), tertiaryFixedDim = Color(0xFFC9C1EA),
    onTertiaryFixed = Color(0xFF1C1736), onTertiaryFixedVariant = Color(0xFF484264),
)
```

### `Type.kt`

We build one font family out of the single variable file, one `Font` entry per weight we use. Every entry sets the `ROND` (roundness) axis to 50, as in the design system. `Typography(fontFamily = …)` then applies that family to all 30 text styles: the 15 base ones and their 15 "Emphasized" twins.

```kotlin
package app.kairo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.kairo.R

private fun flex(weight: FontWeight) = Font(
    resId = R.font.google_sans_flex,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.Setting("ROND", 50f),
    ),
)

val GoogleSansFlex = FontFamily(
    flex(FontWeight.Normal),   // 400
    flex(FontWeight.Medium),   // 500
    flex(FontWeight.SemiBold), // 600
    flex(FontWeight.Bold),     // 700
    flex(FontWeight.ExtraBold),// 800 (hero numbers, later)
)

val KairoTypography = Typography(fontFamily = GoogleSansFlex)
```

Not in this step: tabular digits (`"tnum"`). We add them in step 3, where the first numbers appear.

### `Theme.kt`

```kotlin
package app.kairo.ui.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable

@Composable
fun KairoTheme(
    darkTheme: Boolean = true, // Kairo rule 1: dark is the default. Settings (step 12) will let users pick.
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) KairoDarkColors else KairoLightColors,
        motionScheme = MotionScheme.expressive(),
        typography = KairoTypography,
        content = content,
    )
}
```

A correction to what I said earlier: the app **does not** follow the system light/dark setting yet. Rule 1 says dark is the default, so it opens dark. The light theme is used in previews now and becomes user-selectable in step 12.

Wallpaper (dynamic) color also waits until step 12. Leaving it out keeps this step to two ideas.

## 5. The screen

Replace `MainActivity.kt`:

```kotlin
package app.kairo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.kairo.ui.theme.KairoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KairoTheme {
                KairoHello()
            }
        }
    }
}

@Composable
fun KairoHello() {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "Kairo",
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(name = "Dark")
@Composable
fun KairoHelloDarkPreview() {
    KairoTheme(darkTheme = true) { KairoHello() }
}

@Preview(name = "Light")
@Composable
fun KairoHelloLightPreview() {
    KairoTheme(darkTheme = false) { KairoHello() }
}
```

What's going on:
- `setContent { … }` swaps the old XML layout system for Compose. Everything inside it is composables.
- `MaterialTheme.colorScheme.background` and `.typography.displaySmallEmphasized` read from the theme that `KairoTheme` set up. That's how every screen will get its colors and type. Never hard-code a color in a screen.
- `Modifier` and `Box` appear here only to fill the screen and center the text. They're properly explained in step 2.

### Idea: `@Preview`

`@Preview` tells Android Studio to draw a composable in the editor without running the app. Open the **Split** or **Design** view at the top right of `MainActivity.kt` to see Dark and Light side by side. Preview functions take no parameters, so they wrap your real composable in the theme, as shown above. From now on, every screen gets a dark and a light preview, because rule 1 says every screen ships in both.

## 6. Run it

Press **Run**. Done when all of these are true:

- [ ] The home screen shows the cat icon on a light gray background. Long-press → the icon is not cropped.
- [ ] If you use themed icons (Settings → Wallpaper & style → Themed icons), the cat appears tinted.
- [ ] The app opens on a near-black gray screen (`#0E0E0E`), not blue-tinted, with "Kairo" in light blue.
- [ ] The text is visibly Google Sans Flex: rounded ends, not Roboto.
- [ ] Both previews render in Studio.

Then make it a git repo, so every step after this is a commit:

```bash
cd ~/coding/mobile-applications/current/kairo && git init && git add . && git commit -m "Step 1: project, theme, icon"
```

## Decided / found in this step

- minSdk 31.
- Icon: the cat mark traced to a vector; background keeps the original gray gradient (#E4E4E4 → #C4C4C4); monochrome layer added.
- Google Sans Flex has no `GRAD` axis, so the dark-theme `GRAD -25` from the design system can't be done (noted in [03-design-system.md](../03-design-system.md)).
- `MaterialExpressiveTheme` is stable in Material 3 1.5.0.
