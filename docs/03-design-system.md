# Design system

Mirrors `project/m3e.css` on the design canvas. When the two disagree, fix both.

## Color

Default scheme: **SchemeTonalSpot (2021 spec), seed `#79D0FD`** — the same as Stash. Generated with `@material/material-color-utilities`. In the app: `dynamicDarkColorScheme` / `dynamicLightColorScheme` when wallpaper color is on (Android 12+), otherwise a scheme built from the chosen seed. **Surfaces are then overridden with the neutral grays below**, whatever the source.

### Accent roles

| Role | Light | Dark |
|---|---|---|
| primary / onPrimary | `#1A6585` / `#FFFFFF` | `#8ECFF2` / `#003549` |
| primaryContainer / on | `#C2E8FF` / `#004D68` | `#004D68` / `#C2E8FF` |
| secondary / on | `#4E616D` / `#FFFFFF` | `#B5C9D7` / `#20333D` |
| secondaryContainer / on | `#D1E5F3` / `#364954` | `#364954` / `#D1E5F3` |
| tertiary / on | `#605A7D` / `#FFFFFF` | `#C9C1EA` / `#312C4C` |
| tertiaryContainer / on | `#E5DEFF` / `#484264` | `#484264` / `#E5DEFF` |
| error / on | `#BA1A1A` / `#FFFFFF` | `#FFB4AB` / `#690005` |
| errorContainer / on | `#FFDAD6` / `#93000A` | `#93000A` / `#FFDAD6` |
| outline / outlineVariant | `#71787D` / `#C0C7CD` | `#8A9297` / `#41484D` |
| onSurface / onSurfaceVariant | `#171C1F` / `#41484D` | `#DFE3E7` / `#C0C7CD` |

### Neutral surfaces (always gray, chroma 0)

| Kairo name | Meaning | Light | Dark |
|---|---|---|---|
| `bg` | screen background | `#EEEEEE` | `#0E0E0E` |
| `item` | grouped containers, cards, rows | `#FFFFFF` | `#1F1F1F` |
| `itemHigh` | nested / raised items | `#F3F3F3` | `#2A2A2A` |
| `navBg` | floating nav, toolbars | `#FFFFFF` | `#2A2A2A` |
| surface | | `#F9F9F9` | `#131313` |
| surfaceContainer low / – / high / highest | | `#F3` `#EE` `#E8` `#E2` | `#1B` `#1F` `#2A` `#35` |

### Extended colors (harmonized to the seed)

Priority: high `#BB1713` / `#FFB4A9`, medium `#7C5800` / `#FEBB2A`, low `#3351D3` / `#B9C3FF` (light / dark).

List and category colors, each with container (`l-`), on-container and accent (`a-`):

| Name | Container L / D | Accent L / D |
|---|---|---|
| indigo | `#DEE1FF` / `#0D35BB` | `#3351D3` / `#B9C3FF` |
| coral | `#FFDBD2` / `#891E00` | `#AB3514` / `#FFB4A1` |
| amber | `#FFDEA8` / `#5E4200` | `#7C5800` / `#FEBB2A` |
| green | `#99F89C` / `#005319` | `#006E24` / `#7EDB82` |
| teal | `#7DF5F5` / `#004F4F` | `#006A6A` / `#5DD9D8` |
| pink | `#FFD9E5` / `#8C0054` | `#AE236D` / `#FFB0CE` |
| violet | `#EDDCFF` / `#5D22A7` | `#753FC1` / `#D7BAFF` |

User seeds offered in Settings: Indigo, Clay, Moss, Rose, Teal, Violet. Contrast setting: Standard / Medium / High.

## Type

Font: **Google Sans Flex** (fallback Roboto Flex), rounded axis `ROND 50`. *Found 2026-11-20:* the released Google Sans Flex has axes `ROND, opsz, slnt, wdth, wght` and **no `GRAD`**, so the planned dark-theme `GRAD -25` can't be done with it (Roboto Flex has `GRAD`). Open: drop it, or use a slightly lighter `wght` in dark. Icons: **Material Symbols Rounded** (outlined by default, filled when selected).

| Role | Size / line | Weight (base → emphasized) |
|---|---|---|
| displayLarge / Medium / Small | 57/64, 45/52, 36/44 | 400 → 500 |
| headlineLarge / Medium / Small | 32/40, 28/36, 24/32 | 400 → 500 |
| titleLarge | 22/28 | 400 → 500 |
| titleMedium / Small | 16/24, 14/20 | 500 → 700 |
| bodyLarge / Medium / Small | 16/24, 14/20, 12/16 | 400 → 500 |
| labelLarge / Medium / Small | 14/20, 12/16, 11/16 | 500 → 700 |
| editorial L / XL (hero numbers only) | 64/64, 96/92 | 750–800, ROND 100 |

Rules: screen titles = displaySmall emphasized; section headers = titleSmall emphasized in primary; all numbers tabular.

## Shape

Corner scale (dp): none 0 · xs 4 · s 8 · m 12 · l 16 · l-increased 20 · xl 28 · xl-increased 32 · xxl 48 · full.

| Thing | Corners |
|---|---|
| Segmented list group | outer 16, inner 4, 2dp gaps |
| Cards | 28; hero cards 32 |
| Pressed card / row | morphs toward 16 (fastSpatial) |
| Text fields | 12, filled |
| Sheets | 28 top |
| Button group (connected) | outer full, inner 8; selected morphs to full |
| Floating nav | 32 outside, 8 where it meets the FAB; FAB the mirror |
| Widgets | system radius outside, 12 inside |

### Library shapes in use (`MaterialShapes`)

| Use | Shape |
|---|---|
| Smart lists (Inbox, Today, Next 7 days, Completed) | Cookie4Sided |
| Work | Cookie9Sided |
| Home | Arch |
| Groceries | Clover4Leaf |
| Health | Sunny |
| Family | Pentagon |
| Personal | Gem *(canvas draws a hexagon; the library has no hexagon — use Gem)* |
| Reading | Cookie6Sided |
| Completion burst | SoftBurst / Burst |
| High priority icon | Burst |
| Streak, Calendar today badge | Sunny |
| Selected seed, today cell, budget jar, insights center, progress widget | Cookie9Sided |
| Stats badge | morphs Cookie9Sided → Clover4Leaf → Sunny (Week/Month/Year) |
| Save success | Cookie12Sided (scalloped) |
| Focus timer | Cookie12Sided |

Banned: **Puffy, PuffyDiamond, Heart.**

## Motion

All from `MaterialTheme.motionScheme` (expressive). See the spring table in [01-research.md](01-research.md#spring-values-we-use-from-androidx-tokens).

| Moment | Spec |
|---|---|
| Press | corners tighten, fastSpatial |
| Toggle / checkbox | fill fastEffects, scale 1 → 0.85 → 1 fastSpatial |
| Complete task | burst 0–350ms, strike 60ms, collapse at 700ms (defaultSpatial), ring advances (slowSpatial), undo snackbar 4s |
| FAB → quick add | container transform, slowSpatial bounds + slowEffects color; content fades in after 30% |
| Nav slot change | new slot widens + label slides in (defaultSpatial), icon outlined → filled |
| Large app bar | collapses 1:1 with scroll, snaps to nearest state (defaultSpatial); container bg → surfaceContainer |
| Floating nav on scroll | hides on scroll down, returns on scroll up (defaultSpatial) |
| Reorder | lifted row tertiaryContainer, 16 corners, elevation 2, 1° tilt |
| Charts | bars grow from baseline, 40ms stagger; numbers count up |
| List → detail | container transform from the row (slowSpatial); predictive back previews it |

## Components (Compose names)

| Kairo use | Component |
|---|---|
| Theme | `MaterialExpressiveTheme(colorScheme, motionScheme = MotionScheme.expressive(), typography, shapes)` |
| Top-level screen titles | `LargeFlexibleTopAppBar` + `exitUntilCollapsedScrollBehavior` |
| Detail screens | `TopAppBar` + `pinnedScrollBehavior` |
| Range/view/theme pickers | connected `ButtonGroup` with `ToggleButton`s |
| Task / detail actions | `HorizontalFloatingToolbar` + FAB |
| Create menu | `FloatingActionButtonMenu` (Task, Expense, Income, Transfer, List) |
| Progress | `CircularWavyProgressIndicator`, `LinearWavyProgressIndicator` |
| Loading | `LoadingIndicator` (shape-morphing) |
| Lists | `LazyColumn` + segmented-list item shapes |
| Sheets | `ModalBottomSheet` |
| Dates / times | `DatePicker`, `TimePicker` (styled) |
| Nav | custom floating nav (port of Stash `FloatingNavBar`) |
| Widgets | Jetpack Glance + `GlanceTheme` |

API names change between Material3 alphas — confirm against the current release before building.
