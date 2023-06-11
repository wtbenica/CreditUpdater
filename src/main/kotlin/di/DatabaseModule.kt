package di

import Credentials.Companion.PASSWORD
import Credentials.Companion.USERNAME
import dagger.Component
import dagger.Module
import dagger.Provides
import db.ConnectionSource
import db.DatabaseUtil
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

@Module
class DatabaseModule() {
    @Provides
    fun provideConnectionSource(): ConnectionSource {
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
fun interface DatabaseComponent {
    fun inject(databaseUtil: DatabaseUtil)
}