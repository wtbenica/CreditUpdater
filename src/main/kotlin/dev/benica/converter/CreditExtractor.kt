package dev.benica.converter

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
    override suspend fun extractAndInsert(
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

        createCreditsForNames(scriptNames, storyId, 1)
        createCreditsForNames(pencilsNames, storyId, 2)
        createCreditsForNames(inksNames, storyId, 3)
        createCreditsForNames(colorsNames, storyId, 4)
        createCreditsForNames(lettersNames, storyId, 5)
        createCreditsForNames(editingNames, storyId, 6)
        return storyId
    }

    override fun finish() {
        // do nothing
    }

    /**
     * Make credits - calls [createOrUpdateStoryCredit] for each name in [scriptNames]
     *
     * @param scriptNames the names to create credits for
     * @param storyId the story id
     * @param roleId the credit_type id
     */
    private fun createCreditsForNames(
        scriptNames: List<String>,
        storyId: Int,
        roleId: Int
    ) {
        for (name in scriptNames) {
            if (name != "") {
                createOrUpdateStoryCredit(name.prepareName(), storyId, roleId)
            }
        }
    }

    /**
     * Create or Update Story Credit - checks for an existing story credit for [extractedName],
     * [storyId], and [roleId]. If one does not exist, it creates one with
     * [insertStoryCredit].
     *
     * @param extractedName the name to check
     * @param storyId the story id
     * @param roleId the credit_type id
     */
    internal fun createOrUpdateStoryCredit(extractedName: String, storyId: Int, roleId: Int) {
        lookupGcndId(extractedName, conn)?.let { gcndId ->
            lookupStoryCreditId(gcndId, storyId, roleId) ?: insertStoryCredit(
                gcndId,
                roleId,
                storyId
            )
        }
    }

    /**
     * Lookup Gcnd Id - looks for a gcd_creator_name_detail with the given
     * [extractedName]
     *
     * @param extractedName the name to check
     * @param conn the connection to the database
     * @return the gcd_creator_name_detail id if found, null otherwise
     */
    internal fun lookupGcndId(extractedName: String, conn: Connection?): Int? {
        var gcndId: Int? = null

        try {
            val getGcndSql = """
               SELECT * 
               FROM gcd_creator_name_detail gcnd
               WHERE gcnd.name = ?                
            """

            conn?.prepareStatement(getGcndSql).use { getCondStmt ->
                getCondStmt?.setString(1, extractedName)

                getCondStmt?.executeQuery()?.use { gcndResultSet ->
                    if (gcndResultSet.next()) {
                        gcndId = gcndResultSet.getInt("id")
                    }
                }
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        return gcndId
    }

    /**
     * Lookup Story Credit Id - looks for a story credit with the given [gcndId],
     * [storyId], and [roleId] in the gcd_story_credit and m_story_credit
     * tables
     *
     * @param gcndId the gcd_creator_name_detail id
     * @param storyId the story id
     * @param roleId the credit_type id
     * @return the story credit id if found, null otherwise
     */
    internal fun lookupStoryCreditId(gcndId: Int, storyId: Int, roleId: Int): Int? {
        var storyCreditId: Int? = null

        try {
            /** Get story credit id sql */
            val getStoryCreditIdSql = """
                    SELECT gsc.id
                    FROM ${database}.gcd_story_credit gsc
                    WHERE gsc.creator_id = ?
                    AND gsc.story_id = ?
                    AND gsc.credit_type_id = ?
            """

            conn.prepareStatement(getStoryCreditIdSql)?.use { statement ->
                statement.setInt(1, gcndId)
                statement.setInt(2, storyId)
                statement.setInt(3, roleId)

                statement.executeQuery().use { resultSet ->
                    if (resultSet?.next() == true) {
                        storyCreditId = resultSet.getInt("id")
                    }
                }
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        if (storyCreditId == null) {
            try {
                val getMStoryCreditIdSql = """
                    SELECT gsc.id
                    FROM ${database}.m_story_credit gsc
                    WHERE gsc.creator_id = ?
                    AND gsc.story_id = ?
                    AND gsc.credit_type_id = ?
            """

                conn.prepareStatement(getMStoryCreditIdSql)?.use { statement2 ->
                    statement2.setInt(1, gcndId)
                    statement2.setInt(2, storyId)
                    statement2.setInt(3, roleId)

                    statement2.executeQuery().use { resultSet ->
                        if (resultSet?.next() == true) {
                            storyCreditId = resultSet.getInt("id")
                        }
                    }
                }
            } catch (sqlEx: SQLException) {
                sqlEx.printStackTrace()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        return storyCreditId
    }

    /**
     * Insert story credit - prepares a statement with [gcndId], [roleId], and
     * [storyId] to insert a story credit into m_story_credit in [database]
     * and executes it.
     *
     * @param gcndId gcd_creator_name_detail.id
     * @param roleId credit_type.id
     * @param storyId gcd_story.id
     */
    private fun insertStoryCredit(gcndId: Int, roleId: Int, storyId: Int) {
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