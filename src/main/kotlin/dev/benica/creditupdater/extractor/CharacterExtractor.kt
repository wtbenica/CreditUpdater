package dev.benica.creditupdater.extractor

import dev.benica.creditupdater.db.CharacterRepository
import dev.benica.creditupdater.di.CharacterRepositoryComponent
import dev.benica.creditupdater.di.DaggerCharacterRepositoryComponent
import dev.benica.creditupdater.di.RepoSource
import dev.benica.creditupdater.extractor.utils.CharacterParser
import dev.benica.creditupdater.models.Appearance
import dev.benica.creditupdater.models.CharacterAppearance
import dev.benica.creditupdater.models.Individual
import dev.benica.creditupdater.models.Team
import mu.KLogger
import mu.KotlinLogging
import java.sql.ResultSet
import java.sql.SQLException
import javax.inject.Inject

/**
 * Character extractor - extracts character records and character
 * appearance records from the 'character' text field in gcd_story
 * and creates linked entries for them in the 'm_character' and
 * 'm_character_appearance' tables.
 *
 * @param database the database to which to write the extracted character
 *     and appearance data.
 */
class CharacterExtractor(
    database: String,
    repositoryComponent: CharacterRepositoryComponent = DaggerCharacterRepositoryComponent.create()
) : Extractor(database) {
    override val extractTable: String = "gcd_story"
    override val extractedItem: String = "Character"
    override val fromValue: String = "StoryId"

    @Inject
    internal lateinit var repoSource: RepoSource<CharacterRepository>

    private val repository: CharacterRepository

    init {
        repositoryComponent.inject(this)
        repository = repoSource.getRepo(database)
    }

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
    override fun extractAndInsert(
        resultSet: ResultSet,
    ): Int {
        try {
            val storyId = resultSet.getInt("id")
            val characters = resultSet.getString("characters")
            val publisherId = resultSet.getInt("publisher_id")

            val characterList: List<CharacterAppearance> = CharacterParser.parseCharacters(characters)

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
                            appearanceInfo = character.appearanceInfo,
                            notes = null,
                            membership = null
                        )

                        is Team -> Appearance(
                            storyId = storyId,
                            characterId = it,
                            appearanceInfo = character.appearanceInfo,
                            notes = null,
                            membership = character.members
                        )
                    }
                }

                appearance?.let { repository.insertCharacterAppearance(it) }
            }

            return storyId
        } catch (sqlEx: SQLException) {
            logger.error("Error in extract and insert characters", sqlEx)
            throw sqlEx
        }
    }
}
