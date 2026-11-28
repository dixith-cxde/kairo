@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.habits

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.graphics.shapes.RoundedPolygon
import app.kairo.R

/**
 * What a habit looks like (canvas Habits block): one of ten official MaterialShapes (no puffy,
 * clover, cookie, triangle or burst for habits), a Material Symbols icon, and its list colour.
 */
object HabitLook {
    val shapes = listOf("Circle", "Square", "ClamShell", "Pentagon", "Gem", "Diamond", "Slanted", "Arch", "Sunny", "Pill")

    fun polygon(name: String): RoundedPolygon = when (name) {
        "Square" -> MaterialShapes.Square
        "ClamShell" -> MaterialShapes.ClamShell
        "Pentagon" -> MaterialShapes.Pentagon
        "Gem" -> MaterialShapes.Gem
        "Diamond" -> MaterialShapes.Diamond
        "Slanted" -> MaterialShapes.Slanted
        "Arch" -> MaterialShapes.Arch
        "Sunny" -> MaterialShapes.Sunny
        "Pill" -> MaterialShapes.Pill
        else -> MaterialShapes.Circle
    }

    /** Icon picker order and labels, from the canvas (hsIcons). */
    val icons = listOf(
        "star" to "Star", "water_drop" to "Water", "self_improvement" to "Meditate", "fitness_center" to "Gym",
        "directions_run" to "Run", "directions_walk" to "Walk", "menu_book" to "Read", "edit_note" to "Journal",
        "bedtime" to "Sleep", "restaurant" to "Eat well", "no_food" to "No junk food", "local_cafe" to "Coffee",
        "smoke_free" to "No smoking", "medication" to "Medicine", "translate" to "Language", "music_note" to "Music",
        "brush" to "Art", "code" to "Code", "savings" to "Save money", "cleaning_services" to "Tidy up",
        "potted_plant" to "Plants", "pets" to "Pet", "phonelink_off" to "Less screen time", "call" to "Call someone",
    )

    private val outline = mapOf(
        "star" to R.drawable.hi_star, "water_drop" to R.drawable.hi_water_drop, "self_improvement" to R.drawable.hi_self_improvement,
        "fitness_center" to R.drawable.hi_fitness_center, "directions_run" to R.drawable.hi_directions_run,
        "directions_walk" to R.drawable.hi_directions_walk, "menu_book" to R.drawable.hi_menu_book, "edit_note" to R.drawable.hi_edit_note,
        "bedtime" to R.drawable.hi_bedtime, "restaurant" to R.drawable.hi_restaurant, "no_food" to R.drawable.hi_no_food,
        "local_cafe" to R.drawable.hi_local_cafe, "smoke_free" to R.drawable.hi_smoke_free, "medication" to R.drawable.hi_medication,
        "translate" to R.drawable.hi_translate, "music_note" to R.drawable.hi_music_note, "brush" to R.drawable.hi_brush,
        "code" to R.drawable.hi_code, "savings" to R.drawable.hi_savings, "cleaning_services" to R.drawable.hi_cleaning_services,
        "potted_plant" to R.drawable.hi_potted_plant, "pets" to R.drawable.hi_pets, "phonelink_off" to R.drawable.hi_phonelink_off,
        "call" to R.drawable.hi_call,
    )
    private val filled = mapOf(
        "star" to R.drawable.hi_star_filled, "water_drop" to R.drawable.hi_water_drop_filled, "self_improvement" to R.drawable.hi_self_improvement_filled,
        "fitness_center" to R.drawable.hi_fitness_center_filled, "directions_run" to R.drawable.hi_directions_run_filled,
        "directions_walk" to R.drawable.hi_directions_walk_filled, "menu_book" to R.drawable.hi_menu_book_filled, "edit_note" to R.drawable.hi_edit_note_filled,
        "bedtime" to R.drawable.hi_bedtime_filled, "restaurant" to R.drawable.hi_restaurant_filled, "no_food" to R.drawable.hi_no_food_filled,
        "local_cafe" to R.drawable.hi_local_cafe_filled, "smoke_free" to R.drawable.hi_smoke_free_filled, "medication" to R.drawable.hi_medication_filled,
        "translate" to R.drawable.hi_translate_filled, "music_note" to R.drawable.hi_music_note_filled, "brush" to R.drawable.hi_brush_filled,
        "code" to R.drawable.hi_code_filled, "savings" to R.drawable.hi_savings_filled, "cleaning_services" to R.drawable.hi_cleaning_services_filled,
        "potted_plant" to R.drawable.hi_potted_plant_filled, "pets" to R.drawable.hi_pets_filled, "phonelink_off" to R.drawable.hi_phonelink_off_filled,
        "call" to R.drawable.hi_call_filled,
    )

    fun icon(name: String, fill: Boolean): Int? = (if (fill) filled else outline)[name]

    /** Text/icon colour on a filled habit shape (canvas --hon). */
    @Composable
    fun onColor(dark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f): Color =
        if (dark) Color(0xFF1B1B1B) else Color.White
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
