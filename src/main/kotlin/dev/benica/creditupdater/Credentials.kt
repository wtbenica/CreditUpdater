package dev.benica.creditupdater

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
        const val CHARACTER_STORY_START_ID: Long = 0
        const val CREDITS_STORY_START_ID: Long = 0

        // MIGRATE
        const val CREDITS_STORY_START_NEW: Long = 0
        const val CREDITS_STORIES_COMPLETE_NEW: Long = 0
        const val CHARACTER_STORY_START_NEW: Long = 0
        const val CHARACTER_STORIES_COMPLETE_NEW: Long = 0

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
        const val ISSUE_SERIES_PATH = "src/main/resources/sql/add_issue_series_to_credits.sql"
    }
}
