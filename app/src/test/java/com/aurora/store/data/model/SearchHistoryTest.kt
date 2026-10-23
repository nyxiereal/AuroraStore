/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchHistoryTest {
    @Test
    fun blankQueriesAreNotSubmittedOrSaved() {
        assertNull(normalizeSearchQuery(" \t\n"))
        assertEquals(listOf("Firefox"), addSearchHistory(listOf("Firefox"), "  "))
    }

    @Test
    fun queryTrimsEdgesWithoutChangingSearchSyntax() {
        assertEquals("pub:The Tor Project", normalizeSearchQuery("  pub:The Tor Project\n"))
        assertEquals("two  words", normalizeSearchQuery(" two  words "))
    }

    @Test
    fun repeatMovesToFrontAndUsesLatestSpelling() {
        val history = listOf("Firefox", "Aurora Store", "Maps")
        assertEquals(
            listOf("aurora store", "Firefox", "Maps"),
            addSearchHistory(history, " aurora store ")
        )
    }

    @Test
    fun newestSearchEvictsOldestAtLimit() {
        val history = List(SEARCH_HISTORY_LIMIT) { "query $it" }
        val updated = addSearchHistory(history, "new query")
        assertEquals(SEARCH_HISTORY_LIMIT, updated.size)
        assertEquals(listOf("new query") + history.dropLast(1), updated)
    }

    @Test
    fun savedHistoryIsTrimmedDeduplicatedAndBounded() {
        val history = listOf("", "  Firefox ", "firefox", " \n") + List(30) { "query $it" }
        val normalized = normalizeSearchHistory(history)
        assertEquals(SEARCH_HISTORY_LIMIT, normalized.size)
        assertEquals("Firefox", normalized.first())
        assertEquals("query 18", normalized.last())
    }

    @Test
    fun deletionRemovesOnlyRequestedQuery() {
        val history = listOf("Firefox", "Aurora Store", "Maps")
        assertEquals(listOf("Firefox", "Maps"), removeSearchHistory(history, " AURORA STORE "))
        assertEquals(history, removeSearchHistory(history, "missing"))
    }

    @Test
    fun jsonPreservesOrderAndQueryCharacters() {
        val history = listOf("日本語", "a \"quoted\" query", "one,two", "back\\slash")
        val restored = Json.decodeFromString<List<String>>(Json.encodeToString(history))
        assertEquals(history, normalizeSearchHistory(restored))
    }
}
