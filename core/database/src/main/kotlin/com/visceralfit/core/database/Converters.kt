package com.visceralfit.core.database

import androidx.room.TypeConverter

/**
 * String-list persistence.
 *
 * Newline-delimited rather than JSON because these lists are short, human-readable
 * in a database inspector, and never queried into. The delimiter is a newline
 * because exercise cues legitimately contain commas and semicolons.
 *
 * [encode] rejects entries containing a newline instead of silently corrupting the
 * round trip — a cue that splits into two on read would show up as a nonsense
 * instruction, which is worse than a loud failure during seeding.
 */
class Converters {

    @TypeConverter
    fun encode(values: List<String>?): String? = values?.let {
        require(it.none { entry -> entry.contains(DELIMITER) }) {
            "List entries must not contain a newline; offending list: $it"
        }
        it.joinToString(DELIMITER.toString())
    }

    @TypeConverter
    fun decode(stored: String?): List<String>? =
        stored?.takeIf { it.isNotEmpty() }?.split(DELIMITER) ?: stored?.let { emptyList() }

    private companion object {
        const val DELIMITER = '\n'
    }
}
