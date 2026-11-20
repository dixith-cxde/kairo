# Roadmap

## Phases

| Phase | Scope |
|---|---|
| **0 · Design** (done 2026-11-20) | Canvas complete in dark + light, docs written, owner sign-off. |
| **1 · v1** (now) | Today, tasks + lists, quick add, task detail, reminders, calendar (read + write), stats, settings, Money (accounts, transactions, transfers, budgets, insights, recurring), widgets. |
| **2** | Focus (pomodoro) and Habits, with their graphs. |
| **Later** | Alarms and timers. |

## Build plan for v1

Step guides: [docs/build/](build/).

Each step runs on a phone at the end, and introduces at most one or two new Compose ideas, explained as they appear.

1. **Project + theme.** Android Studio project, Kotlin, Compose BOM, Material3 with Expressive APIs. `MaterialExpressiveTheme` with Kairo colors (gray surfaces override), Google Sans Flex, expressive motion. *Ideas: `@Composable`, preview, theme.*
2. **Floating nav + empty tabs.** Port Stash's floating nav + FAB; navigation between five tabs. *Ideas: `Modifier`, layout (Row/Column/Box), Navigation Compose.*
3. **Today, static.** Large flexible app bar, hero card, segmented task list with fake data. *Ideas: `LazyColumn`, state, `remember`.*
4. **Room + real tasks.** Entities, DAO, repository, `ViewModel`, `StateFlow`. *Ideas: ViewModel, Flow, coroutines.*
5. **Complete-a-task hero moment.** Checkbox, burst, strike, collapse, undo snackbar, haptic. *Ideas: `animate*AsState`, springs from `motionScheme`.*
6. **Quick add sheet** with natural-language parsing and FAB container transform.
7. **Task detail + date/reminder picker**, floating toolbar.
8. **Lists + list detail**, list shapes (`MaterialShapes`), reorder drag.
9. **Reminders:** notifications, exact alarms, actions, reboot.
10. **Calendar:** permissions flow, read Instances, month grid, agenda, write timed tasks.
11. **Stats:** bars, wavy ring, streak, heatmap, morphing badge (`Morph`).
12. **Settings:** dynamic color, seeds with shape morph, theme, contrast, reduced motion.
13. **Money data:** accounts, categories, transactions (paise), transfers.
14. **Money screens:** overview, add expense keypad, transfer, account, budgets jar, insights donut, recurring.
15. **Widgets** (Glance): Today, Progress, Money, Quick add, Next up, Streak.
16. **Polish:** empty/error/permission states, accessibility (TalkBack, font scale, contrast), predictive back, light theme pass, performance.

## Open questions

- ~~Final app icon~~ **Decided (2026-11-20):** the black cat-head mark (two ears, two smiling eyes) on neutral gray, with as few changes as possible. To ship it as an Android adaptive icon: drop the drawn rounded square and gradient (the launcher applies its own mask), use a flat neutral-gray background layer, keep the mark inside the 66dp safe zone of the 108dp canvas, and add a one-color monochrome layer for themed icons (Android 13+). Still to check: that the mark is not too close to another app's logo before publishing.
- Minimum Android version (proposal: Android 12 / API 31, for dynamic color and widget checkboxes).
- Currency other than ₹ and multi-currency accounts.
- Whether to add the optional Google Tasks import later (would need OAuth).
- "Kairo" name check on Play Store before publishing.
