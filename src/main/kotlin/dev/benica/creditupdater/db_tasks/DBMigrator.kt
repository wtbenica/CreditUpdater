package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.INCOMING_DATABASE
import dev.benica.creditupdater.Credentials.Companion.PRIMARY_DATABASE
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.di.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mu.KLogger
import mu.KotlinLogging
import java.io.File
import java.sql.Connection
import java.sql.SQLException
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

    @Inject
    internal lateinit var connectionSource: ConnectionSource

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
        val conn: Connection = connectionSource.getConnection(sourceSchema).connection

        withContext(ioDispatcher) {
            try {
                logger.info { "Migrating to $targetSchema from $sourceSchema" }

                if (startAtStep == 1) {
                    logger.info { "starting tables..." }
                    addTablesNew(queryExecutor, conn, sourceSchema, targetSchema)
                }

                if (startAtStep <= 2) {
                    logger.info { "starting characters..." }
                    dbTask.extractCharactersAndAppearances(
                        schema = sourceSchema,
                        initial = true,
                        startingId = startingId
                    )
                }

                if (startAtStep <= 3) {
                    logger.info { "starting credits..." }
                    dbTask.extractCredits(
                        schema = sourceSchema,
                        initial = true,
                        startingId = startingId.takeIf { startAtStep == 3 }
                    )
                }

                if (startAtStep <= 4) {
                    logger.info { "starting foreign keys updates" }
                    addIssueSeriesToCreditsNew(queryExecutor, conn)
                    logger.info { "Done prepping $sourceSchema for migration" }
                }

                if (startAtStep <= 5) {
                    logger.info { "starting migration..." }
                    migrateRecords(queryExecutor, conn)
                    logger.info { "Done migrating records" }
                }
            } catch (sqlEx: SQLException) {
                logger.error { "Error migrating to $targetSchema from $sourceSchema" }
                logger.error { sqlEx.message }
                logger.error { sqlEx.stackTrace }
                throw sqlEx
            } finally {
                conn.close()
            }
        }
    }

    companion object {
        /**
         * This SQL script adds issue_id and series_id columns to the
         * gcd_story_credit and m_character_appearance tables if they don't already
         * exist. It then creates the m_story_credit, m_character_appearance, and
         * m_character tables. The script also creates several views of "good"
         * items based on certain criteria, and creates tables for items that need
         * to be migrated to the new database. Finally, the script adds constraints
         * and indexes to the m_character and m_character_appearance tables.
         */
        private const val ADD_MODIFY_TABLES_PATH_NEW = "src/main/resources/sql/my_tables_new.sql"

        private const val ADD_ISSUE_SERIES_TO_CREDITS_PATH_NEW =
            "src/main/resources/sql/add_issue_series_to_credits_new.sql"

        private const val MIGRATE_PATH_NEW = "src/main/resources/sql/migrate.sql"

        internal fun migrateRecords(queryExecutor: QueryExecutor, conn: Connection) =
            queryExecutor.executeSqlScript(File(MIGRATE_PATH_NEW), conn = conn)

        internal fun addTablesNew(
            queryExecutor: QueryExecutor,
            conn: Connection,
            sourceSchema: String? = null,
            targetSchema: String? = null
        ) =
            queryExecutor.executeSqlScript(
                File(ADD_MODIFY_TABLES_PATH_NEW),
                conn = conn,
                sourceSchema = sourceSchema,
                targetSchema = targetSchema
            )

        internal fun addIssueSeriesToCreditsNew(queryExecutor: QueryExecutor, conn: Connection) =
            queryExecutor.executeSqlScript(File(ADD_ISSUE_SERIES_TO_CREDITS_PATH_NEW), conn = conn)
    }
}