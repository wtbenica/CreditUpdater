package dev.benica.di

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import dagger.Module
import dagger.Provides
import dev.benica.Credentials.Companion.PASSWORD
import dev.benica.Credentials.Companion.USERNAME
import dev.benica.db.ConnectionSource
import dev.benica.db.DatabaseUtil
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import javax.inject.Singleton

@Module
class DatabaseModule() {
    @Provides
    @Singleton
    fun provideConnectionSource(): ConnectionSource {
        return object : ConnectionSource() {
            override fun getConnection(database: String): Connection {
                return HikariDataSource(
                    HikariConfig().apply {
                        jdbcUrl = "jdbc:mysql://localhost:3306/$database"
                        username = USERNAME
                        password = PASSWORD
                        driverClassName = "com.mysql.cj.jdbc.Driver"
                        maximumPoolSize = 10
                        minimumIdle = 2
                        idleTimeout = 3000
                        connectionTimeout = 5000
                    }
                ).connection
            }
        }
    }

    @Provides
    @Singleton
    fun provideConnectionSourceOld(): ConnectionSource {
        val connectionProps = Properties()
        connectionProps["user"] = USERNAME
        connectionProps["password"] = PASSWORD
        return object : ConnectionSource() {
            override fun getConnection(database: String): Connection {
                return DriverManager.getConnection(
                    "jdbc:mysql://127.0.0.1:3306/$database",
                    connectionProps
                )
            }
        }
    }
}

@Component(modules = [DatabaseModule::class])
@Singleton
fun interface DatabaseComponent {
    fun inject(databaseUtil: DatabaseUtil)
}

