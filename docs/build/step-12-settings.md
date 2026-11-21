# Step 12 · Settings

**Status:** built 2026-11-21, compiles. Not yet run on the phone.

**New idea:** DataStore (small settings on disk, read as a `Flow`).

## What's there

- Open from the **settings** icon on Today's app bar.
- **Colors from wallpaper** (dynamic color via `dynamicDark/LightColorScheme`, Android 12+).
- **Six seed colors** from the canvas (Indigo #4859AA, Clay #8B4A33, Moss #386A20, Rose #984061, Teal #006A6A, Violet #6750A4), plus the default (#79D0FD). Each swatch shows primary / secondary / tertiary-container and **morphs circle → Cookie9 when picked** (`Morph` + `defaultSpatialSpec`). Picking a seed turns wallpaper colors off.
- **Theme**: System / Light / Dark (default Dark, rule 1). **Contrast**: Standard / Medium / High. Both are M3 Expressive connected button groups.
- **Exact alarms** status row; tap to open Android's "Alarms & reminders" permission page if not allowed.
- Whatever the color source, **surfaces stay neutral gray** (rule 3): `kairoScheme()` swaps them back in.

## Where colors come from

`ui/theme/SeedSchemes.kt` is **generated** with Google's `@material/material-color-utilities` 0.3.0 (`SchemeTonalSpot`, contrast 0 / 0.5 / 1), for 7 seeds × light/dark × 3 contrasts. The default standard scheme matches docs/03-design-system.md exactly. Generating ahead of time avoids adding a color library to the app.

## Not yet (honest)

- Contrast doesn't apply to wallpaper colors (Android's dynamic scheme has its own contrast setting), so the buttons are disabled then.
- Backup / export, default reminder time, morning summary, "add timed tasks to calendar" aren't built.
- The window background before Compose draws is always dark, so a light theme shows a brief dark frame on launch.
