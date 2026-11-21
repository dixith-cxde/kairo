# Step 3 · Today, static

**Status:** built 2026-11-21 and compiles. **Not yet run on the phone** (it was disconnected); install with `./gradlew :app:installDebug`.

**New ideas:** `LazyColumn` (a scrolling list that only builds visible rows, like `FlatList`), state with `remember` / `rememberSaveable`.

## What's on screen (canvas Today board)

- `LargeFlexibleTopAppBar` (M3 Expressive, stable in 1.5.0): "Today" + date subtitle, search + settings. It collapses as you scroll (`exitUntilCollapsedScrollBehavior`). Gray container, not tinted.
- Hero card: `primaryContainer`, 32dp corners, `CircularWavyProgressIndicator` (real M3 component) showing done/total, "N to go today", "Next: …". A Cookie12 shape at 10% sits behind it (static; the canvas spins it slowly; left out because there's no motion-scheme token for a 60s spin).
- Sections: "Overdue · 1" in error with Reschedule, "Today · N left" in primary.
- Rows: `SegmentedListItem` + `ListItemDefaults.segmentedShapes` (real M3 segmented list: 16dp outer, 4dp inner, 2dp gap). M3 `Checkbox` with a priority-colored border. Meta line: time, the list's own shape in its accent color, list name, subtasks, reminder, high-priority flag.
- "Completed" row toggles the done tasks.
- Floating nav hides on scroll down, returns on scroll up (`defaultSpatialSpec`).

## Ideas

- **`LazyColumn`**: you describe items (`item {}`, `itemsIndexed(list) {}`), and Compose builds only those on screen. `key = …` lets Compose keep a row's state when the list changes.
- **State**: `var x by remember { mutableStateOf(…) }` holds a value that, when changed, re-runs the composables that read it. `rememberSaveable` also survives rotation. Ticking a checkbox here changes `todayState` in `KairoApp`, and Today redraws: that's **state hoisting** (state lives above the screen; the screen gets values and callbacks).

## Also in this step

- **Tabular digits everywhere** (rule 8): `"tnum"` on every type style.
- `KairoTheme.colors` now also holds priority colors and the seven list colors (accent / container / on-container, dark + light).
- Data model (`Task`, `TaskList`, `ListShape`, `ListColor`, `Priority`) ready for Room in step 4.
