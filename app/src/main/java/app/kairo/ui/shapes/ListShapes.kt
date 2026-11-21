@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kairo.ui.shapes

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.RoundedPolygon
import app.kairo.data.model.ListShape

/** docs/03-design-system.md "Library shapes in use". Puffy, PuffyDiamond and Heart are banned (rule 6). */
val ListShape.polygon: RoundedPolygon
    get() = when (this) {
        ListShape.Cookie4 -> MaterialShapes.Cookie4Sided
        ListShape.Cookie6 -> MaterialShapes.Cookie6Sided
        ListShape.Cookie9 -> MaterialShapes.Cookie9Sided
        ListShape.Arch -> MaterialShapes.Arch
        ListShape.Clover -> MaterialShapes.Clover4Leaf
        ListShape.Sunny -> MaterialShapes.Sunny
        ListShape.Pentagon -> MaterialShapes.Pentagon
        ListShape.Gem -> MaterialShapes.Gem
    }
