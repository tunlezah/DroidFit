package com.visceralfit.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette tokens. Derived in /framework/04_ux_research_and_design_system.md; every
 * pair below was checked for a >= 4.5:1 contrast ratio against its surface, which is
 * the WCAG AA floor the PRD requires.
 *
 * Do not add a raw Color anywhere else in the codebase. Screens read colours from
 * `MaterialTheme.colorScheme` or from [ZoneColours].
 */
internal object Palette {
    // Primary: deep teal. Reads as clinical rather than "fat-burning red", which
    // matters for an app that must not over-claim.
    val Teal10 = Color(0xFF00201C)
    val Teal20 = Color(0xFF003733)
    val Teal30 = Color(0xFF00504A)
    val Teal40 = Color(0xFF006A62)
    val Teal80 = Color(0xFF4FDBCB)
    val Teal90 = Color(0xFF6FF7E7)

    // Secondary: slate, for chrome that must not compete with the timer.
    val Slate20 = Color(0xFF1E2A2B)
    val Slate30 = Color(0xFF334142)
    val Slate40 = Color(0xFF4A5859)
    val Slate80 = Color(0xFFB1CBCC)
    val Slate90 = Color(0xFFCCE8E9)

    // Tertiary: warm amber, reserved for streaks and achievements.
    val Amber40 = Color(0xFF7A5900)
    val Amber80 = Color(0xFFF3BF48)
    val Amber90 = Color(0xFFFFDF9E)

    val Red40 = Color(0xFFBA1A1A)
    val Red80 = Color(0xFFFFB4AB)
    val Red90 = Color(0xFFFFDAD6)

    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)

    val NeutralLightSurface = Color(0xFFFAFDFB)
    val NeutralLightOnSurface = Color(0xFF191C1C)
    val NeutralDarkSurface = Color(0xFF0E1514)
    val NeutralDarkOnSurface = Color(0xFFDDE4E3)
}

/**
 * Intensity-zone colours, used by the workout player ring and the history charts.
 *
 * These are semantic, not decorative: the same zone must be the same colour in
 * every surface, and colour is never the only signal — each zone also carries a
 * label and a distinct ring thickness, because roughly 1 in 12 men has a colour
 * vision deficiency (see /framework/12_accessibility_spec.md §Colour).
 */
data class ZoneColours(
    val recovery: Color,
    val zone2: Color,
    val threshold: Color,
    val vigorous: Color,
    val rest: Color,
) {
    companion object {
        val Light = ZoneColours(
            recovery = Color(0xFF6E8B8C),
            zone2 = Color(0xFF00786D),
            threshold = Color(0xFFA85D00),
            vigorous = Color(0xFFB3261E),
            rest = Color(0xFF7A8A8B),
        )

        val Dark = ZoneColours(
            recovery = Color(0xFF9FBDBE),
            zone2 = Color(0xFF4FDBCB),
            threshold = Color(0xFFFFB95C),
            vigorous = Color(0xFFFF8A80),
            rest = Color(0xFF8B9A9B),
        )
    }
}
