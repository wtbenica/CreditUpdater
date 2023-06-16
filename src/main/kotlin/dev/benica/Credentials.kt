package dev.benica

class Credentials {
    companion object {
        // Required privileges for Initializer:
        // - SELECT, INSERT, UPDATE, DELETE, ALTER, CREATE, DROP, CREATE VIEW
        const val USERNAME_INITIALIZER = "il_initializer"
        const val PASSWORD_INITIALIZER = "773Infinite!"

        // If either of these is changed, the sql scripts must be manually updated to match
        // IOW, don't change these
        const val PRIMARY_DATABASE = "inf_lb_test"
        const val INCOMING_DATABASE = "gcdb_temp"

        // dev.benica.CreditUpdater.PrimaryDatabaseInitializer
        const val CHARACTER_STORY_ID_START: Long = 128659
        const val CHARACTER_STORIES_NUM_COMPLETE: Long = 108709
        const val CREDITS_STORY_ID_START: Long = 0
        const val CREDITS_STORIES_NUM_COMPLETE: Long = 0

        const val UPDATE_DATABASE = false
        const val UPDATE_CHARACTERS = true
        const val UPDATE_CREDITS = true
        const val ADD_COLUMNS_TO_CREDITS = true

        // MIGRATE
        const val CREDITS_STORY_START_NEW: Long = 0
        const val CREDITS_STORIES_COMPLETE_NEW: Long = 0
        const val CHARACTER_STORY_START_NEW: Long = 0
        const val CHARACTER_STORIES_COMPLETE_NEW: Long = 0

        /**
         * This SQL query adds issue and series columns to the gcd_story_credit
         * table. It creates the m_character and m_character_appearance tables if
         * they don't already exist.
         */
        const val ADD_MODIFY_TABLES_PATH = "src/main/sql/add_tables.sql"

        /**
         * This script creates several views that filter out records from the
         * database based on certain criteria. These views are then used to delete
         * records from various tables in the database.
         */
        const val SHRINK_DATABASE_PRE_PATH = "src/main/sql/shrink_database_pre.sql"

        /**
         * This script creates several views that filter out records from the
         * database based on certain criteria. These views are then used to delete
         * records from various tables in the database. The script also updates
         * some records in the gcd_issue and gcd_series tables by setting their
         * variant_of_id, first_issue_id, and last_issue_id fields to NULL.
         * Finally, the script deletes records from the gcd_indicia_publisher,
         * gcd_brand_group, gcd_brand_emblem_group, gcd_brand_use, and
         * gcd_publisher tables based on certain criteria. Overall, this script is
         * used to limit the records in the database based on certain criteria.
         */
        const val SHRINK_DATABASE_PATH = "src/main/sql/shrink_database.sql"

        /**
         * This SQL query updates the gcd_story_credit and m_story_credit
         * tables by setting the issue_id and series_id columns to the
         * corresponding values from the gcd_issue and gcd_story tables. It
         * then creates a temporary table called story_with_missing_issue that
         * contains the IDs of stories with missing issue IDs. It deletes
         * all rows from the m_character_appearance table where the story_id
         * is in the story_with_missing_issue table. Finally, it updates the
         * m_character_appearance table by setting the issue_id and series_id
         * columns to the corresponding values from the gcd_issue and gcd_story
         * tables. The comments in the code provide a summary of each section of
         * the query.
         */
        const val ADD_ISSUE_SERIES_TO_CREDITS_PATH = "src/main/sql/add_issue_series_to_credits.sql"
    }
}
