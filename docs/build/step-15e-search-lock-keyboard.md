# Search, app lock, keyboards (2026-11-21)

- **Softer triangle**: the list/category triangle is now a custom `RoundedPolygon(3)` with large, smoothed corners (radius 0.42, smoothing 0.8), pointing up — still reads as a triangle but has room for the initial.
- **Money sheet**: the custom keypad is gone. The amount is a real text field with the **system number keyboard** (`KeyboardType.Decimal`) and live **₹ Indian grouping** while you type (a `VisualTransformation` keeps the stored text as plain digits). **Title** and **Tags** use the normal keyboard. For **income** (e.g. salary) both category and title are **optional**; expenses still need a category so budgets and insights work. Tap a picked category again to clear it.
- **Universal search**: a wide **Search pill** (icon + "Search") in every tab's app bar opens a full search screen (M3 `SearchBar` + input field) across tasks (title, notes), lists and money (title, tags, category), with results animating in.
- **Titles**: the Expressive large app bar uses the theme's `displaySmall`; it was regular weight. Display, headline and large-title styles now use Google Sans Flex **fully rounded (ROND 100)**, and `displaySmall` is SemiBold (the design rule: screen titles = displaySmall emphasized). Body text stays at ROND 50.
- **Settings button**: filled gear on a tonal, press-morphing `FilledTonalIconButton`.
- **App lock** (like Stash): Settings → Privacy → App lock. Uses Android's own `BiometricPrompt` (fingerprint/face, falling back to PIN/pattern/password; `androidx.biometric` 1.1.0 stable). Turning it on asks you to prove it's you; it needs a screen lock on the phone. **Lock after**: Immediately / 1 min / 5 min. Covers the app with a lock screen and opens the prompt automatically; returning from the PIN screen doesn't re-lock. On Android 13+ the recents thumbnail is hidden while the lock is on.
- Overdue dates read "Yesterday", "3 days ago", or "Mon, 16 Nov" instead of a raw date.

Not tested by me: the actual unlock (needs your fingerprint/PIN).
