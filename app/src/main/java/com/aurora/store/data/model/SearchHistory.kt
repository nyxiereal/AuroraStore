/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import java.util.Locale

const val SEARCH_HISTORY_LIMIT = 20

fun normalizeSearchQuery(query: String): String? = query.trim().takeIf { it.isNotEmpty() }

fun normalizeSearchHistory(history: List<String>): List<String> = history
    .mapNotNull(::normalizeSearchQuery)
    .distinctBy { it.lowercase(Locale.ROOT) }
    .take(SEARCH_HISTORY_LIMIT)

fun addSearchHistory(history: List<String>, query: String): List<String> =
    normalizeSearchHistory(listOfNotNull(normalizeSearchQuery(query)) + history)

fun removeSearchHistory(history: List<String>, query: String): List<String> =
    history.filterNot { it.equals(query.trim(), ignoreCase = true) }
