/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import org.junit.Assert.assertEquals
import org.junit.Test

class SortFilterTest {
    private data class App(
        override val packageName: String,
        override val label: String,
        override val firstInstallTime: Long,
        override val lastUpdateTime: Long,
        override val sizeBytes: Long,
        override val isSystem: Boolean = false,
        override val installer: String? = null
    ) : InstalledAppMeta

    @Test
    fun everySortSupportsBothDirections() {
        val first = App("com.zebra", "alpha", Long.MAX_VALUE, 0, Long.MAX_VALUE)
        val second = App("com.Alpha", "Zebra", 0, Long.MAX_VALUE, 0)
        val apps = listOf(first, second)
        val expectedAscending = mapOf(
            SortBy.NAME to listOf(first, second),
            SortBy.PACKAGE_NAME to listOf(second, first),
            SortBy.DATE_INSTALLED to listOf(second, first),
            SortBy.DATE_UPDATED to listOf(first, second),
            SortBy.SIZE to listOf(second, first)
        )

        SortBy.entries.forEach { sortBy ->
            val ascending = expectedAscending.getValue(sortBy)
            assertEquals(
                sortBy.name,
                ascending,
                apps.applySort(SortFilterState(sortBy, SortOrder.ASC))
            )
            assertEquals(
                sortBy.name,
                ascending.reversed(),
                apps.applySort(SortFilterState(sortBy, SortOrder.DESC))
            )
        }
    }

    @Test
    fun descendingSortKeepsEqualItemsInTheirOriginalOrder() {
        val first = App("com.first", "Same", 1, 1, 1)
        val second = first.copy(packageName = "com.second")
        val apps = listOf(first, second)

        assertEquals(apps, apps.applySort(SortFilterState(sortOrder = SortOrder.DESC)))
        assertEquals(
            emptyList<App>(),
            emptyList<App>().applySort(SortFilterState(sortOrder = SortOrder.DESC))
        )
    }
}
