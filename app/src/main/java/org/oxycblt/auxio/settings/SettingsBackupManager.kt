/*
 * Copyright (c) 2024 Auxio Project
 * SettingsBackupManager.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oxycblt.auxio.settings

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber as L

/**
 * Helper to export and import user preferences (including custom artist merges,
 * exclusion filters, UI themes, and playback configurations) to/from JSON.
 */
object SettingsBackupManager {
    private const val BACKUP_VERSION = 1
    private const val KEY_VERSION = "version"
    private const val KEY_APP = "app"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val KEY_PREFERENCES = "preferences"

    /**
     * Generate a default filename for exported settings.
     */
    fun generateBackupFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "auxio_settings_${dateFormat.format(Date())}.json"
    }

    /**
     * Export all SharedPreferences to the given URI in JSON format.
     */
    fun exportSettings(context: Context, uri: Uri): Result<Int> {
        return runCatching {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val allPrefs = sharedPreferences.all

            val prefsJson = JSONObject()
            var count = 0

            for ((key, value) in allPrefs) {
                if (key == null) continue
                when (value) {
                    is Boolean -> {
                        prefsJson.put(key, value)
                        count++
                    }
                    is Int -> {
                        prefsJson.put(key, value)
                        count++
                    }
                    is Long -> {
                        prefsJson.put(key, value)
                        count++
                    }
                    is Float -> {
                        prefsJson.put(key, value.toDouble())
                        count++
                    }
                    is String -> {
                        prefsJson.put(key, value)
                        count++
                    }
                    is Set<*> -> {
                        val array = JSONArray()
                        for (item in value) {
                            array.put(item?.toString() ?: "")
                        }
                        prefsJson.put(key, array)
                        count++
                    }
                }
            }

            val rootJson = JSONObject().apply {
                put(KEY_VERSION, BACKUP_VERSION)
                put(KEY_APP, "Auxio")
                put(KEY_TIMESTAMP, System.currentTimeMillis())
                put(KEY_PREFERENCES, prefsJson)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } ?: error("Failed to open output stream for URI: $uri")

            L.d("Exported $count preferences to $uri")
            count
        }
    }

    /**
     * Import SharedPreferences from the given JSON URI.
     */
    fun importSettings(context: Context, uri: Uri): Result<Int> {
        return runCatching {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
            } ?: error("Failed to open input stream for URI: $uri")

            val rootJson = JSONObject(content)
            val prefsJson = if (rootJson.has(KEY_PREFERENCES)) {
                rootJson.getJSONObject(KEY_PREFERENCES)
            } else {
                rootJson
            }

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            var count = 0

            sharedPreferences.edit {
                val keys = prefsJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key == KEY_VERSION || key == KEY_APP || key == KEY_TIMESTAMP) {
                        continue
                    }

                    val value = prefsJson.get(key)
                    when (value) {
                        is Boolean -> putBoolean(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> {
                            if (value == value.toLong().toDouble()) {
                                putInt(key, value.toInt())
                            } else {
                                putFloat(key, value.toFloat())
                            }
                        }
                        is String -> putString(key, value)
                        is JSONArray -> {
                            val set = mutableSetOf<String>()
                            for (i in 0 until value.length()) {
                                set.add(value.getString(i))
                            }
                            putStringSet(key, set)
                        }
                        else -> {
                            putString(key, value.toString())
                        }
                    }
                    count++
                }
            }

            L.d("Imported $count preferences from $uri")
            count
        }
    }
}
