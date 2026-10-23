/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import android.os.Parcelable
import com.aurora.extensions.requiresGMS
import com.aurora.gplayapi.data.models.App
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Filter for list with search results
 */
@Parcelize
@Serializable
data class SearchFilter(
    val noAds: Boolean = false,
    val isFree: Boolean = false,
    val noGMS: Boolean = false,
    val minRating: Float = 0F,
    val minInstalls: Long = 0
) : Parcelable {
    fun normalized(): SearchFilter = copy(
        minRating = if (minRating.isFinite()) minRating.coerceIn(0F, 5F) else 0F,
        minInstalls = minInstalls.coerceAtLeast(0)
    )

    fun matches(app: App, isAnonymous: Boolean): Boolean = when {
        noAds && app.containsAds -> false
        isFree && !app.isFree -> false
        noGMS && !isAnonymous && app.requiresGMS() -> false
        app.rating.average < minRating -> false
        app.installs < minInstalls -> false
        else -> true
    }
}
