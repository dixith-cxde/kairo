# Screens

Every screen on the canvas, in dark and light. Navigation: **Today · Lists · Calendar · Money · Stats** in the floating nav, + FAB on the right. Focus opens from Today; Settings from Today's app bar.

## Tasks, lists & reminders

### Today (interactive)
- Large flexible app bar: "Today", subtitle "Monday, 16 November", search + settings.
- **Hero card** (primary container, 32 corners): wavy progress ring "3/9", "6 to go today", next task, next calendar event. A faint 12-sided cookie turns slowly behind it.
- **Overdue** section (error-colored header, Reschedule action).
- **Today** section sorted by time: checkbox tinted by priority, title, time, list shape + name, subtasks, reminder bell.
- **Completed** collapsible row with count.
- Completing a task plays the main hero animation.

### Quick add (sheet from the FAB)
- "New task in Family". Natural-language input ("Call Mom tomorrow 7pm !high #Family") highlights tokens inline as you type: date/time, priority, list.
- Parsed chips below (Tomorrow 7:00 PM, High, Remind, Repeat), then icon buttons (list, subtasks, tag, expand) and a big solid send button (72×56, 16 corners). System keyboard underneath.

### Task detail
- List chip (with list shape), checkbox + title, "Due in 1 h 20 min" tertiary pill.
- Grouped details: Date, Reminder, Repeat, Priority (burst icon for high), Calendar (switch "Added to Personal"). Icons in 4-sided cookie shapes.
- Subtasks with linear wavy progress, drag handles, add row. Notes.
- Floating toolbar (date, tags, attach, delete) + solid "Done" FAB.

### Date & reminder
- Sheet with a Date / Duration button group.
- Four big quick picks (Today, Tomorrow, Next Mon, No date); the picked one turns solid primary and its corners tighten from full to 16.
- Month grid in a 28-corner container (round day cells, tertiary dots for days with tasks), then Time / Reminder / Repeat rows, Clear + Done.

### Lists (create menu open)
- Four smart-list tiles (cookie4 icons, big counts).
- My lists, each with its own shape and color.
- FAB menu open: Task, Expense, Income, Transfer, List.

### List detail
- Filter chips, sections, a row being dragged (tertiary container, lifted) and its drop gap.

## Plan & review

### Calendar
- Month grid of rounded, gapped day cells; dots for tasks (primary) and phone-calendar events (tertiary). Today is a cookie-shaped number.
- View button group: Month / Week / Agenda.
- **Day hero:** sunny badge "MON 16" + "Today · 3 tasks · 2 events · free after 2 PM".
- Mixed agenda: tasks (checkbox) and events (tertiary icon), 12-hour times.

### Stats (interactive)
- Week / Month / Year button group.
- Total completed in editorial numbers + **badge that morphs and rotates** per range showing the change.
- Bar chart (best bar in tertiary), On-time wavy ring, streak card (sunny), By-list split bar, month heatmap.

### Settings
- Top card: app icon (cat mark), "Stored on this phone · no account", Backup.
- Appearance: wallpaper colors switch; six seed colors — **tapping one morphs it from circle to 9-sided cookie** and turns wallpaper color off; Theme (System/Light/Dark) and Contrast button groups.
- Calendar: show phone calendar events; add timed tasks to a chosen calendar.
- Reminders: default reminder, morning summary, exact alarms status.
- Icons in colored circles (Android 16 style).

### Focus (phase 2, from Today)
- The task being focused as a chip, a 300dp 12-sided cookie (slowly turning) holding a wavy ring and "18:42 of 25 min · session 2 of 4".
- Session dots, Stop · Pause · +5 controls, "Focused today: 1 h 40 min".

### Habits (phase 2)
- Habit rows with sunny-shaped done days, heatmap.

## Money

### Money (interactive)
- Account carousel (Personal, Shared, Savings, Cash) — cards expand on tap.
- Income and Spent tiles, budget strip, recent transactions grouped by date.

### Add expense (interactive)
- Expense / Income / Transfer button group, big amount with live ₹ grouping, custom keypad, category shapes that morph when picked, account + date + #tags, **scalloped success** on save.

### Transfer
- From / To account cards with a swap button, quick amounts, repeat.

### Account (Shared)
- Hero balance, filters, date groups, `#tags`.

### Budgets
- **Liquid jar hero:** a 9-sided cookie filled 79% with a moving wave; "₹8,360 left of ₹40,000 · 14 days to go · ≈ ₹597 a day".
- Per-category bars; over budget (Shopping) turns the row error container.

### Money insights (interactive)
- Donut of rounded, gapped slices; tap a slice or row to pull it out and dim the rest. Center is a slowly turning cookie that takes the category's color and icon.
- "12% less than October" pill, category list, daily spending bars against a dashed daily budget line.

### Recurring & bills
- Bills tied to reminders (due dates, paid state), subscriptions. Total ₹17,697 · 3 bills · 3 subscriptions.

## Cross-cutting boards

- **Foundations** — principles, palettes, type, corner scale, shapes.
- **Components** — every component in both themes.
- **Motion** — playable springs and the choreography specs.
- **Flexible app bar** (interactive) — At top / Scrolling / Collapsed / Scroll up, with the floating nav hiding and returning.
- **Widgets** — see [05-widgets.md](05-widgets.md).

## States still to design (before or during build)

Empty states (no tasks, no accounts), first-run / permission screens (notifications, exact alarms, calendar), search, undo snackbar, error states, tablet/foldable layout.
