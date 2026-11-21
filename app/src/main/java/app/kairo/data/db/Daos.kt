package app.kairo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY due IS NULL, due, time IS NULL, time, createdAt")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observe(id: Long): Flow<TaskEntity?>

    @Insert suspend fun insert(task: TaskEntity): Long
    @Insert suspend fun insertAll(tasks: List<TaskEntity>)
    @Update suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET done = :done, completedAt = :at WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, at: Long?)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM tasks WHERE id IN (:ids)") suspend fun get(ids: List<Long>): List<TaskEntity>
    @Query("DELETE FROM tasks WHERE id IN (:ids)") suspend fun deleteAll(ids: List<Long>)
    @Query("UPDATE tasks SET archived = :archived WHERE id IN (:ids)") suspend fun setArchived(ids: List<Long>, archived: Boolean)
    @Query("UPDATE tasks SET listId = :listId WHERE id IN (:ids)") suspend fun move(ids: List<Long>, listId: Long?)
    @Query("UPDATE tasks SET due = :due, time = :time, reminder = :reminder WHERE id IN (:ids)")
    suspend fun reschedule(ids: List<Long>, due: java.time.LocalDate?, time: java.time.LocalTime?, reminder: Boolean)
    @Query("UPDATE tasks SET done = :done, completedAt = :at WHERE id IN (:ids)") suspend fun setDoneAll(ids: List<Long>, done: Boolean, at: Long?)
}

@Dao
interface ListDao {
    @Query("SELECT * FROM lists ORDER BY position, id")
    fun observeAll(): Flow<List<ListEntity>>

    @Insert suspend fun insert(list: ListEntity): Long
    @Insert suspend fun insertAll(lists: List<ListEntity>)
    @Update suspend fun update(list: ListEntity)
    @Update suspend fun updateAll(lists: List<ListEntity>)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun delete(id: Long)

    /** Owner retired these shapes (2026-11-21); move old rows to their replacements. */
    @Query("UPDATE lists SET shape = CASE shape WHEN 'Arch' THEN 'Hexagon' WHEN 'Sunny' THEN 'Triangle' WHEN 'Cookie4' THEN 'Square' ELSE shape END")
    suspend fun migrateShapes()

    /** Removes a list by name if no task uses it (cleanup of the old default lists). */
    @Query("DELETE FROM lists WHERE name = :name AND NOT EXISTS (SELECT 1 FROM tasks WHERE tasks.listId = lists.id)")
    suspend fun deleteIfEmpty(name: String)

    @Query("SELECT COUNT(*) FROM lists")
    suspend fun count(): Int
}
