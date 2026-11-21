# Step 8 · Lists + list detail

**Status:** built 2026-11-21, compiles. Not yet run on the phone.

## Lists tab

- Flexible app bar "Lists".
- **Smart lists** as 2×2 tiles (28dp corners, Cookie4 icon, big tabular count): Inbox (no date), Today (due today or overdue), Next 7 days, Completed. Smart lists are queries over all tasks, not stored lists (ids −1…−4).
- **My lists**: segmented rows; each list shows **its own shape** in its container color with its initial (rule 7), and its open-task count.
- **New list** row → dialog: name, pick one of the 8 list shapes, pick one of 7 colors.

## List detail

- App bar with the list's shape badge and name, back.
- Filter chips: Open / Upcoming / No date / Done.
- Sections: Overdue (error color), Today, Upcoming, No date (or Done). Rows are the same `TaskRow` as Today (checkbox burst, tap → task detail), and they animate in and out.

## Not yet (honest)

- **Drag to reorder** (lists and tasks; canvas shows a lifted tertiary row with a drop gap) isn't built. Compose has no built-in drag-reorder for `LazyColumn`; it needs a small custom gesture. Left for polish.
- Rename / recolor / delete a list (no menu yet).
- FAB menu with "List" (comes with Money's FAB menu).
