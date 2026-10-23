/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.providers

import android.content.Context
import androidx.core.content.edit
import com.aurora.extensions.observeAsStateFlow
import com.aurora.store.data.model.SearchFilter
import com.aurora.store.data.model.addSearchHistory
import com.aurora.store.data.model.normalizeSearchHistory
import com.aurora.store.data.model.removeSearchHistory
import com.aurora.store.util.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

@Singleton
class SearchPreferences @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json
) {
    private val preferences = Preferences.getPrefs(context)

    private fun readHistory(): List<String> = runCatching {
        val history = json.decodeFromString<List<String>>(preferences.getString(HISTORY, "[]")!!)
        normalizeSearchHistory(history)
    }.getOrDefault(emptyList())

    private fun readFilters(): SearchFilter = runCatching {
        json.decodeFromString<SearchFilter>(preferences.getString(FILTERS, "{}")!!).normalized()
    }.getOrDefault(SearchFilter())

    fun observeHistory(scope: CoroutineScope) = preferences.observeAsStateFlow(
        key = HISTORY,
        scope = scope,
        initial = readHistory(),
        valueProvider = ::readHistory
    )

    fun observeFilters(scope: CoroutineScope) = preferences.observeAsStateFlow(
        key = FILTERS,
        scope = scope,
        initial = readFilters(),
        valueProvider = ::readFilters
    )

    fun addHistory(query: String) {
        val history = addSearchHistory(readHistory(), query)
        preferences.edit { putString(HISTORY, json.encodeToString(history)) }
    }

    fun removeHistory(query: String) {
        val history = removeSearchHistory(readHistory(), query)
        preferences.edit { putString(HISTORY, json.encodeToString(history)) }
    }

    fun clearHistory() {
        preferences.edit { remove(HISTORY) }
    }

    fun saveFilters(filter: SearchFilter) {
        preferences.edit { putString(FILTERS, json.encodeToString(filter.normalized())) }
    }

    private companion object {
        const val HISTORY = "PREFERENCE_SEARCH_HISTORY"
        const val FILTERS = "PREFERENCE_SEARCH_FILTERS"
    }
}
