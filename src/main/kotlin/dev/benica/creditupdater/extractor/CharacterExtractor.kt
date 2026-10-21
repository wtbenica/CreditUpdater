package dev.benica.creditupdater.extractor

import dev.benica.creditupdater.Credentials.Companion.PRIMARY_DATABASE
import dev.benica.creditupdater.db.CharacterIdCache
import dev.benica.creditupdater.db.CharacterRepository
import dev.benica.creditupdater.di.CharacterRepositoryComponent
import dev.benica.creditupdater.di.DaggerCharacterRepositoryComponent
import dev.benica.creditupdater.di.RepoSource
import dev.benica.creditupdater.extractor.utils.CharacterParser
import dev.benica.creditupdater.models.Appearance
import dev.benica.creditupdater.models.Character
import dev.benica.creditupdater.models.Individual
import dev.benica.creditupdater.models.Team
import mu.KLogger
import mu.KotlinLogging
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import javax.inject.Inject

/**
 * Character extractor - extracts character records and character
 * appearance records from the 'character' text field in gcd_story
 * and creates linked entries for them in the 'm_character' and
 * 'm_character_appearance' tables.
 *
 * @param schema the database to which to write the extracted character and
 *     appearance data.
 * @param reuseExistingIds a character already in the primary database keeps the id it has there
 *     (a migration); otherwise only this schema's characters are looked at
 * @note The caller is responsible for closing the extractor.
 */
class CharacterExtractor(
    schema: String,
    private val reuseExistingIds: Boolean = false,
    repositoryComponent: CharacterRepositoryComponent = DaggerCharacterRepositoryComponent.create(),
) : Extractor(schema) {
    override val extractTable: String = "gcd_story"
    override val extractedItem: String = "Character"
    override val fromValue: String = "StoryId"

    @Inject
    internal lateinit var repoSource: RepoSource<CharacterRepository>

    private val repository: CharacterRepository

    init {
        repositoryComponent.inject(this)
        repository = repoSource.getRepo(schema)
    }

    /** Every character's id, in memory; made when the first chunk is extracted. */
    private var characterIds: CharacterIdCache? = null

    private val logger: KLogger
        get() = KotlinLogging.logger(this::class.java.simpleName)

    /**
     * Extracts character records and character appearance records from the
     * 'character' text field in a gcd_story and creates linked entries
     * for them in the 'm_character' and 'm_character_appearance' tables.
     *
     * @param resultSet expecting a result set containing a story
     * @return the story id
     * @throws SQLException
     */
    @Throws(SQLException::class)
    override fun extractAndInsert(resultSet: ResultSet, conn: Connection): Int {
        try {
            val storyId = resultSet.getInt("id")
            val characters = resultSet.getString("characters")
            val publisherId = resultSet.getInt("publisher_id")

            val characterList: List<Character> = CharacterParser.parseCharacters(characters)
            val appearanceSet = mutableSetOf<Appearance>()

            for (character in characterList) {
                val characterId = when (character) {
                    is Individual -> repository.upsertCharacter(character.name, character.alterEgo, publisherId)
                    is Team -> repository.upsertCharacter(character.name, null, publisherId)
                }

                val appearance: Appearance? = characterId?.let {
                    when (character) {
                        is Individual -> Appearance(
                            storyId = storyId,
                            characterId = it,
                            details = character.details,
                            notes = null,
                            membership = null
                        )

                        is Team -> Appearance(
                            storyId = storyId,
                            characterId = it,
                            details = character.details,
                            notes = null,
                            membership = character.members
                        )
                    }
                }

                appearance?.let { appearanceSet.add(it) }
            }

            repository.insertCharacterAppearances(appearanceSet, conn)

            return storyId
        } catch (sqlEx: SQLException) {
            logger.error("Error in extract and insert characters", sqlEx)
            throw sqlEx
        }
    }

    /**
     * Extracts a chunk of stories at once: the characters' ids come from memory, the new
     * characters and all the appearances are inserted in bulk. The characters and their ids are
     * the same as one story at a time would give, in the same order.
     */
    @Throws(SQLException::class)
    override fun extractAndInsertBatch(resultSet: ResultSet, conn: Connection): Pair<Int, Int> {
        val ids = characterIds ?: CharacterIdCache(
            targetSchema = schema,
            primarySchema = PRIMARY_DATABASE.takeIf { reuseExistingIds && schema != PRIMARY_DATABASE },
        ).also {
            it.load(conn)
            characterIds = it
        }

        val appearances = LinkedHashSet<Appearance>()
        var lastId = 0
        var count = 0
        do {
            val storyId = resultSet.getInt("id")
            val publisherId = resultSet.getInt("publisher_id")

            CharacterParser.parseCharacters(resultSet.getString("characters")).forEach { character ->
                appearances += when (character) {
                    is Individual -> Appearance(
                        storyId = storyId,
                        characterId = ids.idFor(character.name, character.alterEgo, publisherId),
                        details = character.details,
                        notes = null,
                        membership = null
                    )

                    is Team -> Appearance(
                        storyId = storyId,
                        characterId = ids.idFor(character.name, null, publisherId),
                        details = character.details,
                        notes = null,
                        membership = character.members
                    )
                }
            }
            lastId = storyId
            count++
        } while (resultSet.next())

        // the characters first: the appearances refer to them
        ids.flush(conn)
        repository.insertCharacterAppearances(
            appearances.mapTo(LinkedHashSet()) { it.copy(characterId = ids.resolve(it.characterId)) },
            conn
        )
        return lastId to count
    }
}
