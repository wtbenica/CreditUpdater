package dev.benica.db_tasks

import dev.benica.converter.CharacterExtractor
import dev.benica.converter.CreditExtractor
import dev.benica.db.DatabaseUtil
import dev.benica.di.DaggerDatabaseComponent
import java.sql.Connection

/**
 *
 *
 * @constructor Create empty dev.benica.CreditUpdater.Doer
 * @property targetSchema The schema to use.
 */
abstract class DBTask(protected val targetSchema: String) {
    private val databaseComponent = DaggerDatabaseComponent.create()
    protected val database = DatabaseUtil(targetSchema, databaseComponent)

    /** The connection to [targetSchema]. */
    private val databaseConnection: Connection?
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
        numComplete: Long,
        initial: Boolean
    ) {
        println("starting credits...")
        /**
         * Script sql - an sql snippet to get the writer, penciller, inker,
         * colorist, letterer, and editor from the database.
         */
        val selectStoriesQuery = if (initial) {
            """SELECT g.script, g.id, g.pencils, g.inks, g.colors, g.letters, g.editing
                FROM ${sourceSchema}.gcd_story g
                where g.id > $lastIdCompleted
                ORDER BY g.id """
        } else {
            """SELECT g.script, g.id, g.pencils, g.inks, g.colors, g.letters, g.editing
                FROM ${sourceSchema}.stories_to_migrate g
                WHERE g.id > $lastIdCompleted
                ORDER BY g.id """
        }

        databaseConnection?.let {
            database.extractAndInsertItems(
                selectItemsQuery = selectStoriesQuery,
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
        storyCount: Int,
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
        val selectStoriesQuery = if (initial) {
            """SELECT g.id, g.characters, gs.publisher_id
                FROM $schema.gcd_story g
                JOIN $schema.gcd_issue gi on gi.id = g.issue_id
                JOIN $schema.gcd_series gs on gs.id = gi.series_id
                where g.id > $lastIdCompleted
                ORDER BY g.id """
        } else {
            """SELECT g.id, g.characters
                FROM $schema.stories_to_migrate g
                WHERE g.id > $lastIdCompleted
                ORDER BY g.id """
        }

        databaseConnection?.let {
            database.extractAndInsertItems(
                selectItemsQuery = selectStoriesQuery,
                startingComplete = numComplete,
                totalItems = storyCount,
                extractor = CharacterExtractor(schema, it),
            )
        }
    }
}