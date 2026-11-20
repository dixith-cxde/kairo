# Research notes

What we learned while designing Kairo, and where it came from. Facts here drove the decisions in the other docs. Dates: research done November 2026.

## 1. Material 3 Expressive (M3E) in one page

Google's 2025 update to Material 3 (Android 16, Pixel apps, Wear OS 6). It is still Material 3 — same color roles, same components — with five additions:

1. **Springs instead of easing curves.** `MotionScheme.expressive()` gives spatial springs (position, size, shape; may overshoot) and effects springs (color, opacity; never overshoot), each in fast / default / slow.
2. **A shape library.** 35 shapes (`MaterialShapes`: cookies, sunny, clover, burst, gem, pill, arch …) that morph into each other, plus a 10-step corner scale.
3. **Emphasized type.** Every type role has a heavier "emphasized" twin, used for titles, numbers and actions. Google Sans Flex with rounded variation.
4. **New / updated components:** button groups (connected and standard), split button, FAB menu, floating toolbars, loading indicator, wavy progress indicators, flexible app bars, toggle buttons, segmented lists with gaps.
5. **Containment and hierarchy:** grouped items in containers with small gaps (2dp) and big outer corners, so screens read as a few clear blocks.

Google's guidance: use expressive moments sparingly — one or two hero moments per flow, not everything bouncing.

### Spring values we use (from AndroidX tokens)

| Token | Damping ζ | Stiffness k | Use |
|---|---|---|---|
| fastSpatial | 0.6 | 800 | things a finger caused (press, toggle, checkbox) |
| defaultSpatial | 0.8 | 380 | components moving or resizing |
| slowSpatial | 0.8 | 200 | full-screen and container transforms |
| fastEffects | 1.0 | 3800 | quick color/opacity |
| defaultEffects | 1.0 | 1600 | most color/opacity |
| slowEffects | 1.0 | 800 | large fades |

Reduced motion: switch to `MotionScheme.standard()` (same speeds, almost no bounce).

## 2. What Google's own Expressive apps do (patterns we copied)

- **Reorder (Google Tasks / Keep):** the lifted item turns tertiary container, corners grow, slight elevation.
- **Swipe actions:** the revealed main action uses primary with 16dp corners.
- **Text fields:** rounded (12dp) filled fields, not underlined.
- **Google Calendar month view:** rounded day cells with gaps between them; today is a filled shape.
- **Google Clock:** big numbers need tabular digits or they jitter while counting.
- **Google Wallet:** thicker cards; success shown with a scalloped/cookie shape.
- **Android 16 Settings:** gray screen, white (or dark gray) grouped containers, colored circular icons. This is the base for Kairo's "always gray" surfaces.
- **Charts:** M3 has **no** chart spec. Our charts are built from M3 pieces: rounded bars, wavy rings, shape-cornered segments, emphasized tabular numbers.

## 3. Home-screen widgets

From the Android widget design guides (style, sizing, layouts, quality tiers) and Glance docs:

- Outer corners use the **system widget radius** (`system_app_widget_background_radius`, 28dp max, newer Pixels 24dp). Never set a custom radius.
- Inner corners = outer radius − padding (12dp padding is common → 12–16dp inner).
- Widgets fill their grid cell. A widget holding one thing (photo, weather, progress) can be **one whole expressive shape**; complex widgets use shapes only for hierarchy or a call to action.
- Use Material color roles; support dynamic color and **both light and dark** (required for the higher quality tiers).
- Touch targets ≥ 48dp.
- Resizing: Tier 1 asks for 2×1 plus at least two of 1×1, 2×2, 4×2, 4×4. More space should show more content, not more empty space.
- Canonical layouts: text; toolbar (standard, search, expressive); list (text+image, checklist, action list); grid.
- Expressive type: bolder headlines, labels and data.
- **Glance limits:** no springs, no shape morphing, no custom animation. Updates crossfade. Shapes must be drawn as static vector drawables. The platform `CheckBox` works in widgets from Android 12.
- Google itself was slow to update widgets to M3E (Keep and Gemini first; At a Glance got more compact).

Sources: developer.android.com/design/ui/mobile/guides/widgets (style, sizing, layouts, widget_quality_guide), developer.android.com/develop/ui/compose/glance/create-app-widget, Android Authority's M3E widget pack review, 9to5Google's Android 16 M3E coverage.

## 4. Google Calendar without sign-in

- The **Android Calendar Provider** (`CalendarContract`) exposes every calendar already synced on the phone, including Google accounts. Kairo needs only `READ_CALENDAR` (and `WRITE_CALENDAR` to add tasks). No OAuth, no server.
- You get calendars (name, color, account), events including recurring instances (`Instances` table), times, location, attendees, reminders.
- Writing an event into a Google-synced calendar makes Android sync it up to Google by itself.
- Limits: only calendars that are synced to the device; changes appear when Android syncs, not instantly; **Google Tasks is not in the provider** (needs the Tasks API + OAuth).
- Alternative considered and rejected: Google Calendar's secret iCal URL — read-only, needs internet, one calendar at a time, and the URL is a secret.

## 5. Stash learnings carried over

From `~/coding/reactnative/stash/docs/m3-expressive.md` and `docs/animation.md`:

- Floating curved nav + FAB connected group works well and the owner likes it.
- The `@material/material-color-utilities` palette generation (SchemeTonalSpot 2021, seed `#79D0FD`) produced the colors the owner wants as default.
- Puffy and heart shapes looked wrong; banned.
- Haptics kept to save, cancel, delete.

## 6. Honest gaps

What the design canvas cannot show, and where Compose will do better:

- **Real shape morphing** (point-by-point `Morph`) — the canvas only fakes it with polar shapes and CSS.
- **Interruptible springs** that keep velocity and follow the finger (drags, sheets, predictive back).
- **The real components** — the canvas has HTML look-alikes.

Deliberate departures a strict M3 review would flag: the floating nav, always-gray backgrounds, the custom money keypad. All three are the owner's choices.
