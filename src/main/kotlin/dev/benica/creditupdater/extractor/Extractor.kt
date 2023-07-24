/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.creditupdater.extractor

import java.sql.Connection
import java.sql.ResultSet

/**
 * Extractor - Converts text fields from a gcd_story_credit to a relational
 * link. There are two types of fields: Creator Credits and Character
 * Appearances.
 *
 * @constructor Create empty Extractor
 * @property schema The schema to use.
 */
abstract class Extractor(
    protected val schema: String,
) {
    // The table from which data will be extracted
    abstract val extractTable: String

    // The type of item being extracted
    abstract val extractedItem: String

    // The column name of the id of the item being extracted
    abstract val fromValue: String

    /**
     * Extract and insert - extracts the items from the [resultSet] and inserts
     * them into the database.
     *
     * @param resultSet The result set to extract from.
     * @return the id of the story from which items were extracted.
     */
    abstract fun extractAndInsert(
        resultSet: ResultSet,
        conn: Connection
    ): Int
}

/**
 * Cleanup - removes leading and trailing whitespace and reduces any
 * interior multiple-spaces to single spaces.
 */
fun String.cleanup(): String = this
    .replace(regex = Regex(pattern = "^\\s*"), replacement = "")
    .replace(regex = Regex(pattern = "\\s*$"), replacement = "")
    .replace(regex = Regex(pattern = "\\s+"), replacement = " ")
