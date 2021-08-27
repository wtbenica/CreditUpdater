class Credentials {
    companion object {
        const val USERNAME = "dbadmin"
        const val PASSWORD = "773Gcdb!"
        
        // If either of these is changed, the sql scripts must be manually updated to match
        // IOW, don't change these
        const val PRIMARY_DATABASE = "gcdb2"
        const val NEW_DATABASE = "new_gcd_dump"

        // UPDATE
        const val CREDITS_STORY_ID_START: Long = 2783735
        const val CREDITS_STORIES_NUM_COMPLETE: Long = 1617774
        const val CHARACTER_STORY_ID_START: Long = 0
        const val CHARACTER_STORIES_NUM_COMPLETE: Long = 0

        const val UPDATE_CREDITS = false
        const val UPDATE_DATABASE = false
        const val UPDATE_CHARACTERS = true

        // MIGRATE
        const val CREDITS_STORY_START_NEW: Long = 0
        const val CREDITS_STORIES_COMPLETE_NEW: Long = 0
        const val CHARACTER_STORY_START_NEW: Long = 0
        const val CHARACTER_STORIES_COMPLETE_NEW: Long = 0

        const val COLLECTOR_LIMIT = 2500

        const val ADD_MODIFY_TABLES_PATH = "./src/main/sql/my_tables.sql"
        const val SHRINK_DATABASE_PATH = "./src/main/sql/remove_records.sql"
        const val ADD_ISSUE_SERIES_TO_CREDITS_PATH = "src/main/sql/add_issue_series_to_credits.sql"

    }
}
