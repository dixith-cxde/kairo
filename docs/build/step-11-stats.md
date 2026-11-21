# Step 11 · Stats

**Status:** built 2026-11-21, compiles. Not yet run on the phone.

**New idea:** `Morph` (graphics-shapes) — animating one `RoundedPolygon` into another.

## What's there

- **Week / Month / Year** as an M3 Expressive connected button group (`ToggleButton` + `ButtonGroupDefaults.connected…Shapes`).
- **Total completed** in big editorial numbers, with a **badge that morphs and turns** per range: Cookie9 (Week) → Clover4Leaf (Month) → Sunny (Year), showing the % change vs the previous period. `Morph(from, to)` + a progress `Animatable` on `defaultSpatialSpec`, drawn through a small `MorphShape`.
- **Bar chart**: days (week), ~6-day blocks (month), months (year). The best bar is tertiary.
- **On time** wavy ring: share of dated tasks completed on or before their date.
- **Streak** in a Sunny shape: consecutive days with at least one completion.
- **By list** split bar in list accent colors, top four listed.
- **Month heatmap**: this month, cell strength by completions.

Stats use `completedAt`, which now flows through the `Task` model (and editing a done task keeps its original completion time). Debug builds seed 40 past completions so the screen has data; release builds don't.

## Not yet (honest)

- Bars don't animate in, and you can't tap a bar for details.
- "Month" uses rolling 30 days, not the calendar month.
