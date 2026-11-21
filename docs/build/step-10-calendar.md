# Step 10 · Calendar

**Status:** built 2026-11-21, compiles. Not yet run on the phone.

**New ideas:** content providers (reading another app's data), `produceState`.

## What's there

- **Month grid** in a 28dp container: rounded, gapped day cells; **today is a Cookie9 shape** in primary; selected day on secondaryContainer; dots = tasks (primary) and phone-calendar events (tertiary). Previous/next month in the app bar.
- **Day hero**: Sunny badge "SAT 21" on tertiaryContainer + "Today · 3 tasks · 2 events".
- **Agenda** for the selected day: events (tertiary icon, time range, calendar name) then tasks (same `TaskRow`: complete with burst, tap → detail).
- **Permission**: a card explains, then asks for `READ_CALENDAR` only when you tap Allow (docs/06: ask when the feature is used).

## How reading works

`PhoneCalendar` queries `CalendarContract.Instances` for the visible range (not `Events`, so repeating events expand to each occurrence). All-day events are read in UTC so they land on the right date. Google Calendar shows up because the phone already syncs it; Kairo never signs in.

## Not yet (honest)

- **Writing timed tasks to the calendar** (`WRITE_CALENDAR`, store the event id, update/delete with the task). It's an opt-in setting, so it lands with Settings.
- Month / Week / Agenda **button group**: only Month is built.
- "Free after 2 PM" in the day hero, and next event on the Today hero card.
- Events don't refresh live while the screen is open (they reload on month change or reopening).
