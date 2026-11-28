# Instructions for building Kairo

These are the working rules for Claude (or anyone) working on Kairo. They come from decisions the owner made during the design phase. Follow them unless the owner changes them.

## 1. Where we are

- **v2 build in progress (since 2026-11-28).** The v2 design is on the "Kairo v2 Design" canvas (https://claude.ai/artifact/XuXAdkKY8P3N7g4Ps5GiHa); the owner started building it screen by screen. Start from `research/build-handoff.md` (what's built, rules, what's next); design notes are in `research/redesign-handoff.md`. No commits until the owner says so. The old v1 build steps in `docs/build/` are superseded.
- The Android project lives in this `kairo/` folder (Gradle files next to `docs/`).
- The source of truth for the look is the v2 canvas above (the v1 canvas `RthtmHKkEsUMm2WUMRUE49` was deleted). These docs are the source of truth for the *why* and the rules.
- Keep all Kairo docs inside this `kairo/` folder.

## 2. The owner

- Building their **second** app (the first was Stash, a React Native/Expo app at `~/coding/reactnative/stash`). New to Kotlin and Compose.
- Wants Material 3 Expressive **"to the core"** — research-backed, real tokens, nothing guessed. When something is unclear, research it (Android developer docs, m3.material.io, Google's own apps) instead of inventing it, and say where it came from.
- Prefers short, plain answers; asks a lot of "is this really the best?" questions — answer honestly, including what is *not* yet at the bar.

## 3. How to build (once approved)

- **Claude writes the code when the owner asks (since step 1), and explains as it goes.** Build and run on the owner's phone (`./gradlew :app:installDebug`) before calling a step done. Work in small steps. Each step introduces at most one or two new Compose ideas (e.g. `@Composable`, state hoisting, `remember`, `LazyColumn`, `Modifier`, navigation, `ViewModel`, `Flow`, Room). Explain each idea in plain words the first time it appears, then build with it.
- One screen or one feature per step. The app should run after every step.
- Follow [docs/07-roadmap.md](docs/07-roadmap.md) for the order.
- Use the real Expressive components from `androidx.compose.material3` before hand-rolling anything. Check the current library version and API names when starting; Expressive APIs moved between alpha releases (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)` may be needed).
- Read motion from `MaterialTheme.motionScheme` (`fastSpatialSpec()`, `defaultEffectsSpec()` …). Never hand-write durations or easing curves. Exception (owner, 2026-11-21): slow ambient loops on decorative hero shapes (`slowSpin` in `ui/common/Motion.kt`), since the motion scheme has no looping token. The owner wants every tab to feel alive: entrances, count-ups, springy transitions.
- Shapes from `MaterialShapes` (graphics-shapes `RoundedPolygon`) and `Morph` for transitions.

## 4. Design rules that must not drift

1. **Dark theme is the default.** Light theme is fully designed too — every screen ships in both.
2. **Default palette = Google blue/green** (owner, 2026-11-28): primary and secondary are Google blue (`#0B57D0` light / `#A8C7FA` dark), tertiary is Google green (`#146C2E` / `#6DD58C`), from the owner's reference image. Exact role values are in the canvas tokens and the handoff. Users can choose wallpaper (dynamic) color or other seeds.
3. **Backgrounds are always neutral gray** (chroma 0), whatever the seed or wallpaper. Screen = gray, grouped containers = white (light) / dark gray (dark), like Android 16 Settings. Only accents take color.
4. **The create (+) button is solid primary with a bold plus.** Never transparent, never tonal.
5. **Floating nav pill + create button**: a grey pill sized to its tabs (Tasks · Habits · Calendar · Finance · Focus) and the + sit together, centred, 8dp apart, 40dp from the bottom. All tabs are icons only; the selected one sits on a solid primary pill (owner, 2026-11-28: the label crowded the edges on a 360dp phone). A deliberate departure from the docked M3 nav bar.
6. **Never use the Puffy, Puffy Diamond or Heart shapes.** (Carried over from Stash.)
7. **Shapes carry meaning, not decoration.** Each list owns a shape; selection morphs shape; hero moments use shape. Plain content rows stay rectangles with the segmented-list corners.
8. **Tabular digits for every number** (amounts, times, counts). Google Clock's digits jittered without them.
9. **Haptics only on complete, delete and undo** (plus save/cancel on sheets).
10. **At most two hero moments per flow.** Completing a task and the Today progress ring are the app's main ones.
11. **Money is in ₹ with Indian digit grouping** (₹1,23,456) by default; currency is a setting.
12. **Drawers, not dialogs** (owner, 2026-11-21). Pickers, forms and choices open as M3 bottom sheets (`ModalBottomSheet`); menus use the Expressive grouped `DropdownMenuPopup`/`DropdownMenuGroup`. No `AlertDialog` / `DatePickerDialog` / `TimePickerDialog`.
13. **Real M3 Expressive components first**: selectable `SegmentedListItem`, `ButtonDefaults.shapes()` / `IconButtonDefaults.shapes()` on every button, connected button groups, floating toolbars, `LoadingIndicator`, wavy progress.

## 5. Keeping the canvas in sync

- The owner rearranges boards on the canvas. **Never re-layout `canvas.json`**; only add boards in free space.
- Always fetch the latest canvas before editing; merge, never overwrite.
- When a dark board changes, update its light twin the same way (the twin differs only in default theme, links to other light boards, and the title).
- Shared tokens live in the canvas's `project/m3e.css`; [docs/03-design-system.md](docs/03-design-system.md) mirrors them.

## 6. Data and privacy

- Local only. No backend, no analytics, no account. Backup is a local/exported file (optional cloud copy via Android's own backup).
- Google Calendar works through the **Android Calendar Provider** — only calendar permissions, no sign-in. Google Tasks would need OAuth and is out of scope. See [docs/06-data-and-calendar.md](docs/06-data-and-calendar.md).

## 7. Writing style for docs and answers

Plain, specific, short. Say what is decided, what is assumed, and what is still open. Don't oversell; if something is not yet "max Expressive", say so and say why.
