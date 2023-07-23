import dev.benica.creditupdater.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class InteractiveStartupTest {

    @Test
    fun testStartWithInitializeDatabase() {
        // Given
        val input = "I\nN\nTestDB\nTestUser\nTestPassword\n123"
        val expectedArguments = StartupArguments(
            DatabaseTask.INITIALIZE,
            ExtractedType.NEW,
            "TestDB",
            null,
            "TestUser",
            "TestPassword",
            123
        )

        // When
        val output = captureOutput {
            val inputStream = ByteArrayInputStream(input.toByteArray())
            System.setIn(inputStream)
            val arguments = InteractiveStartup.start()
            assertEquals(expectedArguments, arguments)
        }

        // verify output
        assertEquals(
            "Starting in interactive mode\n" +
            "Initialize database or Migrate database? (I/M) [I]: \n" +
            "New, Characters or Credits? (N/H/R) [N]: \n" +
            "Database name [${Credentials.PRIMARY_DATABASE}]: \n" +
            "Username [${Credentials.USERNAME_INITIALIZER}]: \n" +
            "Password [${Credentials.PASSWORD_INITIALIZER}]: \n" +
            "Starting story id [0]: \n",
            output
        )
    }

    @Test
    fun testStartWithDefaultValues() {
        // Given
        val input = "\n\n\n\n\n\n"
        val expectedArguments = StartupArguments(
            DatabaseTask.INITIALIZE,
            ExtractedType.NEW,
            Credentials.PRIMARY_DATABASE,
            null,
            Credentials.USERNAME_INITIALIZER,
            Credentials.PASSWORD_INITIALIZER,
            0
        )

        // When
        val output = captureOutput {
            val inputStream = ByteArrayInputStream(input.toByteArray())
            System.setIn(inputStream)
            val arguments = InteractiveStartup.start()
            assertEquals(expectedArguments, arguments)
        }

        // verify output
        assertEquals(
            "Starting in interactive mode\n" +
            "Initialize database or Migrate database? (I/M) [I]: \n" +
            "New, Characters or Credits? (N/H/R) [N]: \n" +
            "Database name [${Credentials.PRIMARY_DATABASE}]: \n" +
            "Username [${Credentials.USERNAME_INITIALIZER}]: \n" +
            "Password [${Credentials.PASSWORD_INITIALIZER}]: \n" +
            "Starting story id [0]: \n",
            output
        )
    }

    @Test
    fun testStartWithMigrateDatabase() {
        // Given
        val input = "M\nSourceDB\nTargetDB\nTestUser\nTestPassword\n456"
        val expectedArguments = StartupArguments(
            DatabaseTask.MIGRATE,
            null,
            "SourceDB",
            "TargetDB",
            "TestUser",
            "TestPassword",
            456
        )

        // When
        val output = captureOutput {
            val inputStream = ByteArrayInputStream(input.toByteArray())
            System.setIn(inputStream)
            val arguments = InteractiveStartup.start()
            assertEquals(expectedArguments, arguments)
        }

        // verify output
        assertEquals(
            "Starting in interactive mode\n" +
            "Initialize database or Migrate database? (I/M) [I]: \n" +
            "Source database name [${Credentials.PRIMARY_DATABASE}]: \n" +
            "Target database name [${Credentials.INCOMING_DATABASE}]: \n" +
            "Username [${Credentials.USERNAME_INITIALIZER}]: \n" +
            "Password [${Credentials.PASSWORD_INITIALIZER}]: \n" +
            "Starting story id [0]: \n",
            output
        )
    }

    private fun captureOutput(block: () -> Unit): String {
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
        block()
        return outputStream.toString()
    }
}