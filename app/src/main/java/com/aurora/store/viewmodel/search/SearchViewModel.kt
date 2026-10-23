/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2021 Rahul Kumar Patel <whyorean@gmail.com>
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.aurora.extensions.TAG
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.exceptions.GooglePlayException
import com.aurora.gplayapi.helpers.contracts.SearchContract
import com.aurora.gplayapi.helpers.web.WebSearchHelper
import com.aurora.store.AuroraApp
import com.aurora.store.data.PageResult
import com.aurora.store.data.event.AuthEvent
import com.aurora.store.data.model.normalizeSearchQuery
import com.aurora.store.data.paging.GenericPagingSource.Companion.manualPager
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.SearchPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SearchViewModel @Inject constructor(
    val authProvider: AuthProvider,
    private val webSearchHelper: WebSearchHelper,
    private val searchPreferences: SearchPreferences
) : ViewModel() {

    private val contract: SearchContract
        get() = webSearchHelper

    private val _suggestions = MutableStateFlow<List<SearchSuggestEntry>>(emptyList())
    val suggestions = _suggestions.asStateFlow()
    private var suggestionJob: Job? = null
    private var suggestionRequest = 0

    val history = searchPreferences.observeHistory(viewModelScope)
    private val searchFilter = searchPreferences.observeFilters(viewModelScope)
    private val searchQueries = MutableSharedFlow<String>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchResults = searchQueries.flatMapLatest(::resultsFor)
        .onStart { emit(PagingData.empty()) }
        .cachedIn(viewModelScope)

    val apps = combine(searchFilter, searchResults) { filter, pagingData ->
        pagingData.filter { app -> filter.matches(app, authProvider.isAnonymous) }
    }.cachedIn(viewModelScope)

    fun submitSearch(query: String) {
        val normalized = normalizeSearchQuery(query) ?: return
        searchPreferences.addHistory(normalized)
        search(normalized)
    }

    fun removeHistory(query: String) = searchPreferences.removeHistory(query)

    fun clearHistory() = searchPreferences.clearHistory()

    fun search(query: String) {
        val normalized = normalizeSearchQuery(query) ?: return
        fetchSuggestions("")
        searchQueries.tryEmit(normalized)
    }

    private fun resultsFor(query: String): Flow<PagingData<App>> {
        var nextBundleUrl: String? = null
        val nextStreamUrls = mutableSetOf<String>()

        fun Collection<StreamCluster>.flatClusters(): List<App> = this.flatMap { streamCluster ->
            if (streamCluster.hasNext()) {
                nextStreamUrls.add(streamCluster.clusterNextPageUrl)
            }
            streamCluster.clusterAppList
        }.distinctBy { app -> app.packageName }

        return manualPager { page ->
            val items = try {
                when (page) {
                    1 -> {
                        nextStreamUrls.clear()
                        contract.searchResults(query)
                            .also { nextBundleUrl = it.streamNextPageUrl }
                            .streamClusters.values
                            .flatClusters()
                    }

                    else -> {
                        when {
                            nextStreamUrls.isNotEmpty() -> {
                                nextStreamUrls.map { nextPageStreamUrl ->
                                    contract.nextStreamCluster(query, nextPageStreamUrl)
                                }.also { nextStreamUrls.clear() }.flatClusters()
                            }

                            !nextBundleUrl.isNullOrBlank() -> {
                                contract.nextStreamBundle(query, nextBundleUrl!!)
                                    .also { nextBundleUrl = it.streamNextPageUrl }
                                    .streamClusters.values
                                    .flatClusters()
                            }

                            else -> emptyList()
                        }
                    }
                }
            } catch (exception: GooglePlayException.AuthException) {
                Log.w(TAG, "Search returned ${exception.code}, redirecting to Splash")
                AuroraApp.events.send(AuthEvent.SessionExpired())
                throw exception
            }
            PageResult(items)
        }.flow
    }

    fun fetchSuggestions(query: String) {
        suggestionJob?.cancel()
        val request = ++suggestionRequest
        _suggestions.value = emptyList()
        val normalized = normalizeSearchQuery(query) ?: return
        suggestionJob = viewModelScope.launch {
            delay(300)
            try {
                val entries = withContext(Dispatchers.IO) {
                    contract.searchSuggestions(normalized)
                        .filter { it.title.isNotBlank() }
                        .distinctBy { it.title }
                        .take(5)
                }
                ensureActive()
                if (request == suggestionRequest) _suggestions.value = entries
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: GooglePlayException.AuthException) {
                if (request == suggestionRequest) {
                    Log.w(TAG, "Search suggestions returned ${exception.code}")
                    AuroraApp.events.send(AuthEvent.SessionExpired())
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to fetch search suggestions", exception)
            }
        }
    }
}
