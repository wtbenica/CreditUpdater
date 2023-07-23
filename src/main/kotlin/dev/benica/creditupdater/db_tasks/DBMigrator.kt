package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.INCOMING_DATABASE
import dev.benica.creditupdater.Credentials.Companion.PRIMARY_DATABASE
import dev.benica.creditupdater.db.ConnectionProvider
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.db_tasks.DBInitializer.Companion.dropIsSourcedAndSourcedByColumns
import dev.benica.creditupdater.di.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mu.KLogger
import mu.KotlinLogging
import java.io.File
import java.sql.Connection
import javax.inject.Inject
import javax.inject.Named

/**
 * Migrator - migrates the data from the old database to the new database.
 *
 * @constructor Create empty dev.benica.CreditUpdater.Migrator
 */
class DBMigrator(
    private val sourceSchema: String = INCOMING_DATABASE,
    private val targetSchema: String = PRIMARY_DATABASE,
    private val startAtStep: Int = 1,
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

    private val dbTask: DBTask = DBTask(sourceSchema)

    init {
        dispatcherComponent.inject(this)
    }

    /** Migrate - migrates the data from the old database to the new database. */
    suspend fun migrate() {
        val queryExecutor = QueryExecutor()
        ConnectionProvider.getConnection(sourceSchema).connection.use { conn ->
            withContext(ioDispatcher) {
                logger.info { "Migrating to $targetSchema from $sourceSchema" }

                if (startAtStep <= 1) {
                    logger.info { "starting tables..." }
                    dropIsSourcedAndSourcedByColumns(
                        queryExecutor = queryExecutor,
                        targetSchema = sourceSchema,
                        conn = conn
                    )

                    addTablesNew(
                        queryExecutor = queryExecutor,
                        conn = conn,
                        sourceSchema = sourceSchema,
                        targetSchema = targetSchema
                    )
                }

                // step 1 complete
                if (startAtStep <= 2) {
                    logger.info { "starting characters..." }
                    dbTask.extractCharactersAndAppearances(
                        schema = sourceSchema,
                        initial = false,
                        startingId = startingId
                    )
                }

                // step 2 complete
                if (startAtStep <= 3) {
                    logger.info { "starting credits..." }
                    dbTask.extractCredits(
                        schema = sourceSchema,
                        initial = false,
                        startingId = startingId.takeIf { startAtStep == 3 }
                    )
                }

                // step 3 complete
                if (startAtStep <= 4) {
                    logger.info { "starting foreign keys updates" }
                    addIssueSeriesToCreditsNew(
                        queryExecutor = queryExecutor,
                        conn = conn,
                        targetSchema = sourceSchema
                    )
                    logger.info { "Done prepping $sourceSchema for migration" }
                }

                // step 4 complete
                if (startAtStep <= 5) {
                    logger.info { "starting migration..." }
                    migrateRecords(
                        queryExecutor = queryExecutor,
                        conn = conn,
                        sourceSchema = sourceSchema,
                        targetSchema = targetSchema
                    )
                    logger.info { "Done migrating records" }
                }
            }
        }
    }

    companion object {
        /**
         * Source Schema:
         * - Adds the `issue_id` and `series_id` columns to the `gcd_story_credit`
         *   table.
         * - Creates tables: `m_*` tables like the `m_*` tables in the target
         *   schema.
         * - Drops & creates `good_*` and `migrate_*` views for publishers,
         *   indicia_publishers, series, issues, stories
         */
        internal const val MIGRATE_ADD_TABLES = "src/main/resources/sql/migrate_add_tables.sql"
        private const val MIGRATE_FILL_ID_COLUMNS = "src/main/resources/sql/migrate_fill_id_columns.sql"
        private const val MIGRATE_RECORDS = "src/main/resources/sql/migrate.sql"

        /** Executes [MIGRATE_ADD_TABLES] */
        internal fun addTablesNew(
            queryExecutor: QueryExecutor,
            conn: Connection,
            sourceSchema: String? = null,
            targetSchema: String? = null
        ) =
            queryExecutor.executeSqlScript(
                File(MIGRATE_ADD_TABLES),
                conn = conn,
                targetSchema = targetSchema,
                sourceSchema = sourceSchema
            )

        /**
         * Fills in issue/series id columns in gcd_story_credit, m_story_credit,
         * and m_character_appearance tables.
         */
        internal fun addIssueSeriesToCreditsNew(
            queryExecutor: QueryExecutor,
            conn: Connection,
            targetSchema: String? = null
        ) =
            queryExecutor.executeSqlScript(
                sqlScript = File(MIGRATE_FILL_ID_COLUMNS),
                conn = conn,
                targetSchema = targetSchema
            )

        internal fun migrateRecords(
            queryExecutor: QueryExecutor,
            conn: Connection,
            sourceSchema: String? = null,
            targetSchema: String? = null
        ) =
            queryExecutor.executeSqlScript(
                sqlScript = File(MIGRATE_RECORDS),
                conn = conn,
                targetSchema = targetSchema,
                sourceSchema = sourceSchema
            )
    }
}