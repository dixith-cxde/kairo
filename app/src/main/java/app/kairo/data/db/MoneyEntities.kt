package app.kairo.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
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

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null,  // null = overall monthly budget
    val limitPaise: Long,
)

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
    @Insert suspend fun insertCategory(c: CategoryEntity): Long
    @Insert suspend fun insertTx(t: TransactionEntity): Long
    @Update suspend fun updateTx(t: TransactionEntity)
    @Query("DELETE FROM transactions WHERE id = :id OR transferPeerId = :id") suspend fun deleteTx(id: Long)
    @Insert suspend fun insertBudget(b: BudgetEntity): Long
    @Insert suspend fun insertRecurring(r: RecurringEntity): Long
    @Update suspend fun updateRecurring(r: RecurringEntity)
    @Query("SELECT COUNT(*) FROM accounts") suspend fun accountCount(): Int
}
