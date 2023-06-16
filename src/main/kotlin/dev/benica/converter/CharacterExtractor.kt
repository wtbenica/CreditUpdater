package dev.benica.converter

import dev.benica.db.CharacterRepository
import dev.benica.models.Appearance
import kotlinx.coroutines.*
import mu.KLogger
import mu.KotlinLogging
import java.sql.*

private val logger: KLogger
    get() = KotlinLogging.logger { }

/**
 * Character extractor - extracts character records and character
 * appearance records from the 'character' text field in gcd_story
 * and creates linked entries for them in the 'm_character' and
 * 'm_character_appearance' tables.
 *
 * @param database the database to which to write the extracted character
 *     and appearance data.
 * @param conn
 */
class CharacterExtractor(database: String, conn: Connection) : Extractor(database, conn) {
    override val extractedItem: String = "Character"
    override val fromValue: String = "StoryId"

    private val repository = CharacterRepository(database, conn)

    /**
     * Extract and insert - extracts characters from the 'characters' field
     * in the [resultSet] and inserts them into the database.
     *
     * @param resultSet The result set of stories from which to extract
     *     characters.
     * @param destDatabase The destination database.
     * @return the story id of the story from which characters were extracted.
     */
    override suspend fun extractAndInsert(
        resultSet: ResultSet,
        destDatabase: String?
    ): Int {
        val storyId = resultSet.getInt("id")
        val characters = resultSet.getString("characters")
        val publisherId: Int? = repository.getPublisherId(storyId)

        val characterList: List<CharacterAppearance> = CharacterParser.parseCharacters(characters)

        for (character in characterList) {
            val characterId = publisherId?.let {
                when (character) {
                    is Individual -> repository.upsertCharacter(character.name, character.alterEgo, it)
                    is Team -> repository.upsertCharacter(character.name, null, it)
                }
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
    }
}
