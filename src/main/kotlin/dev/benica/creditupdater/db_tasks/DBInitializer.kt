package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.PRIMARY_DATABASE
import dev.benica.creditupdater.db.ConnectionProvider
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.di.*
import kotlinx.coroutines.*
import mu.KLogger
import mu.KotlinLogging
import java.io.File
import java.sql.Connection
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Named

/**
 * Prepares an initial database installation from a gcd dump.
 *
 * Steps:
 * 1. Add new tables to the database schema and shrink the database.
 * 2. Extract character and appearance data from the 'gcd_story' table and
 *    update the 'm_character' and 'm_character_appearance' tables.
 * 3. Extract credit data from the 'gcd_story' table and update the
 *    'gcd_story_credit' and 'm_story_credit' tables.
 * 4. Add foreign key constraints to the 'gcd_story_credit',
 *    'm_character_appearance', and 'm_story_credit' tables.
 * 5. Add the characters the gcd links to stories ('gcd_story_character') to
 *    'm_character' and 'm_character_appearance'. See [INIT_ADD_LINKED_CHARACTERS].
 * 6. Merge characters that are the same character written differently. See
 *    [INIT_MERGE_DUPLICATE_CHARACTERS].
 *
 * @param targetSchema The schema to update.
 * @param startAtStep The step to start at (1-6).
 */
class DBInitializer(
    private val targetSchema: String = PRIMARY_DATABASE,
    private val startAtStep: Int,
    private val startingId: Int? = null,
    dispatcherComponent: DispatchAndExecuteComponent = DaggerDispatchAndExecuteComponent.create(),
) {
    // Dependencies
    @Inject
    @Named("IO")
    internal lateinit var ioDispatcher: CoroutineDispatcher

    // Private properties
    private val logger: KLogger
        get() = KotlinLogging.logger(this::class.java.simpleName)

    private val dbTask: DBTask = DBTask(targetSchema)

    init {
        dispatcherComponent.inject(this)
    }


    /**
     * Prepare database - this is the main entry point for the
     * PrimaryDatabaseInitializer class. It is responsible
     * for preparing the database for the first time.
     *
     * @throws SQLException
     * @see DBTask
     */
    @Throws(SQLException::class)
    suspend fun prepareDb() {
        val queryExecutor = QueryExecutor()

        withContext(ioDispatcher) {
            ConnectionProvider.getConnection(targetSchema).connection.use { conn ->
                try {
                    logger.info { "Updating $targetSchema" }
                    if (startAtStep <= 1) {
                        logger.info { "Starting Table Updates..." }
                        dropUnusedTables(
                            queryExecutor = queryExecutor,
                            targetSchema = targetSchema,
                            conn = conn
                        )
                        // step 1A
                        dropIsSourcedAndSourcedByColumns(queryExecutor, targetSchema, conn)

                        // step 1B
                        DBInitAddTables(
                            queryExecutor = QueryExecutor(),
                            targetSchema = targetSchema,
                            conn = conn
                        ).addTablesAndConstraints()

                        // step 1C
                        createDeleteViews(
                            queryExecutor = queryExecutor,
                            targetSchema = targetSchema,
                            conn = conn
                        )

                        // step 1D
                        removeUnnecessaryRecords(
                            queryExecutor = queryExecutor,
                            targetSchema = targetSchema,
                            conn = conn
                        )
                    }

                    if (startAtStep <= 2) {
                        logger.info { "Starting Character Updates..." }
                        dbTask.extractCharactersAndAppearances(
                            schema = targetSchema,
                            initial = true,
                            startingId = startingId,
                        )
                    }

                    if (startAtStep <= 3) {
                        logger.info { "Starting Credit Updates..." }
                        dbTask.extractCredits(
                            schema = targetSchema,
                            initial = true,
                            startingId = startingId.takeIf { startAtStep == 3 },
                        )
                    }

                    if (startAtStep <= 4) {
                        logger.info { "Starting foreign key updates" }
                        addIssueSeriesColumnsAndConstraints(
                            queryExecutor = queryExecutor,
                            targetSchema = targetSchema,
                            conn = conn
                        )
                    }

                    if (startAtStep <= 5) {
                        logger.info { "Adding linked characters..." }
                        addLinkedCharacters(
                            queryExecutor = queryExecutor,
                            targetSchema = targetSchema,
                            conn = conn
                        )
                    }

                    if (startAtStep <= 6) {
                        logger.info { "Merging duplicate characters..." }
                        mergeDuplicateCharacters(
                            queryExecutor = queryExecutor,
                            targetSchema = targetSchema,
                            conn = conn
                        )
                    }

                    logger.info { "Successfully updated $targetSchema" }
                } catch (sqlEx: SQLException) {
                    logger.error { "Failed to update $targetSchema" }
                    logger.error { sqlEx.message }
                    logger.error { sqlEx.stackTrace }
                    throw sqlEx
                }
            }
        }
    }

    companion object {
        /**
         * Target Schema:
         * - Adds the 'issue' and 'series' columns to the 'gcd_story_credit',
         *   'm_story_credit', and 'm_character_appearance' tables.
         * - Removes any m_character_appearances whose story is missing an issue_id
         * - Adds NOT NULL constraints to the 'issue' and 'series' columns in the
         *   'gcd_story_credit', 'm_story_credit', and 'm_character_appearance'
         *   tables.
         */
        const val ISSUE_SERIES_PATH = "src/main/resources/sql/init_add_issue_series_to_credits.sql"
        const val INIT_REMOVE_ITEMS = "src/main/resources/sql/init_remove_items.sql"
        const val INIT_DROP_UNUSED_TABLES = "src/main/resources/sql/init_drop_unused_tables.sql"

        /**
         * Target Schema:
         * - The gcd now records most characters as links from stories to its own
         *   character records ('gcd_story_character'); the text the extractor
         *   reads only holds those nobody has linked. This is not a copy of the
         *   gcd's records: a character is a name, an alter ego and a publisher,
         *   and records are merged when nothing tells them apart.
         * - A linked character belongs to the publisher of the series it
         *   appears in, so one character at two publishers is two characters.
         * - A record the gcd says was 'republished as' another (a reprint or
         *   foreign edition) counts as the original, so Robin (Dick Grayson) is
         *   one character. Alternate universe versions stay separate.
         * - A character that is the 'alias of' exactly one other gets that
         *   one's name as its alter ego, which keeps the Starmen, Robins and
         *   Batgirls apart.
         * - Records with the same name, alter ego and publisher are one
         *   character, whatever the gcd's disambiguation says.
         * - An extracted character is taken over by the linked one whose name
         *   matches, ignoring case and punctuation, any name the gcd has for it,
         *   with the same alter ego and publisher. Extracted characters that
         *   match the same linked one are the same character spelled
         *   differently, and are folded into it with their appearances. A name
         *   two linked characters have matches neither, unless it is the
         *   official name of just one.
         * - Adds 'gcd_character_id' to 'm_character' and
         *   'gcd_story_character_id' to 'm_character_appearance'. Run it again
         *   any time: it only adds what is missing.
         */
        const val INIT_ADD_LINKED_CHARACTERS = "src/main/resources/sql/init_add_linked_characters.sql"

        /**
         * Target Schema:
         * - Merges characters that are the same character written differently.
         *   Two characters of one publisher are merged when they have the same
         *   name and alter ego, or the name of one is the alter ego of the other
         *   and the other way round, in both cases ignoring case, spaces and
         *   punctuation. Near misses are left alone, and a character with no
         *   alter ego is never merged with one that has one. The one kept is a
         *   linked one, else the one with the most appearances, else the oldest;
         *   linked characters are never removed. Run it again any time.
         */
        const val INIT_MERGE_DUPLICATE_CHARACTERS = "src/main/resources/sql/init_merge_duplicate_characters.sql"

        /**
         * Target Schema:
         * - Creates views `bad_publishers`, `bad_indicia_publishers`,
         *   `bad_series`, `bad_issues`, and `bad_stories`.
         */
        const val INIT_CREATE_BAD_VIEWS = "src/main/resources/sql/init_create_bad_views.sql"

        /**
         * Drop is sourced and sourced by columns - this function drops the
         * 'is_sourced' and 'sourced_by' columns from the 'gcd_story_credit' table.
         * These columns have been added to the GCD, but are not needed by Infinite
         * Longbox.
         *
         * @throws SQLException
         */
        @Throws(SQLException::class)
        internal fun dropIsSourcedAndSourcedByColumns(queryExecutor: QueryExecutor, targetSchema: String, conn: Connection) =
            queryExecutor.executeSqlStatement(
                sqlStmt = """ALTER TABLE $targetSchema.gcd_story_credit
                                DROP COLUMN IF EXISTS is_sourced,
                                DROP COLUMN IF EXISTS sourced_by;
                            """.trimIndent(),
                connection = conn
            )

        internal fun createDeleteViews(queryExecutor: QueryExecutor, targetSchema: String, conn: Connection) =
            queryExecutor.executeSqlScript(
                sqlScript = File(INIT_CREATE_BAD_VIEWS),
                conn = conn,
                targetSchema = targetSchema
            )

        @Throws(SQLException::class)
        internal fun addLinkedCharacters(queryExecutor: QueryExecutor, targetSchema: String, conn: Connection) =
            queryExecutor.executeSqlScript(
                sqlScript = File(INIT_ADD_LINKED_CHARACTERS),
                conn = conn,
                targetSchema = targetSchema
            )

        @Throws(SQLException::class)
        internal fun mergeDuplicateCharacters(queryExecutor: QueryExecutor, targetSchema: String, conn: Connection) =
            queryExecutor.executeSqlScript(
                sqlScript = File(INIT_MERGE_DUPLICATE_CHARACTERS),
                conn = conn,
                targetSchema = targetSchema
            )

        internal fun dropUnusedTables(queryExecutor: QueryExecutor, targetSchema: String, conn: Connection) =
            queryExecutor.executeSqlScript(
                sqlScript = File(INIT_DROP_UNUSED_TABLES),
                conn = conn,
                targetSchema = targetSchema
            )

        /**
         * Shrink database - this function shrinks the database by removing a bunch
         * of records: Non-US/Canadian publishers, non-English languages, and
         * pre-1900 stories.
         *
         * @throws SQLException
         */
        @Throws(SQLException::class)
        internal fun removeUnnecessaryRecords(queryExecutor: QueryExecutor, targetSchema: String, conn: Connection) =
            queryExecutor.executeSqlScript(
                sqlScript = File(INIT_REMOVE_ITEMS),
                conn = conn,
                targetSchema = targetSchema
            )


        /**
         * Adds the 'issue' and 'series' columns to the 'gcd_story_credit',
         * 'm_story_credit', and 'm_character_appearance' tables. It also adds
         * the appropriate foreign key constraints to the 'gcd_story_credit',
         * 'm_character_appearance', and 'm_story_credit' tables.
         *
         * @throws SQLException
         */
        @Throws(SQLException::class)
        internal fun addIssueSeriesColumnsAndConstraints(
            queryExecutor: QueryExecutor,
            targetSchema: String,
            conn: Connection
        ) =
            queryExecutor.executeSqlScript(
                sqlScript = File(ISSUE_SERIES_PATH),
                runAsTransaction = true,
                conn = conn,
                targetSchema = targetSchema
            )
    }
}

