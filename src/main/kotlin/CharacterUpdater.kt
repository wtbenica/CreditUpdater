import Credentials.Companion.CHARACTER_STORIES_COMPLETE
import Updater.Companion.clearTerminal
import Updater.Companion.millisToPretty
import Updater.Companion.storyCount
import Updater.Companion.upFourLines
import java.sql.*
import kotlin.system.measureTimeMillis


fun String.cleanup(): String = this.dropLastWhile { it == ' ' }.dropWhile { it == ' ' }

class CharacterUpdater(private val conn: Connection?) {

    fun updateCharacters() {
        clearTerminal()

        var totalComplete: Long = CHARACTER_STORIES_COMPLETE

        val statement: Statement?
        var storyIds: ResultSet? = null

        try {
            var totalTimeMillis: Long = 0

            statement = conn?.createStatement()
            storyIds = getStoryIds(statement)

            var storyId: Int
            while (storyIds?.next() == true) {
                totalTimeMillis += measureTimeMillis workLoop@{
                    storyId = storyIds.getInt("id")
                    val publisherId: Int? = getPublisherId(storyId)

                    val characterList = storyIds.getString("characters")
                    if (characterList.isEmpty())
                        return@workLoop

                    val characters = splitOnOuterSemicolons(characterList)
                    for (character in characters) {
                        val (openParen, closeParen) = parenthesesIndexes(character)
                        val (openBracket, closeBracket) = bracketIndexes(character)

                        val endIndexName = minOf(openBracket ?: character.length, openParen ?: character.length)
                        val name: String = Updater.prepareName(character.substring(0, endIndexName))

                        if (name.isNotEmpty()) {
                            val appearanceInfo: String? = extractAppearanceInfo(openParen, closeParen, character)
                            // need to check for internal brackets. maybe?
                            val bracketedText: String? = extractBracketedText(openBracket, closeBracket, character)
                            val splitText: MutableList<String>? = bracketedText?.let { splitOnOuterSemicolons(it) }
                            val (alterEgo: String?, notes: String?, membership: String?) =
                                parseBracketedText(splitText, bracketedText)
                            val characterId: Int? = publisherId?.let { getCharacterId(name, alterEgo, it) }
                            characterId?.let { getCharacterAppearance(storyId, it, appearanceInfo, notes, membership) }
                        }
                    }

                    val numComplete = ++totalComplete - CHARACTER_STORIES_COMPLETE
                    val pctComplete: String = storyCount?.let { (totalComplete.toFloat() / it).toPercent() } ?: "???"

                    val averageTime: Long = totalTimeMillis / numComplete
                    val remainingTime: Long? = storyCount?.let {
                        averageTime * (it - numComplete)
                    }
                    val pair = millisToPretty(remainingTime)
                    val fair = millisToPretty(totalTimeMillis)

                    upFourLines()

                    println("Extract Character StoryId: $storyId")
                    println("Complete: $totalComplete${storyCount?.let { "/$it" } ?: ""} $pctComplete")
                    println("Avg: ${averageTime}ms")
                    println("Elapsed: $fair ETR: $pair")
                }
            }
            println("END\n\n\n")
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } finally {
            Updater.closeResultSet(storyIds)
        }
    }

    private fun getStoryIds(statement: Statement?): ResultSet? {
        // Get storyids and character lists
        var resultSet: ResultSet?
        val scriptSql = """SELECT g.id, g.characters
                    FROM gcd_story g
                    WHERE g.id > ${Credentials.CHARACTER_STORY_START}
                    ORDER BY g.id """

        resultSet = statement?.executeQuery(scriptSql)

        if (statement?.execute(scriptSql) == true) {
            resultSet = statement.resultSet
        }
        return resultSet
    }

    private fun parseBracketedText(
        splitText: MutableList<String>?,
        bracketedText: String?
    ): Triple<String?, String?, String?> {
        var alterEgo: String? = null
        var notes: String? = null
        var membership: String? = null
        if (splitText != null) {
            if (splitText.size > 2) {
                membership = bracketedText
            } else {
                if (splitText.size > 0) {
                    alterEgo = splitText[0].cleanup()
                }
                if (splitText.size == 2) {
                    notes = splitText[1].cleanup()
                }
            }
        }
        return Triple(alterEgo, notes, membership)
    }

    private fun extractBracketedText(
        openBracket: Int?,
        closeBracket: Int?,
        character: String
    ) = if (openBracket != null && closeBracket != null && closeBracket > openBracket) {
        try {
            character.substring(openBracket + 1, closeBracket).cleanup()
        } catch (e: StringIndexOutOfBoundsException) {
            println("ch: $character ob: $openBracket cb: $closeBracket $e")
            throw e
        }
    } else {
        null
    }

    private fun extractAppearanceInfo(
        openParen: Int?,
        closeParen: Int?,
        character: String
    ) = if (openParen != null && closeParen != null) {
        try {
            character.substring(openParen + 1, closeParen).cleanup()
        } catch (e: StringIndexOutOfBoundsException) {
            println("ch: $character op: $openParen cp: $closeParen $e")
            throw e
        }
    } else {
        null
    }

    private fun bracketIndexes(character: String): Pair<Int?, Int?> {
        val openBracket = character.indexOf('[').let {
            if (it < 0) null else it
        }
        val closeBracket = character.lastIndexOf(']').let {
            if (it < 0) null else it
        }
        return Pair(openBracket, closeBracket)
    }

    private fun parenthesesIndexes(character: String): Pair<Int?, Int?> {
        val openParen = character.indexOf('(').let {
            if (it < 0) null else it
        }
        val closeParen = character.indexOf(')', openParen ?: 0).let {
            if (it < 0) null else it
        }
        return Pair(openParen, closeParen)
    }

    /**
     * getCharacterAppearance - either returns
     *
     * @param storyId
     * @param publisherId
     * @param appearanceInfo
     * @param notes
     * @param membership
     */
    private fun getCharacterAppearance(
        storyId: Int,
        publisherId: Int,
        appearanceInfo: String?,
        notes: String?,
        membership: String?
    ): Int? =
        lookupCharacterAppearance(storyId, publisherId)
            ?: makeCharacterAppearance(storyId, publisherId, appearanceInfo, notes, membership)

    /**
     * getCharacterId - either returns existing character id, or attempts to create a character record
     *
     * @return character id or null on error
     */
    private fun getCharacterId(name: String, alterEgo: String?, publisherId: Int): Int? =
        lookupCharacter(name, alterEgo, publisherId) ?: makeCharacter(name, alterEgo, publisherId)

    // "xxxxxx [xxxxx]; xxxxx [xxxxxx [xxx]; xxxx [xxxx]]"
// -> ["xxxxxx [xxxxx]", "xxxxx [xxxxxx [xxx]; xxxx [xxxx]"]
    private fun splitOnOuterSemicolons(listString: String): MutableList<String> {
        var bracketCount = 0
        var parenCount = 0
        val splitPoints = mutableListOf<Int>()


        listString.forEachIndexed { idx, c ->
            when (c) {
                '[' -> bracketCount++
                ']' -> bracketCount--
                '(' -> parenCount++
                ')' -> parenCount--
                ';' -> if (bracketCount <= 0 && parenCount <= 0) {
                    splitPoints.add(idx)
                }
            }
        }

        if (splitPoints.lastOrNull() != listString.length) {
            splitPoints.add(listString.length)
        }

        val characters = mutableListOf<String>()
        var startIndex = 0
        for (index in splitPoints) {
            characters.add(listString.substring(startIndex, index))
            startIndex = index + 1
        }
        return characters
    }

    private fun getPublisherId(storyId: Int): Int? {
        var publisherId: Int? = null
        var resultSet: ResultSet? = null

        try {
            val getPublisherSql = """
               SELECT gs.publisher_id
               FROM gcd_series gs
               JOIN gcd_issue gi ON gs.id = gi.series_id
               JOIN gcd_story g ON gi.id = g.issue_id
               WHERE g.id = ?
            """

            val statement = conn?.prepareStatement(getPublisherSql)
            statement?.setInt(1, storyId)

            resultSet = statement?.executeQuery()

            if (statement?.execute() == true) {
                resultSet = statement.resultSet
            }

            if (resultSet?.next() == true) {
                publisherId = resultSet.getInt("publisher_id")
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            Updater.closeResultSet(resultSet)
        }

        return publisherId
    }

    private fun lookupCharacterAppearance(storyId: Int, characterId: Int): Int? {
        var appearanceId: Int? = null
        var resultSet: ResultSet? = null

        try {
            val getCharacterSql = """
               SELECT *
               FROM m_character_appearance mca
               WHERE mca.story_id = ?
               AND mca.character_id = ?
            """

            val statement = conn?.prepareStatement(getCharacterSql)
            statement?.setInt(1, storyId)
            statement?.setInt(2, characterId)

            resultSet = statement?.executeQuery()

            if (statement?.execute() == true) {
                resultSet = statement.resultSet
            }

            if (resultSet?.next() == true) {
                appearanceId = resultSet.getInt("id")
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            Updater.closeResultSet(resultSet)
        }

        return appearanceId
    }

    private fun makeCharacterAppearance(
        storyId: Int,
        characterId: Int,
        appearanceInfo: String?,
        notes: String?,
        membership: String?
    ): Int? {
        val statement: PreparedStatement?

        val sql = """
           INSERT INTO m_character_appearance(details, character_id, story_id, notes, membership)
           VALUE (?, ?, ?, ?, ?)
        """

        statement = conn?.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        statement?.setString(1, appearanceInfo)
        statement?.setInt(2, characterId)
        statement?.setInt(3, storyId)
        statement?.setString(4, notes)
        statement?.setString(5, membership)

        val affectedRows = statement?.executeUpdate()

        return if (affectedRows == 0) {
            null
        } else {
            statement!!.generatedKeys.use { generatedKeys ->
                if (generatedKeys.next()) {
                    generatedKeys.getInt(1)
                } else {
                    null
                }
            }
        }
    }

    private fun makeCharacter(name: String, alterEgo: String?, publisher_id: Int): Int? {
        val statement: PreparedStatement?
        var characterId: Int? = null

        val sql = """
           INSERT INTO m_character(name, alter_ego, publisher_id)
           VALUE (?, ?, ?)
        """

        statement = conn?.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        statement?.setString(1, name)
        statement?.setString(2, alterEgo)
        statement?.setInt(3, publisher_id)

        statement?.executeUpdate()

        val genKeys = statement?.generatedKeys
        if (genKeys?.next() == true) {
            characterId = genKeys.getInt("GENERATED_KEY")
        }

        return characterId
    }

    private fun lookupCharacter(name: String, alterEgo: String?, publisherId: Int): Int? {
        var characterId: Int? = null
        var resultSet: ResultSet? = null

        try {
            var getCharacterSql = """
               SELECT *
               FROM m_character mc
               WHERE mc.name = ?
               AND mc.publisher_id = ?
               AND mc.alter_ego 
            """

            getCharacterSql += if (alterEgo == null) {
                "IS NULL"
            } else {
                "= ?"
            }

            val statement = conn?.prepareStatement(getCharacterSql)
            statement?.setString(1, name)
            statement?.setInt(2, publisherId)
            alterEgo?.let { statement?.setString(3, it) }

            resultSet = statement?.executeQuery()

            if (statement?.execute() == true) {
                resultSet = statement.resultSet
            }

            if (resultSet?.next() == true) {
                characterId = resultSet.getInt("id")
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            Updater.closeResultSet(resultSet)
        }

        return characterId
    }
}
