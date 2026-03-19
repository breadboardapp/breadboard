package moe.apex.rule34.util

import android.graphics.Matrix
import android.graphics.Path
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.core.graphics.scaleMatrix
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import androidx.graphics.shapes.transformed


// Most of the values for the shapes in this file come from MaterialShapes

data class MorphableRoundedPolygon(
    val shape: RoundedPolygon,
    val scale: Float  = 1f
)


private fun RoundedPolygon.getBounds(): Rect {
    val b = calculateBounds()
    return Rect(b[0], b[1], b[2], b[3])
}


private fun CacheDrawScope.transformShape(morphableRoundedPolygon: MorphableRoundedPolygon): RoundedPolygon {
    val matrix = scaleMatrix(
        sx = size.minDimension * morphableRoundedPolygon.scale,
        sy = size.minDimension * morphableRoundedPolygon.scale
    )
    val scaledShape = morphableRoundedPolygon.shape.transformed(matrix)

    val bounds = scaledShape.getBounds()
    val offsetX = (size.width - bounds.width) / 2f - bounds.left
    val offsetY = (size.height - bounds.height) / 2f - bounds.top

    return scaledShape.transformed(
        Matrix().apply {
            setTranslate(offsetX, offsetY)
        }
    )
}


/** Modifier for morphing the background of an item between two shapes.
 *
 * @param start The starting shape of the morphing animation.
 * @param end The ending shape of the morphing animation.
 * @param progress The progress of the morphing animation, between 0 and 1.
 * @param color The colour to draw the morphing shape with.
 */
fun Modifier.morphingBackground(
    start: MorphableRoundedPolygon,
    end: MorphableRoundedPolygon,
    progress: Float,
    color: Color
): Modifier = drawWithCache {
    val startTransformed = transformShape(start)
    val endTransformed = transformShape(end)

    val morph = Morph(startTransformed, endTransformed)
    val path = Path()
    val composePath = path.asComposePath()

    onDrawBehind {
        morph.toPath(progress, path)
        drawPath(path = composePath, color = color)
    }
}


