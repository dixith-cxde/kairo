package app.kairo.habits

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/*
 * Habits in the database. Room v8 added the two tables; v9 (Kairo v2) adds goals, schedules,
 * pause, icon, a habit shape and per-day values. Every new column has a default, so existing
 * habits keep working: an old check-in row reads as "done".
 */

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val shape: ListShape,                 // 1.3.1 shape; v2 uses [hshape]
    val color: ListColor,
    val reminderTime: LocalTime? = null,
    val createdAt: LocalDate = LocalDate.now(),
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
    val position: Int = 0,
    /** Material Symbols name (canvas icon picker); "" = show the first letter. */
    @ColumnInfo(defaultValue = "''") val icon: String = "",
    /** MaterialShapes name (Circle, Square, ClamShell, …); "" = derived from the 1.3.1 [shape]. */
    @ColumnInfo(defaultValue = "''") val hshape: String = "",
    /** "yes", "count" or "time" (minutes). */
    @ColumnInfo(defaultValue = "'yes'") val goal: String = "yes",
    @ColumnInfo(defaultValue = "1") val target: Double = 1.0,
    @ColumnInfo(defaultValue = "''") val unit: String = "",
    /** How much one tap or right swipe adds to a count habit. */
    @ColumnInfo(defaultValue = "1") val step: Double = 1.0,
    /** Weekdays it's due, Monday = 0, e.g. "0,2,4"; null = every day. */
    val days: String? = null,
    /** "3 times a week": due any day until the week's goal is met. */
    val perWeek: Int? = null,
    /** Paused days are like rest days: nothing due, the streak is kept. pauseTo null = until resumed. */
    val pauseFrom: LocalDate? = null,
    val pauseTo: LocalDate? = null,
    @ColumnInfo(defaultValue = "''") val notes: String = "",
)

/**
 * One row per logged day. state 1 = logged (done, or a partial count/time in [value]);
 * state 0 = "not done" (decided, like a dropped task). No row = pending.
 * 1.3.1 rows get state 1 and value 0, which means done.
 */
@Entity(
    tableName = "habit_checks",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [ForeignKey(HabitEntity::class, ["id"], ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId")],
)
data class HabitCheckEntity(
    val habitId: Long,
    val date: LocalDate,
    @ColumnInfo(defaultValue = "1") val state: Int = 1,
    @ColumnInfo(defaultValue = "0") val value: Double = 0.0,
)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY position, id") fun habits(): Flow<List<HabitEntity>>
    @Query("SELECT * FROM habit_checks") fun checks(): Flow<List<HabitCheckEntity>>
    @Query("SELECT * FROM habits WHERE id = :id") suspend fun get(id: Long): HabitEntity?
    @Insert suspend fun insert(h: HabitEntity): Long
    @Update suspend fun update(h: HabitEntity)
    @Update suspend fun updateAll(hs: List<HabitEntity>)
    @Query("DELETE FROM habits WHERE id = :id") suspend fun delete(id: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(c: HabitCheckEntity)
    @Query("DELETE FROM habit_checks WHERE habitId = :id AND date = :date") suspend fun clear(id: Long, date: LocalDate)
    @Query("SELECT * FROM habit_checks WHERE habitId = :id AND date = :date") suspend fun check(id: Long, date: LocalDate): HabitCheckEntity?
}
