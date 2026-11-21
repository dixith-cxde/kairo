# Step 14 · Money screens

**Status:** built 2026-11-21, compiles. Not yet run on the phone.

## Screens

- **Money tab**: flexible app bar with total balance; **M3 `HorizontalMultiBrowseCarousel`** of accounts (list-color containers, 28dp mask); Income / Spent tiles for this month; budget strip with `LinearWavyProgressIndicator` (error when over); Budgets / Insights / Bills buttons; recent transactions grouped by date (category shape badges, +₹ in tertiary).
- **Add (the + on the Money tab)**: Expense / Income / Transfer connected button group; big amount with **live ₹ Indian grouping**; **custom keypad** (paise-safe, max 2 decimals); **category shapes morph from circle to their own shape when picked**; account chips; transfer mode with From / To and a swap button; note with `#tags`; **scalloped Cookie12 success** blooms on save (`fastSpatialSpec`), `Confirm` haptic.
- **Account**: hero balance in the account's color, transactions by date.
- **Budgets**: **liquid jar** hero (Cookie9 filled to the share left, wavy surface, `slowSpatialSpec` level), "₹… left of ₹… · N days to go · ≈ ₹… a day"; per-category rows with wavy bars; over budget turns the row `errorContainer`.
- **Insights**: donut of rounded, gapped slices; tap a row to pull its slice out and dim the rest; center Cookie9 takes the category's color; "N% less/more than last month" pill.
- **Bills & subscriptions**: monthly total, bills and subscriptions with due dates and a **Pay** button (records the expense, moves the due date a month on).

## Not yet (honest)

- **FAB menu** (Task / Expense / Income / Transfer / List): for now the + is context-aware (Money tab → transaction, other tabs → task). M3 has `FloatingActionButtonMenu`; wiring it into the connected nav group is polish work.
- The jar's wave is **static**. The canvas animates it continuously, but the motion scheme has no token for a looping animation, and rule says no hand-written durations. Open question for the owner.
- The donut center cookie doesn't turn; no daily-spend bars with a dashed budget line yet.
- No editing/deleting transactions, no account/category/budget management screens, no tag filters.
- Bills aren't tied to reminders yet.
