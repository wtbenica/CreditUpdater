package dev.benica.creditupdater.db

import java.sql.Connection

abstract class ConnectionSource {
    abstract fun getConnection(database: String): Connection
}