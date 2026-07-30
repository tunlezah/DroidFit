package com.visceralfit.core.designsystem.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview

/**
 * Exercise artwork, drawn in code.
 *
 * WHY NO IMAGE ASSETS (ADR-0003): licensed exercise photography or stock vector
 * figures would either cost money or impose attribution the app cannot honour
 * offline, and an APK carrying 120 raster demos would be tens of megabytes. Drawing
 * figures on a Canvas keeps the APK at a few hundred kilobytes, scales perfectly on
 * the Edge 60's 444 ppi panel, and re-tints automatically in dark and AMOLED modes.
 *
 * HOW TO ADD ONE (the building agent will add roughly 60 of these in phase 07):
 *  1. Add a branch to [ExerciseIllustration] keyed on the exercise's
 *     `illustrationId`.
 *  2. Draw inside a 100x100 logical box using the [Figure] helpers, which normalise
 *     coordinates so a drawing is resolution-independent.
 *  3. Supply a `contentDescription` that describes the *position*, not the artwork
 *     — a screen-reader user needs "lying on back, knees bent, arms extended
 *     upward", not "line drawing of a person".
 *  4. Add a @Preview so the drawing is reviewable without running the app.
 *
 * Unknown ids fall back to [PlaceholderFigure] rather than crashing: a missing
 * drawing must never block a workout.
 */
@Composable
fun ExerciseIllustration(
    illustrationId: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val stroke = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            when (illustrationId) {
                "pilates_dead_bug" -> drawDeadBug(stroke, accent)
                "spin_seated" -> drawSeatedCycling(stroke, accent)
                else -> drawPlaceholderFigure(stroke)
            }
        }
    }
}

/** Coordinate helpers: every drawing works in a 0..100 space and is scaled to fit. */
private object Figure {
    const val CANVAS = 100f

    fun DrawScope.p(x: Float, y: Float): Offset =
        Offset(x / CANVAS * size.width, y / CANVAS * size.height)

    fun DrawScope.unit(value: Float): Float = value / CANVAS * size.minDimension
}

private fun DrawScope.figureStroke(colour: Color, width: Float = 3.2f) = Stroke(
    width = with(Figure) { unit(width) },
    cap = StrokeCap.Round,
)
    .let { it to colour }

/** Supine, hips and knees at 90 degrees, opposite arm and leg extending. */
private fun DrawScope.drawDeadBug(stroke: Color, accent: Color) = with(Figure) {
    val (s, _) = figureStroke(stroke)
    // Mat.
    drawLine(stroke.copy(alpha = 0.35f), p(8f, 78f), p(92f, 78f), strokeWidth = unit(2f), cap = StrokeCap.Round)
    // Torso along the mat.
    drawLine(stroke, p(30f, 72f), p(62f, 72f), strokeWidth = s.width, cap = StrokeCap.Round)
    // Head.
    drawCircle(stroke, radius = unit(6f), center = p(24f, 72f))
    // Near leg: hip and knee stacked at 90 degrees.
    drawLine(stroke, p(62f, 72f), p(62f, 46f), strokeWidth = s.width, cap = StrokeCap.Round)
    drawLine(stroke, p(62f, 46f), p(80f, 46f), strokeWidth = s.width, cap = StrokeCap.Round)
    // Far leg extending away — the moving limb, drawn in the accent colour.
    drawLine(accent, p(62f, 72f), p(88f, 66f), strokeWidth = s.width, cap = StrokeCap.Round)
    // Near arm reaching to the ceiling.
    drawLine(stroke, p(34f, 72f), p(34f, 48f), strokeWidth = s.width, cap = StrokeCap.Round)
    // Far arm extending overhead — also moving.
    drawLine(accent, p(34f, 72f), p(14f, 58f), strokeWidth = s.width, cap = StrokeCap.Round)
}

/** Seated on an indoor cycle, torso tall, hands light on the bars. */
private fun DrawScope.drawSeatedCycling(stroke: Color, accent: Color) = with(Figure) {
    val (s, _) = figureStroke(stroke)
    // Flywheel and frame.
    drawCircle(stroke.copy(alpha = 0.45f), radius = unit(14f), center = p(30f, 74f), style = Stroke(unit(2.4f)))
    drawLine(stroke.copy(alpha = 0.6f), p(30f, 74f), p(66f, 74f), strokeWidth = unit(2.4f), cap = StrokeCap.Round)
    drawLine(stroke.copy(alpha = 0.6f), p(66f, 74f), p(66f, 52f), strokeWidth = unit(2.4f), cap = StrokeCap.Round)
    drawLine(stroke.copy(alpha = 0.6f), p(40f, 74f), p(40f, 50f), strokeWidth = unit(2.4f), cap = StrokeCap.Round)
    // Saddle and bars.
    drawLine(stroke.copy(alpha = 0.6f), p(58f, 50f), p(72f, 50f), strokeWidth = unit(2.4f), cap = StrokeCap.Round)
    drawLine(stroke.copy(alpha = 0.6f), p(34f, 46f), p(46f, 46f), strokeWidth = unit(2.4f), cap = StrokeCap.Round)
    // Rider: hips on the saddle, tall spine, slight forward lean.
    drawLine(accent, p(64f, 48f), p(52f, 24f), strokeWidth = s.width, cap = StrokeCap.Round)
    drawCircle(accent, radius = unit(6f), center = p(50f, 18f))
    // Arm to the bars.
    drawLine(accent, p(55f, 30f), p(41f, 44f), strokeWidth = s.width, cap = StrokeCap.Round)
    // Driving leg: hip to knee to pedal.
    drawLine(accent, p(64f, 48f), p(48f, 62f), strokeWidth = s.width, cap = StrokeCap.Round)
    drawLine(accent, p(48f, 62f), p(38f, 70f), strokeWidth = s.width, cap = StrokeCap.Round)
}

/**
 * Neutral standing figure. Shown when an exercise's drawing has not been authored
 * yet. It is deliberately plain so it reads as "no diagram" rather than as a wrong
 * diagram the user might try to copy.
 */
private fun DrawScope.drawPlaceholderFigure(stroke: Color) = with(Figure) {
    val muted = stroke.copy(alpha = 0.45f)
    val width = unit(3.2f)
    drawCircle(muted, radius = unit(8f), center = p(50f, 20f))
    drawLine(muted, p(50f, 28f), p(50f, 58f), strokeWidth = width, cap = StrokeCap.Round)
    drawLine(muted, p(50f, 36f), p(34f, 48f), strokeWidth = width, cap = StrokeCap.Round)
    drawLine(muted, p(50f, 36f), p(66f, 48f), strokeWidth = width, cap = StrokeCap.Round)
    drawLine(muted, p(50f, 58f), p(38f, 82f), strokeWidth = width, cap = StrokeCap.Round)
    drawLine(muted, p(50f, 58f), p(62f, 82f), strokeWidth = width, cap = StrokeCap.Round)
    drawRect(
        color = muted.copy(alpha = 0.2f),
        topLeft = p(10f, 8f),
        size = Size(unit(80f), unit(84f)),
        style = Stroke(unit(1.2f)),
    )
}

@Preview(name = "Dead bug", showBackground = true)
@Composable
private fun DeadBugPreview() {
    ExerciseIllustration(
        illustrationId = "pilates_dead_bug",
        contentDescription = "Lying on the back, knees bent above the hips, opposite arm and leg extending away.",
    )
}

@Preview(name = "Seated cycling", showBackground = true)
@Composable
private fun SeatedCyclingPreview() {
    ExerciseIllustration(
        illustrationId = "spin_seated",
        contentDescription = "Seated on an indoor cycle, spine tall, hands resting on the bars.",
    )
}

@Preview(name = "Placeholder", showBackground = true)
@Composable
private fun PlaceholderPreview() {
    ExerciseIllustration(
        illustrationId = "not_yet_drawn",
        contentDescription = "No diagram available for this exercise yet.",
    )
}
