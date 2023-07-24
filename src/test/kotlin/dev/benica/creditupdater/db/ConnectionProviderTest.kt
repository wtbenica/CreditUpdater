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

package dev.benica.creditupdater.db

import dev.benica.creditupdater.Credentials.TEST_DATABASE
import dev.benica.creditupdater.Credentials.TEST_DATABASE_UPDATE
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ConnectionProviderTest {
    @Test
    @DisplayName("getConnection should return a new connection when no connection in connectionMap")
    fun shouldReturnANewConnection() {
        // Arrange
        val targetSchema = TEST_DATABASE

        // Act
        val connection = ConnectionProvider.getConnection(targetSchema)

        // Assert
        assertNotNull(connection)
    }

    @Test
    @DisplayName("getConnection should return the same connection when called multiple times with the same targetSchema")
    fun shouldReturnSameConnectionForSameTargetSchema() {
        // Arrange
        val targetSchema = TEST_DATABASE

        // Act
        val connection1 = ConnectionProvider.getConnection(targetSchema)
        val connection2 = ConnectionProvider.getConnection(targetSchema)

        // Assert
        assertEquals(connection1, connection2)
    }

    @Test
    @DisplayName("getConnection should return different connections for different targetSchemas")
    fun shouldReturnDifferentConnectionsForDifferentTargetSchemas() {
        // Arrange
        val targetSchema1 = TEST_DATABASE
        val targetSchema2 = TEST_DATABASE_UPDATE

        // Act
        val connection1 = ConnectionProvider.getConnection(targetSchema1)
        val connection2 = ConnectionProvider.getConnection(targetSchema2)

        // Assert
        assertNotNull(connection1)
        assertNotNull(connection2)
        assertNotEquals(connection1, connection2)
    }
}