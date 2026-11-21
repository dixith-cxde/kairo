@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.shapes

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import app.kairo.data.model.ListShape

/** docs/03-design-system.md "Library shapes in use". Puffy, PuffyDiamond and Heart are banned (rule 6). */
@Suppress("DEPRECATION")
val ListShape.polygon: RoundedPolygon
    get() = when (this) {
        ListShape.Hexagon, ListShape.Arch -> Hexagon
        ListShape.Triangle, ListShape.Sunny -> MaterialShapes.Triangle
        ListShape.Square, ListShape.Cookie4 -> MaterialShapes.Square
        ListShape.Pentagon -> MaterialShapes.Pentagon
        ListShape.Gem -> MaterialShapes.Gem
        ListShape.Cookie6 -> MaterialShapes.Cookie6Sided
        ListShape.Cookie9 -> MaterialShapes.Cookie9Sided
        ListShape.Clover -> MaterialShapes.Clover4Leaf
    }

/** The library has no hexagon, so build one the same way MaterialShapes does: rounded, unit-sized. */
private val Hexagon: RoundedPolygon = RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.2f)).normalized()
