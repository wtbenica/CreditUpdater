import java.sql.*

class CharacterUpdater(val conn: Connection?) {
    fun updateCharacters() {
        val statement: Statement?
        var resultSet: ResultSet? = null
        val TOTAL = 1770869
        try {
            statement = conn?.createStatement()

            // Get storyids and character lists
            val scriptSql = """SELECT g.id, g.characters
                FROM gcd_story g;"""

            resultSet = statement?.executeQuery(scriptSql)

            if (statement?.execute(scriptSql) == true) {
                resultSet = statement.resultSet
            }

            var i = 0F
            while (resultSet?.next() == true) {
                println("$i/$TOTAL ${i / TOTAL * 100}")
                i++

                val storyId = resultSet.getInt("id")
                val publisherId: Int? = getPublisherId(storyId)
                val characterList = resultSet.getString("characters")

                if (characterList.length == 0)
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
                    val closeBracket = character.lastIndexOf(']', openBracket ?: 0).let {
                        if (it < 0) null else it
                    }

                    val appearanceInfo: String? = if (openParen != null && closeParen != null) {
                        character.substring(openParen + 1, closeParen)
                    } else {
                        null
                    }

                    val bracketedText = if (openBracket != null && closeBracket != null) {
                        character.substring(openBracket + 1, closeBracket)
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
                                alterEgo = splitText[0]
                            }
                            if (splitText.size == 2) {
                                notes = splitText[1]
                            }
                        }
                    }

                    val name: String = Updater.prepareName(
                        character.substring(0, minOf(openBracket ?: character.length, openParen ?: character.length))
                    )

                    val characterId: Int? = publisherId?.let { getCharacterId(name, alterEgo, it) }

                    characterId?.let { getCharacterAppearance(storyId, it, appearanceInfo, notes, membership) }
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

        statement?.executeUpdate()
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