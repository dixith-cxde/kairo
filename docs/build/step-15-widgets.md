# Step 15 · Widgets (Glance)

**Status:** built 2026-11-21. Needs a check on the phone's home screen.

**New idea:** Jetpack Glance — widgets written like Compose, rendered by the launcher as RemoteViews.

## Widgets (all six from the canvas Widgets board, rebuilt 2026-11-21)

| Widget | What it shows | Taps |
|---|---|---|
| **Today** (4×2; 2×2 → 4×4) | Cookie4 + "Today · N of M done · K left", solid primary +, open tasks as segmented rows (2 / 3 / up to 7 by height) with **working checkboxes** and times. 2×2 shows "K left". | checkbox completes in place; row opens the task; + opens quick add |
| **Progress** (2×2; 1×1) | The whole widget is a Cookie9 in primary container with the canvas's wavy progress ring and "N left today" | opens the app |
| **Money** (2×2; 4×2) | "Spent · Nov ₹…", two-part budget bar (error when over), "79% of ₹40,000", solid "+ Expense" button | Expense opens Add expense |
| **Quick add** (4×1; 2×1) | Search-style "Add a task" pill with mic; tonal ₹ and timer buttons (hidden at 2×1) | pill opens quick add; mic opens the system speech recognizer, then quick add with the words filled in; ₹ adds an expense; timer opens Focus |
| **Next up** (2×1; 4×1) | Sunny in tertiary with a bolt, time + the next timed task or calendar event today (4×1 adds "in 25 min" or the calendar name) | opens the task, or the event in the calendar app |
| **Streak** (1×1) | The whole widget is a Sunny in tertiary container, flame + days in a row with a finished task (same rule as Stats) | opens the app |

- Colors: the app's own scheme (seed or wallpaper) through `GlanceTheme`, honoring the app's Light/Dark setting; containers are Kairo's `item` / `itemHigh` grays (rule 3). Outer corners use the system widget radius; rows use 12/4dp segmented corners.
- Shapes: the real `MaterialShapes` polygons (Cookie4, Cookie9, Sunny) drawn to bitmaps and tinted, not hand-exported paths.
- Data: the same repositories. Widgets refresh ~0.5 s after any task, transaction or settings change, and every 30 min.
- Picker previews: generated from real content on Android 15+ (`providePreview` + `setWidgetPreviews`), once per app version.
- Widget taps reach the app through `MainActivity.EXTRA_ACTION` (`quick_add`, `add_expense`, `focus`, `voice_add`, `task:<id>`).

## Not yet (honest)

- Glance can't animate: no springs, morphs, or the canvas's slow cookie spin (expected, per docs/05).
- Widget text uses the launcher's font, not Google Sans Flex, and can't force tabular digits; RemoteViews don't support custom fonts or font features.
- Next up refreshes on data changes and every 30 min, so it can lag up to 30 min after a task's time passes.
- No preview images on Android 12–14 (only the generated previews on 15+).
