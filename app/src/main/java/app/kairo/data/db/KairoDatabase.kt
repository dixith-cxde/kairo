package app.kairo.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ListEntity::class, TaskEntity::class,
        AccountEntity::class, CategoryEntity::class, TransactionEntity::class, BudgetEntity::class, RecurringEntity::class,
        app.kairo.focus.FocusSessionEntity::class,
    ],
    version = 7,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // v2 adds the Money tables
        AutoMigration(from = 2, to = 3), // v3 adds tasks.archived
        AutoMigration(from = 3, to = 4), // v4 adds focus_sessions
        AutoMigration(from = 4, to = 5), // v5 adds budgets.accountId (per-account budgets)
        AutoMigration(from = 5, to = 6), // v6 makes tasks.listId optional
        AutoMigration(from = 6, to = 7), // v7 adds tasks.repeat
    ],
)
@TypeConverters(Converters::class)
abstract class KairoDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao
    abstract fun lists(): ListDao
    abstract fun money(): MoneyDao
    abstract fun focus(): app.kairo.focus.FocusDao

    companion object {
        fun create(context: Context): KairoDatabase =
            Room.databaseBuilder(context, KairoDatabase::class.java, "kairo.db").build()
    }
}
