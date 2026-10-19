package com.example.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class VideoPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("nova_player_prefs", Context.MODE_PRIVATE)

    fun getFavoriteIds(): Set<String> {
        return prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
    }

    fun toggleFavorite(id: String): Boolean {
        val current = getFavoriteIds().toMutableSet()
        val isFav = if (current.contains(id)) {
            current.remove(id)
            false
        } else {
            current.add(id)
            true
        }
        prefs.edit().putStringSet("favorite_ids", current).apply()
        return isFav
    }

    fun isFavorite(id: String): Boolean {
        return getFavoriteIds().contains(id)
    }

    fun savePlaybackPosition(id: String, positionMs: Long) {
        val historyJson = prefs.getString("history_records", "{}") ?: "{}"
        try {
            val json = JSONObject(historyJson)
            val record = JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("positionMs", positionMs)
            }
            json.put(id, record)
            prefs.edit().putString("history_records", json.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getPlaybackHistory(): Map<String, Pair<Long, Long>> {
        // Map of id -> (timestamp, positionMs)
        val result = mutableMapOf<String, Pair<Long, Long>>()
        val historyJson = prefs.getString("history_records", "{}") ?: "{}"
        try {
            val json = JSONObject(historyJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val record = json.getJSONObject(key)
                val ts = record.optLong("timestamp", 0L)
                val pos = record.optLong("positionMs", 0L)
                result[key] = Pair(ts, pos)
            }
        } catch (_: Exception) {}
        return result
    }

    fun clearHistory() {
        prefs.edit().remove("history_records").apply()
    }
}
