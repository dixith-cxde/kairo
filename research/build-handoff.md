# Kairo v2 build — handoff

*Written 2026-11-28 at the end of the first build session. The v2 design is on the canvas (https://claude.ai/artifact/XuXAdkKY8P3N7g4Ps5GiHa); this file says what is built in code, how it is built, and what is next. The design notes stay in [redesign-handoff.md](redesign-handoff.md) and [redesign-tracker.md](redesign-tracker.md).*

## Start here (next session)

1. **Part 1 is committed** (390b351). Work after it (Finance drawer, Ivy import preview, v2 habit drawer; session 2) is uncommitted. Ask the owner before committing; commit author is `dixith-cxde`, never a Co-Authored-By line (memory).
2. **Build rules (owner):** every Kotlin file ≤ 300 lines (split by screen part); Gradle with `--max-workers=2`; no emulator; install the debug build on the vivo: `./gradlew :app:installDebug -q --max-workers=2`. Never a release build unless asked.
3. **Check on the phone** after each piece: `adb shell uiautomator dump` to find bounds, `adb shell input tap`, `adb exec-out screencap -p`. If adb says no device after a reconnect, `adb kill-server; adb start-server`. Don't save test data into the owner's real calendar/finance; if you add a test habit, delete it after.
4. **Read the canvas board before building a screen.** Copy `research/canvas/render.py` into the scratchpad with `project/Main.dc.html`, render a `startState` to HTML, screenshot it with `google-chrome-stable --headless=new`. Split `Main.dc.html` into helmet (CSS) / body (template) / logic (JS) to read exact sizes and rules.
5. **Name clash gotcha:** inside composables that have a local `app` (KairoApplication), never write `app.kairo.ui...` fully-qualified names; import instead (it fails to compile).
6. The vivo is **360dp wide**; the canvas boards are 390dp. Check every row/toolbar for overflow (labels clipped, numbers wrapping).

## Owner instruction (2026-11-28, end of session 2)

**Build all remaining designed UI and functionality steps back to back, without testing each step on the phone.** Compile-check each step (`./gradlew :app:compileDebugKotlin -q --max-workers=2`), install on the vivo once at the end of a batch, check usage after each step and stop at 99% weekly / 95% 5-hour. Don't commit unless the owner says so. Skip anything not designed on the canvas (budgets, categories/tags management, account page, onboarding).

Done so far in this run (compile-checked, installed, not checked on screen): habit notes (DB v11 `habit_notes`), habit select + drag reorder (`ui/common/Reorder.kt`, `HabitSelect.kt`), habits in Search, New account sheet (`NewAccountSheet.kt`, AccountType.UPI), task row ⋮ grouped menu (`TaskRowMenu.kt`; old `TaskOptionsSheet` now unused).

Also done: widgets no longer turn into the Timer (`redraw()` in `widgets/Widgets.kt` asks Android for each receiver's ids instead of Glance's stored map). Not yet confirmed on the phone over time; if a widget still switches, look at Glance session/state next.

Owner (2026-11-28, last message): build Tasks (Quick Add, task page, date + repeat sheets, Trash), Focus boards, Calendar items and Widgets (habit widgets too, "clear and persistent"), then commit and make a **2.1.0 release** (versionCode 7). Release signing is set up (keystore props).

Also done: **Trash** (DB v12 `tasks.deletedAt`; Delete = Move to trash with Undo; purge after 30 days on app start; `ui/lists/TrashScreen.kt` via `ListRoute(TRASH_ID = -6)`; drawer item with count; ⋮ Restore | Delete forever). KairoApp.kt is now exactly 300 lines: move code out before adding to it.

Order (remaining):
1. ~~Habits~~ done.
2. Tasks: ~~row ⋮ grouped menu~~ done; ~~Trash~~ done; v2 Quick Add; v2 task page; date sheet and repeat sheet restyle; Trash (keep deleted 30 days, needs a DB step; the row menu's Delete becomes Move to trash then). (List shape picker dropped: the canvas "new list" board has none; shapes are auto-assigned.)
3. ~~Finance: New account sheet~~ done.
4. Focus tab: all canvas Focus boards (pick, timer/range/custom, run ring/liquid/minimal, stop, done, settings, stats, session).
5. Calendar: Kairo-owned events (DB step, editable, repeat/link/location), multi-select, search, event reminders, drag to move/resize, time-zone row.
6. Widgets: canvas Widgets board (habit widgets too).

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
| Habit drawer | `ui/habits/HabitDrawer.kt`, `HabitDrawerParts.kt` | v2 New/Edit habit (replaces the 1.3.1 `HabitSheet`, same name): preview row (tap shape → 24-icon grid), name with clash check, Shape + Colour group, Goal (Yes/no · Count with amount + unit chips · Time with an M3 slider on the canvas stops), Repeat on (Every day / Weekdays / Times a week, day toggles or 1–6), Remind me (quick times + Other → Cookie dial), Delete habit row. Habit page's Reminder / Repeat rows open the focused one-item drawer (`part`). Draft survives rotation. Created + deleted a test habit on the vivo. |
| Finance drawer + Ivy | `ui/finance/FinanceDrawer.kt`, `IvyImportSheet.kt`, `data/money/IvyCsv.kt`, `IvyImport.kt` | ☰ on Finance: Bills, Reports, Add account (1.3.1 account drawer for now), Import (file picker → preview: counts, new accounts/categories, already-in-Kairo, planned payments → monthly bills switch), Export. Import now keeps time + description (v10); export writes them. Checked on the vivo with a test CSV, cancelled (nothing written). |

## Not built yet (designed on the canvas)

**Next up:** Finance extras need **design first** (not on the canvas): set budget, per-category budgets, categories & tags management, account page. Restyle Add account to the canvas "New account" sheet (name, Bank/UPI/Cash/Card/Wallet chips, balance now; Kairo has no UPI account type yet). Otherwise go on with Habits below.

Then, roughly in order:
- **Habits:** per-day notes; long-press select + reorder; habit search; habit widgets.
- **Tasks:** row ⋮ as the grouped DropdownMenuPopup (still the 1.3.1 sheet); Trash (needs "keep deleted 30 days" data); v2 Quick Add, task page, date sheet, repeat sheet restyle; shape picker for lists (official shapes only).
- **Calendar:** Kairo-owned events (in lists, repeat/link/location, editable) need a data step; drag to move/resize; multi-select; search; event reminders; time-zone row.
- **Focus tab:** the canvas Focus boards (ring / liquid / minimal run, pick, settings, stats, session) — the tab still shows the 1.3.1 focus screen.
- **Account page** (avatar): not designed; the avatar opens Settings for now. Onboarding: not designed (1.3.1 welcome still shows; the spotlight tour is off).
- **Widgets:** still 1.3.1.

## Known gaps / not verified

- Saving a new/edited event or transaction wasn't tested on the phone (to keep the owner's data clean). Same for actually running an Ivy import or export (preview only).
- In landscape, sheets keep their title and buttons outside the scroll, so the scroll area is short (usable, not designed).
- adb gotcha: the package is `app.kairo` (no `.debug` suffix); launch with `adb shell monkey -p app.kairo -c android.intent.category.LAUNCHER 1` and check `dumpsys activity activities | grep mResumedActivity` before tapping.
- Ivy planned payments always become *monthly* bills (Kairo bills are monthly only).
- Old 1.3.1 files over 300 lines still exist (CalendarScreen was rewritten; `ui/money/MoneyDetails.kt`, `MoneyScreen.kt`, `Widgets.kt`, `FocusScreen.kt`, `SettingsScreen.kt`, `StatsScreen.kt`…): split them when a step touches them. Unused 1.3.1 money screens (`MoneyScreen`, Insights) can be removed once Finance is complete.
- CLAUDE.md §1 still describes the design phase in places; the owner started the build on 2026-11-28.
