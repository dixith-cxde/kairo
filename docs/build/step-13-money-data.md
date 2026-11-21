# Step 13 · Money data

**Status:** built 2026-11-21, compiles.

## Tables (Room v2, auto-migrated from v1)

`accounts`, `categories`, `transactions`, `budgets`, `recurring`, following docs/06-data-and-calendar.md.

- **Amounts are integer paise** (`Long`), never floating point. Keypad text is parsed straight to paise (`Money.parsePaise`).
- Expense and income amounts are positive. A **transfer is two rows** linked by `transferPeerId`: the from-leg negative, the to-leg positive. Deleting one leg deletes both.
- Balance = opening balance + income − expense + transfer legs.
- Recurring rules are "every month on day N" for v1; `pay()` records the transaction and moves the next due date a month on.
- `Money.format()` gives **₹ with Indian grouping** (₹1,23,456; paise only when not zero) via the platform's `en-IN` currency formatter (rule 11).

## Seeding

Default categories (8 expense, 2 income), each with an icon, a list color and a shape. Release: accounts Personal + Cash at ₹0. Debug: four accounts with balances, a monthly budget (₹40,000) plus three category budgets, ~45 sample expenses, a salary, and four bills/subscriptions.

## Migration

`@AutoMigration(from = 1, to = 2)`: Room generates it from the exported schemas in `app/schemas/`. Existing task data is kept.

Note: Money uses the Room rows directly as models (they're plain data), unlike tasks.
