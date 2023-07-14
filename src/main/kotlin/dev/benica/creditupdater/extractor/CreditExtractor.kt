package dev.benica.creditupdater.extractor

import dev.benica.creditupdater.db.CreditRepository
import dev.benica.creditupdater.di.CreditRepositoryComponent
import dev.benica.creditupdater.di.DaggerCreditRepositoryComponent
import dev.benica.creditupdater.di.RepoSource
import mu.KLogger
import mu.KotlinLogging
import java.sql.ResultSet
import java.sql.SQLException
import javax.inject.Inject
import kotlin.jvm.Throws

/**
 * Credit extractor - creates linked credits from named credits
 *
 * @param schema the database
 * @constructor Create empty Credit extractor
 * @note The caller is responsible for closing the extractor.
 */
class CreditExtractor(
    schema: String,
    repositoryComponent: CreditRepositoryComponent = DaggerCreditRepositoryComponent.create()
) : Extractor(schema) {
    override val extractTable: String = "gcd_story"
    override val extractedItem = "Credit"
    override val fromValue = "StoryId"

    @Inject
    internal lateinit var repoSource: RepoSource<CreditRepository>

    private val repository: CreditRepository

    init {
        repositoryComponent.inject(this)
        repository = repoSource.getRepo(schema)
    }

    private val logger: KLogger
        get() = KotlinLogging.logger(this::class.java.simpleName)

    /**
     * Extracts credits from the 'script', 'pencils', 'inks', 'colors',
     * 'letters', and 'editing' text fields in a gcd_story and
     * creates linked entries for them in the 'm_story_credit' table.
     *
     * @param resultSet expecting a result set containing a story
     * @return the story id
     * @throws SQLException
     */
    @Throws(SQLException::class)
    override fun extractAndInsert(resultSet: ResultSet): Int {
        try {
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
        } catch (sqlEx: SQLException) {
            logger.error("Error extracting credits from story: ${sqlEx.message}")
            throw sqlEx
        }
    }

    /**
     * Create credits for names - calls
     * [CreditRepository.insertStoryCreditIfNotExists] for each name
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
            repository.insertStoryCreditIfNotExists(
                extractedName = name.prepareName(),
                storyId = storyId,
                roleId = roleId
            )
        }
    }

    /**
     * Prepare name - removes any parentheticals, brackets, and question marks
     * from the name.
     *
     * @return the name with parentheticals, brackets, and question marks
     *     removed.
     */
    private fun String.prepareName(): String = this
        .replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
        .replace(Regex("\\s*\\[[^]]*]\\s*"), "")
        .replace(Regex("\\s*\\?\\s*"), "")
        .replace(Regex("^\\s*"), "")
        .cleanup()
}