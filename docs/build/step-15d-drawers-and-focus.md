# Drawers + Focus (2026-11-21)

## Every dialog is now a drawer
- **Date & reminder drawer** (`ui/common/DateSheet.kt`, canvas DatePicker board): Date / Duration connected group; four big quick picks (Today, Tomorrow, Next Mon, No date) — the picked one turns solid primary and its corners tighten full → 16dp; Duration mode offers In 1 h / In 3 h / Tonight / Weekend; month grid in a 28dp container with tertiary dots on days that have tasks (months slide); Time row expands an inline M3 `TimePicker`; Reminder switch; Clear + Done.
- Used by: task detail (Date/Time rows and toolbar), quick add's date chip, Today's **Reschedule** (moves every overdue task), and a new **Reschedule** action in the selection toolbar.
- **New list** is a drawer with a live preview badge that morphs as you pick a shape and color.
- No `AlertDialog`, `DatePickerDialog` or `TimePickerDialog` left in the app. Recorded as rule 12 in CLAUDE.md.

## Focus timer (phase 2, pulled forward)
- Open from the **timer icon on Today** (or tap the focus notification).
- Canvas Focus board: task chip (pick a task in a drawer, optional), a 300dp **Cookie12 that turns slowly** with a thick **wavy progress ring**, 72sp tabular time, "of 25 min · session 2 of 4", session dots (current one grows), **Stop · Start/Pause (large, press-morphing) · +5**, "Focused today: 1 h 40 min".
- Breaks switch the hero to tertiary colors. Settings drawer: focus 15/25/45/60, break 5/10/15, sessions 2/4/6.
- **How it runs without draining battery**: the state is just "this phase ends at T", saved to SharedPreferences, so it survives the app closing. An exact alarm ends each phase; the ongoing notification counts down on its own (`setUsesChronometer`), so there's no foreground service and no polling. When a focus phase ends: haptic (it's a completion), "done" notification, session saved to Room (v4 `focus_sessions`), break starts.

## Not done yet
- Focus history screen and focus stats (sessions are being recorded for it).
- Canvas not updated with this round.
