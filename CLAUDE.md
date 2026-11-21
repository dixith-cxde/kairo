# Instructions for building Kairo

These are the working rules for Claude (or anyone) working on Kairo. They come from decisions the owner made during the design phase. Follow them unless the owner changes them.

## 1. Where we are

- **Build phase (v1).** The owner approved the design and started building on 2026-11-20. Step guides live in `docs/build/`; the current step is the lowest-numbered one not marked done. Steps 1–15 are done (3–15 not yet run on the phone).
- The Android project lives in this `kairo/` folder (Gradle files next to `docs/`).
- The source of truth for the look is the canvas: https://claude.ai/artifact/RthtmHKkEsUMm2WUMRUE49. These docs are the source of truth for the *why* and the rules.
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
- Read motion from `MaterialTheme.motionScheme` (`fastSpatialSpec()`, `defaultEffectsSpec()` …). Never hand-write durations or easing curves.
- Shapes from `MaterialShapes` (graphics-shapes `RoundedPolygon`) and `Morph` for transitions.

## 4. Design rules that must not drift

1. **Dark theme is the default.** Light theme is fully designed too — every screen ships in both.
2. **Default palette = Stash's**: SchemeTonalSpot (2021 spec) from seed `#79D0FD`. Users can choose wallpaper (dynamic) color or one of six seeds.
3. **Backgrounds are always neutral gray** (chroma 0), whatever the seed or wallpaper. Screen = gray, grouped containers = white (light) / dark gray (dark), like Android 16 Settings. Only accents take color.
4. **The create (+) button is solid primary with a bold plus.** Never transparent, never tonal.
5. **Floating nav copied from Stash** (`stash/src/ui/FloatingNavBar.tsx`): a floating, curved pill nav joined to the FAB as a connected group, 16dp from the sides, 20dp from the bottom. This is a deliberate departure from the docked M3 nav bar.
6. **Never use the Puffy, Puffy Diamond or Heart shapes.** (Carried over from Stash.)
7. **Shapes carry meaning, not decoration.** Each list owns a shape; selection morphs shape; hero moments use shape. Plain content rows stay rectangles with the segmented-list corners.
8. **Tabular digits for every number** (amounts, times, counts). Google Clock's digits jittered without them.
9. **Haptics only on complete, delete and undo** (plus save/cancel on sheets).
10. **At most two hero moments per flow.** Completing a task and the Today progress ring are the app's main ones.
11. **Money is in ₹ with Indian digit grouping** (₹1,23,456) by default; currency is a setting.

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
