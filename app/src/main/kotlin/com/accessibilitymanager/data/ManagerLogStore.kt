package com.accessibilitymanager.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

enum class ManagerLogLevel {
    INFO,
    SUCCESS,
    ERROR,
}

data class ManagerLogEntry(
    val timestamp: Long,
    val level: ManagerLogLevel,
    val message: String,
)

class ManagerLogStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun load(): List<ManagerLogEntry> = decode(preferences.getString(KEY_ENTRIES, null))

    @Synchronized
    fun append(level: ManagerLogLevel, message: String): List<ManagerLogEntry> {
        val entries = (load() + ManagerLogEntry(System.currentTimeMillis(), level, message))
            .takeLast(MAX_ENTRIES)
        persist(entries)
        return entries
    }

    @Synchronized
    fun clear() {
        preferences.edit { remove(KEY_ENTRIES) }
    }

    private fun persist(entries: List<ManagerLogEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("timestamp", entry.timestamp)
                    .put("level", entry.level.name)
                    .put("message", entry.message),
            )
        }
        preferences.edit { putString(KEY_ENTRIES, array.toString()) }
    }

    private fun decode(raw: String?): List<ManagerLogEntry> = runCatching {
        val array = JSONArray(raw ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val message = item.optString("message").trim()
                if (message.isEmpty()) continue
                val level = runCatching {
                    ManagerLogLevel.valueOf(item.optString("level"))
                }.getOrDefault(ManagerLogLevel.INFO)
                add(
                    ManagerLogEntry(
                        timestamp = item.optLong("timestamp", 0L),
                        level = level,
                        message = message,
                    ),
                )
            }
        }.takeLast(MAX_ENTRIES)
    }.getOrDefault(emptyList())

    private companion object {
        const val PREFERENCES_NAME = "manager_operation_log"
        const val KEY_ENTRIES = "entries"
        const val MAX_ENTRIES = 200
    }
}
