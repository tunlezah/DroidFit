package com.visceralfit.core.designsystem.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Type scale.
 *
 * The app deliberately ships no bundled font files: the system font on the Edge 60
 * is already well-hinted, and a bundled variable font would add ~400 KB to an APK
 * whose whole appeal is being small and offline. See ADR-0011.
 */
internal fun buildVisceralFitTypography(): Typography {
    val default = Typography()
    return default.copy(
        displayLarge = default.displayLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = default.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = default.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = default.labelLarge.copy(fontWeight = FontWeight.Medium),
    )
}

/**
 * The countdown numerals.
 *
 * Tabular figures matter here: with proportional digits the timer visibly jitters
 * as it counts down through 1s, which is distracting when it is the only thing on
 * screen. `FontFamily.Monospace` is the reliable way to get fixed advance widths
 * without bundling a font.
 */
object TimerTypography {
    /** Full-screen countdown, one-handed portrait use. */
    val Countdown: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 96.sp,
        lineHeight = 100.sp,
        textAlign = TextAlign.Center,
    )

    /**
     * Machine mode: read at arm's length from an elliptical or spin bike, where the
     * phone sits in a cradle 60-80 cm away rather than in the hand.
     */
    val CountdownLarge: TextStyle = Countdown.copy(fontSize = 148.sp, lineHeight = 152.sp)

    val SegmentLabel: TextStyle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    )
}

/** Corner radii. Generous, because every interactive target is large by design. */
internal val VisceralFitShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
