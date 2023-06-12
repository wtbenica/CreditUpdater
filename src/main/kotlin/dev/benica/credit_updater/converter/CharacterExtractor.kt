package dev.benica.credit_updater.converter

import dev.benica.credit_updater.Credentials.Companion.COLLECTOR_LIMIT
import dev.benica.credit_updater.Credentials.Companion.PRIMARY_DATABASE
import mu.KLogger
import mu.KotlinLogging
import java.lang.Integer.min
import java.sql.*
import java.time.Instant

val Any.logger: KLogger
    get() = KotlinLogging.logger(this::class.java.name)

/**
 * Character extractor - extracts character records and character
 * appearance records from the 'character' text field in gcd_story
 * and creates linked entries for them in the 'm_character' and
 * 'm_character_appearance' tables.
 *
 * @param database the database to which to write the extracted character
 *     and appearance data.
 * @param conn
 */
class CharacterExtractor(database: String, conn: Connection) : Extractor(database, conn) {
    override val extractedItem: String = "Character"
    override val fromValue: String = "Story"

    @Volatile
    private var _newAppearanceInsertionBuffer: NewAppearanceInsertionBuffer? = null

    private val newAppearanceInsertionBuffer: NewAppearanceInsertionBuffer
        get() = _newAppearanceInsertionBuffer ?: synchronized(this) {
            NewAppearanceInsertionBuffer(database, conn)
        }.also {
            _newAppearanceInsertionBuffer = it
        }

    /**
     * Extracts characters from a result set and adds them to the
     * [newAppearanceInsertionBuffer] for database insertion.
     *
     * @param resultSet The result set of stories from which to extract
     *     characters.
     * @param destDatabase The destination database.
     * @return the story id of the story from which characters were extracted.
     */
    override suspend fun extract(
        resultSet: ResultSet,
        destDatabase: String?
    ): Int {
        val storyId = resultSet.getInt("id")
        val publisherId: Int? = getPublisherId(storyId, conn)

        val characterList = resultSet.getString("characters")

        if (characterList.isNotEmpty()) {
            val characters = splitOnOuterSemicolons(characterList)
            for (character in characters) {
                val (openParen, closeParen) = parenthesesIndexes(character)
                val (openBracket, closeBracket) = bracketIndexes(character)

                val endIndexName =
                    minOf(openBracket ?: character.length, openParen ?: character.length)
                val name: String = character.substring(0, endIndexName).prepareName()

                if (name.isNotEmpty()) {
                    val appearanceInfo: String? =
                        extractAppearanceInfo(openParen, closeParen, character)

                    // need to check for internal brackets. maybe?
                    val bracketedText: String? =
                        extractBracketedText(openBracket, closeBracket, character)

                    val splitText: List<String>? =
                        bracketedText?.let { splitOnOuterSemicolons(it) }

                    val (alterEgo: String?, notes: String?, membership: String?) =
                        parseBracketedText(splitText, bracketedText)

                    val characterId: Int? =
                        publisherId?.let { upsertCharacter(name, alterEgo, it) }

                    val appearance: Appearance? = characterId?.let {
                        Appearance(
                            storyId,
                            it,
                            appearanceInfo,
                            notes,
                            membership
                        )
                    }

                    appearance?.let {
                        newAppearanceInsertionBuffer.addToBuffer(it)
                    }
                }
            }
        }

        return storyId
    }

    override fun finish() {
        newAppearanceInsertionBuffer.save()
        logger.info { "FINISHING" }
    }

    /**
     * Gets the ID of a character with the given name, alter ego, and publisher
     * ID. If the character does not exist in the database, it is inserted and
     * the new ID is returned.
     *
     * @param name The name of the character.
     * @param alterEgo The alter ego of the character, or null if the character
     *     does not have an alter ego.
     * @param publisherId The ID of the publisher of the character.
     * @return The ID of the character, or null if the character could not be
     *     found or inserted.
     */
    private fun upsertCharacter(
        name: String,
        alterEgo: String?,
        publisherId: Int,
    ): Int? = lookupCharacter(name, alterEgo, publisherId, database, conn)
        ?: insertCharacter(
            name,
            alterEgo,
            publisherId
        )

    /**
     * Parses bracketed text and returns a triple of strings containing the
     * alter ego and notes, or a list of team members.
     *
     * @param splitText The text to parse, split into a list of strings.
     * @param bracketedText The bracketed text to parse.
     * @return A triple of strings containing the alter ego, notes, and team
     *     members.
     */
    private fun parseBracketedText(
        splitText: List<String>?,
        bracketedText: String?
    ): Triple<String?, String?, String?> {
        var alterEgo: String? = null
        var notes: String? = null
        var membership: String? = null

        // Check if the split text is not null
        if (splitText != null) {
            // If the split text has more than two elements, assume the bracketed text is a list of team members
            if (splitText.size > 2) {
                membership = bracketedText
            } else {
                // If the split text has at least one element, assume the first element is the alter ego
                if (splitText.isNotEmpty()) {
                    alterEgo = splitText[0].cleanup()
                }
                // If the split text has two elements, assume the second element is the notes
                if (splitText.size == 2) {
                    notes = splitText[1].cleanup()
                }
            }
        }
        return Triple(alterEgo, notes, membership)
    }

    /**
     * Extract bracketed text - extracts the text that appears between
     * [openBracket] and [closeBracket] in the given [character] string.
     *
     * @param openBracket the index of the opening bracket
     * @param closeBracket the index of the closing bracket
     * @param character a character appearance string in the format "Name
     *     [Alter Ego/Notes?/Membership?] (Appearance Info)"
     */
    private fun extractBracketedText(
        openBracket: Int?,
        closeBracket: Int?,
        character: String
    ) = if ((openBracket == null) || (closeBracket == null) || (closeBracket <= openBracket)) {
        null
    } else {
        try {
            character.substring(openBracket + 1, closeBracket).cleanup().ifBlank { null }
        } catch (e: StringIndexOutOfBoundsException) {
            logger.info { "ch: $character ob: $openBracket cb: $closeBracket $e" }
            throw e
        }
    }

    /**
     * Extract appearance info - extracts the appearance info (e.g. cameo,
     * first appearance, death, etc.) that appears between [openParen] and
     * [closeParen] in the given [character] string.
     *
     * @param openParen the index of the opening parenthesis
     * @param closeParen the index of the closing parenthesis
     * @param character a character appearance string in the format "Name
     *     [Alter Ego] (Appearance Info)"
     */
    private fun extractAppearanceInfo(
        openParen: Int?,
        closeParen: Int?,
        character: String
    ) = if (openParen != null && closeParen != null) {
        try {
            character.substring(openParen + 1, closeParen).cleanup()
        } catch (e: StringIndexOutOfBoundsException) {
            logger.info { "ch: $character op: $openParen cp: $closeParen $e" }
            throw e
        }
    } else {
        null
    }

    /**
     * Finds the indices of the first opening and closing square brackets in
     * the given [character] string.
     *
     * @param character the string to search for square brackets
     * @return a pair of integers representing the indices of the opening and
     *     closing square brackets, respectively. If either index is `null`, it
     *     means that the corresponding parenthesis was not found in the
     *     string.
     */
    private fun bracketIndexes(character: String): Pair<Int?, Int?> {
        val openBracket = character.indexOf('[')
        val closeBracket = character.lastIndexOf(']')
        return Pair(openBracket.takeIf { it >= -1 }, closeBracket.takeIf { it >= -1 })
    }

    /**
     * Finds the indices of the first opening and closing parentheses in the
     * given [character] string.
     *
     * @param character the string to search for parentheses
     * @return a pair of integers representing the indices of the opening and
     *     closing parentheses, respectively. If either index is `null`, it
     *     means that the corresponding parenthesis was not found in the
     *     string.
     */
    private fun parenthesesIndexes(character: String): Pair<Int?, Int?> {
        val openParen = character.indexOf('(')
        val closeParen = character.indexOf(')', openParen)
        return Pair(openParen.takeIf { it >= -1 }, closeParen.takeIf { it >= -1 })
    }

    /**
     * Splits a string into substrings based on semicolons that are not inside
     * brackets or parentheses.
     *
     * @param input the input string to split
     * @return a mutable list of substrings
     */
    private fun splitOnOuterSemicolons(input: String): List<String> {
        var depth = 0
        var start = 0
        val characters = mutableListOf<String>()
        for (i in input.indices) {
            when (input[i]) {
                '(', '[' -> depth++
                ')', ']' -> depth--
                ';' -> if (depth == 0) {
                    characters.add(input.substring(start, i))
                    start = i + 1
                }
            }
        }
        characters.add(input.substring(start))
        return characters
    }

    /**
     * Insert character appearances - inserts [appearances] into [database]
     * using [conn]
     */
    private fun insertCharacterAppearances(
        appearances: Set<Appearance>,
        database: String,
        conn: Connection?
    ) {
        val sql = """
               INSERT IGNORE INTO ${database}.m_character_appearance(id, details, character_id, story_id, notes, membership)
               VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()

        conn?.prepareStatement(sql).use { statement ->
            appearances.forEach { appearance ->
                statement?.setInt(1, appearance.id)
                statement?.setString(2, appearance.appearanceInfo)
                statement?.setInt(3, appearance.characterId)
                statement?.setInt(4, appearance.storyId)
                statement?.setString(5, appearance.notes)
                statement?.setString(6, appearance.membership)
                statement?.addBatch()
            }

            statement?.executeBatch()
            logger.info { "Inserted ${appearances.size} appearances" }
        }
    }

    /**
     * Insert character - inserts [name], [alterEgo], and [publisherId] into
     * [database] table m_character
     *
     * @param name character name
     * @param alterEgo character alter ego
     * @param publisherId publisher id
     * @return inserted character's id
     */
    private fun insertCharacter(
        name: String,
        alterEgo: String?,
        publisherId: Int
    ): Int? {
        val statement: PreparedStatement?
        var characterId: Int? = null
        val truncatedName = name.substring(0, min(255, name.length))
        val sql = """
           INSERT INTO ${database}.m_character(name, alter_ego, publisher_id)
           VALUE (?, ?, ?)
        """

        statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        statement?.setString(1, truncatedName)
        statement?.setString(2, alterEgo)
        statement?.setInt(3, publisherId)

        statement?.executeUpdate()

        val genKeys = statement?.generatedKeys
        if (genKeys?.next() == true) {
            characterId = genKeys.getInt("GENERATED_KEY")
        }

        return characterId
    }

    /**
     * Lookup character - looks up character by [name], [alterEgo], and
     * [publisherId]. If [checkPrimary] is true, [PRIMARY_DATABASE] is checked
     * first, then [database]. If [checkPrimary] is false, only [database]
     * is checked. If [checkPrimary] is true, [PRIMARY_DATABASE] will not be
     * checked, only [database].
     *
     * @param name character name
     * @param alterEgo character alter ego
     * @param publisherId publisher id
     * @param database database to check
     * @param conn connection to use
     * @param checkPrimary whether or not to check [PRIMARY_DATABASE] first
     * @return character id if found, null otherwise
     */
    private fun lookupCharacter(
        name: String,
        alterEgo: String?,
        publisherId: Int,
        database: String,
        conn: Connection?,
        checkPrimary: Boolean = database != PRIMARY_DATABASE
    ): Int? {
        var characterId: Int? = null
        val db = if (checkPrimary) {
            PRIMARY_DATABASE
        } else {
            database
        }

        try {
            val getCharacterSql = """
            SELECT *
            FROM $db.m_character mc
            WHERE mc.name = ?
            AND mc.publisher_id = ?
            AND mc.alter_ego ${if (alterEgo == null) "IS NULL" else "= ?"}
        """

            conn?.prepareStatement(getCharacterSql).use { statement ->
                statement?.setString(1, name)
                statement?.setInt(2, publisherId)
                if (alterEgo != null) {
                    statement?.setString(3, alterEgo)
                }

                statement?.executeQuery().use { resultSet ->
                    if (resultSet?.next() == true) {
                        characterId = resultSet.getInt("id")
                    }
                }
            }

            if (characterId == null && database != PRIMARY_DATABASE && checkPrimary) {
                characterId =
                    lookupCharacter(
                        name = name,
                        alterEgo = alterEgo,
                        publisherId = publisherId,
                        database = database,
                        conn = conn,
                        checkPrimary = false
                    )
            }
        } catch (sqlEx: SQLException) {
            logger.error("Error looking up character", sqlEx)
        }
        return characterId
    }

    /**
     * Get publisher id - gets publisher id for [storyId] from [conn]
     *
     * @return publisher id if found, null otherwise
     */
    private fun getPublisherId(storyId: Int, conn: Connection?): Int? {
        var publisherId: Int? = null

        try {
            val getPublisherSql = """
               SELECT gs.publisher_id
               FROM gcd_series gs
               JOIN gcd_issue gi ON gs.id = gi.series_id
               JOIN gcd_story g ON gi.id = g.issue_id
               WHERE g.id = ?
            """

            conn?.prepareStatement(getPublisherSql).use { statement ->
                statement?.setInt(1, storyId)

                statement?.executeQuery().use { resultSet ->
                    if (resultSet?.next() == true) {
                        publisherId = resultSet.getInt("publisher_id")
                    }
                }
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        return publisherId
    }

    /**
     * A buffer for temporarily storing new character appearances before saving
     * them to the database. This buffer is designed to improve performance by
     * allowing multiple appearances to be saved at once.
     *
     * @property database the name of the database to save the appearances to
     * @property conn the database connection to use for saving the appearances
     */
    inner class NewAppearanceInsertionBuffer(
        private val database: String,
        private val conn: Connection?
    ) {
        private val _appearances: MutableSet<Appearance> = mutableSetOf()
        private val appearances: Set<Appearance>
            get() = _appearances
        private var lastUpdated: Long = Instant.MAX.epochSecond

        /**
         * Adds a new appearance to the buffer. If the buffer has reached its
         * limit, the current set of appearances will be saved to the database.
         *
         * @param appearance the appearance to add to the buffer
         */
        @Synchronized
        fun addToBuffer(appearance: Appearance?) {
            if (appearance != null) {
                // Add the appearance to the set and update the timestamp
                _appearances.add(appearance)
                lastUpdated = Instant.now().epochSecond

                // If the set has reached the collector limit, save the current set of appearances
                if (_appearances.size >= COLLECTOR_LIMIT) {
                    save()
                }
            }
        }

        /**
         * Saves the current set of appearances in the buffer to the database.
         * This method is synchronized to prevent multiple threads from saving the
         * buffer simultaneously.
         */
        @Synchronized
        fun save() {
            logger.info { "Saving character appearances" }
            insertCharacterAppearances(appearances, database, conn)
            _appearances.clear()
        }
    }

    /** Appearance - represents a character appearance in a story. */
    data class Appearance(
        val storyId: Int,
        val characterId: Int,
        val appearanceInfo: String?,
        val notes: String?,
        val membership: String?,
        val id: Int = 0
    )
}
