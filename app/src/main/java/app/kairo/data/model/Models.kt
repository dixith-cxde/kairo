package app.kairo.data.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Each list owns a shape (rule 7). Mapped in ui/shapes/ListShapes.kt.
 * Owner, 2026-11-21: hexagon, rounded triangle, rounded square and friends; no arch (semi-circle)
 * or sunny. Arch/Sunny/Cookie4 stay only so old rows still load; they are migrated at startup.
 */
enum class ListShape {
    Hexagon, Triangle, Square, Pentagon, Gem, Cookie6, Cookie9, Clover,
    @Deprecated("migrated to Hexagon") Arch,
    @Deprecated("migrated to Triangle") Sunny,
    @Deprecated("migrated to Square") Cookie4;

    companion object {
        /** Shapes offered when creating a list. */
        val choices = listOf(Hexagon, Triangle, Square, Pentagon, Gem, Cookie6, Cookie9, Clover)
    }
}

/** List / category colors, harmonized to the seed (docs/03-design-system.md). */
enum class ListColor { Indigo, Coral, Amber, Green, Teal, Pink, Violet }

enum class Priority { None, Low, Medium, High }

data class TaskList(
    val id: Long,
    val name: String,
    val shape: ListShape,
    val color: ListColor,
    val position: Int = 0,
) {
    companion object {
        /** Stand-in shown for tasks without a list (id 0 never matches a real row). */
        val None = TaskList(0, "Inbox", ListShape.Square, ListColor.Indigo)
    }
}

data class Task(
    val id: Long,
    val title: String,
    val listId: Long?,
    val due: LocalDate? = null,
    val time: LocalTime? = null,
    val priority: Priority = Priority.None,
    val done: Boolean = false,
    val subtasksDone: Int = 0,
    val subtasksTotal: Int = 0,
    val reminder: Boolean = true, // on by default (owner, 2026-11-21)
    val notes: String = "",
    val completedAt: Long? = null, // epoch millis, for stats
    val archived: Boolean = false,
    val repeat: Repeat? = null,
)
