# Design principles

Seven rules every Kairo screen follows. If a design choice breaks one, it needs a reason written down.

## 1. Calm base, expressive moments

The everyday surface is quiet: neutral gray background, grouped containers, clear type. Color, bold shapes and bouncy motion are saved for the moments that matter — finishing a task, saving an expense, hitting a streak. One or two hero moments per flow, no more.

## 2. Gray is the canvas, color is the signal

Backgrounds and containers are always neutral gray, even when the palette comes from the wallpaper or a seed. Color appears only where it means something: the primary action, selection, progress, list identity, money in/out, priority, errors.

## 3. Shapes mean something

- Every list has its own library shape (Work = 9-sided cookie, Home = arch, Groceries = clover, Health = sunny …). The shape follows the list everywhere: Lists, Today rows, task detail, stats.
- Selection is shown by **morphing** shape (circle → cookie), not just by a color change.
- Hero numbers and badges sit in shapes (streak in sunny, budget in a cookie jar, today in a sunny badge).
- Content rows stay rectangles with segmented-list corners. Shapes on everything would be noise.

## 4. Motion explains what happened

- Every motion is a spring from `MotionScheme.expressive()`. Fast for finger-caused changes, default for components, slow for whole screens.
- Spatial springs may overshoot; color/opacity never.
- Things move from where they came from: the FAB grows into the quick-add sheet, a row grows into task detail, a completed task drops into "Completed".
- Reduced-motion setting switches to the standard scheme.

## 5. Big, bold, tabular numbers

Counts, amounts, times and percentages use emphasized type and tabular digits. The most important number on a screen is the biggest thing on it.

## 6. One thumb, one primary action

- The primary action is always a solid primary button (the + FAB, "Done", "Save", "Expense").
- Main actions sit in the bottom third: floating nav, FAB, floating toolbars, sheets.
- Touch targets ≥ 48dp.

## 7. Local, honest, fast

No sign-in, no spinner waiting for a server. Calendar via the phone's own provider. Empty, error and permission states are designed, not afterthoughts. Where Kairo can't do something (e.g. Google Tasks), it says so plainly.

---

## Deliberate departures from Google's spec

| Departure | Spec says | Why we do it |
|---|---|---|
| Floating curved nav joined to the FAB (from Stash) | Docked navigation bar | Owner's choice; frees the full screen width, keeps + one thumb away |
| Always-gray backgrounds | Surfaces tinted by the scheme | Owner's choice; calmer, matches Android 16 Settings, lets accents stand out |
| Custom money keypad | System keyboard | Faster amount entry, big keys, live ₹ grouping |
| Charts | No M3 chart spec | Built from M3 parts: rounded bars, wavy rings, shapes |

## Hero moments in v1

1. **Complete a task** — checkbox pops, a tertiary burst blooms, title strikes through, row drops into Completed, progress ring advances. Haptic.
2. **Today progress ring** — wavy ring on primary container.
3. **Save an expense** — scalloped success shape.
4. Smaller standouts: Stats badge morphs per range, Budgets liquid jar, Calendar sunny day badge, Money insights donut with cookie center.
