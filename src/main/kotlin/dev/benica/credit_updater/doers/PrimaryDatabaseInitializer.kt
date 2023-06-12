package dev.benica.credit_updater.doers

import dev.benica.credit_updater.Credentials.Companion.ADD_ISSUE_SERIES_TO_CREDITS_PATH
import dev.benica.credit_updater.Credentials.Companion.ADD_MODIFY_TABLES_PATH
import dev.benica.credit_updater.Credentials.Companion.CHARACTER_STORIES_NUM_COMPLETE
import dev.benica.credit_updater.Credentials.Companion.CHARACTER_STORY_ID_START
import dev.benica.credit_updater.Credentials.Companion.CREDITS_STORIES_NUM_COMPLETE
import dev.benica.credit_updater.Credentials.Companion.CREDITS_STORY_ID_START
import dev.benica.credit_updater.Credentials.Companion.PRIMARY_DATABASE
import dev.benica.credit_updater.Credentials.Companion.SHRINK_DATABASE_PATH
import dev.benica.credit_updater.Credentials.Companion.UPDATE_CHARACTERS
import dev.benica.credit_updater.Credentials.Companion.UPDATE_CREDITS
import dev.benica.credit_updater.Credentials.Companion.UPDATE_DATABASE
import kotlinx.coroutines.coroutineScope

/**
 * The dev.benica.CreditUpdater.PrimaryDatabaseInitializer class is
 * responsible for preparing an initial database installation from a gcd
 * dump.
 * - It adds issue and series columns and fks to gcd_story_credit if they
 *   have not already been added
 * - It creates m_character, m_character_appearance, and m_story_credit
 *   tables if they don't already exist
 * - It removes unused records from the database
 * - It extracts character and appearance data from the gcd_story table
 * - It extracts credit data from the gcd_story table
 */
class PrimaryDatabaseInitializer(targetSchema: String? = null) :
    Doer(targetSchema = targetSchema ?: PRIMARY_DATABASE) {
    /**
     * Updates the database with new data from the GCD. This function adds
     * new tables to the database schema and shrinks the database size, if
     * 'UPDATE_DATABASE' is true. It also extracts character and appearance
     * data from the 'gcd_story' table and updates the 'm_character' and
     * 'm_character_appearance' tables, if 'UPDATE_CHARACTERS' is true.
     * Credit data is extracted from the 'gcd_story' table and updates the
     * 'gcd_story_credit' and 'm_story_credit' tables, if 'UPDATE_CREDITS'
     * is true. Finally, foreign key constraints are added to the
     * 'gcd_story_credit', 'm_character_appearance', and 'm_story_credit'
     * tables, as needed.
     */
    suspend fun update() {
        coroutineScope {
            println("Updating $targetSchema")
            if (UPDATE_DATABASE) {
                println("Starting Database Updates...")
                addTables()
                shrinkDatabase()
            }

            val storyCount = database.getItemCount(
                tableName = "$targetSchema.gcd_story"
            )

            if (UPDATE_CHARACTERS) {
                extractCharactersAndAppearances(
                    storyCount = storyCount,
                    schema = targetSchema,
                    lastIdCompleted = CHARACTER_STORY_ID_START,
                    numComplete = CHARACTER_STORIES_NUM_COMPLETE
                )
            }

            if (UPDATE_CREDITS) {
                extractCredits(
                    storyCount,
                    targetSchema,
                    CREDITS_STORY_ID_START,
                    CREDITS_STORIES_NUM_COMPLETE
                )

                println("Starting FKey updates")
                addIssueSeriesToCredits()
            }
        }
    }

    /**
     * Adds the 'issue' and 'series' columns to the 'gcd_story_credit'
     * and 'm_character_appearance' tables, respectively, if they don't
     * already exist. Then creates the 'm_story_credit' table if it doesn't
     * exist. Also adds foreign key constraints to the 'gcd_story_credit',
     * 'm_character_appearance', and 'm_story_credit' tables, as needed.
     * Finally, creates the 'm_character' and 'm_character_appearance' tables
     * if they don't exist.
     *
     * Note: This code block uses SQL statements to modify the database schema
     * and create tables. It assumes that the database connection is already
     * established and that the user has the necessary permissions to execute
     * these statements.
     *
     * @param connection The connection to the database.
     */
    private fun addTables() = database.runSqlScriptQuery(sqlScriptPath = ADD_MODIFY_TABLES_PATH)

    /**
     * Adds the 'issue' and 'series' columns to the 'gcd_story_credit'
     * and 'm_character_appearance' tables, respectively, if they don't
     * already exist. Also creates the 'm_story_credit' table if it doesn't
     * exist. Adds foreign key constraints to the 'gcd_story_credit',
     * 'm_character_appearance', and 'm_story_credit' tables, as needed.
     * Finally, creates the 'm_character' and 'm_character_appearance' tables
     * if they don't exist.
     *
     * Note: This function assumes that the database connection is already
     * established and that the user has the necessary permissions to execute
     * these statements.
     *
     * @param connection The connection to the database.
     */
    private fun shrinkDatabase() = database.runSqlScriptUpdate(sqlScriptPath = SHRINK_DATABASE_PATH)

    /**
     * Updates the 'issue_id' and 'series_id' columns in the 'gcd_story_credit'
     * and 'm_story_credit' tables, respectively, by setting them to the
     * corresponding values in the 'gcd_issue' and 'gcd_series' tables. The
     * 'issue_id' and 'series_id' columns are set to NULL if they don't already
     * have a value.
     *
     * Then, creates a temporary table 'story_with_missing_issue' that contains
     * the IDs of stories that have a NULL 'issue_id' value. Deletes records
     * from 'm_character_appearance' table where the 'story_id' matches the IDs
     * in 'story_with_missing_issue'.
     *
     * Finally, updates the 'issue_id' and 'series_id' columns in the
     * 'm_character_appearance' table by setting them to the corresponding
     * values in the 'gcd_issue' and 'gcd_series' tables. The 'issue_id' and
     * 'series_id' columns are set to NULL if they don't already have a value.
     *
     * Note: This function assumes that the database connection is already
     * established and that the user has the necessary permissions to execute
     * these statements.
     *
     * @param connection The connection to the database.
     */
    private fun addIssueSeriesToCredits() =
        database.runSqlScriptUpdate(sqlScriptPath = ADD_ISSUE_SERIES_TO_CREDITS_PATH)

}

