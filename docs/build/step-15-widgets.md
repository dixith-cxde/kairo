# Step 15 · Widgets (Glance)

**Status:** built 2026-11-21. Needs a check on the phone's home screen.

**New idea:** Jetpack Glance — widgets written like Compose, rendered by the launcher as RemoteViews.

## Widgets

| Widget | What it shows | Taps |
|---|---|---|
| **Today** (4×2, resizes) | "Today · N of M done", solid primary + button, open tasks (2 / 3 / 7 rows by height) with **working checkboxes** | checkbox completes the task in place; + opens quick add; title opens the app |
| **Progress** (2×2) | done/total big number, "N left today", progress bar on primaryContainer | opens the app |
| **Money** (2×2) | "Spent · Nov ₹…", budget bar (error when over), solid + | + opens Add expense on the Money tab |

- Colors: `GlanceTheme` (dynamic color, day/night) for accents; Kairo's gray containers for backgrounds (rule 3). Outer corners 28dp, rows 12dp.
- Data: the same repositories. Widgets refresh ~0.5 s after any task or transaction change, and every 30 min.
- Widget taps reach the app through `MainActivity.EXTRA_ACTION` (`quick_add`, `add_expense`).

## Not yet (honest)

- **Quick add (toolbar), Next up and Streak** widgets aren't built.
- **Shape widgets** (Progress as a whole Cookie9, Streak as Sunny) need the MaterialShapes paths exported to vector XML; for now they're rounded rectangles.
- No widget preview images in the picker.
- Glance can't animate: no springs or morphs (expected, per docs/05).
