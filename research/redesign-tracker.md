# Kairo v2 redesign tracker: Tasks

What the v2 canvas (https://claude.ai/artifact/XuXAdkKY8P3N7g4Ps5GiHa) covers for tasks, and what is still to design. Checked 2026-11-24, updated 2026-11-25 after session 3 against [app-inventory.md](app-inventory.md) and the canvas backup in [canvas/project/](canvas/project/) (17 edits).

Status: `done` = on the canvas · `todo` = not designed · `open` = needs an owner decision first.

## Covered

| Operation | How v2 does it | Status |
|---|---|---|
| Complete / mark not done | Swipe left → right, Undo snackbar | done |
| Delete | Swipe right → left to Trash (30 days), Undo | done |
| Open a task | Tap the row (Enter on keyboard) | done |
| Edit title, notes, subtasks | Keep-style editor on the task page | done |
| Date, time, repeat, alert, priority | Task page "When" group, Priority connected group | done |
| Move to another list (one task) | List chip on the task page | done |
| Duplicate, archive, delete from the page | Floating toolbar on the task page, with Undo | done |
| Filters and progress | All · Overdue · Today · Upcoming · Done, hero ring | done |
| Repeat rules, birthdays | Repeat sheet | done |

## To design

| # | Item | Today in 1.3.1 | Notes | Status |
|---|---|---|---|---|
| 1 | **Multi-select** | Long-press; top bar "N selected", Select all/none; floating toolbar: Open, Duplicate (one), Complete (2+), Reschedule, Move to list, Archive/Unarchive, Delete; Undo on all | Swipe and long-press must not clash | done (v2 canvas) |
| 2 | **Reschedule** (one task) | ⋮ sheet → date sheet | Needs the date sheet (#5) | done (v2 canvas) |
| 3 | **Reschedule all overdue** | Button on the Overdue header | | done (v2 canvas) |
| 4 | **Row quick actions (⋮)** | Open, Mark done, Reschedule, Move to list, Duplicate, Archive, Delete | Keep a ⋮, or rely on swipe + long-press? | done: keep ⋮ for one task, long-press for many |
| 5 | **Date sheet** | Date / Duration modes, month grid, time row, reminder; Clear / Done | Shared by task page, Quick Add, reschedule, Repeat "Until", events | done (v2 canvas) |
| 6 | **Quick Add** | + → Task: one-line field with parser (dates, times, !priority, #list), Date/Remind/Repeat chips, priority menu, list picker | Default date depends on where it opens | done (v2 canvas) |
| 7 | **No date** and **Later** groups | Shown under the All filter | v2 All has only Overdue, Today, Upcoming, Done | done: sections in date order (All: …Later, No date; Upcoming: …Later) |
| 8 | **Lists inside Tasks** | Smart lists (Inbox, Today, Next 7 days, Completed, Archived), my lists, New list sheet, list detail with Open/Upcoming/No date/Done filters | Where Lists live in Tasks is undecided | done (v65): single-select connected group under the hero card; drawer only manages (New list, Edit lists, Archived, Trash) |
| 9 | **Archived list + Unarchive** | Smart list; Unarchive in multi-select | Part of #8 | done (v2 canvas) |
| 10 | **Trash list** | New in v2 | Restore, delete forever, days left, auto-empty after 30 days; lives with Lists | done (v2 canvas) |
| 11 | **Empty states per filter** | Overdue / Upcoming / Done / blank day messages | Only "All done" is designed | done (v2 canvas) |
| 12 | **Task search results** | Search pill; rows tick, swipe, multi-select | Search pill exists, results don't | done (v2 canvas) |
| 13 | **Reminder notification** | Done, Snooze 10 min | System notification; match the look | done (v2 canvas) |
| 14 | **Edit lists** | Not possible today (inventory §17) | Rename, recolor, reshape, reorder, delete | done (v46): rename, recolour, reorder, delete; no shape picker yet |

## Added in v2 (2026-11-26)

| Item | Status |
|---|---|
| **Inbox**: permanent first list, default for new tasks, can't be deleted; tasks from a deleted list move here | done (v61) |
| **List filter row**: single-select, combines with the view group | done (v65) |
| Date sheet: Today/Tomorrow/No date picks, collapsible Date and Time group, M3 time dial, calendar like the Calendar tab | done (v60) |
| Time format: 12-hour default, 24-hour option | done on the canvas (tweak); Settings row not designed |
| New list shape picker (official shapes only) | todo |
| Focus entry point | open: removed from Tasks; owner to say where |

## Order

The Tasks tab is done and approved (canvas v65, 2026-11-26). Left: the shape picker, plus the open items above. Then Habits, Calendar, Finance and the Account page.

---

# Habits tab tracker

Canvas version 87 is live (2026-11-27). v85 was the last version before H1–H8. The revert point for the Tasks-only design is `research/canvas-v65-revert/`. Statuses as above.

## Done on the canvas (v66–v85)

Week card (coloured rings, Cookie-9 for a full day), the habit shape as the check (outline → fill), three day states (pending / done / not done), swipe right = done and left = not done, a stable sort, the habit page (streak hero, stats, Month and Year heat maps, notes, pause), the icon picker, count / time / yes-or-no goals with a log drawer and timer, times a week, the reminder time dial, and the focused Reminder and Repeat drawers. Details are in [redesign-handoff.md](redesign-handoff.md), Session 8.

## Owner's list from 2026-11-26 (done 2026-11-27, canvas v87)

| # | Item | What the owner asked | Status |
|---|---|---|---|
| H1 | **Count swipe = one step** | After "not done", swiping right on a count habit should add one step (✕ → 1 of 8), not fill it. Built and tested in a scratch copy but **not published**, and the scratch copy is gone after the session. Redo it in `setDay`: when `v && isCount(h)`, start from 0 if the day was "not done" (else the current count), add `stepOf(h)`, cap at the goal. | done (v87) |
| H2 | **Simpler Goal UI** in New / Edit habit | Strip the Goal section down. Remove the hint box ("Each tap adds… Hold the habit…" and the Time note); the owner called it ugly. | done (v87) |
| H3 | **Time goal picker** | No fixed 5 / 10 / 15… chips. Use a slider from 5 min to 5 h, or a better M3 way. Research first: the M3 Expressive Slider (with value indicator and stops) is the likely fit; a duration wheel is the other option. | done (v87) |
| H4 | **No repeated info in section headers** | Drop "· 10 min a day" after Goal and "· Every day" after Repeat on. The picked chip already says it. | done (v87) |
| H5 | **Group the sheet into two segmented groups** | Group 1: Shape and Colour. Group 2: Goal, Repeat on, Reminder and time. Use the bubbly segmented groups (28/8 corners, 3dp gaps) used elsewhere. | done (v87) |
| H6 | **Reminder row text** | Show only the time under "Remind me" (e.g. "7:45 AM"), not "On due days at 7:45 AM. It has a Done button". | done (v87) |
| H7 | **Confirm before deleting a habit** | Delete (page toolbar and sheet) opens a confirmation drawer, like "Empty trash?": the habit name, its check-in count, Cancel and Delete (errorContainer). Keep Undo after. Drawers, not dialogs (CLAUDE.md rule 12). | done (v87) |
| H8 | **Bouncy neighbour spring-back** | When a swiped row lets go at 30%, the rows above and below should bounce a little as they settle, not just slide back. They use the `--fs` spring now; try an Expressive spatial spring with visible overshoot (e.g. damping 0.6–0.7), no hand-written curves beyond the motion tokens. Applies to tasks and habits. | done (v87) |

## Still open from before (updated 2026-11-27, canvas v88)

| Item | Status |
|---|---|
| Habit reminder notification (Done; skipped if already checked) | done (v88): heads-up with Done, tap opens the habit page |
| Multi-select (long-press) with actions | done (v88): Edit and Log (one habit), Pause/Resume, Archive/Unarchive, Delete (confirm drawer), FAB Done/Reset for the picked day |
| Reorder habits | done (v88): drag handles while selecting; the list shows your own order then |
| Habit widgets | done (v88): own board. Habits today 4×2 (shapes as checks) plus 2×2 single-habit widgets for a count and a time habit |
| Boards: empty, Edit habit, archived habit, past day | done (v88) |
| Habit search results (the search pill on Habits opens task search) | todo |
| Week rings vs week heat map: which one stays | open |

Holding a count or time habit used to open the log drawer. Long-press now selects instead; logging moved to the habit page's Today row and the toolbar's **Log**.

## Rule added by the owner (2026-11-26)

**Say less; let the control explain itself.** Imply the action from the UI instead of explaining it in text: no hint boxes, no helper sentences, no repeating a value in a header that a chip or field already shows. Keep only the text a person needs to act.

---

# Calendar tab (designed 2026-11-27, canvas v89)

Owner's brief: Month as in the code; Day = the hours with events in the scroll; Week = a week strip with the picked day's hours below. Done on the canvas: header (month, year, Month/Week/Day connected group, Today, previous/next), month grid + day hero + agenda, sticky week strip, the hour timeline (56dp/hour, lanes, now line, all-day strip, opens an hour before now), New event drawer (title, when → date sheet, length group, calendar chips), calendar access card. Boards on the row at y=8828.

Done through v101 (owner: Calendar tab design complete, 2026-11-28): event page, edit/duplicate/delete with repeat scope, swipe and multi-select in the day list, drag to move/resize, calendar drawer (lists, tasks, phone calendars), search, clash warning, event reminder, Join/Directions links, Next up widget with events. Left out on purpose: events on the Tasks tab. Not done: selecting blocks in Week/Day, onboarding calendar ask.

## Components from the 1.3.1 code

Source: `app/.../ui/calendar/CalendarScreen.kt`, `TimeGrid.kt`, `NewEventSheet.kt`, `data/calendar/PhoneCalendar.kt`.

1. Header: month title, year below, the search pill, a **view menu** pill (Month / Week / Day, then Today, Refresh) and previous/next tonal icon buttons.
2. **Month grid**: six weeks, Monday first, 12dp rounded cells, today on a Cookie-9, the picked day in secondaryContainer, dots for tasks (primary) and events (tertiary), swiped month to month.
3. **Day hero**: spinning Sunny with "FRI 27", plus "Today · 3 tasks · 2 events".
4. **Day agenda**: events as read-only segmented rows (time range · calendar name), then the day's tasks as normal task rows (tick, swipe, long-press select).
5. **Empty day**: "Nothing planned".
6. **Week view**: a pager of day headers (tap a day for Day view), an all-day strip ("+N" when full), a 24-hour grid (56dp per hour) with a now line, blocks (events tertiary, tasks in their list colour, 30 min, faded when done), overlaps in lanes, and a header that collapses as you scroll.
7. **Day view**: the same grid for one day.
8. **Permission card**: "See your calendar here" plus Allow calendar access, in every view.
9. **New event sheet**: title, a date/time pill (opens the date sheet), length connected group (All day, 30 min, 1 hr, 2 hr), a calendar picker menu, and a primary send button. Variants: no permission, and no writable calendar.
10. Task multi-select on the agenda (shared top bar and toolbar).
11. "+ → Task / Event" lands on the picked day.

Gaps in 1.3.1: events can't be opened, edited or deleted; event blocks can't be tapped; there's no event detail.
