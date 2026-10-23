/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import com.aurora.Constants.PACKAGE_NAME_GMS
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Rating
import com.aurora.gplayapi.data.models.details.Dependencies
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFilterTest {
    private val app = App(
        packageName = "com.example.app",
        isFree = true,
        containsAds = false,
        rating = Rating(average = 4.5F),
        installs = 1_000_000
    )

    @Test
    fun defaultFilterKeepsAllApps() {
        assertTrue(SearchFilter().matches(app.copy(isFree = false, containsAds = true), true))
    }

    @Test
    fun freeAndAdFiltersAreIndependent() {
        assertFalse(SearchFilter(isFree = true).matches(app.copy(isFree = false), true))
        assertFalse(SearchFilter(noAds = true).matches(app.copy(containsAds = true), true))
        assertTrue(SearchFilter(isFree = true).matches(app.copy(containsAds = true), true))
        assertTrue(SearchFilter(noAds = true).matches(app.copy(isFree = false), true))
    }

    @Test
    fun ratingAndDownloadMinimumsAreInclusive() {
        val filter = SearchFilter(minRating = 4.5F, minInstalls = 1_000_000)
        assertTrue(filter.matches(app, true))
        assertFalse(filter.matches(app.copy(rating = Rating(average = 4.4F)), true))
        assertFalse(filter.matches(app.copy(installs = 999_999), true))
    }

    @Test
    fun savedGmsFilterIsIgnoredForAnonymousAccounts() {
        val requiresGms = app.copy(
            dependencies = Dependencies(dependentPackages = listOf(PACKAGE_NAME_GMS))
        )
        val filter = SearchFilter(noGMS = true)
        assertTrue(filter.matches(requiresGms, isAnonymous = true))
        assertFalse(filter.matches(requiresGms, isAnonymous = false))
        assertTrue(filter.matches(app, isAnonymous = false))
    }

    @Test
    fun allFiltersApplyTogether() {
        val filter = SearchFilter(
            noAds = true,
            isFree = true,
            noGMS = true,
            minRating = 4F,
            minInstalls = 100_000
        )
        assertTrue(filter.matches(app, false))
        assertFalse(filter.matches(app.copy(containsAds = true), false))
        assertFalse(filter.matches(app.copy(isFree = false), false))
    }

    @Test
    fun invalidSavedMinimumsAreMadeSafe() {
        assertEquals(0F, SearchFilter(minRating = Float.NaN).normalized().minRating)
        assertEquals(0F, SearchFilter(minRating = Float.POSITIVE_INFINITY).normalized().minRating)
        assertEquals(5F, SearchFilter(minRating = 7F).normalized().minRating)
        assertEquals(SearchFilter(), SearchFilter(minRating = -1F, minInstalls = -1).normalized())
    }

    @Test
    fun filtersRoundTripAndMissingValuesUseDefaults() {
        val filter = SearchFilter(
            noAds = true,
            isFree = true,
            noGMS = true,
            minRating = 4.5F,
            minInstalls = 50_000_000
        )
        assertEquals(filter, Json.decodeFromString<SearchFilter>(Json.encodeToString(filter)))
        assertEquals(SearchFilter(), Json.decodeFromString<SearchFilter>("{}"))
    }
}
