# Data and calendar

Everything lives on the phone. No Kairo server, no account, no analytics.

## Storage

- **Room** (SQLite) for tasks, lists, money, habits.
- **DataStore** for settings (theme, seed, contrast, reminder defaults, chosen calendar).
- **Backup:** Android Auto Backup of the database plus a manual "Export / Import" file (JSON). No custom cloud.

## Data model (first draft)

**Tasks**
- `List` — id, name, color key, shape key, sort order, archived.
- `Task` — id, listId, title, notes, dueDate, dueTime, priority (none/low/med/high), reminder offsets, repeat rule (RRULE text), completedAt, sort order, calendarEventId (if written to a calendar).
- `Subtask` — id, taskId, title, done, order.
- `Tag` + `TaskTag`.

**Money**
- `Account` — id, name (Personal, Shared, Savings, Cash …), type, color, opening balance, currency.
- `Category` — id, name, icon, color, shape, kind (expense/income).
- `Transaction` — id, accountId, kind (expense/income/transfer), amount (integer paise, never floating point), categoryId, date, note, tags, transferPeerId (other leg of a transfer), recurringId.
- `Budget` — month, categoryId (null = overall), limit.
- `Recurring` — rule, amount, account, category, next due, reminder, kind (bill/subscription/income).

**Phase 2:** `FocusSession`, `Habit`, `HabitCheck`.

Balances and stats are computed with queries, not stored, so they can never drift.

## Reminders

- `AlarmManager.setExactAndAllowWhileIdle` for timed reminders (needs `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` rules — show the status in Settings, as designed).
- `WorkManager` for the morning summary, recurring bill creation, widget refresh.
- Notifications with actions: Complete, Snooze. Re-schedule after reboot (`BOOT_COMPLETED`).

## Google Calendar via Calendar Provider

**No sign-in.** Android already syncs the user's Google calendars; Kairo reads them through `CalendarContract`.

- **Permissions:** `READ_CALENDAR` to show events; `WRITE_CALENDAR` only if the user turns on "Add timed tasks to calendar". Ask at the moment the feature is turned on, with a short explanation screen.
- **Reading:** query `CalendarContract.Calendars` for the list (name, color, account, visible), then `CalendarContract.Instances` for a date range (handles recurring events). Show events in Today (next event), Calendar (tertiary dots and agenda rows) and the Next up widget. Events are read-only in Kairo.
- **Writing:** for a timed task, insert into `CalendarContract.Events` in the user's chosen calendar; store the event id on the task; update/delete it when the task changes or is completed (setting). Android syncs it up to Google.
- **Freshness:** register a `ContentObserver` on the provider; changes appear when Android has synced.
- **Not possible here:** Google Tasks (needs the Tasks API + OAuth) — out of scope; say so in Settings if asked.
- **Rejected alternative:** Google's secret iCal URL (read-only, needs internet, one calendar per URL, URL is a secret).

## Privacy promises (shown in Settings)

- Data stays on this phone (plus Android's own backup if enabled).
- Calendar data is read only on-device and never copied out of the app's display.
- No internet permission needed for v1 (to verify when the build starts; widgets, calendar and reminders all work offline).
