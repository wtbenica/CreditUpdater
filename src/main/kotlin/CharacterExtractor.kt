import Credentials.Companion.COLLECTOR_LIMIT
import Credentials.Companion.PRIMARY_DATABASE
import DatabaseUtil.Companion.closeResultSet
import kotlinx.coroutines.*
import java.sql.*
import java.time.Instant


class CharacterExtractor(database: String, conn: Connection) : Extractor(database, conn) {
    @Volatile
    private var _appearanceCollector: Collector? = null
    private val lastUpdated = Instant.MAX.epochSecond

    private val appearanceCollector: Collector
        get() = _appearanceCollector ?: synchronized(this) {
            Collector(database, conn)
        }.also {
            _appearanceCollector = it
        }


    inner class Collector(private val database: String, private val conn: Connection?) {
        private val _appearances: MutableSet<Appearance> = mutableSetOf()
        private val appearances: Set<Appearance>
            get() = _appearances
        private var lastUpdated: Long = Instant.MAX.epochSecond

        @Synchronized
        fun addToSet(appearance: Appearance?) {
            if (appearance == null) {
                save()
            } else {
                _appearances.add(appearance)
                lastUpdated = Instant.now().epochSecond

                if (_appearances.size >= COLLECTOR_LIMIT) {
                    save()
                }
            }
        }

        @Synchronized
        fun save() {
            println("Saving character appearances")
            makeCharacterAppearance(appearances, database, conn)
            _appearances.clear()
        }
    }

    override suspend fun extract(
        extractFrom: ResultSet,
        destDatabase: String?
    ): Int {
        val storyId = extractFrom.getInt("id")
        val publisherId: Int? = getPublisherId(storyId, conn)

        val characterList = extractFrom.getString("characters")

        if (characterList.isNotEmpty()) {
            val characters = splitOnOuterSemicolons(characterList)
            for (character in characters) {
                val (openParen, closeParen) = parenthesesIndexes(character)
                val (openBracket, closeBracket) = bracketIndexes(character)

                val endIndexName = minOf(openBracket ?: character.length, openParen ?: character.length)
                val name: String = character.substring(0, endIndexName).prepareName()

                if (name.isNotEmpty()) {
                    val appearanceInfo: String? = extractAppearanceInfo(openParen, closeParen, character)
                    // need to check for internal brackets. maybe?
                    val bracketedText: String? = extractBracketedText(openBracket, closeBracket, character)
                    val splitText: MutableList<String>? = bracketedText?.let { splitOnOuterSemicolons(it) }
                    val (alterEgo: String?, notes: String?, membership: String?) =
                        parseBracketedText(splitText, bracketedText)
                    val characterId: Int? =
                        publisherId?.let { getCharacterId(name, alterEgo, it, destDatabase) }
                    val appearance: Appearance? = characterId?.let {
                        getCharacterAppearance(
                            storyId,
                            it,
                            appearanceInfo,
                            notes,
                            membership
                        )
                    }

                    appearance?.let {
                        appearanceCollector.addToSet(it)
                    }
                }
            }
        }

        return storyId
    }

    override fun finish() {
        appearanceCollector.save()
        println("FINISHING")
    }

    private fun getCharacterId(
        name: String,
        alterEgo: String?,
        publisherId: Int,
        destDatabase: String? = null
    ): Int? = lookupCharacter(name, alterEgo, publisherId, database, conn)
        ?: makeCharacter(
            name,
            alterEgo,
            publisherId
        )

    data class Appearance(
        val storyId: Int,
        val characterId: Int,
        val appearanceInfo: String?,
        val notes: String?,
        val membership: String?,
        val id: Int = 0
    )

    /**
     * getCharacterAppearance - either returns
     *
     * @param storyId
     * @param characterId
     * @param appearanceInfo
     * @param notes
     * @param membership
     */
    private fun getCharacterAppearance(
        storyId: Int,
        characterId: Int,
        appearanceInfo: String?,
        notes: String?,
        membership: String?
    ): Appearance = Appearance(storyId, characterId, appearanceInfo, notes, membership, 0)


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

    /**
     * Looks in primary database then [database] for matching character. INE creates
     * and inserts a new record.
     *
     * @param storyId
     * @param characterId
     * @param appearanceInfo
     * @param notes
     * @param membership
     * @param database
     * @param conn
     * @return
     */
    /* TODO:
    WHEN IT COMES TIME TO MIGRATE MY IDEA IS THIS:
        FOR EACH CHARACTER:
            SAVE 'OLD' ID
            SET ID TO ZERO TO GET AUTO INCREMENT ID ON INSERT
            GET ALL APPEARANCES FOR 'OLD' ID
            
            FOR EACH OF THOSE APPEARANCES:
                (because new character, must be new appearance)
                SET CHARACTER ID FIELD TO NEW ID
                TRY AND CHECK AGAINST DUPLICATE ENTRIES
                INSERT
                REMOVE
        EVERY APPEARANCE LEFT COMES FROM AN EXISTING CHARACTER AND IS POSSIBLY A DUPLICATE
        INSERT/UPDATE/IGNORE AS APPROPRIATE
     */
//        private fun lookupCharacterAppearance(
//            storyId: Int,
//            characterId: Int,
//            appearanceInfo: String?,
//            notes: String?,
//            membership: String?,
//            database: String,
//            conn: Connection?
//        ): Appearance? {
//            var result: Appearance? = null
//            var resultSet: ResultSet? = null
//
//            // this is just duplicate checking which is unlikely 
//            try {
//                val getCharacterSql = """
//               SELECT *
//               FROM ${database}.m_character_appearance mca
//               WHERE mca.story_id = ?
//               AND mca.character_id = ?
//            """
//
//                val statement = conn?.prepareStatement(getCharacterSql)
//                statement?.setInt(1, storyId)
//                statement?.setInt(2, characterId)
//
//                resultSet = statement?.executeQuery()
//
//                if (statement?.execute() == true) {
//                    resultSet = statement.resultSet
//                }
//
//                val appearanceId: Int? = resultSet?.let j@{
//                    return@j if (it.next()) resultSet.getInt("id")
//                    else null
//                }
//                appearanceId?.let {
//                    result = Appearance(
//                        storyId = storyId,
//                        characterId = characterId,
//                        appearanceInfo = appearanceInfo ?: resultSet?.getString("details"),
//                        notes = notes ?: resultSet?.getString("notes"),
//                        membership = membership ?: resultSet?.getString("membership"),
//                        id = it
//                    )
//                }
//            } catch (sqlEx: SQLException) {
//                sqlEx.printStackTrace()
//            } catch (e: java.lang.Exception) {
//                e.printStackTrace()
//            } finally {
//                closeResultSet(resultSet)
//            }
//
//            return result
//        }

    private fun makeCharacterAppearance(
        appearances: Set<Appearance>,
        database: String,
        conn: Connection?
    ) {
        val statement: PreparedStatement?
        var argIndex = 1
        val sql = StringBuilder()
        sql.append(
            """
               INSERT INTO ${database}.m_character_appearance(id, details, character_id, story_id, notes, membership)
               VALUES 
            """
        )

        appearances.forEachIndexed { index, _ ->
            sql.append("""(?, ?, ?, ?, ?, ?)""")
            if (index + 1 < appearances.size)
                sql.append(
                    """,
                    """
                )
        }

        statement = conn?.prepareStatement(sql.toString())

        appearances.forEach {
            statement?.setInt(argIndex++, it.id)
            statement?.setString(argIndex++, it.appearanceInfo)
            statement?.setInt(argIndex++, it.characterId)
            statement?.setInt(argIndex++, it.storyId)
            statement?.setString(argIndex++, it.notes)
            statement?.setString(argIndex++, it.membership)
        }

        statement?.executeUpdate()
        println("Inserted ${appearances.size} appearances")
    }

    private fun makeCharacter(
        name: String,
        alterEgo: String?,
        publisherId: Int
    ): Int? {
        val statement: PreparedStatement?
        var characterId: Int? = null

        val sql = """
           INSERT INTO ${database}.m_character(name, alter_ego, publisher_id)
           VALUE (?, ?, ?)
        """

        statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        statement?.setString(1, name)
        statement?.setString(2, alterEgo)
        statement?.setInt(3, publisherId)

        statement?.executeUpdate()

        val genKeys = statement?.generatedKeys
        if (genKeys?.next() == true) {
            characterId = genKeys.getInt("GENERATED_KEY")
        }

        return characterId
    }

    private fun lookupCharacter(
        name: String,
        alterEgo: String?,
        publisherId: Int,
        database: String,
        conn: Connection?
    ): Int? {
        var characterId: Int? = null
        var resultSet: ResultSet? = null

        try {
            // Lookup in primary db
            var getCharacterSql = """
                    SELECT *
                    FROM ${PRIMARY_DATABASE}.m_character mc
                    WHERE mc.name = '${escapeSingleQuotes(name)}'
                    AND mc.publisher_id = $publisherId
                    AND mc.alter_ego """

            getCharacterSql += if (alterEgo == null) {
                "IS NULL"
            } else {
                "= '${escapeSingleQuotes(alterEgo)}'"
            }

            val statement = conn?.prepareStatement(getCharacterSql)

            resultSet = statement?.executeQuery()!!

            if (statement.execute()) {
                resultSet = statement.resultSet
            }

            if (resultSet?.next() == true) {
                characterId = resultSet.getInt("id")
            }

            if (characterId == null && database != PRIMARY_DATABASE) {
                try {
                    var getCharacterSqlDest = """
                                SELECT *
                                FROM ${database}.m_character mc
                                WHERE mc.name = '${escapeSingleQuotes(name)}'
                                AND mc.publisher_id = $publisherId
                                AND mc.alter_ego 
                            """

                    getCharacterSqlDest += if (alterEgo == null) {
                        "IS NULL"
                    } else {
                        "= '${escapeSingleQuotes(alterEgo)}'"
                    }

                    val statementDest: PreparedStatement = conn.prepareStatement(getCharacterSqlDest)

                    resultSet = statementDest.executeQuery()!!

                    if (statementDest.execute()) {
                        resultSet = statementDest.resultSet
                    }

                    if (resultSet?.next() == true) {
                        resultSet.let {
                            characterId = it.getInt("id")
                        }
                    }
                } catch (sqlEx: SQLException) {
                    sqlEx.printStackTrace()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            closeResultSet(resultSet)
        }

        return characterId
    }

    private fun escapeSingleQuotes(name: String) = name.replace("'", "\\\'")

    private fun getPublisherId(storyId: Int, conn: Connection?): Int? {
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
            closeResultSet(resultSet)
        }

        return publisherId
    }
}
