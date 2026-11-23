package com.cylonid.nativealpha.ui.webapplist

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.util.ShortcutIconUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WebAppListUiState(
    val allWebApps: List<WebApp> = emptyList(),
    val filteredWebApps: List<WebApp> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = false,
    val lastDeletedWebApp: Triple<WebApp, Int, Bitmap?>? = null,
)

class WebAppListViewModel(
    private val dataManager: DataManager = DataManager.getInstance(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(WebAppListUiState())
    val uiState: StateFlow<WebAppListUiState> = _uiState.asStateFlow()

    init {
        loadWebApps()
    }

    fun loadWebApps() {
        val active = dataManager.activeWebsites.sortedBy { it.order }
        _uiState.update { current ->
            current.copy(
                allWebApps = active,
                filteredWebApps = filterList(active, current.searchQuery),
                isLoading = false,
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredWebApps = filterList(current.allWebApps, query),
            )
        }
    }

    fun setSearchActive(active: Boolean) {
        _uiState.update { current ->
            current.copy(
                isSearchActive = active,
                searchQuery = if (!active) "" else current.searchQuery,
                filteredWebApps = if (!active) current.allWebApps else current.filteredWebApps,
            )
        }
    }

    fun reorderWebApps(
        fromIndex: Int,
        toIndex: Int,
    ) {
        _uiState.update { current ->
            val list = current.filteredWebApps.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                current.copy(
                    filteredWebApps = list,
                    allWebApps = if (current.searchQuery.isBlank()) list else current.allWebApps,
                )
            } else {
                current
            }
        }
    }

    fun commitReorder() {
        viewModelScope.launch {
            val currentList = _uiState.value.allWebApps
            for ((index, webApp) in currentList.withIndex()) {
                val stored = dataManager.websites.firstOrNull { it.ID == webApp.ID }
                if (stored != null) {
                    stored.order = index
                }
            }
            dataManager.saveWebAppData()
        }
    }

    fun deleteWebApp(
        context: Context,
        webApp: WebApp,
    ) {
        val originalIndex = _uiState.value.allWebApps.indexOfFirst { it.ID == webApp.ID }
        val cachedIcon = ShortcutIconUtils.getIcon(context, webApp.ID)
        val storedApp = dataManager.websites.firstOrNull { it.ID == webApp.ID }
        if (storedApp != null) {
            storedApp.markInactive(context)
            webApp.isActiveEntry = false
        } else {
            webApp.markInactive(context)
        }

        val updatedActive =
            dataManager.websites
                .filter { it.isActiveEntry && it.ID != webApp.ID }
                .sortedBy { it.order }

        for ((index, app) in updatedActive.withIndex()) {
            app.order = index
        }
        dataManager.saveWebAppData()

        _uiState.update { current ->
            current.copy(
                allWebApps = updatedActive,
                filteredWebApps = filterList(updatedActive, current.searchQuery),
                lastDeletedWebApp =
                    if (originalIndex >= 0) {
                        Triple(webApp, originalIndex, cachedIcon)
                    } else {
                        null
                    },
            )
        }
    }

    fun undoDelete(context: Context? = null) {
        val lastDeleted = _uiState.value.lastDeletedWebApp ?: return
        val webApp = lastDeleted.first
        val originalIndex = lastDeleted.second
        val cachedIcon = lastDeleted.third
        webApp.isActiveEntry = true
        val storedApp =
            dataManager.websites.firstOrNull { it.ID == webApp.ID }
                ?: webApp.also { dataManager.websites.add(it) }
        storedApp.isActiveEntry = true

        val otherActive =
            dataManager.websites
                .filter { it.isActiveEntry && it.ID != storedApp.ID }
                .sortedBy { it.order }
                .toMutableList()
        val insertIndex = originalIndex.coerceIn(0, otherActive.size)
        otherActive.add(insertIndex, storedApp)
        for ((index, app) in otherActive.withIndex()) {
            app.order = index
        }
        dataManager.saveWebAppData()

        if (context != null && cachedIcon != null) {
            ShortcutIconUtils.saveIcon(context, webApp.ID, cachedIcon)
        }

        loadWebApps()
        _uiState.update { it.copy(lastDeletedWebApp = null) }
    }

    fun addWebApp(
        url: String,
        context: Context? = null,
    ): WebApp {
        val trimmed = url.trim()
        val urlWithProtocol =
            if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
                trimmed
            } else {
                "https://$trimmed"
            }

        val newSite =
            WebApp(
                urlWithProtocol,
                dataManager.incrementedID,
                dataManager.incrementedOrder,
            )
        newSite.applySettingsForNewWebApp()
        dataManager.addWebsite(newSite)

        if (context != null) {
            viewModelScope.launch(Dispatchers.IO) {
                ShortcutIconUtils.autoFetchAndSaveFavicon(context, newSite.ID, newSite.baseUrl)
            }
        }

        loadWebApps()
        return newSite
    }

    private fun filterList(
        list: List<WebApp>,
        query: String,
    ): List<WebApp> {
        if (query.isBlank()) return list
        val lower = query.lowercase().trim()
        return list.filter { app ->
            app.title.lowercase().contains(lower) ||
                app.baseUrl.lowercase().contains(lower)
        }
    }
}
