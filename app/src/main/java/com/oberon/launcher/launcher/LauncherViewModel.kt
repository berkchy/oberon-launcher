package com.oberon.launcher.launcher

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oberon.launcher.badge.BadgeStore
import com.oberon.launcher.data.AppInfo
import com.oberon.launcher.data.AppPrefs
import com.oberon.launcher.data.AppRepository
import com.oberon.launcher.data.IconPackLoader
import com.oberon.launcher.data.UsageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPrefs(application)
    private val repository = AppRepository(application)

    private val iconPackLoader = runCatching { IconPackLoader(application) }.getOrNull()

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    val themeMode = prefs.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
    val accentOption = prefs.accentOption.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "custom")
    val accentColor = prefs.accentColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF6750A4)
    val gridColumns = prefs.gridColumns.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)
    val drawerSort = prefs.drawerSort.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "alpha")
    val searchEngine = prefs.searchEngine.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "google")
    val badgesEnabled = prefs.badgesEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val hidden = prefs.hidden.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val favorites = prefs.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val usage = prefs.usage.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val badges = BadgeStore.badges

    var iconPackName = MutableStateFlow<String?>(null)
        private set

    val visibleApps = combine(_apps, hidden) { apps, hid ->
        apps.filter { it.packageName !in hid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedVisibleApps = combine(visibleApps, drawerSort, usage) { list, sort, use ->
        when (sort) {
            "recent" -> list.sortedByDescending { use[it.key]?.last ?: 0L }
            "most" -> list.sortedByDescending { use[it.key]?.count ?: 0L }
            else -> list.sortedBy { it.label.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            iconPackName.value = iconPackLoader?.installedPackName
        }
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _apps.value = withContext(Dispatchers.IO) { repository.loadApps() }
        }
    }

    fun packIconFor(packageName: String): android.graphics.drawable.Drawable? =
        iconPackLoader?.iconFor(packageName)

    fun launch(app: AppInfo) {
        val ok = repository.launchApp(app.key)
        if (ok) {
            viewModelScope.launch { prefs.recordLaunch(app.key) }
        }
    }

    fun webSearch(query: String, engine: String? = null) {
        val base = when (engine) {
            "bing" -> "https://www.bing.com/search?q="
            "duckduckgo" -> "https://duckduckgo.com/?q="
            else -> "https://www.google.com/search?q="
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(base + Uri.encode(query)))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { getApplication<Application>().startActivity(intent) }
    }

    fun openAppInfo(app: AppInfo) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${app.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { getApplication<Application>().startActivity(intent) }
    }

    fun addFavorite(key: String) = viewModelScope.launch { prefs.toggleFavorite(key) }

    fun removeFavorite(key: String) {
        viewModelScope.launch {
            prefs.setFavorites(prefs.favorites.first().filterNot { it == key })
        }
    }

    fun toggleHidden(packageName: String) = viewModelScope.launch { prefs.toggleHidden(packageName) }

    fun setThemeMode(value: String) = viewModelScope.launch { prefs.setThemeMode(value) }
    fun setAccentOption(value: String) = viewModelScope.launch { prefs.setAccentOption(value) }
    fun setAccentColor(value: Long) = viewModelScope.launch { prefs.setAccentColor(value) }
    fun setGridColumns(value: Int) = viewModelScope.launch { prefs.setGridColumns(value) }
    fun setDrawerSort(value: String) = viewModelScope.launch { prefs.setDrawerSort(value) }
    fun setSearchEngine(value: String) = viewModelScope.launch { prefs.setSearchEngine(value) }
    fun setBadgesEnabled(value: Boolean) = viewModelScope.launch { prefs.setBadgesEnabled(value) }

    suspend fun exportJson(): String = prefs.exportJson()

    suspend fun importJson(text: String) = prefs.importJson(text)

    suspend fun resetAll() = prefs.resetAll()
}