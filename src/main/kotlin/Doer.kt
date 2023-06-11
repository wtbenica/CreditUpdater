import java.sql.Connection

/**
 * Doer - does the work of the Migrator and Updater.
 *
 * @constructor Create empty Doer
 * @property targetSchema The schema to use.
 */
abstract class Doer(protected val targetSchema: String) {
    /** The connection to [targetSchema]. */
    protected val databaseConnection: Connection?
        get() = run {
            println("Getting connection to ${targetSchema}...")
            DatabaseUtil.getConnection(targetSchema)
        }

    companion object {
        /**
         * Extract credits - extracts non-relational creator credits from stories
         * in [sourceSchema].stories_to_migrate
         *
         * @param sourceConn The connection to the source database.
         * @param storyCount The number of stories in the database.
         * @param sourceSchema The schema with the stories_to_migrate table.
         * @param lastIdCompleted The last id that was completed.
         * @param numComplete The number of items that have been completed.
         */
        suspend fun extractCredits(
            sourceConn: Connection?,
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

            sourceConn?.let {
                DatabaseUtil.updateItems(
                    getItems = scriptSql,
                    startingComplete = numComplete,
                    totalItems = storyCount,
                    extractor = CreditExtractor(sourceSchema, it),
                    conn = it
                )
            }
        }

    }

    /**
     * Extract characters and appearances - extracts characters and appearances
     * from the database.
     *
     * @param conn The connection to the database.
     * @param storyCount The number of stories in the database.
     * @param schema The schema with the stories_to_migrate table.
     * @param lastIdCompleted The last id that was completed.
     * @param numComplete The number of items that have been completed.
     */
    suspend fun extractCharactersAndAppearances(
        conn: Connection?,
        storyCount: Int?,
        schema: String,
        lastIdCompleted: Long,
        numComplete: Long
    ) {
        println("Starting Characters...")

        /**
         * Script sql - a nippet that extracts characters and creates appearances
         * for them
         */
        val scriptSql = """SELECT g.id, g.characters
                FROM $schema.stories_to_migrate g
                WHERE g.id > $lastIdCompleted
                ORDER BY g.id """

        conn?.let {
            DatabaseUtil.updateItems(
                getItems = scriptSql,
                startingComplete = numComplete,
                totalItems = storyCount,
                extractor = CharacterExtractor(schema, it),
                conn = it
            )
        }
    }
}