package dev.benica.converter

import java.sql.Connection
import java.sql.ResultSet

/**
 * Prepare name - removes any parentheticals, brackets, and question marks
 * from the name.
 *
 * @return the name with parentheticals, brackets, and question marks
 *     removed.
 */
fun String.prepareName(): String = this
    .replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
    .replace(Regex("\\s*\\[[^]]*]\\s*"), "")
    .replace(Regex("\\s*\\?\\s*"), "")
    .replace(Regex("^\\s*"), "")
    .cleanup()

/**
 * Cleanup - removes leading and trailing whitespace and reduces any
 * interior multiple-spaces to single spaces.
 */
fun String.cleanup(): String = this
    .replace(regex = Regex(pattern = "^[\\s]*"), replacement = "")
    .replace(regex = Regex(pattern = "[\\s]*$"), replacement = "")
    .replace(regex = Regex(pattern = "\\s+"), replacement = " ")

/**
 * Extractor - Converts text fields from a gcd_story_credit to a relational
 * link. There are two types of fields: Creator Credits and Character
 * Appearances.
 *
 * @constructor Create empty Extractor
 * @property database The database to use.
 * @property conn The connection to the database.
 */
abstract class Extractor(
    protected val database: String,
    private val conn: Connection,
) {
    abstract val extractedItem: String
    abstract val fromValue: String

    /**
     * Extract and insert - extracts the items from the [resultSet] and
     * inserts them into the database.
     *
     * @param resultSet The result set to extract from.
     * @param destDatabase The destination database.
     * @return the id of the story from which items were extracted.
     */
    abstract suspend fun extractAndInsert(
        resultSet: ResultSet,
        destDatabase: String? = null
    ): Int
}
