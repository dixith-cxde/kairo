package app.kairo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ListEntity::class, TaskEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class KairoDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao
    abstract fun lists(): ListDao

    companion object {
        fun create(context: Context): KairoDatabase =
            Room.databaseBuilder(context, KairoDatabase::class.java, "kairo.db").build()
    }
}
