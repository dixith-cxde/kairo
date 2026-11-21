# Step 7 · Task detail + date/time pickers

**Status:** built 2026-11-21, compiles. Not yet run on the phone.

**New ideas:** deeper navigation (a back stack with more than tabs), `LaunchedEffect`.

## What's there

- Tap any task on Today → detail. Back (or predictive back) returns.
- **Routes**: the back stack now holds `Route`s: `TabRoute(tab)`, `TaskRoute(id)`, `ListRoute(id)` (step 8). Saved as strings like `task:12`. The floating nav hides on detail screens.
- List chip with the list's shape: tap to move the task to another list.
- Checkbox + editable title (headlineSmall emphasized), "Due in 1 h 20 min" / "Due today" / "Overdue" pill on `tertiaryContainer`.
- Grouped rows (segmented list), each icon inside a **Cookie4** shape: Date (M3 `DatePickerDialog`), Time (M3 `TimePickerDialog` + `TimePicker`), Reminder (switch), Priority (tap cycles).
- Notes field.
- M3 Expressive **`HorizontalFloatingToolbar`** (date, reminder, delete) with a solid primary **Done** FAB.
- Every edit saves immediately (no Save button). Haptics: complete, delete, Done (rule 9).

## Ideas

- **`LaunchedEffect(key) { … }`**: runs a coroutine when the composable appears, and re-runs it when `key` changes. Here: when the title text changes, save it.

## Not yet (honest)

- The canvas's **custom date sheet** (Date/Duration button group, four big quick picks, month grid with task dots) is not built; the standard M3 date picker is used for now. Worth doing in polish.
- **Subtasks** (list with wavy progress, drag handles) aren't in the data model yet; only the counts exist.
- Repeat and "Add to Calendar" rows wait for steps 9–10. Delete has no undo yet.
- Row → detail **container transform** not done (fade-through instead).
