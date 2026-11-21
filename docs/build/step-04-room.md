# Step 4 · Room + real tasks

**Status:** built 2026-11-21, compiles. Not yet run on the phone (disconnected).

**New ideas:** `ViewModel`, `Flow` / `StateFlow`, coroutines. (Room itself is the database library.)

## How data flows

```
Room (kairo.db) ──Flow──▶ TaskRepository ──Flow──▶ TodayViewModel ──StateFlow──▶ TodayScreen
                                  ▲                          │
                                  └──── setDone() ◀── toggle ┘
```

- **Room**: SQLite with Kotlin classes. `@Entity` = a table (`lists`, `tasks`), `@Dao` = the queries, `@Database` ties them together. KSP generates the SQL code at build time. Dates are stored as numbers (`Converters.kt`) so they sort correctly. Schemas are exported to `app/schemas/` for future migrations.
- **Flow**: a stream of values over time. `observeAll()` returns a Flow that emits a new list every time the table changes. You never "refresh"; the screen just updates.
- **Coroutines**: Kotlin's way to run slow work (disk, network) without freezing the UI. `suspend fun` = can pause; `viewModelScope.launch { … }` starts one that's cancelled when the screen goes away.
- **ViewModel**: holds a screen's state outside the composable so it survives rotation. `TodayViewModel` combines the tasks and lists Flows into one `StateFlow<TodayState>`; the screen collects it with `collectAsStateWithLifecycle()` (stops collecting when the app is in the background).
- **Wiring**: `KairoApplication` creates the database and repository once (manual wiring; a DI library like Hilt isn't worth it at this size).

## Seeding

First launch creates the 7 default lists (Work, Home, Groceries, Health, Family, Personal, Reading) with their shapes and colors. **Debug builds only** also add the canvas sample tasks, so the screen isn't empty while we build. Release builds start with no tasks.

## Versions

Room 2.8.5, KSP 2.3.14, Lifecycle 2.11.1 (checked 2026-11-21).
