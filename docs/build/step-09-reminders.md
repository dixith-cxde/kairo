# Step 9 · Reminders

**Status:** built 2026-11-21, compiles. Not yet run on the phone. **Needs a real-device test** (alarms can't be checked by compiling).

**New ideas:** `BroadcastReceiver`, `AlarmManager`, notifications, runtime permissions.

## How it works

- A task with **Reminder on** and a date pings at its time, or at **9:00** if it has no time.
- `KairoApplication` watches the task table; every change re-syncs alarms (`ReminderScheduler.sync`). One alarm per task, keyed by task id, so rescheduling replaces it.
- Alarm fires → `ReminderReceiver` re-reads the task (ignores it if done or deleted) → notification on the "Task reminders" channel (high importance) with **Done** and **Snooze 10 min**.
- `ReminderActionReceiver` handles the buttons. Done marks the task complete; Snooze sets a new alarm.
- `RescheduleReceiver` restores alarms after reboot, app update, time or time-zone change (Android clears alarms on reboot).

## Permissions

- **Notifications** (Android 13+): asked the first time you turn a reminder on (task detail switch/toolbar, quick add Remind chip), not at launch. Your phone is Android 12, which doesn't ask.
- **Exact alarms**: `SCHEDULE_EXACT_ALARM`. If the user hasn't allowed it, Kairo falls back to an inexact alarm (Android may deliver it a few minutes late). A settings row to grant it comes with Settings / first-run screens.
- `RECEIVE_BOOT_COMPLETED` for the reboot case.

## Not yet (honest)

- No in-app prompt to allow exact alarms on Android 13+.
- Tapping the notification opens the app on Today, not on the task.
- The reminder time is fixed (at the due time); "remind me 30 min before" options aren't built.
