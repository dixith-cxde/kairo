# Kairo

> *Kairos* (Greek): the right moment — not clock time, but the time that matters.

Kairo is a personal Android utility app: **tasks, reminders, a calendar, money, and stats** in one place, later **focus, habits, alarms and timers**. It is designed in **Material 3 Expressive, all the way down** — springs, shape morphing, emphasized type, wavy progress — and it keeps everything **on the phone**. No account, no server.

Status: **design phase. No code yet.** Code starts only after the design is signed off.

## Where things are

| File | What it holds |
|---|---|
| [CLAUDE.md](CLAUDE.md) | Working instructions for Claude (and anyone) building Kairo. Read first. |
| [docs/01-research.md](docs/01-research.md) | What the research found: M3 Expressive, Google's own apps, widgets, calendar sync. |
| [docs/02-principles.md](docs/02-principles.md) | The design principles and the deliberate departures from Google's spec. |
| [docs/03-design-system.md](docs/03-design-system.md) | Tokens: color, type, shape, motion, components, with the Compose mapping. |
| [docs/04-screens.md](docs/04-screens.md) | Every screen in the design, what it shows and how it moves. |
| [docs/05-widgets.md](docs/05-widgets.md) | Home-screen widgets (Jetpack Glance). |
| [docs/06-data-and-calendar.md](docs/06-data-and-calendar.md) | Local data model and the Google Calendar connection via Calendar Provider. |
| [docs/07-roadmap.md](docs/07-roadmap.md) | Phases and the step-by-step build plan. |

## The design canvas

All screens live on a Claude Design canvas, in dark (default) and light:
**https://claude.ai/artifact/RthtmHKkEsUMm2WUMRUE49** (private to the owner).

Rows on the canvas: Foundations · Tasks, lists & reminders · Plan & review (+ phase-2 Focus & Habits) · Money · App bars & home-screen widgets. 45 artboards; every dark board has a light twin below it. Several boards are interactive (Today, Stats, Money, Add expense, Money insights, Motion, App bars, Widgets).

The reference images that started this are in `../resources/`.

## Stack at a glance

- Kotlin + Jetpack Compose, Material 3 Expressive (`MaterialExpressiveTheme`, `MotionScheme.expressive()`).
- Room for storage, DataStore for settings, WorkManager / AlarmManager for reminders.
- Android Calendar Provider for Google Calendar (read events, write timed tasks). No OAuth.
- Jetpack Glance for widgets.

## Scope

- **v1:** Today, tasks + lists, reminders, calendar, stats/graphs, **Money** (accounts, income, expenses, transfers, budgets, insights, recurring bills), widgets.
- **Phase 2:** Focus (pomodoro) and Habits.
- **Later:** alarms and timers.
