package com.visceralfit.core.testing

/**
 * Copy the app may never contain, from `framework/02_evidence_base.md` §6.
 *
 * WHY IT LIVES IN A SHARED MODULE: the rule applies to every string a user can read or
 * hear — exercise safety notes, session titles, spoken coaching cues. It was originally a
 * private list inside `:app`'s catalogue test, which meant the spoken cues added in phase 08
 * were not checked against it at all. A rule that only covers the place it was first written
 * is not a rule.
 *
 * The two categories are different failures. [UNSUPPORTED] are health claims the evidence
 * base does not support: no exercise targets fat in a particular place, and nothing here
 * measures visceral fat. [OVERPROMISING] are absolutes — "everyone", "fixes" — which are
 * false about bodies in general and unknowable about this user in particular.
 */
object ProhibitedClaims {

    val UNSUPPORTED = listOf(
        Regex("belly fat", RegexOption.IGNORE_CASE),
        Regex("visceral fat", RegexOption.IGNORE_CASE),
        Regex("spot[- ]reduc", RegexOption.IGNORE_CASE),
        Regex("\\bmelts?\\b", RegexOption.IGNORE_CASE),
        Regex("\\btorch(es|ing)?\\b", RegexOption.IGNORE_CASE),
        Regex("burns? (body )?fat", RegexOption.IGNORE_CASE),
    )

    val OVERPROMISING = listOf(
        Regex("everyone can", RegexOption.IGNORE_CASE),
        Regex("(fixes|cures) your", RegexOption.IGNORE_CASE),
    )

    val ALL: List<Regex> = UNSUPPORTED + OVERPROMISING

    /** The prohibited phrases [text] contains, empty when it is clean. */
    fun violationsIn(text: String): List<String> =
        ALL.mapNotNull { pattern -> pattern.find(text)?.value }
}
