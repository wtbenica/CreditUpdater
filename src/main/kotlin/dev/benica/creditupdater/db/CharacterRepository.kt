package dev.benica.creditupdater.db

import dev.benica.creditupdater.Credentials.Companion.PRIMARY_DATABASE
import dev.benica.creditupdater.models.Appearance
import mu.KLogger
import mu.KotlinLogging
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement

class CharacterRepository(private val database: String, private val conn: Connection) {
    private val logger: KLogger
        get() = KotlinLogging.logger (this::class.java.simpleName)

    /**
     * Gets the ID of a character with the given name, alter ego, and publisher
     * ID. If the character does not exist in the database, it is inserted and
     * the new ID is returned.
     *
     * @param name The name of the character.
     * @param alterEgo The alter ego of the character, or null if the character
     *     does not have an alter ego.
     * @param publisherId The ID of the publisher of the character.
     * @return The ID of the character, or null if the character could not be
     *     found or inserted.
     */
    internal fun upsertCharacter(
        name: String,
        alterEgo: String?,
        publisherId: Int
    ): Int? = lookupCharacter(
        name,
        alterEgo?.truncate(255),
        publisherId
    ) ?: insertCharacter(
        name,
        alterEgo?.truncate(255),
        publisherId
    )

    // TODO: Fix this. It's a hack to avoid having to figure out how to deal with teams and teams within teams
    // See "All-Star Squadron #57"
    private fun String.truncate(maxLength: Int): String {
        return if (this.length > maxLength) {
            this.substring(0, maxLength)
        } else {
            this
        }
    }

    /**
     * Insert character appearances - inserts [appearances] into [database]
     * using [conn]
     */
    internal fun insertCharacterAppearances(appearances: Set<Appearance>) {
        val sql = """
               INSERT IGNORE INTO $database.m_character_appearance(id, details, character_id, story_id, notes, membership)
               VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()

        conn.prepareStatement(sql).use { statement ->
            appearances.forEach { appearance ->
                statement?.setInt(1, appearance.id)
                statement?.setString(2, appearance.appearanceInfo)
                statement?.setInt(3, appearance.characterId)
                statement?.setInt(4, appearance.storyId)
                statement?.setString(5, appearance.notes)
                statement?.setString(6, appearance.membership)
                statement?.addBatch()
            }

            statement?.executeBatch()
        }
    }

    /**
     * Insert character appearance - inserts [appearance] into [database] using
     * [conn]
     */
    fun insertCharacterAppearance(appearance: Appearance) =
        insertCharacterAppearances(setOf(appearance))

    /**
     * Insert character - inserts [name], [alterEgo], and [publisherId] into
     * [database] table m_character
     *
     * @param name character name
     * @param alterEgo character alter ego
     * @param publisherId publisher id
     * @return inserted character's id
     */
    internal fun insertCharacter(
        name: String,
        alterEgo: String?,
        publisherId: Int
    ): Int? {
        val statement: PreparedStatement?
        var characterId: Int? = null
        val truncatedName = name.substring(0, Integer.min(255, name.length))
        val sql = """
           INSERT INTO ${database}.m_character(name, alter_ego, publisher_id)
           VALUE (?, ?, ?)
        """

        statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        statement?.setString(1, truncatedName)
        statement?.setString(2, alterEgo)
        statement?.setInt(3, publisherId)

        statement?.executeUpdate()

        val genKeys = statement?.generatedKeys
        if (genKeys?.next() == true) {
            characterId = genKeys.getInt("GENERATED_KEY")
        }

        return characterId
    }

    /**
     * Lookup character - looks up character by [name], [alterEgo], and
     * [publisherId]. If [checkPrimary] is true, [PRIMARY_DATABASE] is checked
     * first, then [database]. If [checkPrimary] is false, only [database]
     * is checked. If [checkPrimary] is true, [PRIMARY_DATABASE] will not be
     * checked, only [database].
     *
     * @param name character name
     * @param alterEgo character alter ego
     * @param publisherId publisher id
     * @param checkPrimary whether to check [PRIMARY_DATABASE] first
     * @return character id if found, null otherwise
     */
    private fun lookupCharacter(
        name: String,
        alterEgo: String?,
        publisherId: Int,
        checkPrimary: Boolean = database != PRIMARY_DATABASE
    ): Int? {
        var characterId: Int? = null
        val db = if (checkPrimary) {
            PRIMARY_DATABASE
        } else {
            database
        }

        try {
            val getCharacterSql = """
            SELECT *
            FROM $db.m_character mc
            WHERE mc.name = ?
            AND mc.publisher_id = ?
            AND mc.alter_ego ${if (alterEgo == null) "IS NULL" else "= ?"}
        """

            conn.prepareStatement(getCharacterSql).use { statement ->
                statement?.setString(1, name)
                statement?.setInt(2, publisherId)
                if (alterEgo != null) {
                    statement?.setString(3, alterEgo)
                }

                statement?.executeQuery().use { resultSet ->
                    if (resultSet?.next() == true) {
                        characterId = resultSet.getInt("id")
                    }
                }
            }

            if (characterId == null && database != PRIMARY_DATABASE && checkPrimary) {
                characterId =
                    lookupCharacter(
                        name = name,
                        alterEgo = alterEgo,
                        publisherId = publisherId,
                        checkPrimary = false
                    )
            }
        } catch (sqlEx: SQLException) {
            logger.error("Error looking up character", sqlEx)
        }
        return characterId
    }

}