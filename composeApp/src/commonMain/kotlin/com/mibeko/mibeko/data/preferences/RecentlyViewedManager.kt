package com.mibeko.mibeko.data.preferences

import com.mibeko.mibeko.util.recordException
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.mibeko.mibeko.getCurrentTimeMillis

@Serializable
data class RecentlyViewedItem(
    val id: String,
    val title: String,
    val typeCode: String,
    val timestamp: Long
)

class RecentlyViewedManager(private val settings: Settings = Settings()) {

    private val json = Json { ignoreUnknownKeys = true }
    private val _recentItems = MutableStateFlow<List<RecentlyViewedItem>>(emptyList())
    val recentItems: StateFlow<List<RecentlyViewedItem>> = _recentItems.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        val serialized = settings.getString(KEY_RECENTLY_VIEWED, "[]")
        try {
            val items = json.decodeFromString<List<RecentlyViewedItem>>(serialized)
            _recentItems.value = items
        } catch (e: Exception) {
            _recentItems.value = emptyList()
        }
    }

    private fun saveItems(items: List<RecentlyViewedItem>) {
        try {
            val serialized = json.encodeToString(items)
            settings.putString(KEY_RECENTLY_VIEWED, serialized)
            _recentItems.value = items
        } catch (e: Exception) {
            recordException(e, context = "RecentlyViewedManager.saveItems")
        }
    }

    fun addRecentlyViewed(id: String, title: String, typeCode: String) {
        val currentItems = _recentItems.value.toMutableList()
        
        // Remove if it already exists to put it at the top
        currentItems.removeAll { it.id == id }
        
        // Add to top
        val newItem = RecentlyViewedItem(
            id = id,
            title = title,
            typeCode = typeCode,
            timestamp = getCurrentTimeMillis()
        )
        currentItems.add(0, newItem)
        
        // Keep only max items
        if (currentItems.size > MAX_ITEMS) {
            currentItems.removeAt(currentItems.lastIndex)
        }
        
        saveItems(currentItems)
    }

    fun clearHistory() {
        saveItems(emptyList())
    }

    companion object {
        private const val KEY_RECENTLY_VIEWED = "recently_viewed_items"
        private const val MAX_ITEMS = 20
    }
}
