/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.navigation

internal fun recoverySplash(
    backStack: List<Screen>,
    packageName: String?,
    alreadyAttempted: Boolean
): Screen.Splash? {
    // Several in-flight requests can reject the same session. Do not restart recovery for each.
    if (backStack.any { it is Screen.Splash && it.recoverSession }) return null

    val resume = backStack.filterNot { it is Screen.Splash || it is Screen.GoogleLogin }
        .toMutableList()
    if (!packageName.isNullOrBlank() && resume.lastOrNull() != Screen.AppDetails(packageName)) {
        resume.add(Screen.AppDetails(packageName))
    }
    return Screen.Splash(
        recoverSession = true,
        recoveryBlocked = alreadyAttempted,
        resumeBackStack = resume
    )
}

internal fun authenticatedBackStack(splash: Screen.Splash?, fallback: Screen): List<Screen> =
    splash?.resumeBackStack?.takeIf { it.isNotEmpty() } ?: listOf(fallback)
