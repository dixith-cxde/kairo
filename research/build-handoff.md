# Kairo v2 build — handoff

*Written 2026-11-28 at the end of the first build session. The v2 design is on the canvas (https://claude.ai/artifact/XuXAdkKY8P3N7g4Ps5GiHa); this file says what is built in code, how it is built, and what is next. The design notes stay in [redesign-handoff.md](redesign-handoff.md) and [redesign-tracker.md](redesign-tracker.md).*

## Start here (next session)

1. **Nothing is committed.** All v2 work is uncommitted on `main` (last commit is 1.3.1). Ask the owner before committing; commit author is `dixith-cxde`, never a Co-Authored-By line (memory).
2. **Build rules (owner):** every Kotlin file ≤ 300 lines (split by screen part); Gradle with `--max-workers=2`; no emulator; install the debug build on the vivo: `./gradlew :app:installDebug -q --max-workers=2`. Never a release build unless asked.
3. **Check on the phone** after each piece: `adb shell uiautomator dump` to find bounds, `adb shell input tap`, `adb exec-out screencap -p`. If adb says no device after a reconnect, `adb kill-server; adb start-server`. Don't save test data into the owner's real calendar/finance; if you add a test habit, delete it after.
4. **Read the canvas board before building a screen.** Copy `research/canvas/render.py` into the scratchpad with `project/Main.dc.html`, render a `startState` to HTML, screenshot it with `google-chrome-stable --headless=new`. Split `Main.dc.html` into helmet (CSS) / body (template) / logic (JS) to read exact sizes and rules.
5. **Name clash gotcha:** inside composables that have a local `app` (KairoApplication), never write `app.kairo.ui...` fully-qualified names; import instead (it fails to compile).
6. The vivo is **360dp wide**; the canvas boards are 390dp. Check every row/toolbar for overflow (labels clipped, numbers wrapping).

## Decisions made this session (owner)

- **Nav pill is icons only**; the selected tab is a solid primary pill (CLAUDE.md rule 5 updated). Group is 16dp from the edges on 360dp.
- **Expressive time picker** everywhere (habit drawer, date sheet, event times): Kairo-drawn, Cookie-12 dial, wavy hand, M3 header boxes (`ui/common/ExpressiveTimePicker.kt`). M3 TimePicker can't change its dial shape.
- While the keyboard is open, a drawer shows the time as one line instead of the dial (the dial got squeezed).
- Habit/event/transaction pages hide the floating nav (they have their own toolbar); see `fullPage()` in `AppTabs.kt`.

## Database

- **v9** (habits): goal yes/count/time, target, unit, step, weekdays, perWeek, pause from/to, icon, hshape (MaterialShapes name), notes; `habit_checks.state` (1 logged / 0 not done) and `value`.
- **v10** (finance): `transactions.time`, `transactions.description` (`note` is the title).
- Settings (DataStore): `calHidden` (calendars drawer), `finExcluded` (accounts left out of the total).

## Built (v2) — all on the vivo, checked on screen

| Area | Files | Notes |
|---|---|---|
| Theme | `ui/theme/Color.kt`, `KairoColors.kt`, `Schemes.kt` | Google blue/green; chip, searchBg, chartBlue/Green tokens. Default seed ignores contrast. |
| Shell | `KairoApp.kt` (≤300), `AppTabs.kt`, `AppSheets.kt`, `ui/nav/FloatingNavBar.kt` | Tabs Tasks·Habits·Calendar·Finance·Focus; create menu inside the FAB; mic → Quick Add. `V2Page` hosts v2 detail pages. |
| Tasks | `ui/tasks/*` | Search bar, view group, hero, list row, sections, rows (swipe right complete / left delete). |
| Lists drawer | `ui/lists/ListsDrawer.kt`, `ListSheet.kt`, `EditListsScreen.kt` | New list, Edit lists (rename/recolour/↑/delete → tasks move to Inbox), Archived. |
| Habits | `habits/*`, `ui/habits/*` | Week rings card, shape-check rows, swipe done/not done, archived fold; habit page (stats, month/year history, timer, pause, delete confirm). |
| Calendar | `ui/calendar/*` | Month / Week / Day, summary card, week busy bars, hour timeline; event page (phone events, read-only); v2 New event drawer; calendars drawer. |
| Finance | `ui/finance/*` | Activity (spent card, bills row, filter, days), Accounts (total + tiles), add/edit drawer, transaction page, Reports (filters + bars + categories), Bills page. |

## Not built yet (designed on the canvas)

**Next up:** Ivy import (preview), Finance drawer (☰ hidden on Finance), set budget, per-category budgets, categories & tags management.

Then, roughly in order:
- **Habits:** v2 New/Edit habit drawer (icon, shape, colour, goal, repeat, reminder) — still the 1.3.1 drawer; per-day notes; long-press select + reorder; habit search; habit widgets.
- **Tasks:** row ⋮ as the grouped DropdownMenuPopup (still the 1.3.1 sheet); Trash (needs "keep deleted 30 days" data); v2 Quick Add, task page, date sheet, repeat sheet restyle; shape picker for lists (official shapes only).
- **Calendar:** Kairo-owned events (in lists, repeat/link/location, editable) need a data step; drag to move/resize; multi-select; search; event reminders; time-zone row.
- **Focus tab:** the canvas Focus boards (ring / liquid / minimal run, pick, settings, stats, session) — the tab still shows the 1.3.1 focus screen.
- **Account page** (avatar): not designed; the avatar opens Settings for now. Onboarding: not designed (1.3.1 welcome still shows; the spotlight tour is off).
- **Widgets:** still 1.3.1.

## Known gaps / not verified

- Saving a new/edited event or transaction wasn't tested on the phone (to keep the owner's data clean).
- Old 1.3.1 files over 300 lines still exist (CalendarScreen was rewritten; `ui/money/MoneyDetails.kt`, `MoneyScreen.kt`, `Widgets.kt`, `FocusScreen.kt`, `SettingsScreen.kt`, `StatsScreen.kt`…): split them when a step touches them. Unused 1.3.1 money screens (`MoneyScreen`, Insights) can be removed once Finance is complete.
- CLAUDE.md §1 still describes the design phase in places; the owner started the build on 2026-11-28.
