package com.oberon.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "oberon_prefs")

data class UsageStats(val last: Long = 0L, val count: Long = 0L)

class AppPrefs(private val context: Context) {

    private fun <T> prefs(key: androidx.datastore.preferences.core.Preferences.Key<T>, default: T): Flow<T> =
        context.dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[key] ?: default }

    // ---- Flows ----
    val themeMode: Flow<String> = prefs(stringPreferencesKey(Keys.THEME_MODE), "system")
    val accentOption: Flow<String> = prefs(stringPreferencesKey(Keys.ACCENT_OPTION), "custom")
    val accentColor: Flow<Long> = prefs(longPreferencesKey(Keys.ACCENT_COLOR), 0xFF6750A4)
    val gridColumns: Flow<Int> = prefs(intPreferencesKey(Keys.GRID_COLUMNS), 4)
    val drawerSort: Flow<String> = prefs(stringPreferencesKey(Keys.DRAWER_SORT), "alpha")
    val searchEngine: Flow<String> = prefs(stringPreferencesKey(Keys.SEARCH_ENGINE), "google")
    val hidden: Flow<Set<String>> = prefs(stringSetPreferencesKey(Keys.HIDDEN), emptySet())
    val badgesEnabled: Flow<Boolean> = prefs(booleanPreferencesKey(Keys.BADGES), true)
    val animSpeed: Flow<Float> = prefs(floatPreferencesKey(Keys.ANIM_SPEED), 1f)

    val favoritesJson: Flow<String> = prefs(stringPreferencesKey(Keys.FAVORITES), "[]")
    val usageJson: Flow<String> = prefs(stringPreferencesKey(Keys.USAGE), "{}")

    val favorites: Flow<List<String>> = favoritesJson.map { parseList(it) }
    val usage: Flow<Map<String, UsageStats>> = usageJson.map { parseUsage(it) }

    // ---- Setters ----
    suspend fun setThemeMode(value: String) = set(Keys.THEME_MODE, value)
    suspend fun setAccentOption(value: String) = set(Keys.ACCENT_OPTION, value)
    suspend fun setAccentColor(value: Long) = set(Keys.ACCENT_COLOR, value)
    suspend fun setGridColumns(value: Int) = set(Keys.GRID_COLUMNS, value)
    suspend fun setDrawerSort(value: String) = set(Keys.DRAWER_SORT, value)
    suspend fun setSearchEngine(value: String) = set(Keys.SEARCH_ENGINE, value)
    suspend fun setBadgesEnabled(value: Boolean) = set(Keys.BADGES, value)
    suspend fun setAnimSpeed(value: Float) = set(Keys.ANIM_SPEED, value)

    suspend fun setHidden(values: Set<String>) =
        context.dataStore.edit { it[stringSetPreferencesKey(Keys.HIDDEN)] = values }

    suspend fun setFavorites(values: List<String>) =
        context.dataStore.edit { it[stringPreferencesKey(Keys.FAVORITES)] = JSONObject().put("k", values).toString() }

    suspend fun recordLaunch(key: String) {
        context.dataStore.edit { prefs ->
            val json = JSONObject(prefs[stringPreferencesKey(Keys.USAGE)] ?: "{}")
            val entry = json.optJSONObject(key) ?: JSONObject()
            entry.put("l", System.currentTimeMillis())
            entry.put("c", entry.optLong("c") + 1)
            json.put(key, entry)
            prefs[stringPreferencesKey(Keys.USAGE)] = json.toString()
        }
    }

    suspend fun toggleHidden(packageName: String) {
        val latest = context.dataStore.data.first().let { it[stringSetPreferencesKey(Keys.HIDDEN)] ?: emptySet() }
        context.dataStore.edit {
            it[stringSetPreferencesKey(Keys.HIDDEN)] =
                if (packageName in latest) latest - packageName else latest + packageName
        }
    }

    suspend fun toggleFavorite(key: String) {
        val latest = parseList(context.dataStore.data.first()[stringPreferencesKey(Keys.FAVORITES)] ?: "[]")
        val next = if (key in latest) latest - key else latest + key
        context.dataStore.edit { it[stringPreferencesKey(Keys.FAVORITES)] = JSONObject().put("k", next).toString() }
    }

    private suspend fun <T> set(key: String, value: T) {
        context.dataStore.edit { prefs ->
            when (value) {
                is String -> prefs[stringPreferencesKey(key)] = value
                is Int -> prefs[intPreferencesKey(key)] = value
                is Long -> prefs[longPreferencesKey(key)] = value
                is Float -> prefs[floatPreferencesKey(key)] = value
                is Boolean -> prefs[booleanPreferencesKey(key)] = value
            }
        }
    }

    // ---- Backup / Reset ----
    suspend fun exportJson(): String {
        val data = context.dataStore.data.first()
        val out = JSONObject()
        fun str(key: String, def: String = "") = data[stringPreferencesKey(key)] ?: def
        out.put("v", 1)
        out.put("themeMode", str(Keys.THEME_MODE, "system"))
        out.put("accentOption", str(Keys.ACCENT_OPTION, "custom"))
        out.put("accentColor", data[longPreferencesKey(Keys.ACCENT_COLOR)] ?: 0xFF6750A4)
        out.put("gridColumns", data[intPreferencesKey(Keys.GRID_COLUMNS)] ?: 4)
        out.put("drawerSort", str(Keys.DRAWER_SORT, "alpha"))
        out.put("searchEngine", str(Keys.SEARCH_ENGINE, "google"))
        out.put("favorites", str(Keys.FAVORITES, "[]"))
        out.put("usage", str(Keys.USAGE, "{}"))
        out.put("hidden", JSONArray(data[stringSetPreferencesKey(Keys.HIDDEN)] ?: emptySet()))
        return out.toString()
    }

    suspend fun importJson(text: String) {
        val json = JSONObject(text)
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(Keys.THEME_MODE)] = json.optString("themeMode", "system")
            prefs[stringPreferencesKey(Keys.ACCENT_OPTION)] = json.optString("accentOption", "custom")
            prefs[longPreferencesKey(Keys.ACCENT_COLOR)] = json.optLong("accentColor", 0xFF6750A4)
            prefs[intPreferencesKey(Keys.GRID_COLUMNS)] = json.optInt("gridColumns", 4)
            prefs[stringPreferencesKey(Keys.DRAWER_SORT)] = json.optString("drawerSort", "alpha")
            prefs[stringPreferencesKey(Keys.SEARCH_ENGINE)] = json.optString("searchEngine", "google")
            prefs[stringPreferencesKey(Keys.FAVORITES)] = json.optString("favorites", "[]")
            prefs[stringPreferencesKey(Keys.USAGE)] = json.optString("usage", "{}")
            val hiddenArr = json.optJSONArray("hidden")
            val set = HashSet<String>()
            if (hiddenArr != null) {
                for (i in 0 until hiddenArr.length()) set.add(hiddenArr.optString(i))
            }
            prefs[stringSetPreferencesKey(Keys.HIDDEN)] = set
        }
    }

    suspend fun resetAll() = context.dataStore.edit { it.clear() }

    // ---- Helpers ----
    private fun parseList(json: String): List<String> {
        return runCatching {
            val arr = JSONObject(json).optJSONArray("k") ?: return emptyList()
            (0 until arr.length()).map { arr.optString(it) }
        }.getOrDefault(emptyList())
    }

    private fun parseUsage(json: String): Map<String, UsageStats> {
        return runCatching {
            val obj = JSONObject(json)
            val map = HashMap<String, UsageStats>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val e = obj.optJSONObject(k) ?: continue
                map[k] = UsageStats(e.optLong("l"), e.optLong("c"))
            }
            map
        }.getOrDefault(emptyMap())
    }

    private object Keys {
        const val THEME_MODE = "theme_mode"
        const val ACCENT_OPTION = "accent_option"
        const val ACCENT_COLOR = "accent_color"
        const val GRID_COLUMNS = "grid_columns"
        const val DRAWER_SORT = "drawer_sort"
        const val SEARCH_ENGINE = "search_engine"
        const val HIDDEN = "hidden"
        const val FAVORITES = "favorites_json"
        const val USAGE = "usage_json"
        const val BADGES = "badges"
        const val ANIM_SPEED = "anim_speed"
    }
}