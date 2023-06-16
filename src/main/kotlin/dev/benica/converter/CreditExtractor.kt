package dev.benica.converter

import dev.benica.db.CreditRepository
import java.sql.Connection
import java.sql.ResultSet

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
    private val repository = CreditRepository(database, conn)

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

        val scriptNames = resultSet.getString("script").split(';').filter { it.isNotBlank() }
        val pencilsNames = resultSet.getString("pencils").split(';').filter { it.isNotBlank() }
        val inksNames = resultSet.getString("inks").split(';').filter { it.isNotBlank() }
        val colorsNames = resultSet.getString("colors").split(';').filter { it.isNotBlank() }
        val lettersNames = resultSet.getString("letters").split(';').filter { it.isNotBlank() }
        val editingNames = resultSet.getString("editing").split(';').filter { it.isNotBlank() }

        createCreditsForNames(scriptNames, storyId, 1)
        createCreditsForNames(pencilsNames, storyId, 2)
        createCreditsForNames(inksNames, storyId, 3)
        createCreditsForNames(colorsNames, storyId, 4)
        createCreditsForNames(lettersNames, storyId, 5)
        createCreditsForNames(editingNames, storyId, 6)
        return storyId
    }

    /**
     * Create credits for names - calls
     * [CreditRepository.createOrUpdateStoryCredit] for each name
     * in [scriptNames] with the given [storyId] and [roleId].
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
        scriptNames.forEach { name ->
            repository.createOrUpdateStoryCredit(
                extractedName = name.prepareName(),
                storyId = storyId,
                roleId = roleId
            )
        }
    }
}