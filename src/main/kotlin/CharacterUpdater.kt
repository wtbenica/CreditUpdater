import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.*
import java.time.Instant
import java.time.LocalTime
import java.time.Duration

private const val INIT_INDEX = 247285L
private const val INIT_COMPLETE = 199672L
private const val TOTAL = 1770869L

class CharacterUpdater(private val conn: Connection?) {

    private fun String.cleanup(): String = this.dropLastWhile { it == ' ' }.dropWhile { it == ' ' }

    private fun Duration.fancy(): String {
        val days = this.toHours() / 24
        val hours = this.toHours()  % 24
        val minutes = this.toMinutes() % 60
        val seconds = this.seconds % 60

        return "${days}d ${hours}h ${minutes}m ${seconds}s"
    }

    fun updateCharacters() {
        val statement: Statement?
        var resultSet: ResultSet? = null
        val startTime = Instant.now()
        try {
            statement = conn?.createStatement()

            // Get storyids and character lists
            val scriptSql = """SELECT g.id, g.characters
                FROM gcd_story g
                WHERE g.id > $INIT_INDEX
                ORDER BY g.id"""

            resultSet = statement?.executeQuery(scriptSql)

            if (statement?.execute(scriptSql) == true) {
                resultSet = statement.resultSet
            }

            var i = INIT_COMPLETE
            while (resultSet?.next() == true) {
                val storyId = resultSet.getInt("id")
                val currentTime = Instant.now()
                val elapsedTime = currentTime.epochSecond - startTime.epochSecond
                val percentComplete = (i * 10000) / (TOTAL * 100)

                val secondsRemaining = elapsedTime / i * (TOTAL - i)

                val duration = Duration.between(startTime, currentTime)
                val remaining = duration.dividedBy(i).multipliedBy(TOTAL - i)
                println("$storyId $i/$TOTAL $percentComplete elapsed: ${duration.fancy()} remaining: ${remaining.fancy()}")

                i++

                val publisherId: Int? = getPublisherId(storyId)
                val characterList = resultSet.getString("characters")

                if (characterList.isEmpty())
                    continue

                val characters = splitOnOuterSemicolons(characterList)

                for (character in characters) {
                    val openParen = character.indexOf('(').let {
                        if (it < 0) null else it
                    }
                    val closeParen = character.indexOf(')', openParen ?: 0).let {
                        if (it < 0) null else it
                    }
                    val openBracket = character.indexOf('[').let {
                        if (it < 0) null else it
                    }
                    val closeBracket = character.lastIndexOf(']').let {
                        if (it < 0) null else it
                    }

                    val name: String = Updater.prepareName(
                        character.substring(0, minOf(openBracket ?: character.length, openParen ?: character.length))
                            .cleanup()
                    )

                    if (name.isNotEmpty()) {
                        val appearanceInfo: String? =
                            if (openParen != null && closeParen != null) {
                                try {
                                    character.substring(openParen + 1, closeParen).cleanup()
                                } catch (e: StringIndexOutOfBoundsException) {
                                    println("ch: $character op: $openParen cp: $closeParen $e")
                                    throw e
                                }
                            } else {
                                null
                            }

                        // need to check for internal brackets. maybe?
                        val bracketedText: String? =
                            if (openBracket != null && closeBracket != null && closeBracket > openBracket) {
                                try {
                                    character.substring(openBracket + 1, closeBracket).cleanup()
                                } catch (e: StringIndexOutOfBoundsException) {
                                    println("ch: $character ob: $openBracket cb: $closeBracket $e")
                                    throw e
                                }
                            } else {
                                null
                            }

                        val splitText: MutableList<String>? = bracketedText?.let { splitOnOuterSemicolons(it) }

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

                        CoroutineScope(Dispatchers.IO).launch {
                            val characterId: Int? = publisherId?.let { getCharacterId(name, alterEgo, it) }

                            characterId?.let { getCharacterAppearance(storyId, it, appearanceInfo, notes, membership) }
                        }
                    }
                }
            }
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } finally {
            Updater.closeResultSet(resultSet)
        }
    }

    private fun getCharacterAppearance(
        storyId: Int,
        publisherId: Int,
        appearanceInfo: String?,
        notes: String?,
        membership: String?
    ) =
        lookupCharacterAppearance(storyId, publisherId)
            ?: makeCharacterAppearance(storyId, publisherId, appearanceInfo, notes, membership)

    private fun getCharacterId(name: String, alterEgo: String?, publisherId: Int) =
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
    ) {
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

        CoroutineScope(Dispatchers.IO).launch {
            statement?.executeUpdate()
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