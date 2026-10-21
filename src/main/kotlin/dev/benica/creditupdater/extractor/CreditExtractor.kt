package dev.benica.creditupdater.extractor

import dev.benica.creditupdater.db.CreditLookup
import dev.benica.creditupdater.db.CreditRepository
import dev.benica.creditupdater.di.CreditRepositoryComponent
import dev.benica.creditupdater.di.DaggerCreditRepositoryComponent
import dev.benica.creditupdater.di.RepoSource
import mu.KLogger
import mu.KotlinLogging
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import javax.inject.Inject

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
    override fun extractAndInsert(resultSet: ResultSet, conn: Connection): Int {
        try {
            val storyId = resultSet.getInt("id")

            val scriptNames = resultSet.getString("script").split(';').filter { it.isNotBlank() }
            val pencilsNames = resultSet.getString("pencils").split(';').filter { it.isNotBlank() }
            val inksNames = resultSet.getString("inks").split(';').filter { it.isNotBlank() }
            val colorsNames = resultSet.getString("colors").split(';').filter { it.isNotBlank() }
            val lettersNames = resultSet.getString("letters").split(';').filter { it.isNotBlank() }
            val editingNames = resultSet.getString("editing").split(';').filter { it.isNotBlank() }

            createCreditsForNames(scriptNames, storyId, 1, conn)
            createCreditsForNames(pencilsNames, storyId, 2, conn)
            createCreditsForNames(inksNames, storyId, 3, conn)
            createCreditsForNames(colorsNames, storyId, 4, conn)
            createCreditsForNames(lettersNames, storyId, 5, conn)
            createCreditsForNames(editingNames, storyId, 6, conn)
            return storyId
        } catch (sqlEx: SQLException) {
            logger.error("Error extracting credits from story: ${sqlEx.message}")
            throw sqlEx
        }
    }

    /** The creators' names and the credits already there, in memory; made when the first chunk is extracted. */
    private var lookup: CreditLookup? = null

    /**
     * Extracts a chunk of stories at once: names are looked up in memory, the credits already in
     * the database are read for the whole chunk in two queries, and the new ones are inserted in
     * bulk. The credits are the same as one story at a time would give.
     */
    @Throws(SQLException::class)
    override fun extractAndInsertBatch(resultSet: ResultSet, conn: Connection): Pair<Int, Int> {
        val names = lookup ?: CreditLookup(schema).also {
            it.load(conn)
            lookup = it
        }

        class Wanted(val storyId: Int, val creditTypeId: Int, val name: String)

        val wanted = ArrayList<Wanted>()
        var firstId = 0
        var lastId = 0
        var count = 0
        do {
            val storyId = resultSet.getInt("id")
            if (count == 0) firstId = storyId
            listOf("script", "pencils", "inks", "colors", "letters", "editing").forEachIndexed { index, field ->
                resultSet.getString(field).split(';').filter { it.isNotBlank() }.forEach {
                    wanted += Wanted(storyId, index + 1, it.prepareName())
                }
            }
            lastId = storyId
            count++
        } while (resultSet.next())

        val existing = names.creditsBetween(firstId, lastId, conn)
        val credits = LinkedHashSet<CreditLookup.Credit>()
        wanted.forEach {
            val creatorId = names.creatorId(it.name) ?: return@forEach
            val credit = CreditLookup.Credit(creatorId, it.creditTypeId, it.storyId)
            if (credit !in existing) credits += credit
        }

        repository.insertStoryCredits(credits, conn)
        return lastId to count
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
        roleId: Int,
        conn: Connection
    ) {
        scriptNames.forEach { name ->
            repository.insertStoryCreditIfNotExists(
                extractedName = name.prepareName(),
                storyId = storyId,
                roleId = roleId,
                conn = conn
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