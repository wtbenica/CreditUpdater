import DatabaseUtil.Companion.closeResultSet
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Credit extractor - creates linked credits from named credits
 *
 * @param database the database
 * @param conn the connection
 * @constructor Create empty Credit extractor
 */
class CreditExtractor(database: String, conn: Connection) : Extractor(database, conn) {
    override val extractedItem = "Credit"
    override val fromValue = "StoryId"

    /**
     * Extract - extracts named credits from [resultSet] and inserts story
     * credits into the database.
     *
     * @param resultSet expecting a story result set
     * @param destDatabase not used
     * @return the story id
     */
    override suspend fun extract(
        resultSet: ResultSet,
        destDatabase: String?
    ): Int {
        val storyId = resultSet.getInt("id")

        val scriptNames = resultSet.getString("script").split(';')
        val pencilsNames = resultSet.getString("pencils").split(';')
        val inksNames = resultSet.getString("inks").split(';')
        val colorsNames = resultSet.getString("colors").split(';')
        val lettersNames = resultSet.getString("letters").split(';')
        val editingNames = resultSet.getString("editing").split(';')

        makeCredits(scriptNames, storyId, 1)
        makeCredits(pencilsNames, storyId, 2)
        makeCredits(inksNames, storyId, 3)
        makeCredits(colorsNames, storyId, 4)
        makeCredits(lettersNames, storyId, 5)
        makeCredits(editingNames, storyId, 6)
        return storyId
    }

    override fun finish() {
        // do nothing
    }

    /**
     * Make credits - calls [makeCredit] for each name in [scriptNames]
     *
     * @param scriptNames the names to create credits for
     * @param storyId the story id
     * @param roleId the credit_type id
     */
    private fun makeCredits(
        scriptNames: List<String>,
        storyId: Int,
        roleId: Int
    ) {
        for (name in scriptNames) {
            if (name != "") {
                makeCredit(name.prepareName(), storyId, roleId)
            }
        }
    }

    /**
     * Make credit - checks for an existing story credit for [extractedName],
     * [storyId], and [roleId]. If one does not exist, it creates one with [makeStoryCredit].
     *
     * @param extractedName the name to check
     * @param storyId the story id
     * @param roleId the credit_type id
     */
    private fun makeCredit(extractedName: String, storyId: Int, roleId: Int) {
        getGcnd(extractedName, conn)?.let { gcndId ->
            getStoryCredit(gcndId, storyId, roleId) ?: makeStoryCredit(
                gcndId,
                roleId,
                storyId
            )
        }
    }

    /**
     * Get gcnd - looks for a gcd_creator_name_detail with the given
     * [extractedName]
     *
     * @param extractedName the name to check
     * @param conn the connection to the database
     * @return the gcd_creator_name_detail id if found, null otherwise
     */
    private fun getGcnd(extractedName: String, conn: Connection?): Int? {
        var gcndId: Int? = null
        val getCondStmt: PreparedStatement?
        var gcndResultSet: ResultSet? = null

        try {
            val getGcndSql = """
               SELECT * 
               FROM gcd_creator_name_detail gcnd
               WHERE gcnd.name = ?                
            """

            getCondStmt = conn?.prepareStatement(getGcndSql)
            getCondStmt?.setString(1, extractedName)

            gcndResultSet = getCondStmt?.executeQuery()

            if (getCondStmt?.execute() == true) {
                gcndResultSet = getCondStmt.resultSet
            }

            if (gcndResultSet?.next() == true) {
                gcndId = gcndResultSet.getInt("id")
            } else {
                // TODO: save extracted_name storyId, Role to file
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            closeResultSet(gcndResultSet)
        }

        return gcndId
    }

    /**
     * Get story credit - looks for a story credit with the given [gcndId],
     * [storyId], and [roleId] in the gcd_story_credit and m_story_credit
     * tables
     *
     * @param gcndId the gcd_creator_name_detail id
     * @param storyId the story id
     * @param roleId the credit_type id
     * @return the story credit id if found, null otherwise
     */
    private fun getStoryCredit(gcndId: Int, storyId: Int, roleId: Int): Int? {
        var storyCreditId: Int? = null
        val statement: PreparedStatement?
        var resultSet: ResultSet? = null

        try {
            /** Get story credit id sql */
            val getStoryCreditIdSql = """
                    SELECT gsc.id
                    FROM ${database}.gcd_story_credit gsc
                    WHERE gsc.creator_id = ?
                    AND gsc.story_id = ?
                    AND gsc.credit_type_id = ?
            """

            statement = conn.prepareStatement(getStoryCreditIdSql)
            statement?.setInt(1, gcndId)
            statement?.setInt(2, storyId)
            statement?.setInt(3, roleId)

            resultSet = statement?.executeQuery()

            if (statement?.execute() == true) {
                resultSet = statement.resultSet
            }

            if (resultSet?.next() == true) {
                storyCreditId = resultSet.getInt("id")
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            closeResultSet(resultSet)
        }

        if (storyCreditId == null) {
            val statement2: PreparedStatement?
            try {
                val getMStoryCreditIdSql = """
                    SELECT gsc.id
                    FROM ${database}.m_story_credit gsc
                    WHERE gsc.creator_id = ?
                    AND gsc.story_id = ?
                    AND gsc.credit_type_id = ?
            """

                statement2 = conn.prepareStatement(getMStoryCreditIdSql)
                statement2?.setInt(1, gcndId)
                statement2?.setInt(2, storyId)
                statement2?.setInt(3, roleId)

                resultSet = statement2?.executeQuery()

                if (statement2?.execute() == true) {
                    resultSet = statement2.resultSet
                }

                if (resultSet?.next() == true) {
                    storyCreditId = resultSet.getInt("id")
                }
            } catch (sqlEx: SQLException) {
                sqlEx.printStackTrace()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            } finally {
                closeResultSet(resultSet)
            }
        }

        return storyCreditId
    }

    /**
     * Make story credit - prepares a statement with [gcndId], [roleId], and
     * [storyId] to insert a story credit into m_story_credit in [database]
     *
     * @param gcndId gcd_creator_name_detail.id
     * @param roleId credit_type.id
     * @param storyId gcd_story.id
     */
    private fun makeStoryCredit(gcndId: Int, roleId: Int, storyId: Int) {
        val insertStoryCreditStmt: PreparedStatement?

        /** Insert story credit sql */
        val insertStoryCreditSql = """
                            INSERT INTO $database.m_story_credit(created, modified, deleted, is_credited, is_signed, uncertain, signed_as, credited_as, credit_name, creator_id, credit_type_id, story_id, signature_id)
                             VALUE (CURTIME(), CURTIME(), 0, 0, 0, 0, '', '', '', ?, ?, ?, NULL)
                        """

        insertStoryCreditStmt = conn.prepareStatement(insertStoryCreditSql)
        insertStoryCreditStmt?.setInt(1, gcndId)
        insertStoryCreditStmt?.setInt(2, roleId)
        insertStoryCreditStmt?.setInt(3, storyId)

        insertStoryCreditStmt?.executeUpdate()
    }
}