package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.db.QueryExecutor
import java.sql.Connection

@Suppress("kotlin:S1192")
class DBInitAddTables(
    private val queryExecutor: QueryExecutor,
    private val targetSchema: String,
    private val conn: Connection
) {
    // Public Methods
    /**
     * Add the 'm_character', 'm_character_appearance', and 'm_story_credit'
     * tables if they do not exist.
     *
     * Add the 'issue_id' and 'series_id' columns to the 'gcd_story_credit'
     * table if they do not exist.
     *
     * Add the foreign key constraints to the 'gcd_story_credit',
     * 'm_character_appearance', and 'm_story_credit' tables if they do not
     * exist.
     *
     * Add the primary key constraint to the 'm_story_credit' table if it does
     * not exist.
     */
    fun addTablesAndConstraints() {
        addIssueColumnIfNotExists()
        addIssueIdForeignKeyIfNotExists()
        addSeriesColumnIfNotExists()
        addSeriesIdForeignKeyIfNotExists()
        createExtractedCharactersTableIfNotExists()
        createCharacterAppearancesTableIfNotExists()
        createExtractedStoryCreditsTableIfNotExists()
        addExtractedStoryCreditsPrimaryKeyConstraintIfNotExists()
        addExtractedStoryCreditsCreatorIdFKeyConstraintIfNotExists()
        addExtractedStoryCreditsRoleIdFKeyConstraintIfNotExists()
        addExtractedStoryCreditsStoryIdFKeyConstraintIfNotExists()
        addExtractedStoryCreditsIssueIdFKeyConstraintIfNotExists()
        addExtractedStoryCreditsSeriesIdFKeyConstraintIfNotExists()
    }

    /**
     * Add the 'issue_id' column to the 'gcd_story_credit' table if it does not
     * exist.
     */
    internal fun addIssueColumnIfNotExists() {
        val query = """SELECT COUNT(*)
                        INTO @exist
                        FROM information_schema.columns
                        WHERE table_schema = '$targetSchema'
                        AND column_name = 'issue_id'
                        AND table_name = 'gcd_story_credit'
                        LIMIT 1;
                        """.trimIndent()

        val statement = ifNotExistStatement(
            query,
            """ALTER TABLE $targetSchema.gcd_story_credit ADD COLUMN issue_id INT DEFAULT NULL""",
            """select 'Column Exists' status"""
        )

        queryExecutor.executeStatements(statement, conn)
    }

    /**
     * Add the 'issue_id' foreign key constraint to the 'gcd_story_credit'
     * table if it does not exist.
     */
    internal fun addIssueIdForeignKeyIfNotExists() {
        val query = """SELECT COUNT(*)
                        INTO @exist
                        FROM information_schema.key_column_usage
                        WHERE table_schema = '$targetSchema'
                        AND table_name = 'gcd_story_credit'
                        AND column_name = 'issue_id'
                        AND referenced_table_name = 'gcd_issue'
                        AND referenced_column_name = 'id';
                        """.trimIndent()

        val statement = ifNotExistStatement(
            query,
            """ALTER TABLE $targetSchema.gcd_story_credit ADD FOREIGN KEY (issue_id) REFERENCES gcd_issue (id)""",
            """select 'Column Exists' status"""
        )

        queryExecutor.executeStatements(statement, conn)
    }

    /**
     * Add the 'series_id' column to the 'gcd_story_credit' table if it does
     * not exist.
     */
    internal fun addSeriesColumnIfNotExists() {
        val query = """SELECT COUNT(*)
                        INTO @exist
                        FROM information_schema.columns
                        WHERE table_schema = '$targetSchema'
                        AND column_name = 'series_id'
                        AND table_name = 'gcd_story_credit'
                        LIMIT 1;
                        """.trimIndent()

        val statement = ifNotExistStatement(
            query,
            """ALTER TABLE $targetSchema.gcd_story_credit ADD COLUMN series_id INT DEFAULT NULL""",
            """select 'Column Exists' status"""
        )

        queryExecutor.executeStatements(statement, conn)
    }

    /**
     * Add the 'series_id' foreign key constraint to the 'gcd_story_credit'
     * table if it does not exist.
     */
    internal fun addSeriesIdForeignKeyIfNotExists() {
        val query = """SELECT COUNT(*)
                        INTO @exist
                        FROM information_schema.key_column_usage
                        WHERE table_schema = '$targetSchema'
                        AND table_name = 'gcd_story_credit'
                        AND column_name = 'series_id'
                        AND referenced_table_name = 'gcd_series'
                        AND referenced_column_name = 'id';
                        """.trimIndent()

        val statement = ifNotExistStatement(
            query,
            """ALTER TABLE $targetSchema.gcd_story_credit ADD FOREIGN KEY (series_id) REFERENCES gcd_series (id)""",
            """select 'Column Exists' status"""
        )

        queryExecutor.executeStatements(statement, conn)
    }

    /** Add the 'm_characters' table if it does not exist. */
    internal fun createExtractedCharactersTableIfNotExists() {
        val statement = """CREATE TABLE IF NOT EXISTS $targetSchema.m_character (
            id           INTEGER PRIMARY KEY AUTO_INCREMENT,
            name         VARCHAR(255) NOT NULL,
            alter_ego    VARCHAR(255),
            publisher_id INTEGER REFERENCES gcd_publisher (id),
            INDEX (name),
            INDEX (alter_ego),
            UNIQUE INDEX (name, alter_ego, publisher_id)
            );""".trimIndent()

        queryExecutor.executeSqlStatement(statement, conn)
    }

    /** Add the 'm_character_appearance' table if it does not exist. */
    internal fun createCharacterAppearancesTableIfNotExists() {
        val statement = """CREATE TABLE IF NOT EXISTS $targetSchema.m_character_appearance (
            id           INTEGER PRIMARY KEY AUTO_INCREMENT,
            details      VARCHAR(255),
            character_id    INTEGER NOT NULL,
            story_id     INTEGER NOT NULL,
            notes        VARCHAR(255),
            membership   LONGTEXT,
            issue_id     INTEGER DEFAULT NULL,
            series_id    INTEGER REFERENCES gcd_series (id),
            FOREIGN KEY (character_id) REFERENCES m_character (id),
            FOREIGN KEY (story_id) REFERENCES gcd_story (id),
            FOREIGN KEY (issue_id) REFERENCES gcd_issue (id),
            FOREIGN KEY (series_id) REFERENCES gcd_series (id),
            INDEX (notes),
            INDEX (details),
            UNIQUE INDEX (details, character_id, story_id, notes)
            );""".trimIndent()

        queryExecutor.executeSqlStatement(statement, conn)
    }

    /** Add the 'm_story_credit' table if it does not exist. */
    internal fun createExtractedStoryCreditsTableIfNotExists() {
        val statement = """CREATE TABLE IF NOT EXISTS $targetSchema.m_story_credit LIKE gcd_story_credit;""".trimIndent()

        queryExecutor.executeSqlStatement(statement, conn)
    }

    /**
     * Add the primary key constraint to the 'm_story_credit' table if it does
     * not exist.
     */
    internal fun addExtractedStoryCreditsPrimaryKeyConstraintIfNotExists() {
        val query = """SELECT COUNT(*)
                        INTO @exist
                        FROM information_schema.key_column_usage
                        WHERE table_schema = '$targetSchema'
                        AND table_name = 'm_story_credit'
                        AND constraint_name = 'PRIMARY';
                        """.trimIndent()

        val statement = ifNotExistStatement(
            query,
            """ALTER TABLE $targetSchema.m_story_credit MODIFY COLUMN id INT PRIMARY KEY AUTO_INCREMENT""",
            """select 'Constraint Exists' status"""
        )

        queryExecutor.executeStatements(statement, conn)
    }

    /**
     * Add the 'creator_id' foreign key constraint to the 'm_story_credit'
     * table if it does not exist.
     */
    internal fun addExtractedStoryCreditsCreatorIdFKeyConstraintIfNotExists() {
        val query = """SELECT COUNT(*)
                        INTO @exist
                        FROM information_schema.key_column_usage
                        WHERE table_schema = '$targetSchema'
                        AND table_name = 'm_story_credit'
                        AND column_name = 'creator_id'
                        AND referenced_table_name = 'gcd_creator_name_detail'
                        AND referenced_column_name = 'id';
                        """.trimIndent()

        val statement = ifNotExistStatement(
            query,
            """ALTER TABLE $targetSchema.m_story_credit ADD FOREIGN KEY (creator_id) REFERENCES gcd_creator_name_detail (id)""",
            """select 'Constraint Exists' status"""
        )

        queryExecutor.executeStatements(statement, conn)
    }

    /**
     * Add the 'credit_type_id' foreign key constraint to the 'm_story_credit'
     * table if it does not exist.
     */
    internal fun addExtractedStoryCreditsRoleIdFKeyConstraintIfNotExists() {
        val query = """SELECT COUNT(*)
                        INTO @exist
                        FROM information_schema.key_column_usage
                        WHERE table_schema = '$targetSchema'
                        AND table_name = 'm_story_credit'
                        AND column_name = 'credit_type_id'
                        AND referenced_table_name = 'gcd_credit_type'
                        AND referenced_column_name = 'id';
                        """.trimIndent()

        val statement = ifNotExistStatement(
            query,
            """ALTER TABLE $targetSchema.m_story_credit ADD FOREIGN KEY (credit_type_id) REFERENCES gcd_credit_type (id)""",
            """select 'Constraint Exists' status"""
        )

        queryExecutor.executeStatements(statement, conn)
    }

    /**
     * Add the 'story_id' foreign key constraint to the 'm_story_credit' table
     * if it does not exist.
     */
    internal fun addExtractedStoryCreditsStoryIdFKeyConstraintIfNotExists() {
        val query = """SELECT COUNT(*)
                        INTO @exist
                        FROM information_schema.key_column_usage
                        WHERE table_schema = '$targetSchema'
                        AND table_name = 'm_story_credit'
                        AND column_name = 'story_id'
                        AND referenced_table_name = 'gcd_story'
                        AND referenced_column_name = 'id';
                        """.trimIndent()

        val statement = ifNotExistStatement(
            query,
            """ALTER TABLE $targetSchema.m_story_credit ADD FOREIGN KEY (story_id) REFERENCES gcd_story (id)""",
            """select 'Constraint Exists' status"""
        )

        queryExecutor.executeStatements(statement, conn)
    }

    /**
     * Add the 'issue_id' foreign key constraint to the 'm_story_credit' table
     * if it does not exist.
     */
    internal fun addExtractedStoryCreditsIssueIdFKeyConstraintIfNotExists() {
        val query = """SELECT COUNT(*)
                        INTO @exist
                        FROM information_schema.key_column_usage
                        WHERE table_schema = '$targetSchema'
                        AND table_name = 'm_story_credit'
                        AND column_name = 'issue_id'
                        AND referenced_table_name = 'gcd_issue'
                        AND referenced_column_name = 'id';
                        """.trimIndent()

        val statement = ifNotExistStatement(
            query,
            """ALTER TABLE $targetSchema.m_story_credit ADD FOREIGN KEY (issue_id) REFERENCES gcd_issue (id)""",
            """select 'Constraint Exists' status"""
        )

        queryExecutor.executeStatements(statement, conn)
    }

    /**
     * Add the 'series_id' foreign key constraint to the 'm_story_credit' table
     * if it does not exist.
     */
    internal fun addExtractedStoryCreditsSeriesIdFKeyConstraintIfNotExists() {
        val query = """SELECT COUNT(*)
                        INTO @exist
                        FROM information_schema.key_column_usage
                        WHERE table_schema = '$targetSchema'
                        AND table_name = 'm_story_credit'
                        AND column_name = 'series_id'
                        AND referenced_table_name = 'gcd_series'
                        AND referenced_column_name = 'id';
                        """.trimIndent()

        val statement = ifNotExistStatement(
            query,
            """ALTER TABLE $targetSchema.m_story_credit ADD FOREIGN KEY (series_id) REFERENCES gcd_series (id)""",
            """select 'Constraint Exists' status"""
        )

        queryExecutor.executeStatements(statement, conn)
    }

    private fun ifNotExistStatement(
        query: String,
        trueStatement: String,
        falseStatement: String
    ): List<String> {
        return listOf(
            query,
            """IF @exist <= 0 THEN
                    $trueStatement;
                ELSE
                    $falseStatement;
                END IF;"""
        )
    }
}
