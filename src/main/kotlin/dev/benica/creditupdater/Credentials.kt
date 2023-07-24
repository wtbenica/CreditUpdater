package dev.benica.creditupdater

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object Credentials {
    private val config: MutableMap<String, String> by lazy {
        val gson = Gson()
        val configFile = File("cu_config.json")
        if (!configFile.exists()) {
            throw Exception("Config file cu_config.json not found.")
        }
        configFile.reader().use { reader ->
            gson.fromJson(reader, object : TypeToken<MutableMap<String, String>>() {}.type)
        }
    }

    val USERNAME_INITIALIZER: String by lazy { getConfigValue("username") }
    val PASSWORD_INITIALIZER: String by lazy { getConfigValue("password") }
    val TEST_DATABASE: String by lazy { getConfigValue("test_database") }
    val TEST_DATABASE_UPDATE: String by lazy { getConfigValue("update_database") }
    val PRIMARY_DATABASE: String by lazy { getConfigValue("primary_database") }
    val INCOMING_DATABASE: String by lazy { getConfigValue("incoming_database") }
    val CHARACTER_STORY_START_ID: Int by lazy { getConfigValue("characters_starting_story_id").toInt() }
    val CREDITS_STORY_START_ID: Int by lazy { getConfigValue("credits_starting_story_id").toInt() }

    private fun getConfigValue(key: String): String {
        return config[key] ?: throw Exception("Config value for '$key' not found.")
    }
}