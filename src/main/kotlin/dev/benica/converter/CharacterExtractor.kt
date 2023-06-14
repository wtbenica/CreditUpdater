package dev.benica.converter

import dev.benica.db.Repository
import dev.benica.models.Appearance
import kotlinx.coroutines.*
import mu.KLogger
import mu.KotlinLogging
import java.sql.*

val Any.logger: KLogger
    get() = KotlinLogging.logger(this::class.java.name)

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
    override val fromValue: String = "Story"

    private val repository = Repository(database, conn)

    @Volatile
    private var _appearanceBuffer: AppearanceBuffer? = null

    private val appearanceBuffer: AppearanceBuffer
        get() = _appearanceBuffer ?: synchronized(this) {
            AppearanceBuffer(database, conn)
        }.also {
            _appearanceBuffer = it
        }

    /**
     * Extracts characters from a result set and adds them to the
     * [appearanceBuffer] for database insertion.
     *
     * @param resultSet The result set of stories from which to extract
     *     characters.
     * @param destDatabase The destination database.
     * @return the story id of the story from which characters were extracted.
     */
    override suspend fun extract(
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

            appearance?.let {
                appearanceBuffer.addToBuffer(it)
            }
        }

        return storyId
    }

    override fun finish() {
        CoroutineScope(Dispatchers.IO).launch {
            appearanceBuffer.save()
        }
        logger.info { "FINISHING" }
    }
}
