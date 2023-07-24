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

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.benica.creditupdater.Credentials

class ConnectionProvider {
    companion object {
        private val connectionMap: MutableMap<String, HikariDataSource> = mutableMapOf()

        fun getConnection(targetSchema: String): HikariDataSource {
            if (!connectionMap.containsKey(targetSchema) || connectionMap[targetSchema]?.isClosed == true) {
                connectionMap[targetSchema] = createConnection(targetSchema)
            }
            @Suppress("kotlin:S6611")
            return connectionMap[targetSchema]!!
        }

        private fun createConnection(targetSchema: String): HikariDataSource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:mysql://localhost:3306/$targetSchema"
                username = Credentials.USERNAME_INITIALIZER
                password = Credentials.PASSWORD_INITIALIZER
                driverClassName = "com.mysql.cj.jdbc.Driver"
                maximumPoolSize = MAX_CONNECTION_POOL_SIZE
                minimumIdle = 5
                idleTimeout = 10000
                connectionTimeout = 5000
            }
        )

        const val MAX_CONNECTION_POOL_SIZE = 100
    }
}