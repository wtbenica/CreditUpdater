package dev.benica.creditupdater.db

import dev.benica.creditupdater.di.DaggerQueryExecutorComponent
import dev.benica.creditupdater.di.QueryExecutorComponent
import dev.benica.creditupdater.di.QueryExecutorSource
import java.sql.SQLException
import javax.inject.Inject

class CreditRepository(
    private val targetSchema: String,
    queryExecutorProvider: QueryExecutorComponent = DaggerQueryExecutorComponent.create(),
) : Repository {
    // Dependencies
    @Inject
    internal lateinit var queryExecutorSource: QueryExecutorSource

    private val queryExecutor: QueryExecutor

    init {
        queryExecutorProvider.inject(this)
        queryExecutor = queryExecutorSource.getQueryExecutor(targetSchema)
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
    internal fun createOrUpdateStoryCredit(extractedName: String, storyId: Int, roleId: Int) {
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
     */
    private fun lookupGcndId(extractedName: String): Int? {
        var gcndId: Int? = null

        try {
            val getGcndSql = """
               SELECT * 
               FROM gcd_creator_name_detail gcnd
               WHERE gcnd.name = ?                
            """

            queryExecutor.executePreparedStatement(getGcndSql) { statement ->
                statement.setString(1, extractedName)

                statement.executeQuery().use { resultSet ->
                    if (resultSet?.next() == true) {
                        gcndId = resultSet.getInt("id")
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
    private fun lookupStoryCreditId(gcndId: Int, storyId: Int, roleId: Int): Int? {
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

            queryExecutor.executePreparedStatement(getStoryCreditIdSql) { statement ->
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
                    FROM ${targetSchema}.m_story_credit gsc
                    WHERE gsc.creator_id = ?
                    AND gsc.story_id = ?
                    AND gsc.credit_type_id = ?
            """

                queryExecutor.executePreparedStatement(getMStoryCreditIdSql) { statement ->
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
            """INSERT IGNORE INTO $targetSchema.m_story_credit(created, modified, deleted, is_credited, is_signed, 
                uncertain, signed_as, credited_as, credit_name, creator_id, credit_type_id, story_id, signature_id)
                VALUE (CURTIME(), CURTIME(), 0, 0, 0, 0, '', '', '', ?, ?, ?, NULL)"""

        queryExecutor.executePreparedStatement(insertStoryCreditSql) { statement ->
            statement.setInt(1, gcndId)
            statement.setInt(2, roleId)
            statement.setInt(3, storyId)

            statement.executeUpdate()
        }
    }
}