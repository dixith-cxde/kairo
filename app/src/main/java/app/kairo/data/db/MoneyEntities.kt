package app.kairo.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

enum class AccountType { Bank, Cash, Card, Wallet }
enum class TxKind { Expense, Income, Transfer }
enum class RecurringKind { Bill, Subscription, Income }

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val color: ListColor,
    val openingPaise: Long = 0,
    val currency: String = "INR",
    val position: Int = 0,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,          // Material Symbols name, mapped in the UI
    val color: ListColor,
    val shape: ListShape,
    val income: Boolean = false,
)

/**
 * Money is integer paise, never floating point (docs/06). Expense and income amounts are positive.
 * A transfer is two rows linked by transferPeerId: the "from" leg is negative, the "to" leg positive.
 */
@Entity(tableName = "transactions", indices = [Index("accountId"), Index("date"), Index("categoryId")])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val kind: TxKind,
    val amountPaise: Long,
    val categoryId: Long? = null,
    val date: LocalDate,
    val note: String = "",
    val tags: String = "",       // space-separated #tags
    val transferPeerId: Long? = null,
    val recurringId: Long? = null,
)

/** A monthly spending limit: overall (both ids null), for one category, or for one account (v5). */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null,
    val limitPaise: Long,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val accountId: Long? = null,
) {
    val overall get() = categoryId == null && accountId == null
}

@Entity(tableName = "recurring")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: RecurringKind,
    val amountPaise: Long,
    val accountId: Long,
    val categoryId: Long? = null,
    val monthlyDay: Int,           // rule: every month on this day (v1 keeps rules simple)
    val nextDue: LocalDate,
    val reminder: Boolean = true,
)

@Dao
interface MoneyDao {
    @Query("SELECT * FROM accounts ORDER BY position, id") fun accounts(): Flow<List<AccountEntity>>
    @Query("SELECT * FROM categories ORDER BY income, id") fun categories(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC") fun transactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM budgets") fun budgets(): Flow<List<BudgetEntity>>
    @Query("SELECT * FROM recurring ORDER BY nextDue") fun recurring(): Flow<List<RecurringEntity>>

    @Insert suspend fun insertAccount(a: AccountEntity): Long
    @Update suspend fun updateAccount(a: AccountEntity)
    @Query("DELETE FROM accounts WHERE id = :id") suspend fun deleteAccountRow(id: Long)
    /** This account's rows plus the other leg of any transfer touching it. */
    @Query("DELETE FROM transactions WHERE accountId = :id OR id IN (SELECT transferPeerId FROM transactions WHERE accountId = :id AND transferPeerId IS NOT NULL)")
    suspend fun deleteAccountTxs(id: Long)
    @Query("DELETE FROM recurring WHERE accountId = :id") suspend fun deleteAccountRecurring(id: Long)
    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :id") suspend fun txCount(id: Long): Int
    @Query("SELECT COALESCE(MAX(position), -1) FROM accounts") suspend fun maxPosition(): Int
    /** All or nothing: the account, its transactions (both legs of its transfers) and its bills. */
    @Transaction suspend fun deleteAccount(id: Long) { deleteAccountTxs(id); deleteAccountRecurring(id); deleteAccountBudgets(id); deleteAccountRow(id) }
    @Insert suspend fun insertCategory(c: CategoryEntity): Long
    @Insert suspend fun insertTx(t: TransactionEntity): Long
    @Update suspend fun updateTx(t: TransactionEntity)
    @Query("DELETE FROM transactions WHERE id = :id OR transferPeerId = :id") suspend fun deleteTx(id: Long)
    @Insert suspend fun insertBudget(b: BudgetEntity): Long
    @Update suspend fun updateBudget(b: BudgetEntity)
    @Query("DELETE FROM budgets WHERE id = :id") suspend fun deleteBudget(id: Long)
    @Query("DELETE FROM budgets WHERE accountId = :id") suspend fun deleteAccountBudgets(id: Long)
    @Insert suspend fun insertRecurring(r: RecurringEntity): Long
    @Update suspend fun updateRecurring(r: RecurringEntity)
    @Query("SELECT COUNT(*) FROM accounts") suspend fun accountCount(): Int
    @Query("SELECT * FROM transactions WHERE id IN (:ids) OR transferPeerId IN (:ids)") suspend fun txWithPeers(ids: List<Long>): List<TransactionEntity>
    @Query("DELETE FROM transactions WHERE id IN (:ids) OR transferPeerId IN (:ids)") suspend fun deleteTxs(ids: List<Long>)
    @Insert suspend fun insertTxs(t: List<TransactionEntity>)
    @Query("UPDATE categories SET shape = CASE shape WHEN 'Arch' THEN 'Hexagon' WHEN 'Sunny' THEN 'Triangle' WHEN 'Cookie4' THEN 'Square' ELSE shape END")
    suspend fun migrateShapes()
}
