# Home-screen widgets

Built with **Jetpack Glance**, using Google's canonical widget layouts. Same palette and gray containers as the app; one solid primary action per widget at most.

## The widgets

| Widget | Default size | Resizes | Layout | Content |
|---|---|---|---|---|
| **Today** | 4×2 | 2×2 → 4×2 → 4×4 | List · checklist | Cookie4 icon, "Today", "3 of 9 done", solid + button; 3 task rows with working checkboxes (2 at 2×2, 7 at 4×4) |
| **Progress** | 2×2 | 1×1, 2×2 | Shape widget | Whole widget is a 9-sided cookie in primary container; wavy ring; "6 left today" |
| **Money** | 2×2 | 2×2, 4×2 | Text + action | "Spent · Nov ₹31,640", budget bar 79%, solid "Expense" button |
| **Quick add** | 4×1 | 2×1 → 4×1 | Toolbar (search-style) | "Add a task" field with mic; tonal ₹ (expense) and timer (focus) buttons |
| **Next up** | 2×1 | 2×1, 4×1 | Text | Sunny badge + time + next task or calendar event |
| **Streak** | 1×1 | 1×1 | Shape widget | Sunny shape in tertiary container, flame + "12" |

## Rules

- **Corners:** system widget radius outside (24–28dp, never custom). Inside = outer − 12dp padding → 12dp; 4dp between grouped rows, like the app's segmented lists.
- **Shapes:** single-purpose widgets (Progress, Streak) are a whole library shape that fills the cell on one axis. Content widgets are rectangles and use shapes only for icons.
- **Color:** `GlanceTheme` with dynamic color, light and dark. Containers use Kairo's `item` / `itemHigh` grays. One solid primary action; the rest tonal (secondary container).
- **Size:** every widget resizes; extra space shows more content, never empty space. 48dp touch targets.
- **Type:** emphasized titles, big tabular numbers, one line per row (ellipsize).
- **Motion:** Glance can't run springs or shape morphs. Updates crossfade; checkboxes use the platform `CheckBox` (Android 12+); shapes are static vector drawables (export `MaterialShapes` paths to XML).
- **Data:** reads the same Room database through a repository; refresh on data change (`updateAll`) and every 30 minutes; no network.
- **Taps:** rows open the task; + opens quick add; Expense opens Add expense; checkbox completes in place (with the app's undo in the next open).

## Quality target

Aim for **Tier 1** of Google's widget quality guide: theming (dynamic + dark), fills the grid, system corners, resizing, personalised content, good contrast, a widget preview image and description in the picker.
