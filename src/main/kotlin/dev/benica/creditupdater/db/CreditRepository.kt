package dev.benica.creditupdater.db

import dev.benica.creditupdater.di.*
import java.sql.SQLException
import javax.inject.Inject

/**
 * Credit Repository - handles all database interactions for the 'm_credit'
 * table.
 *
 * @param targetSchema the database to which to write the extracted credit
 *     data.
 * @param databaseComponent the database component to use
 * @note The caller is responsible for closing the repository.
 */
class CreditRepository(
    private val targetSchema: String,
    databaseComponent: DatabaseComponent? = DaggerDatabaseComponent.create(),
    private val queryExecutor: QueryExecutor = QueryExecutor(targetSchema)
) : Repository {
    // Dependencies
    @Inject
    internal lateinit var connectionSource: ConnectionSource

    init {
        databaseComponent?.inject(this)
    }

    // Public Methods
    /**
     * Create or Update Story Credit - checks for an existing story credit for
     * [extractedName], [storyId], and [roleId]. If one does not exist, it
     * creates one with [insertStoryCredit].
     *
     * @param extractedName the name to check
     * @param storyId the story id
     * @param roleId the credit_type id
     */
    internal fun insertStoryCreditIfNotExists(extractedName: String, storyId: Int, roleId: Int) {
        lookupGcndId(extractedName)?.let { gcndId ->
            lookupStoryCreditId(gcndId, storyId, roleId) ?: insertStoryCredit(
                gcndId,
                roleId,
                storyId
            )
        }
    }

    // Private Methods
    /**
     * Lookup Gcnd Id - looks for a gcd_creator_name_detail with the given
     * [extractedName]
     *
     * @param extractedName the name to check
     * @return the gcd_creator_name_detail id if found, null otherwise
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    internal fun lookupGcndId(extractedName: String): Int? {
        val conn = connectionSource.getConnection(targetSchema).connection

        var gcndId: Int? = null

        try {
            val getGcndSql = """
               SELECT * 
               FROM gcd_creator_name_detail gcnd
               WHERE gcnd.name = ?                
            """

            queryExecutor.executePreparedStatement(getGcndSql, conn = conn) { statement ->
                statement.setString(1, extractedName)

                statement.executeQuery().use { resultSet ->
                    if (resultSet?.next() == true) {
                        gcndId = resultSet.getInt("id")
                    }
                }
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
            throw sqlEx
        } finally {
            conn.close()
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
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    internal fun lookupStoryCreditId(gcndId: Int, storyId: Int, roleId: Int): Int? {
        val conn = connectionSource.getConnection(targetSchema).connection

        var storyCreditId: Int? = null

        try {
            /** Get story credit id sql */
            val getStoryCreditIdSql = """
                    SELECT gsc.id
                    FROM ${targetSchema}.gcd_story_credit gsc
                    WHERE gsc.creator_id = ?
                    AND gsc.story_id = ?
                    AND gsc.credit_type_id = ?
            """

            queryExecutor.executePreparedStatement(getStoryCreditIdSql, conn = conn) { statement ->
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
            conn.close()
            throw sqlEx
        }

        if (storyCreditId == null) {
            try {
                val getMStoryCreditIdSql = """
                    SELECT gsc.id
                    FROM ${targetSchema}.m_story_credit gsc
                    WHERE gsc.creator_id = ?
                    AND gsc.story_id = ?
                    AND gsc.credit_type_id = ?
            """

                queryExecutor.executePreparedStatement(getMStoryCreditIdSql, conn = conn) { statement ->
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
                throw sqlEx
            } finally {
                conn.close()
            }
        }

        return storyCreditId
    }

    /**
     * Insert story credit - prepares a statement with [gcndId], [roleId], and
     * [storyId] to insert a story credit into m_story_credit in [targetSchema]
     * and executes it.
     *
     * @param gcndId gcd_creator_name_detail.id
     * @param roleId credit_type.id
     * @param storyId gcd_story.id
     */
    private fun insertStoryCredit(gcndId: Int, roleId: Int, storyId: Int) {
        val insertStoryCreditSql =
            """INSERT IGNORE INTO $targetSchema.m_story_credit(creator_id, credit_type_id, story_id)
                VALUE (?, ?, ?)"""

        connectionSource.getConnection(targetSchema).connection.use { conn ->
            queryExecutor.executePreparedStatement(
                sql = insertStoryCreditSql,
                conn = conn
            ) { statement ->
                statement.setInt(1, gcndId)
                statement.setInt(2, roleId)
                statement.setInt(3, storyId)

                statement.executeUpdate()
            }
        }
    }
}