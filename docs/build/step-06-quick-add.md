# Step 6 · Quick add sheet

**Status:** built 2026-11-21, compiles. Not yet run on the phone.

**New ideas:** text input (`BasicTextField`) and a modal sheet (`ModalBottomSheet`).

## What it does

- The + FAB opens an M3 `ModalBottomSheet`; the keyboard comes up straight away.
- Type one line: `Call Mom tomorrow 7pm !high #Family`. `QuickAddParser` pulls out:
  - **date/time**: today, tomorrow/tmr, tonight, weekdays (mon…sun, "next fri"), "next week", "in 3 days / 2 weeks", `7pm`, `7:30 pm`, `19:30`
  - **priority**: `!high` `!med` `!low` (or `!1` `!2` `!3`)
  - **list**: `#Family` (matches a list name)
- Recognised words are highlighted in the field (date = secondaryContainer, priority = coral container, list = that list's container) and removed from the title.
- Chips mirror what was understood: date, priority (tap cycles), Remind (tap toggles). List button picks a list by hand.
- Send (72×56, solid primary, 16dp corners) or the keyboard's Done saves, with a `Confirm` haptic (rule 9: save on a sheet). The sheet stays open for the next task; swipe down to close.
- From Today, a task with no date goes to today; from other tabs it has no date.

## Not yet (honest)

- **FAB → sheet container transform** (canvas Motion board) is not done. `ModalBottomSheet` lives in its own window, so the FAB can't morph into it directly; doing it properly means a custom sheet with `SharedTransitionLayout`. Left for the polish step.
- Date chip doesn't open a picker yet (step 7). No Repeat chip (recurring tasks aren't in the data model yet).
- FAB menu (Task / Expense / Income / Transfer / List) comes with Money.
