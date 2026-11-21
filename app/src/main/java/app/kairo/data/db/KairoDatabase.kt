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
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // v2 adds the Money tables
        AutoMigration(from = 2, to = 3), // v3 adds tasks.archived
    ],
)
@TypeConverters(Converters::class)
abstract class KairoDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao
    abstract fun lists(): ListDao
    abstract fun money(): MoneyDao

    companion object {
        fun create(context: Context): KairoDatabase =
            Room.databaseBuilder(context, KairoDatabase::class.java, "kairo.db").build()
    }
}
