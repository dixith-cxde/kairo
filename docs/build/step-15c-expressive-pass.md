# Expressive pass (2026-11-21)

Owner: "real Material 3 Expressive everywhere"; selection and the top action icons looked native; Completed overlapped; tab switches still abrupt; wants calendar refresh.

## Selection
- Rows use the **selectable `SegmentedListItem(selected = …)`** overload: the component itself morphs the row to its selected shape and `primaryContainer` color (no hand-made corners).
- Leading slot becomes a **select badge**: outlined circle → filled Cookie9 with a check (`Morph`, fastSpatial), like Google's own apps.
- Actions moved off the top bar into an **M3 Expressive `HorizontalFloatingToolbar` (vibrant colors)** that springs up from the bottom; the floating nav steps aside while selecting. The top bar is just ✕, "N selected" (counts), All/None.
- **Move to list** opens a bottom sheet of lists (selectable segmented rows with each list's shape), not a dropdown.

## Menus and buttons
- List pickers (quick add, task detail) use the **Expressive grouped menu**: `DropdownMenuPopup` + `DropdownMenuGroup` + `SelectableDropdownMenuItem` (rounded group, the current list highlighted).
- Every `IconButton`, `TextButton`, `FilledTonalButton`, `Button`, `Filled(Tonal)IconButton` now passes Expressive **`shapes = …Defaults.shapes()`**, so they morph (round → squarer) when pressed.

## Today
- Completed is one expandable group: the header is segment 0, done tasks are the next segments (with the 2dp gap). No more overlap. Chevron turns on the spatial spring.
- Section header no longer clips the Reschedule button.

## Motion
- **Nav pill stretches**: its leading edge follows the new tab on `fastSpatialSpec`, its trailing edge on `slowSpatialSpec`, so it stretches toward the tab, then catches up and settles with a bounce.
- **Page transitions**: slide half a screen in, the old page slides a third out; both scale 0.92↔1 on `slowSpatialSpec` with fades, so pages glide instead of cutting.

## Calendar sync
- **Live**: a `ContentObserver` on the Calendar Provider reloads the screen whenever the phone's calendar changes (e.g. a Google sync lands). Push-based, so it costs nothing while idle — better than polling every 5 minutes.
- **Pull to refresh**: M3 `PullToRefreshBox` with the Expressive **`LoadingIndicator`** (morphing shapes); it asks each synced calendar account to sync now (`ContentResolver.requestSync`, expedited), then reloads.

## Honest notes
- adb taps lag on this phone, so I could verify the screens but couldn't capture the nav pill mid-stretch cleanly. Please judge the feel by hand.
- `slowSpatialSpec` page transitions may feel too slow; if so we can use `defaultSpatialSpec`.
