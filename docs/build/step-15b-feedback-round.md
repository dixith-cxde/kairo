# Feedback round 1 (2026-11-21)

Owner tested steps 1–15 on the phone and asked for more life and better interaction. What changed:

## Motion
- **Nav**: one pill slides between tabs on the expressive spatial spring (overshoots a little, then settles — the Stash feel); the new icon pops in (fast spatial spring). No labels (owner, earlier).
- **Page transitions**: tabs slide in from the side you're moving toward (left/right), detail screens come in from the right and go back to the right; spring + fade. Predictive back slides too.
- **Entrances**: cards and rows rise and fade in, staggered one beat per row, the first time a screen shows (`ui/common/Motion.kt`, `rememberEntrance`). Rows scrolled back into view don't replay.
- **Numbers count up** (Today ring count, list tiles, income/spent, stats totals and %).
- **Hero shapes turn slowly** (Today hero cookie, Calendar day Sunny badge, streak Sunny, account card marks). *Rule change:* the motion scheme has no looping token, so these ambient loops use a written duration (`slowSpin`, 30–60 s). Everything else still uses `motionScheme`.
- Today ring fills from empty; Calendar months slide like pages; the day badge pops when you pick a day; Stats bars grow up one after another.

## Create menu
The + opens the **M3 Expressive FAB menu**: Task, Expense, Income, Transfer, List. The + turns into × (spatial spring), the screen dims below the nav.

## Lists
- List shapes now: **hexagon** (built with graphics-shapes, rounded like MaterialShapes), **rounded triangle**, **rounded square**, pentagon, gem, cookie 6/9, clover. **Arch (semi-circle) and Sunny are retired** for lists and categories; existing rows are migrated at startup.
- **New list** is a button beside the "My lists" header.
- New smart list **Archived**.

## Multi-select (long press)
- Long-press a task (Today, list detail, Calendar) or a transaction (Money) to select; tap to add/remove more. Selected rows turn primaryContainer and round to 16dp. A contextual bar slides down over the app bar.
- **One task**: open, duplicate, move to list, archive, delete. **Several**: complete, move, archive, delete. In Archived: unarchive, delete.
- **Transactions**: duplicate (one, non-transfer), delete (any; transfers delete both legs).
- Delete, archive and bulk complete show **Undo** in one app-wide snackbar above the nav. Haptics only on complete / delete / undo (rule 9).
- Database **v3** (auto-migrated): `tasks.archived`.

## Money carousel
Cards now show an account-type icon in a Cookie4, a large slowly turning Cookie9 mark, name + balance at the bottom. Text follows the visible part of the card and **fades out as the card shrinks**, so it's never cut in half.

## Stats
Each card says what it is in one short line: "tasks done · last 7 days", "vs previous 7 days", "Done per day — highlighted: your best day", "On time — done by the due date", "Streak — days in a row with a task done", "By list — where your done tasks came from", heatmap "each square is a day · darker = more done" with a Less → More legend.

## Not done / open
- The **canvas** hasn't been updated with these changes yet (FAB menu, selection bar, new list shapes, stats captions).
- Multi-select isn't on the Account screen or Bills yet.
