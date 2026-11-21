package app.kairo.data.model

import java.time.LocalDate
import java.time.LocalTime

/** Each list owns a shape (rule 7). Mapped to MaterialShapes in ui/shapes/ListShapes.kt. */
enum class ListShape { Cookie4, Cookie6, Cookie9, Arch, Clover, Sunny, Pentagon, Gem }

/** List / category colors, harmonized to the seed (docs/03-design-system.md). */
enum class ListColor { Indigo, Coral, Amber, Green, Teal, Pink, Violet }

enum class Priority { None, Low, Medium, High }

data class TaskList(
    val id: Long,
    val name: String,
    val shape: ListShape,
    val color: ListColor,
)

data class Task(
    val id: Long,
    val title: String,
    val listId: Long,
    val due: LocalDate? = null,
    val time: LocalTime? = null,
    val priority: Priority = Priority.None,
    val done: Boolean = false,
    val subtasksDone: Int = 0,
    val subtasksTotal: Int = 0,
    val reminder: Boolean = false,
)
