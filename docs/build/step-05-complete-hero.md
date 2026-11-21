# Step 5 · Complete-a-task hero moment

**Status:** built 2026-11-21, compiles. Not yet run on the phone.

**New ideas:** `animate*AsState` and `Animatable` with springs from `motionScheme`.

## The sequence (hero moment #1, rule 10)

1. Tick → the checkbox fills (`primary`), the title strikes through and fades to `onSurfaceVariant` (`defaultEffectsSpec`), **haptic** `ToggleOn` (rule 9).
2. A `MaterialShapes.SoftBurst` in `tertiary` grows from behind the checkbox, turns a little and fades (`Animatable` on `fastSpatialSpec`).
3. **When the burst spring settles**, the task is saved as done. No hard-coded delay: the animation finishing is the trigger.
4. The row leaves Today; the rows below slide up (`Modifier.animateItem` with `defaultSpatialSpec` placement, `fastEffectsSpec` fade).
5. Snackbar "Completed “…” · Undo" above the floating nav. Undo → haptic `Confirm`, task restored.

## Ideas

- **`animateColorAsState(target)`**: set a target, Compose animates to it. Good for "this should look like X now".
- **`Animatable`**: an animation you drive yourself from a coroutine: `snapTo(0f)`, then `animateTo(1f, spec)` *suspends until it finishes*. Good for one-shot sequences like the burst.
- **Springs, not durations**: every spec comes from `MaterialTheme.motionScheme` (expressive), so the feel matches the rest of Material 3 Expressive.
