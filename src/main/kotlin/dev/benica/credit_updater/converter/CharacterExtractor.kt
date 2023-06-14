package dev.benica.credit_updater.converter

import dev.benica.credit_updater.Credentials.Companion.COLLECTOR_LIMIT
import dev.benica.credit_updater.Credentials.Companion.PRIMARY_DATABASE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        val characters = resultSet.getString("characters")
        val publisherId: Int? = getPublisherId(storyId, conn)

        val characterList: List<CharacterAppearance> = parseCharacters(characters)

        for (character in characterList) {
            val characterId = publisherId?.let {
                when (character) {
                    is Individual -> {
                        upsertCharacter(character.name, character.alterEgo, it)
                    }

                    is Team -> {
                        upsertCharacter(character.name, null, it)
                    }
                }
            }

            val appearance: Appearance? = characterId?.let {
                when (character) {
                    is Individual -> {
                        Appearance(
                            storyId = storyId,
                            characterId = it,
                            appearanceInfo = character.appearanceInfo,
                            notes = null,
                            membership = null
                        )
                    }

                    is Team -> {
                        Appearance(
                            storyId = storyId,
                            characterId = it,
                            appearanceInfo = character.appearanceInfo,
                            notes = null,
                            membership = character.members
                        )
                    }
                }
            }

            appearance?.let {
                newAppearanceInsertionBuffer.addToBuffer(it)
            }
        }

        return storyId
    }


    internal fun parseCharacters(characters: String): List<CharacterAppearance> {
        val fixedInput = fixMissingBrackets(characters)
        val characterList = mutableListOf<CharacterAppearance>()
        val characterStrings = splitOnOuterSemicolons(fixedInput)
        characterStrings.forEach { characterString ->
            val (name, bracketedText, appearanceInfo) = splitString(characterString)

            val (alterEgo: String?, membership: String?) =
                parseBracketedText(bracketedText)

            val isTeam = membership != null

            if (name.isNotEmpty()) {
                characterList.add(
                    if (isTeam) {
                        Team(name = name, members = bracketedText, appearanceInfo = appearanceInfo)
                    } else {
                        Individual(name = name, alterEgo = alterEgo, appearanceInfo = appearanceInfo)
                    }
                )
            }
        }
        return characterList
    }

    /**
     * Attempts to fix malformed character strings by adding missing brackets.
     * It expects a string in the format: "name/team name [alter ego/membership] (appearance notes)"
     *
     * @param input the character string to fix.
     * @return the fixed character string
     */
    internal fun fixMissingBrackets(input: String): String {
        val stack = mutableListOf<Char>()
        val fixedString = StringBuilder()
        var depth = 0
        var semicolonSeen = false

        for (char in input) {
            when (char) {
                '[' -> {
                    stack.add(char)
                    depth++
                }

                ']' -> {
                    if (stack.isNotEmpty() && stack.last() == '[') {
                        depth--
                        semicolonSeen = false
                        stack.removeAt(stack.size - 1)
                    }
                }

                ';' -> {
                    if (depth == 2 && !semicolonSeen) {
                        semicolonSeen = true
                    } else if (depth == 2) {
                        fixedString.append(']') // Add missing closing bracket
                        depth--
                        semicolonSeen = false
                        stack.removeAt(stack.size - 1)
                    }
                }
            }
            fixedString.append(char)
        }

        // Add any remaining missing closing brackets
        while (stack.isNotEmpty() && stack.last() == '[') {
            fixedString.append(']')
            stack.removeAt(stack.size - 1)
        }

        return fixedString.toString()
    }

    /**
     * Parses a character string into name/team name, alter ego/membership, and appearance notes.
     * It expects a string in the format: "name/team name [alter ego/membership] (appearance notes)"
     *
     * @param input the character string to parse.
     * @return a triple containing the name/team name, alter ego/membership, and appearance notes.
     */
    fun splitString(input: String): Triple<String, String, String> {
        // a regex to split "name [team name [or membership]] (appearance notes)" to "name", "team name [or membership]", and "appearance notes"
//        val regex = Regex("""^(.*?)(?:\[(.*?)])?\s+?(?:\((.*?)\))?${"$"}""")

        val regex = Regex("^([^\\[(]*)(?:\\[(.*)(?=]))?(?:[^(]+)?(?:\\((.*)(?=\\)))?")

        val matchResult = regex.find(input)

        val textBeforeBrackets = matchResult?.groupValues?.get(1)?.trim() ?: ""
        val textInsideBrackets = matchResult?.groupValues?.get(2)?.trim() ?: ""
        val textInsideParentheses = matchResult?.groupValues?.get(3)?.trim() ?: ""
        return Triple(textBeforeBrackets, textInsideBrackets, textInsideParentheses)
    }

    sealed class CharacterAppearance {
        abstract val name: String
        abstract val appearanceInfo: String?
    }

    data class Individual(override val name: String, val alterEgo: String?, override val appearanceInfo: String?) :
        CharacterAppearance()

    data class Team(override val name: String, val members: String, override val appearanceInfo: String?) :
        CharacterAppearance()

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
     * @param bracketedText The bracketed text to parse.
     * @return A triple of strings containing the alter ego, notes, and team
     *     members.
     */
    internal fun parseBracketedText(bracketedText: String?): Pair<String?, String?> {
        val splitText = bracketedText?.let { splitOnOuterSemicolons(it) }
        var alterEgo: String? = null
        var membership: String? = null

        // Check if the split text is not null
        if (splitText != null) {
            // If the split text has more than two elements, assume the bracketed text is a list of team members
            if (splitText.size > 1) {
                membership = bracketedText
            } else {
                // If the split text has at least one element, assume the first element is the alter ego
                if (splitText.isNotEmpty()) {
                    alterEgo = splitText[0].cleanup()
                }
            }
        }
        return Pair(alterEgo, membership)
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
    internal fun bracketIndexes(character: String): Pair<Int?, Int?> {
        val openBracket = character.indexOf('[')
        val closeBracket = character.lastIndexOf(']')
        return Pair(openBracket.takeIf { it > -1 }, closeBracket.takeIf { it > -1 })
    }

    /**
     * Finds the indices of the first opening and closing parentheses in the
     * given [character] string.
     *
     * @param character the string to search for parentheses
     * @return a pair of integers representing the indices of the opening and closing parentheses respectively. If either index is `null`, it means that the corresponding parenthesis was not found in the string.
     */
    internal fun parenthesesIndexes(character: String): Pair<Int?, Int?> {
        val openParen = character.indexOf('(')
        val closeParen = character.indexOf(')', openParen)
        return Pair(openParen.takeIf { it > -1 }, closeParen.takeIf { it > -1 })
    }

    /**
     * Splits a string into substrings based on semicolons that are not inside
     * brackets or parentheses.
     *
     * @param input the input string to split
     * @return a mutable list of substrings
     */
    internal fun splitOnOuterSemicolons(input: String): List<String> {
        var depth = 0
        var start = 0
        val characters = mutableListOf<String>()
        for (i in input.indices) {
            when (input[i]) {
                '(', '[' -> depth++
                ')', ']' -> depth--
                ';' -> if (depth <= 0) {
                    val element = input.substring(start, i).trim()
                    if (element.isNotEmpty()) {
                        characters.add(element)
                    }
                    start = i + 1
                }
            }
        }
        val element = input.substring(start).trim()
        if (element.isNotEmpty()) {
            characters.add(element)
        }
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
//            logger.info { "Inserted ${appearances.size} appearances" }
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
         */
        @Synchronized
        fun save() {
            // a copy of 'appearances'
            val appearances = this.appearances.toSet()
            _appearances.clear()
            CoroutineScope(Dispatchers.IO).launch {
//            logger.info { "Saving character appearances" }
                insertCharacterAppearances(appearances, database, conn)
            }
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
