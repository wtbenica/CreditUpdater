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