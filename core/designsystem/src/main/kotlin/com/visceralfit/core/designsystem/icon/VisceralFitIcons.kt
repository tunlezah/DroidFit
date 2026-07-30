package com.visceralfit.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The app's complete icon set: four navigation icons, declared here.
 *
 * WHY NO ICON LIBRARY (measured, not preference):
 *  - `material-icons-extended` was tried and reverted. It pushed the debug APK from a
 *    few megabytes to **66 MB**, because it dexes several thousand `ImageVector`
 *    declarations and a debug build does not shrink them away. The app uses four icons.
 *  - `material-icons-core` would cover two of the four, but it is a separate artefact
 *    that Material 3 does not bring in, and it is on the deprecated path in current
 *    Compose. Taking a dependency for two glyphs is not worth the version constraint.
 *
 * Consistent with ADR-0003 (artwork is drawn in code, not imported).
 *
 * TO ADD AN ICON: declare it here on the standard 24×24 Material viewport, using
 * `SolidColor(Color.Black)` for fills and strokes — `Icon()` tints by
 * `LocalContentColor`, so the declared colour is a placeholder and never appears.
 * A regression test on APK size is part of phase 12; do not reintroduce an icon library
 * to save authoring one glyph.
 */
object VisceralFitIcons {

    /** Training. A dumbbell: two end plates joined by a bar. */
    val Dumbbell: ImageVector by lazy {
        icon("Dumbbell") {
            // Outer plates.
            filledRect(1.5f, 9.5f, 3.5f, 14.5f)
            filledRect(20.5f, 9.5f, 22.5f, 14.5f)
            // Inner plates.
            filledRect(4.5f, 7f, 7.5f, 17f)
            filledRect(16.5f, 7f, 19.5f, 17f)
            // Bar.
            filledRect(7.5f, 10.75f, 16.5f, 13.25f)
        }
    }

    /** History. A calendar: a page with a bound header and date marks. */
    val Calendar: ImageVector by lazy {
        icon("Calendar") {
            // Page outline.
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3.5f, 5.5f)
                lineTo(20.5f, 5.5f)
                lineTo(20.5f, 20.5f)
                lineTo(3.5f, 20.5f)
                close()
            }
            // Header band.
            filledRect(3.5f, 5.5f, 20.5f, 9f)
            // Binding rings.
            filledRect(7f, 2.5f, 8.6f, 6f)
            filledRect(15.4f, 2.5f, 17f, 6f)
            // Date marks.
            filledRect(6.5f, 11.5f, 8.5f, 13.5f)
            filledRect(11f, 11.5f, 13f, 13.5f)
            filledRect(15.5f, 11.5f, 17.5f, 13.5f)
            filledRect(6.5f, 15.5f, 8.5f, 17.5f)
            filledRect(11f, 15.5f, 13f, 17.5f)
        }
    }

    /** Progress. A rising polyline with an arrowhead. */
    val TrendingUp: ImageVector by lazy {
        icon("TrendingUp") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 17f)
                lineTo(9f, 11f)
                lineTo(13f, 15f)
                lineTo(20f, 8f)
            }
            // Arrowhead.
            path(fill = SolidColor(Color.Black)) {
                moveTo(14.5f, 7f)
                lineTo(21f, 7f)
                lineTo(21f, 13.5f)
                close()
            }
        }
    }

    /**
     * Settings. Three sliders rather than a gear: a gear's teeth need a dozen path
     * segments to read correctly at 24 dp, and sliders are at least as recognisable
     * for a preferences screen.
     */
    val Settings: ImageVector by lazy {
        icon("Settings") {
            val tracks = listOf(6.5f, 12f, 17.5f)
            val knobCentres = listOf(15.5f, 9f, 13.5f)
            tracks.forEachIndexed { index, y ->
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                ) {
                    moveTo(3.5f, y)
                    lineTo(20.5f, y)
                }
                // Knob.
                filledRect(knobCentres[index] - 1.6f, y - 2.4f, knobCentres[index] + 1.6f, y + 2.4f)
            }
        }
    }
}

/** Axis-aligned filled rectangle, in viewport units. */
private fun ImageVector.Builder.filledRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
): ImageVector.Builder = path(fill = SolidColor(Color.Black)) {
    moveTo(left, top)
    lineTo(right, top)
    lineTo(right, bottom)
    lineTo(left, bottom)
    close()
}

private fun icon(
    name: String,
    block: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = ICON_SIZE_DP.dp,
    defaultHeight = ICON_SIZE_DP.dp,
    viewportWidth = VIEWPORT,
    viewportHeight = VIEWPORT,
).apply(block).build()

private const val ICON_SIZE_DP = 24f
private const val VIEWPORT = 24f
