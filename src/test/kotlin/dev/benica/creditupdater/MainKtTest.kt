/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import dev.benica.creditupdater.Credentials.TEST_DATABASE
import dev.benica.creditupdater.Credentials.TEST_DATABASE_UPDATE
import dev.benica.creditupdater.db.DBState
import dev.benica.creditupdater.db.QueryExecutor
import dev.benica.creditupdater.db.TestDatabaseSetup
import dev.benica.creditupdater.db.TestDatabaseSetup.Companion.getTestDbConnection
import dev.benica.creditupdater.db_tasks.DBInitializerTest.Companion.verifyDbPrepared
import dev.benica.creditupdater.db_tasks.DBMigratorTest.Companion.verifyCharactersHaveBeenMigrated
import dev.benica.creditupdater.db_tasks.DBMigratorTest.Companion.verifyCreditsHaveBeenMigrated
import dev.benica.creditupdater.db_tasks.DBMigratorTest.Companion.verifyTargetTablesAreUpdated
import dev.benica.creditupdater.main
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class MainTest {

    @Test
    @DisplayName("should correctly initialize TEST_DATABASE with interactive startup")
    fun testMainWithInteractiveStartup() {
        // Setup
        TestDatabaseSetup.setup(
            dbState = DBState.INITIAL,
            schema = TEST_DATABASE,
            sourceSchema = null
        )

        // Given
        val args = arrayOf("-i")
        val input = "I\nN\n$TEST_DATABASE\n\n\n0"

        // When
        val output = captureOutput {
            val inputStream = ByteArrayInputStream(input.toByteArray())
            System.setIn(inputStream)
            main(args)
        }

        // Then
        verifyDbPrepared(QueryExecutor(), getTestDbConnection())
    }

    @Test
    @DisplayName("should correctly migrate from TEST_DATABASE_UPDATE to TEST_DATABASE with interactive startup")
    fun testMainWithInteractiveStartupAndMigrate() {
        // Setup
        TestDatabaseSetup.setup(
            dbState = DBState.INITIALIZED,
            schema = TEST_DATABASE,
            sourceSchema = null
        )

        TestDatabaseSetup.setup(
            dbState = DBState.MIGRATE_INITIAL,
            schema = TEST_DATABASE,
            sourceSchema = TEST_DATABASE_UPDATE
        )

        // Given
        val args = arrayOf("-i")
        val input = "M\n$TEST_DATABASE_UPDATE\n$TEST_DATABASE\n\n\n0\n"

        // When
        captureOutput {
            val inputStream = ByteArrayInputStream(input.toByteArray())
            System.setIn(inputStream)

            runBlocking {
                val job = CoroutineScope(Dispatchers.IO).launch {
                    main(args)
                }

                job.join()

                Thread.sleep(5000)

                // Then
                val qe = QueryExecutor()
                val conn = getTestDbConnection()
                verifyTargetTablesAreUpdated(qe, conn)
                verifyCharactersHaveBeenMigrated(qe, conn)
                verifyCreditsHaveBeenMigrated(qe, conn)
            }
        }
    }

    @Test
    @DisplayName("should correctly initialize TEST_DATABASE with command line arguments")
    fun testMainWithCommandLineArguments() {
        // Setup
        TestDatabaseSetup.setup(
            dbState = DBState.INITIAL,
            schema = TEST_DATABASE,
            sourceSchema = null
        )

        // Given
        val args = arrayOf(
            "-p", TEST_DATABASE,
            "-n", "0"
        )

        // When
        runBlocking {
            val job = CoroutineScope(Dispatchers.IO).launch {
                val output = captureOutput {
                    main(args)
                }
            }

            job.join()

            Thread.sleep(5000)

            // Then
            verifyDbPrepared(QueryExecutor(), getTestDbConnection())
        }
    }

    @Test
    @DisplayName("should correctly migrate from TEST_DATABASE_UPDATE to TEST_DATABASE with command line arguments")
    fun testMainWithCommandLineArgumentsAndMigrate() {
        // Setup
        TestDatabaseSetup.setup(
            dbState = DBState.INITIALIZED,
            schema = TEST_DATABASE,
            sourceSchema = null
        )

        TestDatabaseSetup.setup(
            dbState = DBState.MIGRATE_INITIAL,
            schema = TEST_DATABASE,
            sourceSchema = TEST_DATABASE_UPDATE
        )

        // Given
        val args = arrayOf(
            "-m", TEST_DATABASE_UPDATE, TEST_DATABASE,
            "-n", "0"
        )

        // When
        runBlocking {
            val job = CoroutineScope(Dispatchers.IO).launch {
                val output = captureOutput {
                    main(args)
                }
            }

            job.join()

            Thread.sleep(5000)

            // Then
            val qe = QueryExecutor()
            val conn = getTestDbConnection()
            verifyTargetTablesAreUpdated(qe, conn)
            verifyCharactersHaveBeenMigrated(qe, conn)
            verifyCreditsHaveBeenMigrated(qe, conn)
        }
    }

    @Test
    fun testMainWithInvalidArgument() {
        // Given
        val args = arrayOf("-x")

        // When
        val output = captureOutput {
            main(args)
        }

        // Then
        println(output)
        assertTrue("Unrecognized argument: -x" in output.trim())
    }

    private fun captureOutput(block: () -> Unit): String {
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
        block()
        return outputStream.toString()
    }
}