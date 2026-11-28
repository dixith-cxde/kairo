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
        app.kairo.habits.HabitEntity::class, app.kairo.habits.HabitCheckEntity::class,
    ],
    version = 10,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // v2 adds the Money tables
        AutoMigration(from = 2, to = 3), // v3 adds tasks.archived
        AutoMigration(from = 3, to = 4), // v4 adds focus_sessions
        AutoMigration(from = 4, to = 5), // v5 adds budgets.accountId (per-account budgets)
        AutoMigration(from = 5, to = 6), // v6 makes tasks.listId optional
        AutoMigration(from = 6, to = 7), // v7 adds tasks.repeat
        AutoMigration(from = 7, to = 8), // v8 adds habits + habit_checks
        AutoMigration(from = 8, to = 9), // v9 (Kairo v2) adds habit goals, schedules, pause, icon, shape and day values
        AutoMigration(from = 9, to = 10), // v10 adds transactions.time and transactions.description
    ],
)
@TypeConverters(Converters::class)
abstract class KairoDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao
    abstract fun lists(): ListDao
    abstract fun money(): MoneyDao
    abstract fun focus(): app.kairo.focus.FocusDao
    abstract fun habits(): app.kairo.habits.HabitDao

    companion object {
        fun create(context: Context): KairoDatabase =
            Room.databaseBuilder(context, KairoDatabase::class.java, "kairo.db").build()
    }
}
