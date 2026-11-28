# Kairo v2 redesign — handoff notes

*Written 2026-11-24 at the end of a session, so the next session can continue without the chat. Updated the same day after a second session (task page, swipe, repeat sheet), on 2026-11-25 after a third session (the Tasks screens), on 2026-11-26 after sessions 4–7 (lists, widgets, official shapes, date sheet, list filter), and on 2026-11-27/28 after sessions 9–11 (Habits, Calendar, Finance). **Tasks, Habits, Calendar, Finance and Focus are designed; the Account page is the last big piece (canvas version 114).***

## Start here (next session)

1. **Design only.** No app code, no builds, no commits unless the owner asks. Only the canvas changes (see memory `design-only-phase`).
2. Read the live canvas before any edit (`Artifact` read with `paths` for `project/Main.dc.html` and `project/canvas.json`, plus `project/Widgets.dc.html` if you touch widgets). Compare with `research/canvas/project/` to spot owner edits. The backup equals live **version 114**.
3. Copy `research/canvas/gen.py` and `research/canvas/project/` into a scratchpad folder (`<root>/gen.py`, `<root>/project/`). Edit only `Main.dc.html` (and `Widgets.dc.html` for widgets). Run `python3 <root>/gen.py`, which writes every Main twin (about 70) plus `WidgetsLight` and `HabitWidgetsLight`. Publish with `url`, `root`, `file_path` = Main, and `files` = every twin (the index only when boards are added or removed). **Before the first publish of a session, read every file you will send by its path** (one `read` with `paths` listing all twins); otherwise the publish is refused as "haven't viewed the latest version" even when nothing changed. Then copy everything back into `research/canvas/project/`.
4. **Check visually before publishing.** `python3 research/canvas/render.py <board.dc.html> out.html '<props json>' '<state json>'` renders a board's template with its real `renderVals()`. Screenshot it with `google-chrome-stable --headless=new --window-size=390,844 --screenshot=out.png file://…/out.html` and look at the PNG. It's a rough renderer (no events), but it caught every layout bug so far: class-name clashes, overflow, wrapping. Also run `node research/canvas/harness.js` from the folder that holds `project/` (copy it into `<root>`): it runs `renderVals()` for every `startState` in both themes and prints errors. Duplicate `const` names inside one IIFE are the usual failure. To screenshot a scrolled board or one with the Week/Day auto-scroll, append a script to the rendered HTML, or replace `  calScroll() {` with `  calScroll() { return;`.
5. **Shapes:** only official MaterialShapes paths, never hand-drawn (memory `official-shapes-only`). They come from `kairo/Shape Set.svg`, already normalized in `research/canvas/m3shapes.json`, and the boards read them through `this.m3(name)`.
6. Publish after each change and say in a few lines what changed. The canvas itself can't be opened from the session, because the built-in browser isn't signed in to claude.ai.
7. Owner comments arrive as "Artifact comment sent to Claude". Replying in the thread was denied (ArtifactComments permission), so make the change and answer in the chat in one line.
8. Pick up from **Session 12: next session starts here** at the bottom (the older "Next steps" section is out of date).
9. Class names already used for something else bite silently: `empty` (empty state), `top` (header bars; `.sheet.top` now stretches), `sh`, `row`. Pick new, specific names.

## Where things stand

The owner accidentally deleted the original design canvas (claude.ai artifact `RthtmHKkEsUMm2WUMRUE49`), so it is gone. They decided to **redesign the UI** around Material 3 Expressive instead of rebuilding the old one. The work so far:

1. **[research/app-inventory.md](app-inventory.md)** lists every feature in Kairo 1.3.1, written from the source code. It's the checklist for the redesign: anything listed there either keeps working or is dropped on purpose. Section 17 lists the gaps (features with no UI, actions with no undo).
2. **A new Claude Design canvas, "Kairo v2 Design"**, at https://claude.ai/artifact/XuXAdkKY8P3N7g4Ps5GiHa. It's private to the owner. So far it covers the **app shell**, the **Tasks screen** (with swipe and undo), the **task page**, the **Repeat sheet**, and (session 3) multi-select, the row ⋮ menu, the date sheet, reschedule, Quick Add, search, empty states, Archived, Trash and the reminder notification, voice add, a notifications-off banner, and the Lists navigation drawer.
3. **A local backup of every canvas file** in [research/canvas/project/](canvas/project/), last saved in session 3 (canvas version 45), with the twin generator [research/canvas/gen.py](canvas/gen.py). If the live canvas is lost again, republish from here.

No app code has changed. Nothing is committed.

## The canvas

- Type: the Claude **Design** canvas type (`type_url` https://claude.ai/artifact/QKN21svewxgyPb6SYRqWnd). Don't create another canvas; always publish to the URL above.
- Files on the artifact (all 390×844 and interactive; the same files are in `research/canvas/project/`):

  | File | x | Board title | Opens in |
  |---|---|---|---|
  | `Main.dc.html` | 0 | Shell · Dark | the Tasks tab |
  | `Light.dc.html` | 470 | Shell · Light | the Tasks tab |
  | `TaskDark.dc.html` | 940 | Task page · Dark | the task page |
  | `TaskLight.dc.html` | 1410 | Task page · Light | the task page |
  | `RepeatDark.dc.html` | 1880 | Repeat sheet · Dark | the Repeat sheet |
  | `RepeatLight.dc.html` | 2350 | Repeat sheet · Light | the Repeat sheet |

  Rows 2 and 3 (added in session 3), each a dark/light pair 470px apart, pairs 940px apart, `title1` notes 260px above each row:

  | y | Pairs, left to right (`<Name>Dark` / `<Name>Light`) | startState |
  |---|---|---|
  | 1224 | Select · Menu · Date · Overdue · Add | multi-select · row menu · date sheet · overdue · quick add |
  | 2448 | Search · Empty · Archived · Trash · Notify | search · empty · archived · trash · notification |
  | 3672 | Voice · Perm · Drawer · NewList · EditLists | voice · notifications off · drawer · new list · edit lists |
  | 4896 | Hints · Clock24 · Widgets (390×1124, own file) | hints · date sheet with clock 24-hour |

  `title1` notes sit at y=-260 above each pair: "App shell: nav and create" (x=0), "Task page, swipe and undo" (x=940), "Repeat and birthdays" (x=1880).
- **Only `Main.dc.html` (and `Widgets.dc.html`) are edited by hand.** The other boards are generated from them and differ only in the `<title>`, the `theme` default and fallback, and the `startState` default and fallback. After every edit, regenerate them with **`python3 research/canvas/gen.py`** (it reads and writes the files next to it in `research/canvas/project/`; copy it beside your working `project/` folder if you edit elsewhere).
- **To edit:**
  1. Read the live files first: the Artifact tool with `action: "read"`, `paths: ["project/Main.dc.html", "project/canvas.json"]` and an `out_dir` in the scratchpad. A publish is refused if you haven't read the latest version. If a newer version appears, diff it against `research/canvas/project/` to see whether the owner changed anything before you merge.
  2. Copy them into the scratchpad under `<root>/project/`.
  3. Edit `Main.dc.html`, then regenerate the twins with the script above.
  4. Publish with `url`, `root` = that folder, `file_path` = Main, and `files` = the five twins (plus `project/canvas.json` only when boards are added or moved; never re-layout).
  5. Copy the result back into `research/canvas/project/` so the backup stays current.
- **Tweaks on each artboard** (`data-props`):
  - `theme`: dark or light.
  - `fabColor`: primary (Kairo rule 4) or primaryContainer (M3 default).
  - `labels`: selected tab only (the default), all tabs, or none.
  - `clock`: 12-hour (the default) or 24-hour.
  - `startState`: expanded, scrolled, menu open, task page, repeat sheet, multi-select, row menu, date sheet, overdue, quick add, search, empty, archived, trash, notification, voice, notifications off, drawer, new list, edit lists, or hints.
- **The type's rules that matter:**
  - All UI is `<x-dc>` markup. Logic goes in `class Component extends DCLogic` with `renderVals()`.
  - `{{holes}}` are plain lookups only, never expressions.
  - Events use JSX camelCase, like `onClick="{{fn}}"`.
  - Per-item handlers are built inside `renderVals`.
  - The only network access allowed is Google Fonts `css2` links in `<helmet>`.
  - It says **never verify unless the user asks**. Also, the built-in browser pane is *not* signed in to claude.ai, so the canvas can't be checked there anyway.

## What the owner decided (in order)

1. **Tabs:**
   - **Tasks · Habits · Calendar · Finance**, plus a create **+**.
   - Lists move *inside* Tasks (not designed yet).
   - Stats move to a **local account** page, opened from an avatar at the top right (not designed yet).
2. **Nav bar:**
   - It's a floating pill, sized to fit its tabs, not stretched. The pill and the **+** sit together **centred** at the bottom, 8dp apart, 40dp from the bottom (16dp plus the gesture inset).
   - The bar is grey: `#2A2A2A` in dark, white with a shadow in light.
   - **The selected tab is a solid primary (cyan) pill** holding a filled icon and its label, with the icon in onPrimary. The other tabs show only their icon, no label.
   - The pill widens with a spring when you switch. Pressing a tab squares its corners to 8dp.
   - The owner said "let it be as it is now" for the colours.
3. **The + button:**
   - It's 64dp, the same height as the bar, with 20dp corners and a bold 28dp plus. It's solid primary (rule 4).
   - When you scroll down, the bar slides away and the + grows to 80dp with 24dp corners.
4. **Create menu** (M3 `FloatingActionButtonMenu`):
   - It's anchored to the + and opens 8dp above it, so the two can't overlap. An earlier version overlapped, and the owner called that out.
   - Items, top to bottom: List, Transfer, Income, Expense, Event, Task. Each is a 56dp pill in primaryContainer.
   - The + becomes a 56dp primary circle with a close icon. A scrim at 32% sits behind.
5. **Top of the screen:**
   - There's no title or date ("Tasks" and the date were removed on request).
   - A **wide Google-Drive-style search pill** sits at the top:
     - It's 56dp with full corners and a centred hint that changes per tab ("Search in tasks / habits / calendar / finance").
     - It has a **mic** button inside on the right, for voice add.
     - It uses `--search-bg`.
   - The **account avatar sits outside the pill**, on the right: a 44dp Cookie-9 shape in tertiaryContainer with a person icon.
   - There's no hamburger, since Kairo has no drawer.
6. **Tasks screen:**
   - **Hero card:**
     - It's on primaryContainer with 32dp corners.
     - On the left, "**Today**" (36/44) and, below it, two lines aligned left: "**Next · 12:00**" (12sp label) and the task title (16sp, one line).
     - On the right, a **wavy circular progress ring** (stroke 8) over a slowly spinning Cookie-12 shape, with the count **3/10** in the centre.
     - "N to go" and "X of Y done" were **removed on request**, because the ring already shows them.
     - When everything is done, it shows "All done / The rest of the day is yours".
     - Overdue tasks count as today's work.
   - **Connected button group:**
     - All · Overdue · Today · Upcoming · Done, with **Today selected by default**.
     - Buttons are 40dp tall and 2dp apart, with 8dp inner corners and 20dp outer corners.
     - The selected button is a filled primary pill.
     - Pressing a button widens it and drops its corners to 4dp.
   - **Tasks are grouped segmented lists, "bubbly":**
     - Each group has 28dp outer corners and 8dp inner corners, with 3dp between rows. Rows are 72dp minimum, and a row rounds fully when pressed.
     - Section headers are 14/600 in primary. Overdue headers are in error.
     - Groups per filter:
       - Today: "To do" and "Done today".
       - All: Overdue, Today, Upcoming and Done.
       - Upcoming: one group per day.
   - **Task row** (*the round check below was replaced by swipe in session 2, see 8*):
     - On the left, a round 24dp check ringed in the priority colour. Ticking it fills it primary, turns it into an 8dp rounded square and shows a check.
     - The title is struck through when done.
     - The meta line shows the time (or "Yesterday" in error), the list's shape and colour dot, the list name, subtasks d/t and a bell icon.
     - High priority adds a red flag on the right.
     - A ticked task moves to "Done today", and the ring updates.
   - The sample data is the app's `FakeData.kt` tasks, plus three upcoming tasks (Submit expense report, Pick up dry cleaning, Plan weekend trip).
   - The Habits, Calendar and Finance tabs show empty content for now.
7. **Colours** (two rounds of feedback: the first was "too weak", the second "need clear contrast"):
   - **Final:** accents come from **SchemeVibrant, spec 2025, contrast 0.5 (medium)**, seed `#79D0FD`, generated with material-color-utilities 0.4. The **surfaces are neutral greys** (chroma 0).
   - The owner rejected tinted surfaces (the default vibrant, expressive and tonal schemes all looked muddy).
   - Values in use:
     - **Dark:** primary `#63CCFF`, onPrimary `#00374B`, primaryContainer `#00C1FF`, onPrimaryContainer `#002B3D`, tertiaryContainer `#8D94FF`, onTertiaryContainer `#01004D`, error `#FF9F99`, onSurface `#FFFFFF`, onSurfaceVariant `#C6C6C6`, bg `#0E0E0E`, item `#1F1F1F`, item-press/nav/chip `#2A2A2A`, search `#262626`.
     - **Light:** primary `#006384`, onPrimary `#FFFFFF`, primaryContainer `#007DA7`, onPrimaryContainer `#FFFFFF`, tertiaryContainer `#636AD2`, onTertiaryContainer `#FFFFFF`, error `#B31B25`, onSurface `#1B1B1B`, onSurfaceVariant `#474747`, bg `#F1F1F1`, item/nav `#FFFFFF`, chip/search `#E2E2E2`, item-press `#E8E8E8`.
     - **List colours** come from `app/src/main/java/app/kairo/ui/theme/KairoColors.kt`. Priority colours: high = error, med = Amber, low = Indigo.
   - **To regenerate:** Stash has the library at `~/coding/reactnative/stash/node_modules/@material/material-color-utilities`. Its imports have no `.js` extensions, so copy it, add `.js` to the relative imports with sed, add `{"type":"module"}`, then use `new SchemeVibrant(Hct.fromInt(argbFromHex('#79D0FD')), isDark, 0.5, '2025', 'phone')`.

8. **Swipe on task rows** (session 2, replaces the round check):
   - There's **no checkbox** on rows any more. Tapping a row opens the task page; Enter opens it too, and Delete moves it to trash.
   - **Left to right completes** (or marks not done). **Right to left moves to trash.**
   - **Gmail look**, copied from 9to5Google's recording of Gmail's M3 Expressive swipe: the action is its own **full-round pill** that grows out from the screen edge, 8dp clear of the dragged row. It starts as a sliver, becomes a circle with the icon at 48dp wide, then stretches. Complete = primaryContainer with a check; delete = errorContainer with a bin.
   - Past **40% of the row width** the action arms, and the icon pops to 1.2×. Releasing flies the row off, the pill fills the row, and the space collapses.
   - The dragged row rounds fully. **Jelly:** the neighbours in the same group follow at 6% (rows ±1) and 2% (rows ±2), capped at 16dp, and spring back with the fast spatial spring.
   - Priority now shows as a **4dp bar** at the left of the row, coloured by priority (owner hasn't commented yet). High still shows the red flag.
   - The bubbly group is unchanged: 28/8dp corners, 3dp gaps, 72dp rows.
9. **Undo and trash:**
   - One **snackbar** above the dock: inverse surface, 4dp corners, text plus an **Undo** button. It stays 10 s (SnackbarDuration.Long).
   - Used for: complete, mark not done, move to trash, archive, duplicate. Undo restores the earlier state exactly.
   - **Delete is a soft delete:** the task goes to Trash and stays for **30 days**. The **Trash list itself is not designed yet** (it belongs with Lists): restore, delete forever, days left.
   - This also closes inventory §17's "no undo" gaps for archive and delete from task detail.
10. **Task page** (full screen, slides in with shared axis X; the nav dock is replaced by a floating toolbar):
    - **Top:** only a back button.
    - **Editor** (like Google Keep): line 1 is the **title** (28/36). **Enter** starts smaller lines (16/24) below it, as many as you want. Any line can be plain text or a **subtask** with an M3 checkbox. Enter on a subtask makes another subtask; Enter on an empty subtask turns it back into text; Backspace at the start removes the checkbox or joins the line to the one above. With no lines, an empty line reads "Add details or subtasks". There is no separate Subtasks section; the row's d/t count comes from these lines.
    - **Under the editor, one row:** the **list chip** (◆ Home ▾) on the left, and a small **32dp subtask icon** (☑) at the right. The icon toggles the current line into a subtask, or adds a new subtask at the end when nothing is focused. Tapping the list chip opens a row of all seven list chips to pick from.
    - **Due pill:** "Due in 2 h 15 min", "Due today", "Overdue · 1 d" (error), "Completed".
    - **When group:** Date, Time ("Add time" if none), **Repeat** (opens the sheet below), **Alert**: a connected group **On time · 30 min · 15 min · No alert**, with the result shown in the row ("At 11:30 · 30 min before"; with no time it counts back from 9:00). The old Reminder switch is gone.
    - **Notes** (below When), then **Priority**: a connected group None · Low · Medium · High with tinted flags (replaces tap-to-cycle).
    - **Bottom:** a floating toolbar (Duplicate, Archive, Delete in error) next to a primary **Done** button. Done, Archive and Delete go back to the list with Undo.
    - Everything saves as you go.
11. **Repeat sheet** (ModalBottomSheet; owner asked for "a dialog or drawer", rule 12 says drawer):
    - 28dp top corners, 32×4 handle, 32% scrim. Header "Repeat" plus a live summary in primary ("Every 2 weeks on Tue, Thu · until 24 Feb 2027").
    - **Quick picks** (filter chips): Never · Daily · Weekdays · Weekly · Monthly · **Yearly** (= every year on this task's date).
    - **Every N** stepper (1–99) and a connected group **Days · Weeks · Months · Years**.
    - **Weekly:** seven round toggles M–S; selected turns primary and squares to 12dp. At least one day stays on.
    - A line saying when it falls ("On day 24 of the month", "On 24 Nov every year").
    - **Ends:** connected group **Forever · Until a date**; Until shows the date and span chips 1 month · 3 months · 6 months · 1 year.
    - **"It's a birthday"** switch (cake on a cookie shape in tertiaryContainer): locks the rule to yearly on this day, forever. Optional **Born in** stepper adds "turns N". Birthday tasks show a 🎂 icon on the row; other repeating tasks show ↻.
    - Cancel / Done. Nothing saves until Done.

## Tokens used, and where they came from

Confirmed from the androidx `material3` source on GitHub (main branch, not pinned to 1.5.0):
- **Floating toolbar:** 64dp tall, full corners, 8dp padding, 4dp between items, 16dp `ScreenOffset`. The gap to the FAB is 8dp. `FabSizeRange` is 56→80dp.
- **FAB menu:** items are 56dp tall with full corners and 4dp spacing, 24dp side padding, an 8dp gap between icon and label, and the label in titleMedium on primaryContainer. The close button is 56dp with a 20dp icon.
- **FAB sizes:** baseline 56dp with 16dp corners (CornerLarge), medium 80dp with 20dp corners (LargeIncreased), large 96dp with 28dp corners.
- **Shape tokens:** Small 8, Medium 12, Large 16, LargeIncreased 20, ExtraLarge 28, ExtraLargeIncreased 32, and Full.
- **Large flexible app bar:** title in displaySmall, subtitle in titleMedium.
- **Horizontal nav item:** indicator 40dp tall, 16dp padding on each side, 24dp icon.

**Not confirmed (from memory, flag if asked):**
- The 40% swipe threshold, the 8dp pill gap and the 48dp icon width (eyeballed from a 480px GIF), and the jelly 6%/2%/16dp values. None of these came from Google's source.
- Snackbar shape (4dp, from memory of SnackbarTokens) and duration.
- New colours added in session 2, from the same SchemeVibrant 2025 / 0.5 run: errorContainer `#D7383B` / onErrorContainer `#FFFFFF` (both themes). Inverse roles: dark inverseSurface `#E2E2E2`, inverseOnSurface `#1B1B1B`, inversePrimary `#005C7B`; light inverseSurface `#303030`, inverseOnSurface `#F1F1F1`, inversePrimary `#63CCFF`. The light inverse values were chosen by hand to match the neutral greys; the light run of the library gave a different primary than the one in use, which wasn't resolved.
- The expressive spring values: fastSpatial 0.6/800, defaultSpatial 0.8/380, fastEffects 1/3800, defaultEffects 1/1600. They're baked into CSS `linear()` curves in the artboard as `--fs`, `--ds`, `--fe` and `--de`. `MotionTokens.kt` doesn't contain them; they're probably in a MotionScheme tokens file.
- The vertical nav item sizes: 56×32 indicator with a labelMedium label.
- Small icon/button pressed shapes.

**Departures from M3, chosen on purpose:**
- The floating pill is used for navigation (M3 means floating toolbars for actions).
- The + is solid primary, where M3's default is primaryContainer.
- The 64dp FAB isn't an M3 size.
- The Cookie-9 and Cookie-12 shapes are cosine approximations, not the exact `MaterialShapes` paths.

## CLAUDE.md rules that now conflict

The owner hasn't yet said whether to update these:
- **Rule 2** says SchemeTonalSpot 2021. The design now uses SchemeVibrant 2025 at medium contrast.
- **Rule 3** says neutral grey backgrounds. That still holds, since surfaces are chroma 0. The grey values themselves changed slightly: light bg went from T94 to `#F1F1F1`.
- **Rule 5** says the floating nav is copied from Stash, 16dp from the sides. It's now a pill sized to its tabs and centred together with the +.
- **§1** says step 16 (polish) is next. That's superseded by the redesign.

Ask before editing CLAUDE.md.

## Owner preferences learned this session

- Wants real M3 Expressive "to the core", with values researched rather than guessed. Short, plain answers.
- Iterates visually and fast, one or two changes per message. Do exactly what was asked and keep the rest.
- Said "do not publish anything" once, then asked for a Claude Design artifact. So publishing to *this* canvas is fine, but **don't publish new artifacts or anything else without asking**.
- Dislikes: muddy or tinted colours, a stretched nav, title text at the top, and overlapping parts.
- Likes: the bubbly grouped lists, the connected group, the Drive-style search pill and the hero ring.

## Owner preferences learned in session 2

- Points at real apps as the reference ("Gmail-like swipe", Keep-style editor). Look at the real thing (screenshots, GIFs) before building; the first swipe attempt was rejected for not matching Gmail.
- Wants subtle physical motion everywhere (the neighbour "jelly" on swipe).
- Wants small, quiet controls: the subtask button was "too big", so it became a 32dp icon.
- Prefers choices to switches (alert offsets instead of a reminder on/off).

## Session 3 (2026-11-25): the ten Tasks screens

The owner said: **design only, nothing else** (no app code, no builds) until they say otherwise, and stay as close to M3 Expressive as possible. Only the canvas gets updated. They got angry twice this session: once when I spent time digging through library bytecode for tokens ("you are drifting"), and once because they didn't understand why I edited `.dc.html` files (explain up front that the canvas *is* those files and nothing shows until publish). Publish early so they can see progress.

Published as canvas versions 30–36. All of these are in `Main.dc.html` and in every board.

**Owner decisions**
- Keep a **⋮ quick-action menu** on every row for single tasks; **long-press** starts multi-select for several.
- **No date** and **Later** (v40, replacing the earlier "top of every filter" ask): ordinary sections in date order. **All:** Overdue · Today · Next 7 days · Later · No date · Done. **Upcoming:** one group per day, then Later. Not shown in Today, Overdue or Done. Later = open tasks more than 7 days out.
- **Lists:** live in a navigation drawer opened from a menu icon in the search pill (see 15 below).
- **12-hour clock everywhere** ("9:30 AM"). Times are still stored as 24-hour `H:MM` in the prototype; `f12()` formats them.

**What was designed**
1. **Multi-select:** a 450 ms long-press without moving selects (moving first starts the swipe). The search pill becomes "✕ · N selected · All/None". The leading badge morphs from an outlined circle to a filled Cookie-9 with a check; selected rows go full round on primaryContainer. The nav slides away; a **vibrant floating toolbar** (primaryContainer) holds Reschedule, Move to list, Archive, Trash, plus a **Done** FAB. In Archived: Move, Trash + Unarchive. In Trash: Delete forever + Restore. All with Undo.
2. **Row ⋮ menu:** grouped Expressive menu, **one card** (owner: "don't split them with space"), hairline dividers between groups: Open, Mark done / Reschedule, Move to list / Duplicate, Archive / Move to trash (red).
3. **Date sheet** (shared by reschedule, Reschedule all overdue, task page Date and Time rows, Quick Add):
   - Top is the original style (owner reverted my hero header): title, live summary in primary, Date / Duration connected group, outlined quick chips.
   - Calendar in its own rounded card, 40dp round days, today outlined, dots on days with tasks, past days dimmed.
   - **Wide 12-hour time input** (owner asked: no icons, wide): big hour and minute fields with −/+ under each, AM/PM selector in tertiaryContainer, text presets None · 9 AM · 12 PM · 6 PM · 9 PM.
   - Alert connected group with icons.
   - Footer: **56dp Expressive buttons**: Clear (outlined), Cancel (grey), **Done** (big filled primary with check). The Repeat, Move and Empty-trash sheets still use the old text buttons; the owner hasn't asked to change those yet.
4. **Reschedule all overdue:** "Reschedule all" on the Overdue header opens the date sheet at Today.
5. **Quick Add:** + › Task. Sheet docked on a drawn keyboard. Parser words are highlighted in the field (dates, times, !priority, #list). Chips: date, alert, repeat, priority, list (each opens the grouped menu, except date which opens the date sheet). Snackbar "Added for tomorrow" with Undo.
6. **Search:** the pill becomes the field (back, input, clear). Recent searches until you type. Results grouped Open / Done, match highlighted in primary, notes snippet when only the notes match.
7. **Empty states:** one spinning Cookie-12 (tertiaryContainer) with a per-view icon; Today/All/Upcoming have an "Add a task" / "Plan something" tonal button.
8. **Archived** and **Trash** pages: back button, title, subtitle. Trash rows show "N days left" (error at ≤ 3), swipe right restores, left deletes forever (Undo). "Empty trash" confirms in a bottom sheet.
9. **Reminder:** heads-up notification with Done and Snooze 10 min (system-drawn; Kairo only sets icon, accent, text, actions).

10. **Swipe pills** (owner: they jiggled on release; "no jiggle, but fluid"; then "cancelling halfway still shows that part, make it vanish quicker"). The pill width and icon use the default effects spring (no overshoot). Released short of the threshold, the pill fades and shrinks on the fast effects spring and is hidden after 150 ms while the row springs back.
11. **Voice add** (canvas v37): the mic in the search pill opens a listening sheet. A Cookie-9 mic sits in a breathing Cookie-12 halo (in the app the halo follows the voice level), the words appear as they're heard, Done opens Quick Add with the text filled in and parsed.
12. **Notifications off**: when Android's notification permission is denied, a card under the filters says so in plain words ("Reminders are off… for N tasks") with **Turn on** (opens the system prompt) and **Not now**. Rows with reminders show a slashed bell.
14. **Task page due pill** (v39): moved from under the editor to the top right, in the same row as the back button.

15. **Lists = navigation drawer (owner liked it: "yes this is nice"; v44, Expressive pass in v45).** A menu icon at the left of the search pill (Gmail/Drive) opens an M3 modal navigation drawer: 336dp, grey sheet with 28dp end corners. Header "Kairo" (28/36) + "N open tasks". Items are bubbly groups like the task lists (56dp, 28/8 corners, 3dp gaps, item colour): All tasks · "Lists" label (primary) with each list as a filled `label` icon in its colour, New list (primary) · Archived, Trash, Edit lists. Counts are small tabular badges. The current item morphs to a full pill in secondaryContainer with a filled icon. Picking a list scopes the Tasks view and the hero title becomes the list name. Boards DrawerDark/DrawerLight at x 1880, y 3672. New list and Edit lists aren't designed yet.
16. **Search pill is 64dp** (was 56; owner: "a little larger"), with the selection bar and page top bar matching; content starts at 116px.

Tried and rejected (v42–v43): **Lists via the hero title** ("Today ▾" opening a sheet, first as a long list, then as a grid of list shapes). Owner: "not material expressive shapes… this whole list UI is wrong". The shapes were hand-made polygons, not `MaterialShapes`. Don't use hand-made shapes or put list switching in the hero again.

Tried and rejected (v37 → removed in v38): a **completion burst** (check pop + flying bits + ring count pop) on swipe-complete. The owner said it isn't needed; don't bring it back.

**Not confirmed / guessed**
- Menu metrics (16 outer corner, 48dp items) loosely follow `SegmentedMenuTokens` in material3 1.5.0, but the one-card version is the owner's call, not the spec.
- Notification action styling for Android 16 is a guess.
- The time input sizes (72dp fields, 45sp digits, 56dp AM/PM) are from memory of M3 TimeInput, not checked.
- The canvas can't be viewed from the session's built-in browser (it asks to sign in), so nothing was checked visually. The owner's comments are the only visual check. There's also no tool in the session to reply in comment threads; answer in the chat.

**Owner preferences learned in session 3**
- Wants changes scoped exactly to what was pointed at: "change the calendar UI, not the top view". Don't redesign neighbours.
- Likes big, chunky Expressive buttons ("fat main button and normal other button") over text buttons.
- Dislikes icons where they aren't needed (time presets), gaps that split one thing into pieces, and anything that looks "old UI".
- Leaves feedback as canvas comments pinned to the element.

## Session 4 (2026-11-26): lists, focus, hints, widgets, clock

Published as canvas **version 46**; the backup in `canvas/project/` matches it. `gen.py` now also makes the NewList, EditLists, Focus, Hints and Clock24 twins, plus `WidgetsLight` from `Widgets.dc.html`. Widgets is a separate board that you edit by hand; it isn't a copy of Main.

**Owner decision:** time format is a setting, with **12-hour as the default** and 24-hour as the option. On the canvas it's the `clock` tweak. In 24-hour mode the date sheet hides AM/PM, shows hours 00–23 and uses 09:00 / 12:00 / 18:00 / 21:00 as the presets. Times are still stored as 24-hour `H:MM`. Where the setting itself lives (Settings on the Account page) isn't designed yet.

**What was designed**
1. **New list** (drawer › New list): a bottom sheet with a live preview of the drawer row, a filled Name field that rounds fully on focus, and seven colour swatches that square off with a check when picked. A duplicate name shows an error. Create opens the new list, which has its own empty state. The shape is assigned automatically by cycling the existing list shapes; there's no shape picker yet.
2. **Edit lists** (drawer › Edit lists): a page in the Archived/Trash style. You reorder with the drag handle (or Arrow Up/Down on it); the lifted row rounds fully and casts a shadow while the others slide. Tapping a row opens the same sheet as **Edit list**, for rename, recolour and **Delete list**. Delete moves the list's tasks to Trash, with Undo. Lists now have a fixed key, so renaming keeps their tasks.
3. ~~**Focus**~~ (removed in session 5): start it from the task page's floating toolbar (timer icon) or from ⋮ › Start focus. The hero shows a countdown pill (25 min, the Pomodoro default) with pause and stop, plus the task title. The hero shape spins faster while you focus and stops when paused. The task's row shows a timer icon. At zero you get the snackbar "Focus session done".
4. **Onboarding hints:** three M3 rich tooltips (12dp corners, 320dp wide, with a caret) anchored to the first task: swipe, hold to select, then ⋮. The row acts out each gesture in a loop. You get Skip and Next/Got it.
5. **56dp buttons** on the Repeat, Move to list and Empty-trash sheets. Empty trash is a filled errorContainer button.
6. **Widgets board** (390×1124): Quick add 4×1, Today 4×2 (its checks work), Progress 2×2 (the wavy ring), Timer 2×2 (start/pause and stop work), Money 2×2 (wavy budget bar), Next up 2×1, Streak 1×1, Quick actions 4×1 (connected group) and Stats 4×2.

**Not confirmed / guessed (session 4)**
- Widget corner radius is 28dp; in reality the launcher sets it.
- The Streak "Sunny" shape and the cookies are cosine approximations, not the `MaterialShapes` paths.
- The hint loop animations use hand-set keyframes. That's a decorative-loop exception like `slowSpin`; the owner hasn't approved it.
- Money and Stats numbers are sample values.
- Drag to reorder exists for lists only. Tasks stay sorted by date and time.

## Session 5 (2026-11-26, same day): owner feedback on v46 → canvas version 49

- **Focus timer removed** everywhere (hero pill, task-page toolbar button, ⋮ item, Focus boards). Owner: "most definitely not where I want it". Don't bring it back to the hero, task page or ⋮; ask where it goes first.
- **Hero simplified:** it's now one compact card on primaryContainer with 28dp corners and about 84dp tall. A 64dp wavy ring sits on the left with the count; on the right are the title (22/28) and one line, "Next 9:30 AM · task". The big 36sp title and the separate Next label/title are gone.
- **Date sheet calendar now matches the app's Calendar tab** (`CalendarScreen.kt` MonthGrid): a card with 4dp gaps, always six weeks with neighbour-month days dimmed, 12dp rounded-square day cells, today as a Cookie-9 in primary with a bold onPrimary number, the picked day in secondaryContainer, and primary dots on days with tasks. The card stays `--chip`, because the sheet itself is `--item`.
- **Widgets aren't all rectangles now.** Today (4×2) and Stats (4×2) stay rounded rectangles. Quick add and Quick actions are pills. Progress is a Cookie-12 with the ring and "N left". Timer is Ghostish (round top, scalloped hem) in tertiaryContainer. Money is an Arch. Next up is a SemiCircle. Streak is Sunny. All shapes are SVG approximations of MaterialShapes, not the exact paths.

## Session 6 (2026-11-26): official shapes only → canvas version 50

Owner: **never hand-make shapes. Use the official M3 Expressive shapes, with the same names.** The source is `Shape Set.svg` in the kairo root: the 35 MaterialShapes in the official order, row by row (Circle, Square, Slanted, Arch, Fan, Arrow, SemiCircle / Oval, Pill, Triangle, Diamond, ClamShell, Pentagon, Gem / VerySunny, Sunny, Cookie 4/6/7/9/12 / Ghostish, Clover 4/8, Burst, SoftBurst, Boom, SoftBoom / Flower, Puffy, PuffyDiamond, PixelCircle, PixelTriangle, Bun, Heart). [canvas/m3shapes.json](canvas/m3shapes.json) holds every path scaled into a 0–100 box: `norm` is centred like `RoundedPolygon.normalized()`, and `tight` fills the box. The boards read them through `this.m3(name)`; the `...Fill` names are the tight versions, stretched to a widget's bounds the way `toShape()` does.
- Replaced: Cookie-9 (avatar, selection badge, voice, notifications-off card, today in the calendar, birthday badge), Cookie-12 (hero, empty states, voice halo), Sunny, SemiCircle, Arch and Ghostish (widgets).
- List dots: round = Circle, tri = Triangle, pent = Pentagon, gem = Gem. hex = **ClamShell**, because the library has no hexagon; the app hand-builds one. Ask the owner about this.
- Not shapes, so left as they were: the wavy progress ring (a progress indicator), CSS corner-token pills and circles, and the tooltip caret.

## Session 7 (2026-11-26): canvas versions 51–61

- Widgets after owner comments:
  - Quick actions is a plain 28dp widget with a connected group, and Transfer is gone.
  - Quick add has no logo, and its text stays on one line.
  - Next up is a plain 28dp widget instead of SemiCircle.
  - In Stats, the today bar was taking the Today widget's `.today` styles; it's now `.bar.now`.
- **Date sheet:**
  - The quick picks are only Today, Tomorrow and No date.
  - Duration mode is gone.
  - Date and Time are one grouped, collapsible block with one part open at a time.
  - The time is picked on an **M3 TimePicker dial**: 96×80 hour and minute boxes, AM/PM, and a 256dp face. In 24-hour mode the outer ring is 00–11 and the inner ring 12–23; that ring order is unverified against Compose.
  - The task dot is clear of the number, and onSurface in light mode.
- **Lists are a filter now** (owner chose option A): a scrollable row of M3 filter chips under the date filters (All · Inbox · each list with its official shape · +). It combines with the date filter. The drawer keeps only New list, Edit lists, Tasks, Archived and Trash.
- **Inbox** (owner decision): always first, and it can't be deleted or renamed. New tasks go there by default, and deleting a list moves its tasks to Inbox, with Undo. Inbox isn't shown in Edit lists.
- Local render check: `scratchpad/render/build.py` renders a board's template with its `renderVals()` in headless Chrome (`google-chrome-stable`). It's a rough renderer with no events, but good for catching layout bugs.

## Session 7b (2026-11-26): tabs and filter chips, canvas version 62

The owner rejected the grouped list chips ("very old UI, same as the filter row") and gave a spec, now implemented:
- **Row 1: M3 primary tabs.** Overdue (with an errorContainer count badge) · Today · Upcoming · Done. The tabs are 48dp, with a 3dp indicator as wide as the label and a 1dp divider underneath. **All is gone**, so Upcoming now ends with Later and No date. Today stays the default.
- **Row 2: M3 filter chips, multi-select.** No chip picked means every list. A picked chip is secondaryContainer with a leading check; an unpicked one is outlined with its list's official shape. Gaps are 8dp, and a trailing **Clear** text button shows only when a chip is picked. There's no All chip. Inbox stays the first chip with its tray icon. The hero title shows the picked lists ("Work + Home").
- CSS gotcha: the nav already uses `.ind`, so the tab indicator class is `.tind`. Check for class collisions before adding new names.

## Session 7c (2026-11-26): canvas version 63

The owner rejected the tabs and filter chips ("closer to Google Tasks"). Restored the earlier connected group (All · Overdue · Today · Upcoming · Done; Upcoming ends with Later again). The **list filter is now its own connected button group**. Single-select (owner): tap a list to show only it, tap again or the trailing ✕ for all. The picked list is secondaryContainer with a check and rounds fully. Inbox comes first with its tray icon; the others show their official shape. The row scrolls sideways.

## Session 7d (2026-11-26): final Tasks layout, canvas versions 64–65 (approved: "this works")

The Tasks screen, top to bottom:
1. The search pill (lists menu · search · mic) and the avatar.
2. **The view group**: All · Overdue · Today · Upcoming · Done. It's single-select, with filled primary for the picked view; Today is the default.
3. **The compact hero card**: a 64dp wavy ring, the title, and one "Next" line. The title follows the picked list.
4. **The list group**: a connected button group of Inbox · Work · Home · …, each with its official shape. It's **single-select**: tap a list to see only it, then tap it again or the trailing ✕ to see all. The picked list is secondaryContainer with a check.
5. The task groups.

Reasoning the owner accepted: views are mutually exclusive, so they're single-select. A task lives in exactly one list, so the list filter is single-select too. Multi-select is kept for tags, if tags are ever added.

## Session 8 (2026-11-26): Habits tab, canvas version 66

Revert point: `research/canvas-v65-revert/` is a plain copy of the v65 files (Tasks done). To go back, republish those files and drop the eight Habit boards from `canvas.json`.

Added in v66 (new row at y=6380; startStates `habits`, `habits heat map`, `habit page`, `new habit`; `gen.py` makes their twins):
- **Habits tab**:
  - A week card on top: seven day rings (M–S) show done/due per day, and a complete day turns into a filled Cookie-9. Arrows page back through weeks. Tap a day to see and back-fill its check-ins.
  - Variant board: the same card as a habit × day heat map (cells toggle).
  - Below: the picked day's habits ("Today · 2 of 4 done", with a New habit action), then a Rest day section, then an Archived fold.
  - Each habit owns an official shape and a colour (Circle, Square, Triangle, Pentagon, Gem, Cookie6, Clover4, Sunny, Flower, SoftBurst).
  - The check morphs from an outlined circle to the habit's filled shape, with an Undo snackbar.
- **Habit page**:
  - A streak hero in the habit's shape, then tiles for current streak, best streak and this month.
  - History: a connected Month · Year group. Month is a heat map you can tap; Year is 12 mini months.
  - Settings: a Reminder switch, and Repeat on.
  - Floating toolbar (Edit, Archive, Delete) and a Done today / Undo today FAB.
- **New / Edit habit sheet**: a live preview row, Name, Shape (10 official shapes), Colour, Repeat on (Every day / Weekdays / Weekends plus day circles), and Reminder (switch plus quick times). Edit adds Delete with Undo.
- The + menu gained **Habit** (next to the FAB on the Habits tab). The Lists menu button is hidden on Habits.
- New over 1.3.1: schedules, archive, a shape and colour per habit, a week view, a year heat map, and back-filling from the week card.
- v67 (owner comment): the habit page uses the light-theme habit colours in both themes (white text on them), and done days in the month heat map sit on an official Clover-4.
- v68 (owner comment): no puffy, clover or cookie shapes for habits. The picker is Circle, Square, Triangle, Pentagon, Gem, Diamond, Slanted, Arch, Sunny, Burst; Meditate is Burst, Spanish is Diamond. Clover-4 is only the done-day mark.
- v69 (owner comment): never use Burst anywhere. Pill replaces it in the picker; Meditate is Slanted.
- v70 (owner, less clutter, option A): the habit shape is the check (outline + icon = not done, filled = done), meta is the streak only, one list with rest-day habits dimmed (header only for a past day), no "New habit" button (use +). The week card has one line ("This week 14/18") and neutral rings. **Clover-4 is the day mark in every calendar**: date-sheet today, full week-ring days, habit done days.
- v71 (owner): the week card is back to the coloured v69 look (primary rings, secondaryContainer pick, "14 of 18 check-ins · 78%"); a full day keeps the Clover-4. On the habit page, Reminder and Repeat on each open a small drawer with only that part (startState-free: hS.part = remind | repeat); the full Edit sheet is only behind the Edit button.
- v72 (owner): a full day in the week card is a Cookie-9 again; Clover-4 stays the today mark in the date sheet and the done mark in the habit month view. Year view is one contribution-style grid (rows Mon–Sun, 53 week columns, month labels on top), scrolling sideways and opening on this week. The habit sheet preview shows the filled shape.
- v73 (owner comment): the habit sheet day picker is an M3 Expressive connected button group (7 toggles, 48dp, 2dp gaps, outer corners full, inner 8dp, picked = primary, fully round). The task Repeat sheet weekday circles are unchanged.
- v74 (owner): icon picker. New habits start with a generic star; the preview shape in the habit sheet has an edit badge and opens a 24-icon grid. Habit rows swipe like tasks (left to right = check in or undo, right to left = delete with Undo). Swipe neighbours (tasks and habits) follow the row up to 20% of the width, then let go and spring back on the fast spatial spring.
- v75 (owner): habit swipe is left to right = done, right to left = not done, for the picked day (no delete by swipe). Done pill in the habit colour, not-done pill secondaryContainer. Rest-day rows do not swipe.
- v76 (owner comment): done days in the habit month view sit on a Cookie-9 (was Clover-4). Clover-4 is now only the today mark in the date sheet.
- v77 (owner): swipe neighbours copy the Android 16 QPR1 notification swipe (Material 3 Expressive): they lean the same way with rubber-band resistance (max 14px adjacent, 5px next), then at 30% of the width (v80; the action still arms at 40%) the row detaches and they spring back on the fast spatial spring. Sources: Android Authority on the QPR1 notification detach; no official M3 spec page found. Android detaches at about 10%; 30% is the owner's choice. v79 fixed the icon-picker badge pushing the preview icon off centre.
- v81 (owner comment): no Triangle for habits (an icon does not sit well in it). Clam shell replaces it in the picker.
- v82 (owner): each habit day has three states: pending (default), done, and not done (a decided no, like a dropped task; a right-to-left swipe sets it). Not done shows a grey dashed outline with a cross and "Not done today", and it breaks the streak. The list is ordered pending, then done, then not done, but the order is a snapshot taken on entering the tab or changing the day, so checked rows stay in place until you come back.
- v83 (owner picked items 2, 8, 9, 10, 11 from the gap list):
  - **Any reminder time**: "Other" next to the quick times opens the same M3 TimePicker dial as the task date sheet (v84). Owner: a dial or the system picker, never +/- buttons for time.
  - **Count habits**: Goal is a connected group (Yes or no / Count) with a per-day stepper and a unit. Tapping the shape adds one; the shape fills from the bottom with a tint, turns solid at the goal, and a tap when full resets. Sample: Drink water, 5 of 8 glasses.
  - **Times a week**: a Repeat chip with a 1–6 stepper. The habit shows every day, the week ring counts it only on days it was done, and the streak counts weeks. Sample: Run, 3 a week.
  - **Pause**: a Pause button in the habit page toolbar opens a drawer (1 week, 2 weeks, 1 month, Until I resume). Paused days work like rest days, so the streak is kept and there are no reminders. The FAB becomes Resume. Sample: Journal.
  - **Notes**: a "Note for today" field on the habit page, the 3 latest earlier notes below it, and a dot under month days that have a note.
- v85 (owner approved): **three goal types**, a connected group in the habit sheet.
  - **Yes or no**: tap the shape.
  - **Count**: a number field (keyboard, no +/-) and unit chips (glasses 1, pages 10, reps 10, steps 1,000, km 0.5, times 1). The unit sets how much a tap adds.
  - **Time**: minute chips 5–60. Tapping starts a timer (the icon turns to pause, the shape fills, and at the goal the day is marked done).
  - Holding a count or time habit, or tapping **Today** on its page, opens a **log drawer**: big number, linear progress bar, +1/+2/+5 steps (or +5/+10/+15 min), a type-the-total field, and Reset / Cancel / Save. Past days open the log.
  - Times a week is now a 1–6 connected group, not a stepper.
  - Samples: Drink water (count, 5/8), Meditate (time, 4/10 min), and Read 20 pages on the New habit board.
  - A ring around the shape was tried for time habits and dropped, because it cut through the shape's corners.
- Later (owner): the habit reminder notification. Still open: habit search results, reorder, the habit widget, boards for the empty, edit, archived and past-day states, and rings vs heat map.
- Not done: habit reorder, habit search results, and the habit widget.

## Owner rule (2026-11-26): say less

Imply the action from the UI. No hint boxes or helper sentences, and no repeating a value in a section header when the picked chip or field already shows it. Keep only the text a person needs to act. This applies to every screen.

## Session 9 (2026-11-27): Habits H1–H8, canvas version 87

- **Publishing:** a publish is refused until every file it sends has been read by its path in the session (listing isn't enough). Read all twins with `paths` before the first publish.
- **H1** `setDay`: a right swipe on a count habit adds one step (from 0 after "not done", else from the day's count), capped at the goal.
- **H2/H4/H5** New/Edit habit sheet: two segmented groups (`.hgrp` / `.hgi`, 28/8 corners, 3dp gaps, labels inside each item). Group 1 is Shape and Colour; group 2 is Goal, Repeat on and Reminder. Removed: the "Each tap adds…" step hint, the Time timer note, the times-a-week note, and the "· 10 min a day" / "· Every day" header values. Count is now a number field followed by the unit chips. The focused Reminder and Repeat drawers show their single item (`only`), with only the habit name as the subtitle.
- **H3** Time goal: an M3 Expressive Slider (XS: 16dp track, 4×44dp handle that narrows to 2dp while dragged, 6dp gaps, 4dp end stop; primary / secondaryContainer), 5 min to 5 h. Stops are 5 min apart up to 1 h, then 15 min to 2 h, then 30 min (22 stops), with the value above it ("1 h 30 min"). Arrow keys step. There's no value-indicator bubble because the value is always shown. The token values come from Compose SliderTokens from memory and aren't confirmed against the current release.
- **H6** "Remind me" shows only the time.
- **H7** Delete (page toolbar and sheet) opens a drawer: "Delete “Name”?", the check-in count in error colour, Cancel / Delete (errorContainer). Undo stays after. State `hDel`.
- **H8** `--fs` was already the bounciest Expressive token (fastSpatial 0.6/800), but its ~9.5% overshoot on a 14px lean is ~1px. The neighbours now spring back on the same 0.6/800 spring, run in JS, carrying the swipe speed (capped at 2 px/ms; adjacent rows ×0.5, next ×0.2) toward rest, as Compose's `animateTo(…, initialVelocity)` does. A fast swipe gives a few px of overshoot. Applies to tasks and habits.

## Next steps (not started)

**Tasks/Lists leftovers** (small; ask before doing):
- A **shape picker in New list**. 1.3.1 offers eight shapes (Hexagon, Triangle, Square, Pentagon, Gem, Cookie 6, Cookie 9, Clover); v2 assigns the shape automatically. Use only official shapes. There's no official hexagon, and Home currently uses ClamShell: ask.
- Canvas list shapes don't match the app (Work: app Cookie9 / canvas Circle; Groceries: Square / Circle; Reading: Cookie6 / Circle). Fix this alongside the picker.
- **Focus**: removed from Tasks at the owner's request. Ask where starting a focus session should live. The widgets still have a Timer and a Focus quick action.
- The quick times under the time dial (9 AM…): keep or drop?
- Repeat gaps: "ends after N times", and clearing Born in.
- The 24-hour dial ring order (outer 00–11, inner 12–23) isn't checked against Compose `TimePicker`.

**Next big pieces** (inventory §8–§12; one at a time, owner picks the order):
- **Habits tab**, **Calendar tab**, **Finance tab**, and the **Account page with Stats** (it also holds Settings, including the 12h/24h time format row).

**CLAUDE.md**: rules 2 and 5 and §1 still conflict with the redesign (see "CLAUDE.md rules that now conflict"). Ask before editing.

After the whole design is approved, start building it in Compose (CLAUDE.md §3).

## Session 10 (2026-11-27): habit select, reorder, states, widgets, canvas version 88

- New start states: habits select, habits empty, edit habit, archived habit, habits past day, habit reminder. Each has a dark/light pair: row y=7604 (Select, Empty, EditHabit, HabitArchived, HabitsPast) and HabitNotify at x=3760, y=6380.
- `HabitWidgets.dc.html` is a hand-edited board (390×500, x=4700, y=6380); gen.py writes `HabitWidgetsLight`.
- Habit selection lives in `hv` (`hSel`); the shared top bar and toolbar switch to habits through `hv.hSelecting` in the main return. `reorder()` accepts `.sw.hab` rows.
- A publish is refused for any file not read or published in the session; read twins by path first in a new session.

## Session 10b (2026-11-27): Calendar tab, canvas version 89

- `cv` block in renderVals (before `const snack`), spread into the main return. State: `calView` (Month/Week/Day), `calDay` (offset), `calOk`, `calExtra` (added events, in Undo), `evS` (New event drawer).
- Sample events are `EV` in `cv`. Events are tertiaryContainer and read-only; task blocks are their list colour at 34% over `--item` and open the task page.
- `calScroll()` scrolls the hours to an hour before now (today) or 7 AM; it runs on mount for the week/day boards and when switching views.
- The date sheet has an `event` target (title "Event date") that writes back to `evS`.
- Boards: Calendar, CalWeek, CalDay, NewEvent, CalAccess (dark/light) at y=8828; note `calTitle`.
- v91 (owner comment): the week strip is no longer a sticky card. It sits flat on the background and scrolls away with the header; the picked day is a filled Clover-4, today an outlined one. v90: smaller day-badge text.
- v92 (owner comment): the Today button sits at the right end of the month and year row.
- v93 (owner comment): drawers marked `.sheet.top` were shrinking to their content because the header class `.top` centres children; `.sheet.top` now stretches (fixes New event, Edit/New habit, delete confirm, pause). New event drawer has less side padding and wider buttons.
- v94 (owner comment): Day is the default Calendar view (the "calendar" board still shows Month); opening the tab on Day scrolls the hours to an hour before now.
- v95 (owner): events are Kairo items in the same lists as tasks, shown only in the Calendar tab, coloured by list (solid list colour, `--hon` text; tasks stay a 34% tint). Phone-calendar events (`phone:` field) stay tertiary/read-only behind calendar access. Events can span days (`day/from` → `toDay/to`); middle days show as all-day chips. New event drawer: From → To columns (date and time buttons open the date sheet as "Starts"/"Ends"; moving the start keeps the length), All day switch, quick lengths (30 min, 1 hr, 2 hr, 1 day), Time zone row → searchable drawer (8 zones, Nov offsets; local IST time shown under each side when another zone is picked; saved converted to device time), List chips, Reminder, Repeat, Note. Open: writing Kairo events to Google Calendar (Calendar Provider supports EVENT_TIMEZONE, RRULE, reminders, description), custom repeat, real time-zone database with DST.
- v96 (owner): Calendar dates use Cookie-9 marks (filled primary = picked day, outlined = today) in the month grid and week strip; dots sit inside the mark and turn onPrimary on the filled one (event dot = ring). No repeated dates: the hero badge shows only the weekday, the summary is "x tasks · y events", and Day view puts the date in the big title ("27 November 2026"). New event gains Location, Meeting link (provider tag for Meet/Zoom/Teams) and Attachments (file chips, Add file = system picker). Calendar access: "Allow calendar access" opens Android's runtime-permission dialog (board CalAsk at x=4700, y=8828); Don't allow → "Calendar access is off" card with Open settings.
- v97 (owner): New event has a Birthday switch (cake). On: Repeat = Yearly, 9:00–10:00 AM on that day, reminder At start. Birthday events show a cake icon in the day list.
- v98 (owner comment): New event List is a dropdown (row with the list dot and name, opens the Expressive menu with the lists; the picked one is highlighted with a check).
- v99 (owner comment): calendar access is a banner at the top of the Calendar tab (same component as "Reminders are off"): Allow → the system dialog, Not now hides it, Don't allow → "Calendar access is off" with Open settings. The in-page card is gone. The owner will also ask for calendar access during onboarding (not designed yet).
- v100 (owner): Calendar essentials. **Event page** (`evOpen`): title, list chip, when, repeat, reminder, location (Directions), meeting link (Join), attachments, note; toolbar Edit / Duplicate / Delete; phone events read-only with "Open in Google Calendar". **Edit** reuses the New event drawer (`evS.mode='edit'`); a repeating event asks This event / This and following / All events (`evScope`) on save and delete. Edits live in `evEdits` (in Undo). **Day list**: swipe (task: right done / left trash; event: left delete), long-press selects (`cSel`, keys e:/t:), toolbar Move to day (date sheet `calMove`), Move to list (move sheet now takes `evIds`), Duplicate, Delete; FAB Done when tasks are selected. **Week/Day blocks**: tap opens, hold 300 ms then drag moves in 15-min steps (`blockDrag`), bottom grip changes the end; phone events don't move. **Calendar drawer** (menu button on the Calendar tab): tick Tasks, each list, and phone calendars (`calHide`). **Search** in Calendar (events + dated tasks by day). **Clash** line in the drawer. **Event reminder** heads-up (Join, Snooze). Widgets **Next up** includes events. New boards row y=10052: EventPage, EditEvent, CalSelect, CalDrawer, CalSearch, EventNotify. Not done: events on the Tasks tab (owner said events show only in Calendar), selecting blocks in Week/Day, real Directions/Join intents.
- v101 (owner): Join and Directions are real links (anchors, new tab): Join opens the meeting URL (event page row, main button, reminder notification); Directions opens a Google Maps search for the location. In the app these are ACTION_VIEW intents (Meet/Zoom/Teams app, Maps). Owner: this completes the Calendar tab design.

## Session 11 (2026-11-28): Finance tab, canvas version 102

Owner brief: expense / income / transfer with title, category, amount, account, tags, description, date-time; accounts (UPI, Cash, HDFC Bank…) with a total that counts only ticked accounts; recurring bills with Pay / Skip; a Reports page (accounts, date range like Feb–Mar, categories, tags); Ivy Wallet import (format in `data/money/IvyImport.kt`: Date, Title, Category, Account, Amount, Currency, Type, Transfer Amount, Transfer Currency, To Account, Receive Amount, Receive Currency, Description, Due Date, ID; planned payments = Due Date without Date). UI only; import logic later.

- Logic is the `fv` block (before `const snack`), spread into the main return. State: `fView` (Activity/Accounts), `fMo` (month offset), `fType`, `fAcc` (account filter), `fEx` (accounts left out of the total), `fTx`/`fTxEd` (added/edited transactions), `fBills`/`fBillEd`, `fS` (add/edit drawer), `fOpen` (transaction page), `fRep`/`fR`/`fFD` (reports and its filter drawer), `fBillsOpen`, `fImp`, `fAccS`, `fSel`.
- Activity: month title + Reports icon; Activity | Accounts connected group; spent card with wavy budget bar (₹40,000 sample budget), Income and Net chips; upcoming bills (next 3) with Skip / Pay (Got it for income); All / Expense / Income / Transfer filter; transactions by day with day net; swipe left deletes; long-press selects (Duplicate for today, Delete).
- Accounts: total balance + filter chips (tick = counted); account rows with In / Out this month; tap = Activity filtered to that account; Add account drawer.
- Add/edit drawer: type group, big ₹ amount, title, Category and Account dropdown menus (From/To + swap for transfers), date & time (date sheet target `finWhen`), Repeats monthly (adds a bill), tags (type + Enter), description.
- Transaction page: amount, title, category, account, when, tags, description; Duplicate, Delete, Edit.
- Reports: Spending | Income; filter chips Range (presets + custom From/To month steppers), Accounts, Categories, Tags → total, monthly average, bars by month, selected accounts' balance, by-category bars with %.
- Bills page, Finance drawer (Bills, Reports, Add account, Import from Ivy Wallet, Export as Ivy Wallet CSV), Ivy import preview (counts, new accounts/categories, planned payments → bills switch).
- Sample data is invented (the owner's real CSV was read for structure only: 2 accounts, 3 categories, 1 planned payment). Six earlier months are generated for Reports.
- Boards row y=11276: Finance, FinAccounts, FinAdd, FinTxn, FinReports, FinBills, FinImport.
- Not done: budgets editing and per-category budgets, categories/tags management, multi-currency, account page with sparkline, app-lock amount hiding.
- v103 (owner): Upcoming bills is one collapsible row ("3 due · Spotify is overdue"), collapsed by default (`fBillsExp`), with All bills at the end of the list. Accounts are a two-column grid of 156dp tiles (icon, name, balance, +in / −out) with a check in the corner that counts the account in the total; the total card has a share bar (pill segments, 3dp gaps). Reports ask for filters first (`report filters` board: Show, Period with Custom From/To real dates via the date sheet with `past: true`, Accounts / Categories / Tags chips with All, then Show report); the report shows one tappable summary row (period · scope, Edit) instead of filter chips. Bars follow the owner's M3 Expressive reference: full pill, 3dp gaps, blue (`--cb`) or green (`--cg`) when the period beats the average, with a Cookie-12 check badge (`--cgl`), an average line, and buckets by day / week / month by range length. Chart colours come from the owner's reference image (Google blue #0B57D0 / green #146C2E in light; #A8C7FA / #6DD58C in dark); whether they become the app-wide default is waiting on the owner (it would change CLAUDE.md rule 2).
- v104 (owner comment): the check badge sits inside the top cap of a green bar (as in the reference), not floating above it; shown only when the bar is tall enough.
- v105 (owner comment): report bars fill their column with 2px gaps (an old 36px width cap was leaving wide gaps); no ring on the picked bar, its label goes bold.
- v106 (owner comment): linear progress everywhere is the M3 Expressive wavy kind (`this.wavy(f)` → 200x12 SVG): the active part waves in the item colour, a 6-unit gap, then a separate flat outline-variant track with a stop dot. Used in Reports category rows, the Finance budget bar (turns error-red when over), and the habit log drawer.
- v107 (owner comment): a lone FAB sits in the bottom-right corner, not centred (`.tdock.fabr`, Bills page New bill).
- v108 (owner comment, second try): New bill FAB pinned inline to the bottom-right corner (16dp from the side, 20dp from the bottom, like the main FAB).
- v109 (owner comment): wavy progress lines a little thicker (stroke 4 → 5.5, box 14px).
- v110 (owner): transaction page: tapping the amount/title or any row (Category, Account, Date and time, Tags, Description) opens the edit drawer; no separate Edit button (toolbar keeps Duplicate and Delete). Larger: 64dp badge, 57px amount, 24px title, 76dp rows with a small label over an 18px value and a round icon.
- v111 (owner): Calendar in the Finance style. The now line sits behind blocks (z 0), its dot on top (`.cnowd`). A summary card in every view replaces the Sunny badge and the plain week line: label (Today / weekday), big "5 events" (or No events / Free day), chips for tasks (open · done), time booked, and Next (today only). The week strip is a card with a pill bar per day showing how full it is (timed events + 30 min per timed task, blue `--cb`, picked day green `--cg`, empty days grey). Day-list and search rows use tinted round icon tiles (`.fic`) like Finance. Class name `empty` is taken by the empty state; the week bar uses `none`.
- v112 (owner): the v110 transaction-page change is reverted; the page is back to the v102 layout with the separate Edit button.


## Session 12: next session starts here (written 2026-11-28)

**State.** Live canvas = backup = **version 112** (https://claude.ai/artifact/XuXAdkKY8P3N7g4Ps5GiHa). Design only; nothing committed. Board rows on the canvas (each a dark/light pair, 470px apart, pairs 940px apart): Tasks rows y=0…4896, Habits y=6380 and 7604, Calendar y=8828 and 10052, Finance y=11276 (8 pairs, last at x=6580). The next free row is **y=12500** (title note at y=12240).

**What each tab has (short).**
- Tasks: approved at v65 (see Sessions 3–7d).
- Habits: H1–H8 (v87), multi-select + drag reorder, state boards, habit reminder, habit widgets (v88), wavy log bar (v106).
- Calendar: Month (default board) / Week / Day (Day is the default view); summary card, week strip with busy pills, Cookie-9 date marks; events in task lists with From→To, time zone, location, meeting link, attachments, birthday, reminder, repeat; event page with Join/Directions links; edit/duplicate/delete with repeat scope; swipe + multi-select; drag to move/resize; calendar drawer (lists, tasks, phone calendars); search; clash line; access banner + Android permission dialog; event reminder. See Sessions 10b and the v89–v111 notes.
- Finance: Activity | Accounts, collapsible bills (collapsed), account tiles with a count-in-total check, add/edit drawer, transaction page (separate Edit button; the v110 tap-anywhere change was reverted), filter-first Reports with Expressive pill bars (blue/green, Cookie-12 check inside green bars, 2px gaps), Bills page (FAB bottom-right), Ivy import preview, drawer, search, multi-select. See Session 11 and v103–v112.

**Decisions waiting on the owner (ask first).**
1. **App colours:** the owner liked the Google blue/green from their reference (`--cb` #0B57D0/#A8C7FA, `--cg` #146C2E/#6DD58C). Today only charts use them. Making them the app default replaces the Stash palette (CLAUDE.md rule 2). Settle this first; it touches every board.
2. Habits top card: week rings or week heat map (both boards exist).
3. Focus: where starting a focus session lives.
4. Quick times under the time dial: keep or drop.
5. CLAUDE.md rules 2 and 5 and §1 conflict with the redesign; edit only with the owner's OK.

**Still to design.**
- **Account page** (avatar button): Stats from 1.3.1 (tasks done, streaks, on-time %) and Settings (theme and colours, 12/24-hour time, currency, app lock + hide amounts, reminders, calendar access, Ivy import/export, backup, About/updates). Give the owner 2–3 short layout options first.
- Finance extras: set the budget, per-category budgets, categories and tags management, account page with a balance line, hide amounts under app lock, multi-currency.
- Smaller: habit search results; New list shape picker (official shapes only; no official hexagon) and the three mismatched sample list shapes; Repeat "ends after N times" and clearing "Born in"; onboarding (owner wants calendar and notification access asked there).
- After the owner approves the whole design: the Compose build (CLAUDE.md §3).

**How the owner works (this session).** Short, plain answers. Comments on the canvas arrive one by one; answer each with one line in chat (the comment tool is denied). They often send several asks mid-turn; finish the current publish, then do the new ask. They revert things they don't like, so keep the previous markup reachable (the original Finance overlay markup is in the scratch `fin/over.html` of session 11, not in the repo; prefer small, reversible patches). "Say less" in the UI; Material 3 Expressive look; official shapes only; drawers, not dialogs.


## Session 13 (2026-11-28): owner decisions, blue/green palette, Focus tab → canvas versions 113–114

**Decisions (owner).**
1. **Colours:** Google blue/green everywhere (CLAUDE.md rule 2 updated). Tokens in Main, Widgets and HabitWidgets: dark primary `#A8C7FA` / onPrimary `#062E6F` / primaryContainer `#0842A0` / onPrimaryContainer `#D3E3FD`, secondary `#7FCFFF` / secondaryContainer `#004A77` / onSecondaryContainer `#C2E7FF`, tertiary `#6DD58C` / tertiaryContainer `#0F5223` / onTertiaryContainer `#C4EED0`, inversePrimary `#0B57D0`. Light: primary `#0B57D0` / `#FFFFFF` / `#D3E3FD` / `#041E49`, secondary `#00639B` / `#C2E7FF` / `#001D35`, tertiary `#146C2E` / `#C4EED0` / `#072711`, inversePrimary `#A8C7FA`. These are Google's GM3 baseline values **from memory, not checked** against a source. Error, surfaces and list colours unchanged.
2. Habits top card: week rings stay (the heat-map board stays as a variant only).
3. Quick times under the time dial: dropped. Date sheet Clear now clears only the time when the Time part is open.
4. CLAUDE.md §1, rule 2 and rule 5 updated (rule 5: pill + Focus as 5th tab).
5. **Focus is a 5th tab** (Tasks · Habits · Calendar · Finance · Focus, icon `timer`).

**Focus tab (`zv` block before `const snack`; state keys start with `z`).**
- Idle: "Focus on" row (opens the picker drawer), connected group Pomodoro · Timer · From – To + a tune button (settings), clock card in the chosen style, then per mode: Pomodoro presets 25·5 / 50·10 / 15·3 + Custom (drawer: Focus and Break XS sliders, Sessions 1–8); Timer: XS slider 5 min → 10 h; From – To: two time buttons (open the date sheet time-only, `timeOnly`, targets `focusFrom`/`focusTo`), "Up to 10 hours" error. Start (or "Start at 2:00 PM" when From is later; the run waits, play starts it now). Below: Today card (total, wavy goal bar, sessions / % of goal / gave up) → Focus stats.
- Picker drawer: search, No item (Just a timer), Events today, Tasks (today + overdue), Habits (time habits). An event sets From – To to its times (Timer with its length if it ended); a habit sets Timer to the minutes left.
- Running screen (full screen, nav hidden): Hide (down arrow → a running card on the tab), clock style group (Ring = big CircularWavyProgress, Liquid = official Cookie-12 that drains with a drifting wave, Minimal = big digits + wavy linear bar), item chip, session dots, Stop · Pause/Resume · +5. Stop opens a drawer: Give up / Keep going / Save (stopped early). Break: Stop skips to the next focus.
- Done: Cookie-12 hero (tertiaryContainer, slow spin), "All 4 sessions done", Mark task done / Another round / Close; between sessions "Time for a break" or "Break over".
- Settings drawer: daily goal 1–4 h, clock style, When time is up (Chime / Vibrate / Silent), Start breaks by themselves, Do not disturb while focusing, Keep the screen on.
- Focus stats (page): This week / Last 30 days; total, bars per day with the goal line (green + Cookie-12 check when the goal is met, same as Finance reports), tiles Finished / Stopped early / Gave up, "When you focus" 2-hour strip with best hours, streak and longest; By item (tap filters History); History by day with outcome ("Finished", "Stopped at 32 min", "Gave up at 4 min"). A session opens a drawer with Focused / Paused / Planned and a timeline (Started, Paused 2 min, Back to it, Finished/Stopped/Gave up), Delete (Undo) and Focus again.
- Boards: row y=12500 Focus, FocusTimer, FocusRange, FocusPick, FocusCustom, FocusSettings; row y=13724 FocusRun, FocusLiquid, FocusMinimal, FocusStop, FocusDone, FocusStats, FocusSession (title notes at y=12240 and 13464). Next free row: **y=14948**.
- Sample history is generated (seeded) plus four sessions today; events are copied from the Calendar sample (`ZEV`), not shared.

**Not confirmed / open.**
- GM3 colour values (above). Liquid wave drift is a decorative loop (like `slowSpin`); not approved yet.
- Ring wave count/amplitude for the big clock (30 waves, 1.5 amplitude) are eyeballed, not from Compose.
- Not designed: Focus search results, the focus widget/notification in the new style, linking a habit's timer goal to Focus from the Habits tab, Account page.

**Publishing gotcha (this session).** One publish carries at most 64 MB, and the 131 boards are ~63 MB, so publish in two calls. The "haven't viewed the latest version" refusal went away only when the files were published from the same folder the `read` saved them into (`out_dir`): read every file with `paths` into `<scratch>/live/`, copy the edited files over that folder, publish with `root` = that folder.
