package dev.benica.db

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

class CreditRepository(private val database: String, private val conn: Connection) {
    /**
     * Create or Update Story Credit - checks for an existing story credit for
     * [extractedName], [storyId], and [roleId]. If one does not exist, it
     * creates one with [insertStoryCredit].
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
     * Lookup Story Credit Id - looks for a story credit with the given
     * [gcndId], [storyId], and [roleId] in the gcd_story_credit and
     * m_story_credit tables
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
     * [storyId] to insert a story credit into m_story_credit in [database] and
     * executes it.
     *
     * @param gcndId gcd_creator_name_detail.id
     * @param roleId credit_type.id
     * @param storyId gcd_story.id
     */
    private fun insertStoryCredit(gcndId: Int, roleId: Int, storyId: Int) {
        val insertStoryCreditStmt: PreparedStatement?

        /** Insert story credit sql */
        val insertStoryCreditSql =
            """INSERT IGNORE INTO $database.m_story_credit(created, modified, deleted, is_credited, is_signed, 
                uncertain, signed_as, credited_as, credit_name, creator_id, credit_type_id, story_id, signature_id)
                VALUE (CURTIME(), CURTIME(), 0, 0, 0, 0, '', '', '', ?, ?, ?, NULL)"""

        insertStoryCreditStmt = conn.prepareStatement(insertStoryCreditSql)
        insertStoryCreditStmt?.setInt(1, gcndId)
        insertStoryCreditStmt?.setInt(2, roleId)
        insertStoryCreditStmt?.setInt(3, storyId)

        insertStoryCreditStmt?.executeUpdate()
    }
}