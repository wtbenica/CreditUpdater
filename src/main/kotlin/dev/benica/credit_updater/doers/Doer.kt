package dev.benica.credit_updater.doers

import dev.benica.credit_updater.converter.CharacterExtractor
import dev.benica.credit_updater.converter.CreditExtractor
import dev.benica.credit_updater.db.DatabaseUtil
import dev.benica.credit_updater.di.DaggerDatabaseComponent
import java.sql.Connection

/**
 * dev.benica.CreditUpdater.Doer - does the work of the dev.benica.CreditUpdater.Migrator and Updater.
 *
 * @constructor Create empty dev.benica.CreditUpdater.Doer
 * @property targetSchema The schema to use.
 */
abstract class Doer(protected val targetSchema: String) {
    private val databaseComponent = DaggerDatabaseComponent.create()
    protected val database = DatabaseUtil(targetSchema, databaseComponent)

    /** The connection to [targetSchema]. */
    protected val databaseConnection: Connection?
        get() = database.getConnection()

    /**
     * Extract credits - extracts non-relational creator credits from stories
     * in [sourceSchema].stories_to_migrate
     *
     * @param storyCount The number of stories in the database.
     * @param sourceSchema The schema with the stories_to_migrate table.
     * @param lastIdCompleted The last id that was completed.
     * @param numComplete The number of items that have been completed.
     */
    suspend fun extractCredits(
        storyCount: Int?,
        sourceSchema: String,
        lastIdCompleted: Long,
        numComplete: Long
    ) {
        println("starting credits...")
        /**
         * Script sql - an sql snippet to get the writer, penciller, inker,
         * colorist, letterer, and editor from the database.
         */
        val scriptSql =
            """SELECT g.script, g.id, g.pencils, g.inks, g.colors, g.letters, g.editing
                            FROM ${sourceSchema}.stories_to_migrate g
                            WHERE g.id > $lastIdCompleted
                            ORDER BY g.id """

        databaseConnection?.let {
            database.updateItems(
                getItems = scriptSql,
                startingComplete = numComplete,
                totalItems = storyCount,
                extractor = CreditExtractor(sourceSchema, it),
            )
        }
    }

    /**
     * Extract characters and appearances - extracts characters and appearances
     * from the database.
     *
     * @param storyCount The number of stories in the database.
     * @param schema The schema with the stories_to_migrate table.
     * @param lastIdCompleted The last id that was completed.
     * @param numComplete The number of items that have been completed.
     */
    suspend fun extractCharactersAndAppearances(
        storyCount: Int?,
        schema: String,
        lastIdCompleted: Long,
        numComplete: Long,
        initial: Boolean
    ) {
        println("Starting Characters...")

        /**
         * Script sql - a snippet that extracts characters and creates appearances
         * for them
         */
        val scriptSql = if (initial) {
            """SELECT g.id, g.characters
                FROM $schema.gcd_story g
                where g.id > $lastIdCompleted
                ORDER BY g.id """
        } else {
            """SELECT g.id, g.characters
                FROM $schema.stories_to_migrate g
                WHERE g.id > $lastIdCompleted
                ORDER BY g.id """
        }

        databaseConnection?.let {
            database.updateItems(
                getItems = scriptSql,
                startingComplete = numComplete,
                totalItems = storyCount,
                extractor = CharacterExtractor(schema, it),
            )
        }
    }
}