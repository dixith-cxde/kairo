# Kairo — what the app does today

*Inventory of Kairo 1.3.1, written 2026-11-24 as the starting point for the UI redesign.*

This document records every user-facing feature in the current build, screen by screen. I wrote it by reading the source, not from the old design canvas, which was deleted. It describes **behaviour**, not looks: what each screen shows, what the user can do there, and which rules decide the result. The redesign can change every visual detail. Anything listed here should keep working, or be dropped on purpose.

The last section lists the gaps: things the data layer supports with no UI yet, and actions that have no undo. These are the open questions for the redesign.

---

## 1. The app at a glance

Kairo is a local-only Android app for tasks, habits, calendar and money. It has no account and no backend. All data lives in a Room database on the phone, and settings live in DataStore. It's written in Kotlin with Jetpack Compose and Material 3 Expressive, about 13,000 lines across 75 source files.

There are five tabs, always in this order:

| Tab | Purpose |
|---|---|
| **Today** | What's due now: overdue, today, habits, later, no date |
| **Lists** | Smart lists and the user's own lists |
| **Calendar** | Month, week and day views of tasks plus the phone's calendar events |
| **Money** | Accounts, transactions, budgets, bills and insights |
| **Stats** | Task-completion trends |

Four more screens open on top of the tabs: **Settings**, **Focus timer**, **Search** and **Task detail**. Money adds four detail screens of its own: **Account**, **Budgets**, **Insights** and **Bills & subscriptions**.

---

## 2. App shell

### Navigation

The tabs sit in a horizontal pager, but swiping between them is turned off. The user changes tab only from the nav bar. Detail screens are pushed on a back stack that survives process death. Back pops a detail screen. On any tab other than Today with nothing stacked, back returns to Today.

### Floating nav bar and the + button

The nav bar is a floating, icon-only pill joined to a solid **+** button, the design carried over from Stash. The selected tab gets a pill indicator and a filled icon. Tapping a tab closes any open detail screens and animates to that tab.

When the user scrolls down, the pill hides and the + grows into an 80dp FAB. Scrolling up brings the pill back, and so does any tab or screen change. The whole group slides away while a selection is active and on the Focus screen.

Tapping **+** turns it into × and opens a menu over a dimmed scrim:

- **Task** opens Quick Add.
- **Event** opens the New event sheet.
- **Expense**, **Income** and **Transfer** switch to Money and open the Add transaction sheet. These three appear only when at least one account exists.
- **List** switches to Lists and opens the New list sheet.

### Tab header

Every tab uses the same header: a large title, an optional subtitle, action buttons, and a full-width **Search** pill that opens the Search screen.

### Snackbar and undo

There is one snackbar for the whole app, shown just above the nav. Most destructive or completing actions show it with **Undo**: complete, delete, archive in bulk, delete a budget, delete a bill. After adding a task it confirms where the task went: *Added to Inbox*, *Added for today*, *Added for tomorrow* or *Added for Fri, 28 Nov*.

### First run

A new user first sees a three-page welcome pager:

1. *Your day, in one place*
2. *Money, without the noise*
3. *Yours, and only yours*

Each page has Skip, Next and Get started. After that, a spotlight tour runs on Today. It walks through the hero ring, the filters, search, the focus timer, settings, the + button and the tabs, and it explains the row swipes.

### Opening from outside the app

Widgets and notifications open the app with an internal action that jumps straight to the right place:

- **Quick Add**, with text pre-filled after voice input.
- **Voice add**, which starts the system speech recogniser.
- **Add expense**, **Add income**, **Add transfer**.
- **Focus**, **Stats**.
- **A specific task.**

The manifest declares no launcher shortcuts and no share targets.

---

## 3. Today

Today is the home screen. Its header shows the date and two buttons, **Focus timer** and **Settings**.

### Filters

A connected button group switches between **All**, **Overdue**, **Today**, **Upcoming** and **Done**.

### Hero card

At the top is the hero: a progress ring of tasks done against tasks due today, which counts up when it appears. The title changes with progress:

- *Nothing due today*
- *N to go today*
- *All done for today*, with a check badge and the line *X of Y finished. The rest of the day is yours.*

Below the title, *Next: <task> · <time>* names the first timed task still open.

### Sections

What appears depends on the filter:

| Section | Shown on | Contents |
|---|---|---|
| **Overdue · N** | All, Overdue | Open tasks past due. A **Reschedule** button moves all of them at once. |
| **Today · N left** | All, Today | Open tasks due today, sorted by time, untimed last. |
| **Habits** | All, Today | The habit list (see §8). |
| **Later · N** | All | Every open task dated after today. |
| **No date · N** | All | Open tasks with no due date. |
| **Upcoming** | Upcoming | The next 7 days, grouped under day headers. |
| **Completed** | All | A collapsible group of tasks finished today. |
| **Done today · N** | Done | Tasks finished today. |

### Empty states

A fully blank day shows *A clear day: Nothing planned for today. Add a task with + or say it to the Quick add widget.* Each filter also has its own empty message:

- Overdue: *Nothing overdue*
- Upcoming: *Nothing in the next 7 days*
- Done: *Nothing done yet today*

---

## 4. The task row and its actions

One task row is used everywhere: Today, list detail, Calendar and Search.

### What a row shows

- A checkbox tinted by priority.
- The title, struck through when done.
- A meta line, built from these parts in order:
  - When the task is due. On overdue rows this reads *Yesterday*, *N days ago* or a date, in the error colour. Otherwise it's the time, if one is set.
  - The list's shape dot and name.
  - Subtask progress, as *d/t*.
  - A repeat icon, a bell when a reminder is set, and a red flag for high priority.
- A trailing **⋮** button.

### Gestures

| Gesture | Result |
|---|---|
| Tick the checkbox | Completes the task with a burst animation and an Undo snackbar |
| Tap | Opens Task detail. While selecting, it toggles selection instead. |
| Long-press | Starts multi-select |
| Swipe left | Completes |
| Swipe right | Deletes, with Undo |

### The ⋮ sheet

A bottom sheet titled with the task's name offers:

- **Open**
- **Mark done** / **Mark not done**
- **Reschedule**
- **Move to list**
- **Duplicate**
- **Archive**
- **Delete**, in red

### Multi-select

While selecting, a top bar shows *N selected*, a clear button and **Select all / Select none**. A floating toolbar replaces the nav. It offers:

- **Open** and **Duplicate**, when exactly one task is selected.
- **Complete**, when two or more are selected.
- **Reschedule** and **Move to list**.
- **Archive**, which becomes **Unarchive** inside the Archived list.
- **Delete**.

Every bulk action has Undo.

---

## 5. Lists

### Smart lists

Five built-in lists appear as tiles with a count:

| Smart list | Contents |
|---|---|
| **Inbox** | Open tasks with no list |
| **Today** | Open tasks due today or earlier |
| **Next 7 days** | Open tasks due tomorrow through a week out |
| **Completed** | Every finished task |
| **Archived** | Archived tasks. These are hidden everywhere else in the app. |

### My lists

User lists appear below the smart lists. Each row shows a shape badge with the list's first letter, the name, and the count of open tasks. **New list** opens a sheet with:

- a live badge preview
- a name field
- eight shapes: Hexagon, Triangle, Square, Pentagon, Gem, Cookie 6, Cookie 9, Clover
- seven colours: Indigo, Coral, Amber, Green, Teal, Pink, Violet

### List detail

A list shows its badge and title, plus a filter group: **Open**, **Upcoming**, **No date**, **Done**. The filter is hidden for Completed and Archived, which have nothing to filter. Tasks are grouped into Done, Overdue, Today, Upcoming and No date, and sorted by date, then time. Each filter has its own empty state. For example, Archived says *Archive a task from its ⋮ menu to tuck it away here.*

---

## 6. Task detail

Task detail has no Save button. Every change is saved as soon as it's made.

From top to bottom:

- **List chip.** Opens a menu to move the task to another list.
- **Done checkbox and editable title.**
- **Due pill.** Reads *Due in 2 h 15 min*, *Due today*, *Due tomorrow*, *Overdue* or *Overdue · 3 d*.
- **Date and Time rows.** Both open the date sheet.
- **Repeat row.** Opens the repeat sheet. If the task has no date, setting a repeat gives it the rule's first date.
- **Reminder switch.** The first time it's turned on, the app asks for notification permission.
- **Priority row.** Each tap cycles None → Low → Medium → High.
- **Notes.**

A floating toolbar at the bottom has **Date**, **Reminder**, **Delete** and a **Done** button that goes back.

---

## 7. Quick Add and the shared sheets

### Quick Add

Quick Add is a single-line field labelled *New task in <list>*, with the hint *e.g. Call Mom tomorrow 7pm !high #Family*. As the user types, the words it understands are highlighted. Below the field:

- **Date chip**, which opens the date sheet.
- **Remind chip**, on by default.
- **Repeat chip.**
- **Priority menu.**
- **List picker.**

Send is enabled once the title isn't blank, and the sheet closes after adding.

The default date depends on where Quick Add was opened: today from the Today tab, the selected day from Calendar, and no date inside a regular list.

### What the parser understands

Words it recognises are removed from the title.

| Kind | Examples |
|---|---|
| Relative days | today, tonight (8 pm), eod (6 pm), tomorrow and its common misspellings (tmrw, tmr, 2moro…), day after tomorrow, overmorrow |
| Spans | next week (Monday), next month (the 1st), this weekend, next weekend, *in 3 days / 2 weeks / 1 month* |
| Weekdays | mon … sun, optionally with *next*, *this* or *on* |
| Times | 7pm, 7:30 am, 19:00, *at 5* (1–7 is read as pm), morning, noon, afternoon, evening, night, midnight |
| Priority | !high / !h / !1, !med / !m / !2, !low / !l / !3 |
| List | #listname, matched without regard to case |

A time with no date means today. A #tag that doesn't match any list is still removed from the title, and the task goes to the default list.

### Date sheet

The date sheet is shared by Quick Add, Task detail, rescheduling and new events. It has two modes:

- **Date:** Today, Tomorrow, Next Mon, No date.
- **Duration:** In 1 hour, In 3 hours, Tonight, Weekend.

Below the modes:

- **Month grid.** Days with tasks can show dots.
- **Time row.** Expands an inline time picker.
- **Reminder switch.** Reads *At the due time*, or *At 9:00 AM on the day* when there's no time.

The buttons are **Clear** and **Done**.

### Repeat sheet

Quick picks:

- Doesn't repeat
- Every day
- Weekdays
- Every <weekday>
- Every month on day N
- Every year

**Custom** sets *every N* days, weeks, months or years, with weekday toggles for weekly rules.

Completing a repeating task creates the next occurrence. Undoing the completion removes that occurrence again, so no duplicate is left behind.

---

## 8. Habits

Habits live inside Today rather than in a tab of their own. The section header reads *Habits · done of N* and has a **New habit** button. Each row shows a shape dot, the name, the streak (*12-day streak*) or *Daily*, and the reminder time. A check on the right marks today done.

**New / Edit habit** has a name and an optional daily reminder time, on at 09:00 by default. New habits get a random shape and colour.

**Habit detail** shows:

- three stat tiles: current streak, best streak, and this month's count
- a Monday-first heat map for the month, with month navigation that doesn't go past the current month

Past days and today can be tapped to check or uncheck them. The detail sheet also has **Edit** and **Delete**.

A habit's streak isn't broken while today is still unchecked; the count starts from yesterday until the day ends. The reminder notification has a **Done** button and is skipped when the habit is already checked.

---

## 9. Calendar

The header shows the month and the year, plus a **View** menu: **Month**, **Week**, **Day**, **Today** and **Refresh**. Previous and next arrows step by one unit of the current view.

### Month view

A swipeable six-week grid, Monday first. Dots mark days with tasks (primary colour) and with events (tertiary colour). Below the grid, a day hero reads *Today · 3 tasks · 2 events*. Under it come that day's events, which are read-only, and its tasks as full task rows.

### Week and Day views

A 24-hour time grid with a *now* line, opened scrolled to about an hour before the current time. Above the grid, an all-day strip holds all-day events and untimed tasks. Timed tasks appear as 30-minute blocks in their list's colour. Overlapping blocks share the width in lanes, and done tasks are faded. Tapping a task opens its detail. Events can't be tapped.

### Phone calendars

Events are read through Android's Calendar Provider, with no Google sign-in. Until access is granted, a card says *See your calendar here* with an **Allow calendar access** button. The view reloads by itself when the phone's calendar changes.

### New event

The New event sheet has:

- a title
- a date and time, from the date sheet
- a length: All day, 30 min, 1 hr or 2 hr
- a calendar, chosen from the ones that accept new events

Saving writes the event with a 10-minute alert. If no calendar accepts new events, the sheet explains how to add a Google account.

---

## 10. Money

All amounts are in rupees with Indian grouping (₹1,23,456). Paise are shown only when they aren't zero.

### Money tab

The header shows the **total balance** and three tiles for the current month: **Income**, **Spent** and **Budget**. The Budget tile reads *Left of ₹X*, *Over ₹X by*, or *Set up* when there's no budget yet. A switcher chooses between three views:

- **Activity:** every transaction, grouped by day. A transfer appears once.
- **Accounts:** each account with its balance, plus **Add account**.
- **Plan:** three cards. **Budgets** shows how much is left. **Bills & subscriptions** shows the next three due. **Insights** shows the top spending category this month.

Long-pressing a transaction starts multi-select. The toolbar offers **Duplicate for today**, for one non-transfer, and **Delete** with Undo.

With no accounts, the tab shows *Start with an account*.

### Account screen

The account screen shows:

- the balance, with a 30-day balance sparkline
- this month's **In** and **Out**
- buttons to add an expense, income or transfer for this account
- the bills paid from this account
- the account's transactions, grouped by day

The pencil button edits the account or deletes it. Deleting asks for confirmation, lists what will be lost, and can't be undone.

### Budgets

One **monthly budget** covers all spending. It's drawn as a liquid jar, with a hint like *12 days to go · ≈ ₹850 a day*. Below it, **By category** and **By account** budgets each show a progress bar, and over-limit rows are highlighted. Each scope can hold only one budget. Deleting a budget has Undo.

### Insights

A donut chart of this month's spending by category, with a pill comparing it to last month (*12% less than last month*). Tapping a category highlights its slice and dims the others.

### Bills & subscriptions

The header shows the monthly total, with counts of bills and subscriptions. Each row shows the item's name, its kind and its due date, which turns red when overdue. A **Pay ₹X** button (*Got ₹X* for income) records the transaction and moves the due date one month on. There are three kinds: Bill, Subscription and Income.

### Add transaction sheet

- A kind switch: **Expense / Income / Transfer**.
- A large amount field, with the number keyboard open and live ₹ grouping.
- For expense and income:
  - a **category** picker showing the three most used, plus New and All. A category is required for expenses.
  - an **account**
  - an optional **title**
  - **tags**
  - **Repeats monthly**, which also creates a bill
- For transfers: **From**, a swap button, and **To**.
- The Save button names what's missing (*Pick a category*), or reads *Save expense*.

### Accounts and categories

**Account sheet:** name, type (Bank, Cash, Card, Wallet), colour and starting balance.

**New category sheet:** name, one of 22 icons, and a colour.

### Ivy Wallet import and export

Settings → Data can import an Ivy Wallet CSV and export Kairo's transactions in the same format.

The import:

- reads UTF-8 or UTF-16 files, separated by commas or tabs
- understands several date formats
- creates missing accounts and categories as it goes
- skips duplicates, so it's safe to run twice

The export writes one row per transaction, with stable IDs. Tags and bills are not part of either direction.

---

## 11. Focus timer

The focus timer is a Pomodoro cycle of focus, then break, repeated for a set number of sessions. It survives the app being closed.

The screen has:

- an optional task (*Focus on…*)
- three clock faces: **Ring**, **Liquid** and **Minimal**
- session dots
- **Stop**, **Start / Pause** and **+5 min**
- the line *Focused today*

Tapping the clock while it's stopped opens the timer sheet. It has presets (15·3, 25·5 and 50·10) and sliders for focus length (5–300 min), break length (1–30 min) and sessions (1–8).

While running, the timer shows an ongoing countdown notification. When a phase ends, a separate notification says the session is done and a break is starting, that the break is over, or that all sessions are done. Each finished session is saved with its task.

---

## 12. Stats

Stats has a **Week / Month / Year** range and these cards:

- **Tasks done**, with the % change from the previous period
- **Done per day / week / month**, as bars with the best one highlighted
- **On time**: the share of tasks finished by their due date
- **Streak**: days in a row with at least one task done
- **By list**: completed tasks per list
- **Heat map** of the current month

Stats is read-only, apart from the range switch. Focus minutes don't appear here yet.

---

## 13. Search

Search opens from the pill on every tab. One field searches three kinds of thing:

- **Lists**, by name.
- **Tasks**, by title or notes. Archived tasks are left out.
- **Money**, by note, tag or category.

Results behave like everywhere else: task rows can be ticked, swiped and multi-selected, and transactions can be selected.

---

## 14. Home-screen widgets

| Widget | Size | What it shows and does |
|---|---|---|
| **Today** | 4×2 | *X of Y done*, today's open tasks with checkboxes that complete them in place, and + for Quick Add |
| **Progress** | 2×2 | A ring with *N left today* or *all done* |
| **Money** | 2×2 | Spent this month and % of the budget, plus an **Expense** button |
| **Quick add** | 4×1 | An *Add a task* field and a mic. When wide enough, it also has **Add expense** and **Start focus**. |
| **Next up** | 2×1 | The next timed task or calendar event, with *in 12 min* |
| **Streak** | 1×1 | The day streak inside a Sunny shape |
| **Stats** | 4×2 | Seven-day bars, the change from last week, the streak and on-time % |
| **Quick actions** | 4×1 | Task, Expense, Income, Transfer and Focus buttons, as many as fit |
| **Timer** | 2×2 | The live focus countdown, with Start/Pause and Stop that work in place |

Widgets follow the app's theme setting. When app lock is on, the Money widget hides its amounts.

---

## 15. Settings

| Section | Options |
|---|---|
| **Appearance** | Colours from wallpaper (on/off) · Seed colour: Default `#79D0FD`, Indigo, Clay, Moss, Rose, Teal, Violet · Theme: System / Light / **Dark** (default) · Contrast: Standard / Medium / High |
| **Privacy** | App lock · Lock after Instant / 1 min / 5 min · Block screenshots |
| **Reminders** | Shows whether exact alarms are allowed, and links to the system setting |
| **Data** | Import from Ivy Wallet · Export as Ivy Wallet CSV |
| **Calendar** | An info row explaining that calendars are read with no account |
| **About** | App updates · Credits & licenses · Kairo's licence (CC BY-NC-SA 4.0) |

**App lock** uses the phone's fingerprint, face or screen lock. Turning it on requires a screen lock to be set and a successful unlock. While it's on:

- nothing of the app is drawn under the lock screen
- reminders hide task names on the lock screen
- the recents preview is hidden

**Updates** come from the project's GitHub releases. A sheet shows the release notes, then downloads and installs in place.

---

## 16. Notifications

| Notification | Actions |
|---|---|
| Task reminder, at the task's time or 9:00 on the due date | **Done**, **Snooze 10 min** |
| Habit reminder, daily at the habit's time | **Done** |
| Focus countdown (ongoing, silent) | Tap to open Focus |
| Focus phase finished | None |
| Update ready | Opens the update sheet |

Notification permission is asked the first time a reminder is turned on, not at launch. Alarms are set again after a reboot, an app update, or a time or time-zone change.

---

## 17. Gaps and open questions for the redesign

These are places where the code and the UI don't match, or where an action is missing. The redesign should decide on each one.

### Supported by the data layer, no UI yet

- **Lists** can't be renamed, recoloured, reordered or deleted.
- **Subtasks** are counted on rows, but they can't be added or edited anywhere.
- **Habits** can't be archived or reordered, and their shape and colour can't be chosen.
- **Money accounts** can't be reordered.
- **Categories** can't be edited or deleted.
- **Calendar events** are read-only. They can't be edited or deleted.

### Missing in Money

- An existing transaction can't be edited.
- A transaction can't be backdated. It's always saved with today's date.
- There's no period switcher: everything shows the current month.
- There are no money filters, and no search inside Money.
- Only one currency is supported, even though the account record has a currency field.
- Bills have no reminder control and no "skip this month".

### Actions with no undo

- Archive from the ⋮ sheet.
- Delete from Task detail.
- Delete from Habit detail.
- Pay on a bill.
- Delete an account, which asks for confirmation first.

### Backup

There's no full backup or restore. The only export is the money CSV, plus Android's own Auto Backup.
