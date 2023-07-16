package dev.benica.creditupdater.db

import dev.benica.creditupdater.Credentials.Companion.PRIMARY_DATABASE
import dev.benica.creditupdater.di.*
import dev.benica.creditupdater.models.Appearance
import mu.KLogger
import mu.KotlinLogging
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement.RETURN_GENERATED_KEYS
import javax.inject.Inject

/**
 * Character repository - provides methods for inserting and looking up
 * characters and character appearances in the 'm_character' and
 * 'm_character_appearance' tables.
 *
 * @param targetSchema the database to which to write the extracted character
 *     and appearance data.
 * @note The caller is responsible for closing the repository.
 */
class CharacterRepository(
    private val targetSchema: String,
    databaseComponent: DatabaseComponent? = DaggerDatabaseComponent.create(),
    private val queryExecutor: QueryExecutor = QueryExecutor()
) : Repository {
    // Dependencies
    @Inject
    internal lateinit var connectionSource: ConnectionSource

    init {
        databaseComponent?.inject(this)
    }

    // Private Properties
    private val logger: KLogger
        get() = KotlinLogging.logger(this::class.java.simpleName)

    // Public Methods
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
     * @throws SQLException if an error occurs
     */
    @Throws(SQLException::class)
    fun upsertCharacter(
        name: String,
        alterEgo: String?,
        publisherId: Int,
        checkPrimary: Boolean? = null
    ): Int? = if (checkPrimary != null) {
        lookupCharacter(
            name,
            alterEgo?.truncate(255),
            publisherId,
            checkPrimary
        )
    } else {
        lookupCharacter(
            name,
            alterEgo?.truncate(255),
            publisherId
        )
    } ?: insertCharacter(
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

    /** Insert character appearances - inserts [appearances] into [targetSchema] */
    fun insertCharacterAppearances(appearances: Set<Appearance>) {
        val conn = connectionSource.getConnection(targetSchema).connection

        try {
            val sql = """
               INSERT IGNORE INTO $targetSchema.m_character_appearance(id, details, character_id, story_id, notes, membership)
               VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()

            queryExecutor.executePreparedStatementBatch(sql, conn = conn) { statement: PreparedStatement ->
                appearances.forEach { appearance ->
                    statement.setInt(1, appearance.id)
                    statement.setString(2, appearance.details)
                    statement.setInt(3, appearance.characterId)
                    statement.setInt(4, appearance.storyId)
                    statement.setString(5, appearance.notes)
                    statement.setString(6, appearance.membership)
                    statement.addBatch()
                }
            }
        } catch (e: SQLException) {
            logger.error("Error inserting character appearances", e)
            throw e
        } finally {
            conn.close()
        }
    }

    /**
     * Insert character - inserts [name], [alterEgo], and [publisherId] into
     * [targetSchema] table m_character
     *
     * @param name character name
     * @param alterEgo character alter ego
     * @param publisherId publisher id
     * @return inserted character's id
     * @throws SQLException if there is an error inserting the character
     */
    @Throws(SQLException::class)
    internal fun insertCharacter(
        name: String,
        alterEgo: String?,
        publisherId: Int
    ): Int? {
        var characterId: Int? = null
        val truncatedName = name.substring(0, Integer.min(255, name.length))
        val sql = """
           INSERT INTO ${targetSchema}.m_character(name, alter_ego, publisher_id)
           VALUE (?, ?, ?)
        """
        val conn = connectionSource.getConnection(targetSchema).connection

        queryExecutor.executePreparedStatementBatch(sql, RETURN_GENERATED_KEYS, conn = conn) { s: PreparedStatement ->
            try {
                s.setString(1, truncatedName)
                s.setString(2, alterEgo)
                s.setInt(3, publisherId)

                s.executeUpdate()

                val genKeys = s.generatedKeys
                if (genKeys?.next() == true) {
                    characterId = genKeys.getInt("GENERATED_KEY")
                }
            } catch (e: SQLException) {
                logger.error("Error inserting character $name", e)
                conn.close()
                throw e
            }
        }

        if (!conn.isClosed) {
            conn.close()
        }

        return characterId
    }

    /**
     * Lookup character - looks up character by [name], [alterEgo], and
     * [publisherId]. If [checkPrimary] is true, [PRIMARY_DATABASE] is
     * checked first, then [targetSchema]. If [checkPrimary] is false, only
     * [targetSchema] is checked. If [checkPrimary] is true, [PRIMARY_DATABASE]
     * will not be checked, only [targetSchema].
     *
     * @param name character name
     * @param alterEgo character alter ego
     * @param publisherId publisher id
     * @param checkPrimary whether to check [PRIMARY_DATABASE] first
     * @return character id if found, null otherwise
     * @throws SQLException if there is an error looking up the character
     */
    @Throws(SQLException::class)
    internal fun lookupCharacter(
        name: String,
        alterEgo: String?,
        publisherId: Int,
        checkPrimary: Boolean = targetSchema != PRIMARY_DATABASE
    ): Int? {
        val conn = connectionSource.getConnection(targetSchema).connection
        var characterId: Int? = null
        val db = if (checkPrimary) {
            PRIMARY_DATABASE
        } else {
            targetSchema
        }

        try {
            val getCharacterSql = """
            SELECT *
            FROM $db.m_character mc
            WHERE mc.name = ?
            AND mc.publisher_id = ?
            AND mc.alter_ego ${if (alterEgo == null) "IS NULL" else "= ?"}
        """

            queryExecutor.executePreparedStatementBatch(getCharacterSql, conn = conn) { statement ->
                statement.setString(1, name)
                statement.setInt(2, publisherId)
                if (alterEgo != null) {
                    statement.setString(3, alterEgo)
                }

                statement.executeQuery().use { resultSet ->
                    if (resultSet?.next() == true) {
                        characterId = resultSet.getInt("id")
                    }
                }
            }


            if (characterId == null && targetSchema != PRIMARY_DATABASE && checkPrimary) {
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
            throw sqlEx
        } finally {
            conn.close()
        }

        return characterId
    }
}