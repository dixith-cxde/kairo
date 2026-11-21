# Step 2 · Floating nav + empty tabs

**Status:** done 2026-11-21. Built by Claude, run on the owner's phone (vivo, Android 12): tab switching, label/width animation, outlined → filled icons and Back all work.

**Goal:** the Stash floating nav joined to the + FAB, switching between five empty tabs.

**New ideas:** `Modifier` + layout (`Row` / `Column` / `Box`), and navigation (Navigation 3). `remember`, `animate*AsState` and `CompositionLocal` also appear in small doses; they're explained briefly below and properly in later steps.

## Files

| File | What it is |
|---|---|
| `ui/nav/FloatingNavBar.kt` | The nav bar + FAB group |
| `navigation/Tab.kt` | The five tabs: label, outlined icon, filled icon |
| `KairoApp.kt` | App shell: back stack, `NavDisplay`, nav bar on top |
| `ui/screens/TabPlaceholder.kt` | "Lists · Coming in step 8" screens |
| `ui/theme/Surfaces.kt` | Kairo's own surface tokens: `bg`, `item`, `itemHigh`, `navBg`, `floatShadow` |
| `res/drawable/ic_*.xml` | Material Symbols Rounded, from Google's `material-design-icons` repo: outlined, filled (`fill1`), and `add` at weight 600 for the bold plus |

## Idea: `Modifier`

A `Modifier` is an ordered list of instructions for one composable: size, padding, background, clicks, shadow. **Order matters**: each modifier wraps the next. For example, in the nav bar:

```kotlin
Modifier
    .weight(1f)             // take the leftover width in the row
    .height(64.dp)
    .floatShadow(shape, …)  // shadow drawn outside the shape
    .background(navBg, shape)
    .padding(horizontal = 8.dp) // padding *inside* the background
```

Put `.padding` before `.background` and the padding would sit outside the colored area. That's the same idea as margin vs. padding in CSS, but here it's just the order.

## Idea: layout (`Row`, `Column`, `Box`)

- `Row`: children side by side (flexbox `row`). The nav bar is a Row of [bar, FAB], and the bar is a Row of five slots.
- `Column`: children stacked vertically. The placeholder screen is a Column of [title, subtitle].
- `Box`: children layered on top of each other. `KairoApp` is a Box of [current screen, nav bar], with the nav aligned to the bottom.
- `Modifier.weight(w)` inside a Row is `flex-grow`. Every slot has weight 1, and the selected slot animates to 3. That's how it widens, exactly like the canvas's `flex-grow: 3`.

## Idea: navigation (Navigation 3)

We use **Navigation 3** (`androidx.navigation3` 1.2.2, stable), Google's newer navigation library built for Compose, instead of the older Navigation Compose (`navigation-compose`). The roadmap said "Navigation Compose"; Navigation 3 is its Compose-first successor.

The idea is simple: **the back stack is just a list you own.**

```kotlin
val backStack = rememberSaveable(saver = TabStackSaver) { mutableListOf(Tab.Today).toMutableStateList() }
NavDisplay(backStack = backStack, entryProvider = { tab -> NavEntry(tab) { TabPlaceholder(tab, …) } })
```

- `NavDisplay` shows the last item of the list. To navigate, change the list: add, replace or remove.
- Tab rules: Today is always at the bottom. Picking another tab puts it on top (`[Today, Lists]`), and picking a different one replaces it (`[Today, Stats]`). So Back always goes to Today, then out of the app. That's Android's standard behavior for top-level destinations.
- Tab switches cross-fade instead of sliding, because tabs are peers, not deeper levels. The fade timings come from `motionScheme` (`defaultEffectsSpec` in, `fastEffectsSpec` out).
- `rememberSaveable` with a saver keeps the stack when the phone rotates or Android kills the app in the background. The stack is saved as tab names.

## Small ideas used here

- **`remember { … }`**: keeps a value across recompositions. Without it, the value would be recreated every time the function re-runs. Covered properly in step 3.
- **`animateFloatAsState` / `animateColorAsState` / `animateDpAsState`**: you give a target value and Compose animates to it whenever the target changes. Every spec comes from `MaterialTheme.motionScheme`: spatial springs for size and shape, effects springs for color and opacity. No hand-written durations. Covered properly in step 5.
- **`CompositionLocal`** (`KairoTheme.surfaces`): a value that's available to everything inside the theme without passing it through every function. It's how `MaterialTheme.colorScheme` works. Kairo needs its own because `item`, `navBg` etc. aren't the same M3 role in light and dark (light `item` is white = `surfaceContainerLowest`; dark `item` is `surfaceContainer`).

## How the nav matches the spec

| Spec (canvas `.fnav` / Stash) | Code |
|---|---|
| 16dp sides, 20dp above system nav | `navigationBarsPadding()` then `padding(16, 16, bottom 20)` |
| Bar 64dp, corners 32 outside / 8 at the FAB | `RoundedCornerShape(32, 8, 8, 32)` |
| FAB 64dp, corners 8 / 32, pressed → 16 | `FloatingActionButton` with an animated shape (`fastSpatialSpec`) |
| 4dp gap between bar and FAB | `Arrangement.spacedBy(4.dp)` |
| Pill 48dp, `secondaryContainer`, label only when active | `animateColorAsState` + `AnimatedVisibility` |
| Active slot `flex-grow: 3` | `Modifier.weight(animated 1 → 3)` |
| Icon outlined → filled | `Crossfade` between the two vectors |
| Shadow `0 6px 20px`, 60% black (dark) / 28% navy (light) | `Modifier.dropShadow` with `KairoTheme.surfaces.floatShadow` |
| Bold plus, solid primary (rule 4) | `add` at weight 600, `primary` / `onPrimary` |

## Honest notes

- **The nav itself is hand-built, on purpose.** Material 3's `ShortNavigationBar` is docked, shows every label, and doesn't widen the selected item. Rule 5 asks for the Stash floating design. The FAB *is* the real `FloatingActionButton`.
- **Not yet:** the nav hiding on scroll down and coming back on scroll up (it needs a scrolling screen; that comes with Today in step 3 or the flexible app bar), and the FAB opening anything (quick add is step 6).
- **Not yet checked:** light theme on the phone. It renders in the `@Preview`, but the app has no theme switch until step 12.
- **Gap above the gesture bar:** your vivo reserves only about 4dp for its gesture strip, so the bar sits about 24dp from the bottom edge. On a Pixel with a gesture bar it will sit a little higher. That's correct: the 20dp is measured from the system nav area, not the screen edge.
