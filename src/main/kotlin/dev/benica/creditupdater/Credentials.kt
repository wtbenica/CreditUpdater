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
        const val TEST_DATABASE = "credit_updater_test"
        const val TEST_DATABASE_UPDATE = "credit_updater_test_update"

        // dev.benica.CreditUpdater.PrimaryDatabaseInitializer
        const val CHARACTER_STORY_START_ID: Long = 0
        const val CREDITS_STORY_START_ID: Long = 0

        // MIGRATE
        const val CREDITS_STORY_START_NEW: Long = 0
        const val CREDITS_STORIES_COMPLETE_NEW: Long = 0
        const val CHARACTER_STORY_START_NEW: Long = 0
        const val CHARACTER_STORIES_COMPLETE_NEW: Long = 0
    }
}
