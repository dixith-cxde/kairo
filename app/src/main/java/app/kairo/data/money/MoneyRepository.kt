package app.kairo.data.money

import app.kairo.data.db.AccountEntity
import app.kairo.data.db.AccountType
import app.kairo.data.db.BudgetEntity
import app.kairo.data.db.CategoryEntity
import app.kairo.data.db.KairoDatabase
import app.kairo.data.db.RecurringEntity
import app.kairo.data.db.RecurringKind
import app.kairo.data.db.TransactionEntity
import app.kairo.data.db.TxKind
import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.util.Random

data class AccountBalance(val account: AccountEntity, val balancePaise: Long)

/**
 * Money data. The Room rows are simple enough to use directly as models here.
 * Balance = opening + income − expense + transfer legs (signed).
 */
class MoneyRepository(db: KairoDatabase) {
    private val dao = db.money()

    val accounts: Flow<List<AccountEntity>> = dao.accounts()
    val categories: Flow<List<CategoryEntity>> = dao.categories()
    val transactions: Flow<List<TransactionEntity>> = dao.transactions()
    val budgets: Flow<List<BudgetEntity>> = dao.budgets()
    val recurring: Flow<List<RecurringEntity>> = dao.recurring()

    val balances: Flow<List<AccountBalance>> = combine(accounts, transactions) { accs, txs ->
        val byAcc = txs.groupBy { it.accountId }
        accs.map { a ->
            AccountBalance(a, a.openingPaise + byAcc[a.id].orEmpty().sumOf { t ->
                when (t.kind) { TxKind.Income -> t.amountPaise; TxKind.Expense -> -t.amountPaise; TxKind.Transfer -> t.amountPaise }
            })
        }
    }

    suspend fun add(kind: TxKind, amountPaise: Long, accountId: Long, categoryId: Long?, date: LocalDate, note: String, tags: String) =
        dao.insertTx(TransactionEntity(accountId = accountId, kind = kind, amountPaise = amountPaise, categoryId = categoryId,
            date = date, note = note, tags = tags))

    /** Two linked legs. */
    suspend fun transfer(amountPaise: Long, from: Long, to: Long, date: LocalDate, note: String) {
        val out = dao.insertTx(TransactionEntity(accountId = from, kind = TxKind.Transfer, amountPaise = -amountPaise, date = date, note = note))
        val inn = dao.insertTx(TransactionEntity(accountId = to, kind = TxKind.Transfer, amountPaise = amountPaise, date = date, note = note, transferPeerId = out))
        dao.updateTx(TransactionEntity(id = out, accountId = from, kind = TxKind.Transfer, amountPaise = -amountPaise, date = date, note = note, transferPeerId = inn))
    }

    suspend fun delete(tx: TransactionEntity) = dao.deleteTx(tx.id)
    /** Deletes (with transfer peers) and returns the rows for Undo. */
    suspend fun deleteAll(ids: List<Long>): List<TransactionEntity> = dao.txWithPeers(ids).also { dao.deleteTxs(ids) }
    suspend fun restore(rows: List<TransactionEntity>) = dao.insertTxs(rows)
    suspend fun duplicate(tx: TransactionEntity) = dao.insertTx(tx.copy(id = 0, date = LocalDate.now(), transferPeerId = null))

    /** Mark a bill paid: record the expense and move the next due date a month on. */
    suspend fun pay(r: RecurringEntity) {
        dao.insertTx(TransactionEntity(accountId = r.accountId,
            kind = if (r.kind == RecurringKind.Income) TxKind.Income else TxKind.Expense,
            amountPaise = r.amountPaise, categoryId = r.categoryId, date = LocalDate.now(), note = r.name, recurringId = r.id))
        dao.updateRecurring(r.copy(nextDue = r.nextDue.plusMonths(1)))
    }

    suspend fun seedIfEmpty(sample: Boolean) {
        dao.migrateShapes()
        if (dao.accountCount() > 0) return
        val acc = if (sample) listOf(
            AccountEntity(name = "Personal", type = AccountType.Bank, color = ListColor.Indigo, openingPaise = 85_000_00),
            AccountEntity(name = "Shared", type = AccountType.Bank, color = ListColor.Teal, openingPaise = 42_000_00, position = 1),
            AccountEntity(name = "Savings", type = AccountType.Bank, color = ListColor.Green, openingPaise = 2_40_000_00, position = 2),
            AccountEntity(name = "Cash", type = AccountType.Cash, color = ListColor.Amber, openingPaise = 6_500_00, position = 3),
        ) else listOf(
            AccountEntity(name = "Personal", type = AccountType.Bank, color = ListColor.Indigo),
            AccountEntity(name = "Cash", type = AccountType.Cash, color = ListColor.Amber, position = 1),
        )
        val accIds = acc.map { dao.insertAccount(it) }
        val cats = listOf(
            CategoryEntity(name = "Food", icon = "restaurant", color = ListColor.Coral, shape = ListShape.Cookie6),
            CategoryEntity(name = "Groceries", icon = "shopping_cart", color = ListColor.Green, shape = ListShape.Clover),
            CategoryEntity(name = "Transport", icon = "directions_car", color = ListColor.Indigo, shape = ListShape.Pentagon),
            CategoryEntity(name = "Shopping", icon = "shopping_bag", color = ListColor.Pink, shape = ListShape.Gem),
            CategoryEntity(name = "Bills", icon = "receipt_long", color = ListColor.Amber, shape = ListShape.Hexagon),
            CategoryEntity(name = "Health", icon = "medical_services", color = ListColor.Teal, shape = ListShape.Triangle),
            CategoryEntity(name = "Fun", icon = "sports_esports", color = ListColor.Violet, shape = ListShape.Cookie9),
            CategoryEntity(name = "Other", icon = "category", color = ListColor.Indigo, shape = ListShape.Square),
            CategoryEntity(name = "Salary", icon = "payments", color = ListColor.Green, shape = ListShape.Cookie9, income = true),
            CategoryEntity(name = "Other income", icon = "savings", color = ListColor.Teal, shape = ListShape.Square, income = true),
        )
        val catIds = cats.map { dao.insertCategory(it) }
        if (!sample) return
        val today = LocalDate.now()
        dao.insertBudget(BudgetEntity(limitPaise = 40_000_00))
        dao.insertBudget(BudgetEntity(categoryId = catIds[0], limitPaise = 8_000_00))
        dao.insertBudget(BudgetEntity(categoryId = catIds[3], limitPaise = 5_000_00))
        dao.insertBudget(BudgetEntity(categoryId = catIds[2], limitPaise = 4_000_00))
        val rnd = Random(11)
        val notes = listOf("Swiggy", "Metro card", "Big Basket", "Coffee", "Uber", "Amazon", "Pharmacy", "Movie", "Zepto", "Lunch")
        repeat(45) {
            val cat = rnd.nextInt(8)
            dao.insertTx(TransactionEntity(
                accountId = accIds[if (rnd.nextInt(4) == 0) 3 else rnd.nextInt(2)], kind = TxKind.Expense,
                amountPaise = (80 + rnd.nextInt(2400)) * 100L, categoryId = catIds[cat],
                date = today.minusDays(rnd.nextInt(40).toLong()), note = notes[rnd.nextInt(notes.size)],
            ))
        }
        dao.insertTx(TransactionEntity(accountId = accIds[0], kind = TxKind.Income, amountPaise = 1_20_000_00, categoryId = catIds[8],
            date = today.withDayOfMonth(1), note = "Salary"))
        dao.insertRecurring(RecurringEntity(name = "Rent", kind = RecurringKind.Bill, amountPaise = 15_000_00, accountId = accIds[0],
            categoryId = catIds[4], monthlyDay = 5, nextDue = today.plusDays(3)))
        dao.insertRecurring(RecurringEntity(name = "Electricity", kind = RecurringKind.Bill, amountPaise = 1_850_00, accountId = accIds[1],
            categoryId = catIds[4], monthlyDay = 12, nextDue = today.plusDays(9)))
        dao.insertRecurring(RecurringEntity(name = "Netflix", kind = RecurringKind.Subscription, amountPaise = 649_00, accountId = accIds[0],
            categoryId = catIds[6], monthlyDay = 18, nextDue = today.plusDays(14)))
        dao.insertRecurring(RecurringEntity(name = "Spotify", kind = RecurringKind.Subscription, amountPaise = 119_00, accountId = accIds[0],
            categoryId = catIds[6], monthlyDay = 22, nextDue = today.plusDays(18)))
    }
}
