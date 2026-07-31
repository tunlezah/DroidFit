package com.visceralfit.domain.model

/**
 * A single movement or machine effort, fully described so it can be taught
 * offline in text and speech (PRD REQ-040..REQ-047).
 *
 * Every field is mandatory content, not decoration: the QA gate in
 * `/framework/13_testing_strategy.md` asserts that no seeded exercise ships with
 * a blank [howTo], [safetyNotes], [commonMistakes] or [spokenInstruction].
 */
data class Exercise(
    /** Stable slug, e.g. "bodyweight_dead_bug". Never renamed once shipped. */
    val id: String,
    val name: String,
    val modality: Modality,
    val difficulty: ExperienceLevel,
    /**
     * MET value from the 2024 Adult Compendium of Physical Activities, used for
     * the energy-expenditure estimate. See framework/data/met_values.json.
     */
    val metValue: Double,
    /** Ordered cue-by-cue setup and execution, shown on screen. */
    val howTo: List<String>,
    /**
     * A single sentence the text-to-speech coach reads when the exercise starts.
     * Kept separate from [howTo] because spoken cues must be shorter than the
     * shortest interval this exercise can appear in (see REQ-052).
     */
    val spokenInstruction: String,
    val safetyNotes: List<String>,
    val commonMistakes: List<String>,
    val musclesWorked: List<String>,
    /** Identifier of the vector illustration drawn for this exercise. */
    val illustrationId: String,
    /** Contraindication tags the generator filters on, e.g. "lower_back", "pregnancy". */
    val cautionTags: Set<String> = emptySet(),
    /** True when the movement is bilateral-alternating and should be cued per side. */
    val isPerSide: Boolean = false,
    /** Citation keys into framework/data/references.md justifying inclusion. */
    val evidenceKeys: List<String> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "Exercise id must not be blank" }
        require(metValue > 0.0) { "MET value must be positive for $id" }
        require(howTo.isNotEmpty()) { "Exercise $id has no instructions" }
        require(spokenInstruction.isNotBlank()) { "Exercise $id has no spoken cue" }
    }
}
