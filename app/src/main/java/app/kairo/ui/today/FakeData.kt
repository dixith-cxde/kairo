package app.kairo.ui.today

import app.kairo.data.model.ListColor
import app.kairo.data.model.ListShape
import app.kairo.data.model.Priority
import app.kairo.data.model.Task
import app.kairo.data.model.TaskList
import java.time.LocalDate
import java.time.LocalTime

/** Same lists and tasks as the canvas Today board. Replaced by Room in step 4. */
object FakeData {
    val lists = listOf(
        TaskList(1, "Work", ListShape.Cookie9, ListColor.Indigo),
        TaskList(2, "Home", ListShape.Hexagon, ListColor.Amber),
        TaskList(3, "Groceries", ListShape.Clover, ListColor.Green),
        TaskList(4, "Health", ListShape.Triangle, ListColor.Teal),
        TaskList(5, "Family", ListShape.Pentagon, ListColor.Pink),
        TaskList(6, "Personal", ListShape.Gem, ListColor.Coral),
        TaskList(7, "Reading", ListShape.Cookie6, ListColor.Violet),
    )

    private val today = LocalDate.now()

    val tasks = listOf(
        Task(1, "Renew passport photos", 6, due = today.minusDays(1), priority = Priority.High),
        Task(2, "Standup notes for design review", 1, today, LocalTime.of(9, 30), Priority.Medium),
        Task(3, "Pay electricity bill", 2, today, LocalTime.of(12, 0), Priority.High, reminder = true),
        Task(4, "Buy groceries", 3, today, subtasksDone = 2, subtasksTotal = 5),
        Task(5, "30-min run", 4, today, LocalTime.of(18, 0), Priority.Low),
        Task(6, "Call Mom", 5, today, LocalTime.of(19, 30), reminder = true),
        Task(7, "Read 20 pages", 7, today, LocalTime.of(21, 0), Priority.Low),
        Task(8, "Reply to landlord", 2, today, done = true),
        Task(9, "Book dentist", 6, today, done = true),
        Task(10, "Water plants", 2, today, done = true),
    )
}
