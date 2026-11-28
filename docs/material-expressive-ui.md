# Material 3 Expressive: how to design an app in the Kairo style

A playbook for building any Android app the way Kairo is built: Material 3 Expressive (M3E) "to the core", with the house choices the owner made on top. Written 2026-11-24 from Kairo 1.3.1's code, the design docs, and the v2 redesign (`research/redesign-handoff.md`).

Each section says three things:
- **M3E**: what Google's system does (from androidx `material3` source or m3.material.io).
- **House style**: what we do on top, on purpose.
- **Compose**: the real API to use.

Where a number was eyeballed rather than read from Google's source, it says so.

---

## 0. The short version

1. **Calm base, loud moments.** Neutral gray screens, grouped white/dark-gray containers, clear type. Color, shape and bounce only where something happens.
2. **Gray is the canvas, color is the signal.** Backgrounds have zero chroma. Color only on the primary action, selection, progress, identity (list/category), status (error, money in/out, priority).
3. **Shapes carry meaning.** Each category owns a `MaterialShapes` shape. Selection morphs shape. Content rows stay rectangles.
4. **Springs, never durations.** All motion comes from `MaterialTheme.motionScheme`. Spatial springs may overshoot; color and opacity never do.
5. **Big, bold, tabular numbers.** The most important number on a screen is the biggest thing on it.
6. **One thumb.** Primary actions live in the bottom third: floating nav, a solid primary +, floating toolbars, bottom sheets.
7. **Drawers, not dialogs.** Every picker or form is a `ModalBottomSheet`; menus are the grouped Expressive menu.
8. **Every destructive or completing action has Undo.**
9. **Dark is the default, light is fully designed.** Every screen ships in both.
10. **Use the real component first.** Hand-roll only when M3E has nothing (charts, swipe, custom nav).

---

## 1. Project setup

- Compose BOM `2026.11.01` → `material3` 1.5.0, Compose UI 1.13.0. Expressive APIs moved between alphas; check names against the current release before you start.
- Most Expressive APIs need `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`. Put `@file:OptIn(...)` at the top of UI files instead of annotating every function.
- Add `androidx.graphics:graphics-shapes` for `RoundedPolygon` and `Morph`.

### The theme

```kotlin
MaterialExpressiveTheme(
    colorScheme = if (dark) AppDarkColors else AppLightColors,
    motionScheme = MotionScheme.expressive(),   // standard() when the user asks for reduced motion
    typography = AppTypography,
) { content() }
```

Use `MaterialExpressiveTheme`, not `MaterialTheme`: it is what makes components pick up the expressive motion scheme and shapes.

App-only tokens (nav background, list colors, priority colors) go in a `CompositionLocal` and are read like the theme: `AppTheme.colors.navBg`. See `ui/theme/Theme.kt`.

---

## 2. Color

### M3E
Same color roles as M3: primary, secondary, tertiary, each with a container and "on" pair, plus error, surfaces, outline, inverse and fixed roles. Schemes are generated from one seed color with `material-color-utilities`.

### House style

**Accents.** v2 uses **SchemeVibrant, spec 2025, contrast 0.5 (medium)**, from the seed (Kairo: `#79D0FD`). The owner rejected weaker schemes twice ("too weak", then "need clear contrast"). Kairo 1.x used SchemeTonalSpot 2021; that is what the current code still ships.

Generate it like this (Node, `@material/material-color-utilities` 0.4):

```js
new SchemeVibrant(Hct.fromInt(argbFromHex('#79D0FD')), isDark, 0.5, '2025', 'phone')
```

The library's imports lack `.js` extensions. Copy it, add `.js` to the relative imports with sed, and add `{"type":"module"}` to its package.json.

**Surfaces are always neutral gray (chroma 0)**, whatever the seed or wallpaper. After building the scheme (seed or `dynamicDarkColorScheme`), overwrite every surface role with grays. Tinted surfaces looked muddy in every scheme tried.

| Token | Dark | Light | Use |
|---|---|---|---|
| bg (`background`) | `#0E0E0E` | `#F1F1F1` | screen |
| item | `#1F1F1F` | `#FFFFFF` | grouped containers, rows, cards |
| item-press / nav / chip | `#2A2A2A` | `#E8E8E8` press · `#FFFFFF` nav · `#E2E2E2` chip | pressed rows, floating nav, chips |
| search | `#262626` | `#E2E2E2` | search pill |
| onSurface | `#FFFFFF` | `#1B1B1B` | text |
| onSurfaceVariant | `#C6C6C6` | `#474747` | secondary text |

In the light theme the nav is white **with a shadow**, since white on `#F1F1F1` needs an edge. In dark, the lighter gray is enough.

v2 accent values (SchemeVibrant 2025 / 0.5, seed `#79D0FD`):

| Role | Dark | Light |
|---|---|---|
| primary / onPrimary | `#63CCFF` / `#00374B` | `#006384` / `#FFFFFF` |
| primaryContainer / on | `#00C1FF` / `#002B3D` | `#007DA7` / `#FFFFFF` |
| tertiaryContainer / on | `#8D94FF` / `#01004D` | `#636AD2` / `#FFFFFF` |
| error | `#FF9F99` | `#B31B25` |
| errorContainer / on | `#D7383B` / `#FFFFFF` | `#D7383B` / `#FFFFFF` |

**Where color goes:**

| Role | Used for |
|---|---|
| primary | the + button, selected nav tab, selected toggle, Done/Save, progress, section headers |
| primaryContainer | hero card, FAB menu items, "complete" swipe pill, selected list rows |
| tertiaryContainer | avatar, secondary hero accents (birthday badge), lifted row while reordering |
| error / errorContainer | overdue, delete, "trash" swipe pill |
| extended colors | category/list identity, priority, money in/out; harmonize them to the seed |

**User choice.** Offer wallpaper color (Android 12+) or a few seeds, plus contrast Standard / Medium / High. Surfaces stay gray in all of them.

---

## 3. Type

### M3E
The 15 type roles, and each now has an **emphasized** twin (`titleMediumEmphasized`, and so on) that is heavier. Use emphasized for titles, numbers and actions.

### House style
- Font: **Google Sans Flex** (fallback Roboto Flex), loaded as a variable font with `FontVariation`.
- Big type (display, headline, titleLarge) uses the **fully rounded** axis `ROND 100`. Body and labels use `ROND 50` for reading.
- **Tabular digits on every style** (`fontFeatureSettings = "tnum"`). It only changes digits, so it is safe on all text, and counting numbers stop jittering (Google Clock had this bug).
- Hero numbers (money totals, timers): 64–96sp, weight 750–800, `ROND 100`.
- Section headers: titleSmall emphasized (14/600) in primary. Overdue headers in error.
- Google Sans Flex has no `GRAD` axis, so dark-theme grade compensation isn't possible with it.

```kotlin
private fun flex(weight: FontWeight, round: Float = 50f) = Font(
    resId = R.font.google_sans_flex, weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight), FontVariation.Setting("ROND", round),
    ),
)
private fun TextStyle.tnum() = copy(fontFeatureSettings = "tnum")
```

Build all 30 roles (15 base + 15 emphasized) from `Typography(fontFamily = …)` and apply `.tnum()` or `.big()` to each. Full version: `ui/theme/Type.kt`.

Icons: **Material Symbols Rounded**, outlined by default, filled when selected.

---

## 4. Shape

### M3E corner scale (from androidx `ShapeTokens`)

| Token | dp |
|---|---|
| ExtraSmall | 4 |
| Small | 8 |
| Medium | 12 |
| Large | 16 |
| LargeIncreased | 20 |
| ExtraLarge | 28 |
| ExtraLargeIncreased | 32 |
| ExtraExtraLarge | 48 |
| Full | pill |

### Where each corner goes

| Thing | Corners |
|---|---|
| Grouped list ("bubbly", v2) | outer 28, inner 8, 3dp gaps, rows ≥ 72dp; a pressed row rounds fully |
| Grouped list (stock M3E segmented) | outer 16, inner 4, 2dp gaps (`ListItemDefaults.SegmentedGap`) |
| Hero card | 32 |
| Cards | 28 |
| Sheets | 28 top |
| Text fields | 12, filled (not underlined) |
| Connected button group | outer full (20 at 40dp height), inner 8; selected becomes a full pill; pressed drops to 4 |
| Floating nav / toolbar | full |
| + button (64dp) | 20; grows to 80dp with 24 on scroll |
| Snackbar | 4 (from memory of `SnackbarTokens`, not confirmed) |
| Widgets | system widget radius outside, about 12 inside |

### The shape library (`MaterialShapes`)

35 `RoundedPolygon`s: cookies (4, 6, 7, 9, 12 sided), Sunny, Clover4Leaf/8Leaf, Burst, SoftBurst, Gem, Pentagon, Arch, Pill, Circle, Square and more.

Rules:
1. **One shape per category, everywhere.** Kairo: Work = Cookie9Sided, Home = Arch, Groceries = Clover4Leaf, Health = Sunny, Family = Pentagon, Personal = Gem, Reading = Cookie6Sided, smart lists = Cookie4Sided. The shape appears on the list, on each row's meta line, in detail, and in stats.
2. **Hero numbers sit in shapes.** Streak in Sunny, progress ring over Cookie12Sided, save success in Cookie12Sided (scalloped, like Google Wallet).
3. **Selection morphs.** Circle → Cookie9Sided with a check, not just a color change.
4. **Plain content rows stay rectangles** with segmented corners. Shapes on everything is noise.
5. **Banned: Puffy, PuffyDiamond, Heart.** They looked wrong in Stash.
6. Avatar: a 44dp Cookie9Sided in tertiaryContainer.

```kotlin
// Static shape as a Compose Shape
Modifier.background(color, MaterialShapes.Cookie9Sided.toShape())

// Morph between two shapes
val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress)
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}
// Drive progress with animateFloatAsState(..., motionScheme.fastSpatialSpec())
```

---

## 5. Motion

### M3E
Springs instead of duration + easing. `MotionScheme.expressive()` has six specs:

| Spec | Damping | Stiffness | Use |
|---|---|---|---|
| `fastSpatialSpec()` | 0.6 | 800 | things a finger caused: press, toggle, checkbox, swipe snap-back |
| `defaultSpatialSpec()` | 0.8 | 380 | components moving or resizing, entrances |
| `slowSpatialSpec()` | 0.8 | 200 | full-screen and container transforms |
| `fastEffectsSpec()` | 1.0 | 3800 | quick color/opacity |
| `defaultEffectsSpec()` | 1.0 | 1600 | most color/opacity |
| `slowEffectsSpec()` | 1.0 | 800 | big fades, count-ups |

The damping/stiffness numbers were read from AndroidX tokens for Kairo 1.x; the v2 handoff couldn't re-find them in `MotionTokens.kt`. Treat the table as close, and always call the spec rather than copying the numbers.

### Rules
- **Never write a duration or easing curve.** Read `MaterialTheme.motionScheme.xxxSpec()`. The one exception is slow decorative loops (a hero shape spinning once a minute), since the scheme has no looping token. Keep those in one helper (`slowSpin` in `ui/common/Motion.kt`).
- **Spatial may overshoot, effects never.** Position, size and shape use spatial; color and alpha use effects.
- **Things move from where they came from.** The + grows into its sheet; a row grows into its detail page; a completed task drops into "Done".
- **Neighbors react.** Pressing a connected button widens it and squeezes its neighbors (`animateWidth`). When you drag a row, its neighbors follow a little ("jelly").
- **Reduced motion** switches to `MotionScheme.standard()`.
- **Make every tab feel alive:** staggered entrances, count-up numbers, springy tab switches.

### Reusable motion pieces (from `ui/common/Motion.kt`)

| Piece | What it does |
|---|---|
| `Modifier.enter(key, index)` | rises 24dp and fades in on `defaultSpatialSpec`, staggered 35ms per row (capped at 10), first show only |
| `countUp(target)` | number animates from 0 on `slowEffectsSpec` (no overshoot past the real value) |
| `slowSpin()` | 60s linear rotation for decorative hero shapes |

### Motion catalogue

| Moment | Spec |
|---|---|
| Button / row press | corners tighten (or round fully), fastSpatial |
| Toggle / checkbox | fill fastEffects; scale 1 → 0.85 → 1 fastSpatial |
| Nav pill switch | leading edge on fastSpatial, trailing edge on slowSpatial, so it stretches toward the tab, then catches up |
| Page change | new page slides half a screen in, old page a third out, both scale 0.92↔1, slowSpatial plus fades (try defaultSpatial if it feels slow) |
| Detail page | shared axis X; predictive back previews the return |
| Nav on scroll | hides on scroll down, returns on scroll up (defaultSpatial); the + grows 64 → 80dp |
| Complete task | burst, strike-through, row collapses into "Done", ring advances (slowSpatial), haptic, Undo snackbar |
| Charts | bars grow from the baseline, 40ms stagger; numbers count up |
| Reorder | lifted row in tertiaryContainer, 16dp corners, elevation 2, 1° tilt |

---

## 6. Layout and containment

### M3E
Grouped content in containers with small gaps and big outer corners, so a screen reads as a few clear blocks (Android 16 Settings).

### House style
- Screen = bg gray. Groups = item color. 16dp side margins.
- **No screen title in v2.** The top of each tab is a **Drive-style search pill**: 56dp, full corners, search color, centered hint that changes per tab ("Search in tasks"), a mic button inside on the right. The avatar sits outside the pill on the right. No hamburger unless the app has a drawer.
- Detail screens: back button only at the top, content below, actions in a floating toolbar at the bottom.
- **Hero card** at the top of the main tab: primaryContainer, 32dp corners. Big label on the left ("Today", 36/44), a small "Next · 12:00" line and one-line title below it; a wavy progress ring (stroke 8) over a slowly spinning Cookie12Sided on the right, with "3/10" in the middle. Don't repeat in words what the ring already shows. Give it a designed "all done" state.
- **Filters:** a connected button group under the hero (All · Overdue · Today · Upcoming · Done), with the most useful one selected by default.
- **Lists:** bubbly segmented groups with section headers in primary.
- Touch targets ≥ 48dp. Small secondary controls can be visually 32dp; the owner prefers quiet controls ("too big" was a real complaint).
- Leave bottom padding so content clears the floating nav and +.

---

## 7. Navigation and the + button

M3E docks its nav bar to the bottom and uses floating toolbars for actions. **We deliberately use a floating pill for navigation.**

**Nav pill (v2):**
- Floating gray pill, **sized to its tabs, not stretched**, centered with the + beside it, 8dp apart, 16dp above the gesture inset (about 40dp from the bottom).
- 64dp tall, full corners.
- The **selected tab is a solid primary pill** holding a filled icon and its label in onPrimary. Other tabs show only an outlined icon.
- The pill widens with a spring on switch. Pressing a tab squares its corners to 8dp.
- Indicator metrics follow M3's horizontal nav item: 40dp tall indicator, 16dp side padding, 24dp icon.
- 3–5 tabs. Kairo v2: Tasks · Habits · Calendar · Finance. Rarely used areas (stats, settings) live behind the avatar.

**The + button:**
- **Solid primary with a bold plus**, never tonal or transparent (M3's default is primaryContainer; this is our departure).
- 64dp with 20dp corners and a 28dp icon, matching the bar height. Grows to 80dp with 24dp corners when the bar hides on scroll (M3's `FabSizeRange` is 56→80).
- Tap opens a `FloatingActionButtonMenu`: anchored to the +, opening **8dp above it** (never overlapping it), 56dp full-round items in primaryContainer, titleMedium labels, the most common item nearest the thumb. The + becomes a 56dp primary circle with a close icon. 32% scrim behind.

```kotlin
FloatingActionButtonMenu(expanded = open, button = { ToggleFloatingActionButton(...) }) {
    FloatingActionButtonMenuItem(onClick = { … }, text = { Text("Task") }, icon = { Icon(…) })
}
```

Kairo 1.x hides its real + under the custom nav and passes a 64dp `Spacer` as `button`; for a new app, use `ToggleFloatingActionButton` unless you need the custom nav look.

---

## 8. Components: what to use

| Need | Component | Notes |
|---|---|---|
| Theme | `MaterialExpressiveTheme` | with `MotionScheme.expressive()` |
| List rows | `SegmentedListItem` | `shapes = ListItemDefaults.segmentedShapes(index, count)`, `colors = ListItemDefaults.segmentedColors(containerColor = item)`, `Spacer(ListItemDefaults.SegmentedGap)` between rows |
| Selectable rows | `SegmentedListItem(selected = …)` overload | the component morphs the shape and uses primaryContainer itself |
| One-of-N choice | `ButtonGroup` + `ToggleButton` | `ButtonGroupDefaults.connectedLeading/Middle/TrailingButtonShapes()`, `Modifier.animateWidth(interaction)`, `Role.RadioButton` semantics |
| Every button | `shapes = ButtonDefaults.shapes()` / `IconButtonDefaults.shapes()` | so it morphs when pressed; apply to all `Button`, `TextButton`, `FilledTonalButton`, `IconButton`, `FilledIconButton` |
| Bulk / page actions | `HorizontalFloatingToolbar` | 64dp, full corners, 8dp padding, 4dp between items, 16dp from the screen edge; `floatingActionButton` slot for the primary action; `vibrantFloatingToolbarColors()` for selection mode |
| Create menu | `FloatingActionButtonMenu` + `FloatingActionButtonMenuItem` | see §7 |
| Menus | `DropdownMenuPopup` + `DropdownMenuGroup(shapes = MenuDefaults.groupShape(i, n))` + `SelectableDropdownMenuItem(shapes = MenuDefaults.itemShapes())` | grouped, rounded, current item highlighted |
| Pickers, forms | `ModalBottomSheet` | 28dp top corners, 32×4 handle, 32% scrim, header with a live summary in primary, Cancel / Done |
| Progress | `CircularWavyProgressIndicator`, `LinearWavyProgressIndicator` | the wave is the expressive part; use for goals and budgets |
| Loading | `LoadingIndicator` (or `ContainedLoadingIndicator`) | shape-morphing; also inside `PullToRefreshBox` |
| Top bar (title screens) | `LargeFlexibleTopAppBar` + `exitUntilCollapsedScrollBehavior` | title displaySmall, subtitle titleMedium. v2 replaces titles with the search pill |
| Search | `SearchBar` | styled as the Drive-style pill |
| Undo | `SnackbarHost` | inverse surface, text plus Undo |
| Charts | hand-built | M3 has no chart spec: rounded bars, wavy rings, shape segments, emphasized tabular numbers |
| Widgets | Jetpack Glance + `GlanceTheme` | see §13 |

**Banned here:** `AlertDialog`, `DatePickerDialog`, `TimePickerDialog`. Put `DatePicker` / `TimePicker` inside a bottom sheet instead.

---

## 9. Lists in depth

- **Bubbly groups:** 28 outer / 8 inner corners, 3dp gaps, 72dp minimum rows. A pressed row rounds fully.
- **Row anatomy:** leading badge (category shape and color), title (bodyLarge), a meta line (time, or "Yesterday" in error; category shape dot and name; subtasks done/total; bell if it has an alert), trailing indicator (a red flag for high priority).
- **Priority** shows as a 4dp bar on the row's left edge, colored by priority (v2, not yet reviewed by the owner).
- **Done items** get struck through and move to a "Done" group. The header is segment 0 and the items follow as segments, so nothing overlaps.
- **Selection mode:** long press. The leading slot becomes a select badge (outlined circle → filled Cookie9Sided with a check, morph on fastSpatial). The top bar shows ✕, "N selected", All/None. Actions move to a vibrant `HorizontalFloatingToolbar` that springs up while the nav steps aside.
- **Entrances:** `Modifier.enter(key, index)` on each row.

### Swipe (Gmail M3E style, v2)

No checkbox on rows. Tap opens the item.
- **Left to right completes** (or un-completes). **Right to left moves to trash.**
- The action is a **separate full-round pill** growing from the screen edge, about 8dp clear of the dragged row. It starts as a sliver, becomes a circle with the icon at about 48dp wide, then stretches. Complete = primaryContainer + check; delete = errorContainer + bin.
- Past about **40% of the row width** it arms and the icon pops to 1.2×. Release flies the row off, the pill fills the row, and the gap collapses.
- The dragged row rounds fully. **Jelly:** neighbors in the same group follow by 6% (±1 row) and 2% (±2 rows), capped at 16dp, and spring back on fastSpatial.

The 40%, 8dp, 48dp and jelly values were eyeballed from a recording of Gmail, not taken from Google's source.

Look at the real reference app (screenshots, recordings) before building something like this. The first swipe attempt was rejected for not matching Gmail.

---

## 10. Sheets and forms

- Every picker, form or choice is a `ModalBottomSheet`.
- Header: a title plus a **live summary in primary** ("Every 2 weeks on Tue, Thu · until 24 Feb 2027").
- **Choices over switches.** A connected group "On time · 30 min · 15 min · No alert" beats a reminder on/off switch plus a second picker.
- Quick picks as filter chips first (Never · Daily · Weekdays · Weekly · Monthly · Yearly), detail controls below (an "Every N" stepper and a Days/Weeks/Months/Years group).
- Day-of-week toggles are round; selected turns primary and squares to 12dp. At least one stays on.
- Text fields: filled, 12dp corners.
- **Editors like Google Keep:** a big title line (28/36); Enter starts smaller body lines (16/24); any line can become a checkbox subtask. Save as you go, with no Save button on full pages.
- Sheets save only on Done. Pages save as you type.
- Money entry: a custom keypad with big keys and live digit grouping (a deliberate departure from the system keyboard).

---

## 11. Feedback, haptics and Undo

- **Haptics only on complete, delete, undo**, plus save/cancel on sheets. Use `HapticFeedbackType.Confirm` (complete/save), `Reject` (cancel/delete) and `ToggleOn` where it fits. No haptics on scrolling or ordinary taps.
- **Undo snackbar** after complete, un-complete, trash, archive and duplicate. It sits above the nav, uses the inverse surface, and Undo restores the exact earlier state. v2 wants it to stay 10s (`SnackbarDuration.Long`); Kairo 1.x uses `Short`.
- **Soft delete:** items go to Trash for 30 days.
- **At most two hero moments per flow.** Kairo's are completing a task (pop, burst, strike, drop, ring advance, haptic) and the progress ring. Saving an expense gets a smaller scalloped success shape.

---

## 12. States you must design

- **Empty:** one shape (category or hero shape) with a short line of text. "All done / The rest of the day is yours", not a blank list.
- **Loading:** `LoadingIndicator`. Since the app is local-first, there should be almost none.
- **Errors and permissions:** plain words about what is missing and one button to fix it.
- **Locked app:** compose nothing behind the lock. Sheets drew above the lock screen once.
- **Both themes** for every one of these.

---

## 13. Widgets (Glance)

- Outer corners use the **system widget radius** (`system_app_widget_background_radius`). Inner corners = outer radius − padding (about 12dp).
- Use `GlanceTheme` color roles; support dynamic color and both themes.
- A widget that holds one thing (progress, a timer) can be one whole expressive shape.
- **Glance limits:** no springs, no shape morphing, no custom animation; updates crossfade. Draw shapes as static vector drawables.
- Sizes: 2×1 plus at least two of 1×1, 2×2, 4×2, 4×4. More space shows more content, not more padding.
- Touch targets ≥ 48dp.

---

## 14. Departures from Google's spec (on purpose)

| We do | M3 says | Why |
|---|---|---|
| Floating pill nav, sized to its tabs, centered with the + | Docked nav bar; floating toolbars are for actions | Owner's choice; frees width, keeps + one thumb away |
| Solid primary + | primaryContainer FAB | Rule: the create button is the loudest thing |
| 64dp + | 56 / 80 / 96 | Matches the bar height |
| Always-gray surfaces | Scheme-tinted surfaces | Calmer; accents stand out (Android 16 Settings) |
| Bubbly groups 28/8/3dp | Segmented 16/4/2dp | Owner liked the softer look |
| Custom money keypad | System keyboard | Faster amount entry |
| Charts built from M3 parts | No chart spec | Nothing to follow |

Write departures down. Anything not in a table like this should follow the spec.

---

## 15. Process: how to design a new app this way

1. **Inventory first.** List every feature from the code or spec (`research/app-inventory.md` is the model). The redesign is checked against it.
2. **Pick the seed** and generate SchemeVibrant 2025 / 0.5 for dark and light. Replace surfaces with the gray table.
3. **Assign shapes** to each category and hero number. Write the table down. Check the banned list.
4. **Pick at most two hero moments** for the main flow.
5. **Design the shell first:** nav pill, +, create menu, search pill, avatar. Then one tab at a time.
6. **Design dark, then make its light twin** (same layout; only the theme, links and title differ).
7. **For every interaction, name the spring** (fast/default/slow × spatial/effects) and the haptic (usually none).
8. **For every action, name its Undo.**
9. **Research, don't guess.** Read the androidx `material3` source (`*Tokens.kt`, `*Defaults`), m3.material.io, and Google's own apps (Gmail swipe, Keep editor, Drive search, Calendar grid, Wallet success, Clock digits, Android 16 Settings). Say where each value came from, and flag the ones that were eyeballed.
10. **Build in small steps**, one screen or feature per step; install on a real phone and check both themes before calling it done.

### Owner taste, learned the hard way
- Likes: bubbly grouped lists, connected button groups, the Drive-style search pill, the hero ring, subtle physical motion everywhere, choices over switches, small quiet controls.
- Dislikes: muddy or tinted colors, a stretched nav, title text at the top of tabs, anything overlapping, redundant labels next to a visual that already says it.
- Iterates visually, one or two changes per message. Change exactly what was asked and keep the rest.

---

## 16. Checklist before calling a screen done

- [ ] Background gray, groups item-colored, color only where it means something
- [ ] Real M3E component used where one exists; `shapes = …Defaults.shapes()` on every button
- [ ] Every animation reads from `motionScheme`; no durations except decorative loops
- [ ] Numbers tabular; the key number is the biggest thing
- [ ] Category shapes consistent; no Puffy/Heart; plain rows stay rectangles
- [ ] Pickers in bottom sheets, menus grouped, no dialogs
- [ ] Undo for every destructive or completing action; haptics only where allowed
- [ ] Empty, loading, error and permission states designed
- [ ] Works in dark and light, at large font sizes, with 48dp targets
- [ ] Nothing overlaps the floating nav, the +, or the keyboard
- [ ] Installed and checked on a real phone

---

## Sources

- androidx `material3` source on GitHub: `ShapeTokens`, FAB, FAB menu, floating toolbar, nav item and app bar tokens.
- m3.material.io (M3 Expressive), developer.android.com (Compose Material 3, Glance, widget design guides).
- Google apps used as references: Gmail (swipe), Keep (editor, reorder), Drive (search pill), Calendar (grid), Wallet (success shape), Clock (tabular digits), Android 16 Settings (gray containers).
- Kairo: `docs/01-research.md`, `docs/02-principles.md`, `docs/03-design-system.md`, `docs/build/step-15c-expressive-pass.md`, `research/redesign-handoff.md`, and `app/src/main/java/app/kairo/ui/`.
