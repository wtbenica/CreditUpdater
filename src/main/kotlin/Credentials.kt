class Credentials {
    companion object {
        const val USERNAME = "dbadmin"
        const val PASSWORD = "773Gcdb!"
        const val PRIMARY_DATABASE = "gcdb2"
        const val NEW_DATABASE = "new_gcd_dump"

        const val CREDITS_STORY_START: Long = 2783735
        const val CREDITS_STORIES_COMPLETE: Long = 1617774
        const val CREDITS_STORY_START_NEW: Long = 0
        const val CREDITS_STORIES_COMPLETE_NEW: Long = 0
        const val COLLECTOR_LIMIT = 2500

        const val CHARACTER_STORY_START: Long = 0
        const val CHARACTER_STORIES_COMPLETE: Long = 0
        const val CHARACTER_STORY_START_NEW: Long = 0
        const val CHARACTER_STORIES_COMPLETE_NEW: Long = 0

        const val UPDATE_CREDITS = false
        const val UPDATE_DATABASE = false
        const val UPDATE_CHARACTERS = true

        const val LAST_UPDATED = "2021-01-31T00:00"

        const val ADD_MODIFY_TABLES_PATH = "./src/main/sql/my_tables.sql"
        const val SHRINK_DATABASE_PATH = "./src/main/sql/remove_records.sql"
        const val ADD_ISSUE_SERIES_TO_CREDITS_PATH = "src/main/sql/add_issue_series_to_credits.sql"

    }
}
