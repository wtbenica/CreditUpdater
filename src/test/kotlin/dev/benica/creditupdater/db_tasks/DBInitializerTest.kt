package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.Credentials.Companion.TEST_DATABASE
import dev.benica.creditupdater.db.TestDatabaseSetup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName

class DBInitializerTest {

    @Test
    @DisplayName("Test prepareDb()")
    fun prepareDb() {
        val dbInitializer = DBInitializer(TEST_DATABASE, 1)
        CoroutineScope(Dispatchers.IO).launch {
            dbInitializer.prepareDb()

            // verify that everything has been prepared correctly

        }
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupAll() = TestDatabaseSetup.setup()

        @AfterAll
        @JvmStatic
        fun teardownALL() = TestDatabaseSetup.teardown()
    }
}