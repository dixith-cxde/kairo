package app.kairo.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import app.kairo.data.model.Priority
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "lists")
data class ListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val shape: ListShape,
    val color: ListColor,
    val position: Int = 0,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(ListEntity::class, ["id"], ["listId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("listId"), Index("due")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val listId: Long? = null, // v6: optional, no list = Inbox (no default lists, owner 2026-11-21)
    val notes: String = "",
    val due: LocalDate? = null,
    val time: LocalTime? = null,
    val priority: Priority = Priority.None,
    val done: Boolean = false,
    val completedAt: Long? = null, // epoch millis
    val subtasksDone: Int = 0,
    val subtasksTotal: Int = 0,
    val reminder: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false, // v3
    @ColumnInfo(defaultValue = "") val repeat: String = "", // v7: Repeat.encode(), "" = doesn't repeat
    val deletedAt: Long? = null, // v12: in Trash since (epoch millis); purged after 30 days
)
