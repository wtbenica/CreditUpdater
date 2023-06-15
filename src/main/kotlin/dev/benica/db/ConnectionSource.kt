package dev.benica.db

import java.sql.Connection

abstract class ConnectionSource {
    abstract fun getConnection(database: String): Connection
}