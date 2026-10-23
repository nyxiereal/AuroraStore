/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthNavigationTest {
    @Test
    fun `recovery from search keeps the requested details and search back destination`() {
        val splash = recoverySplash(
            listOf(Screen.Main(), Screen.Search),
            "org.example.app",
            false
        )!!

        assertTrue(splash.recoverSession)
        assertFalse(splash.recoveryBlocked)
        assertEquals(
            listOf(Screen.Main(), Screen.Search, Screen.AppDetails("org.example.app")),
            authenticatedBackStack(splash, Screen.Main())
        )
    }

    @Test
    fun `concurrent rejections do not replace a pending recovery`() {
        val splash = recoverySplash(listOf(Screen.Search), "org.example.app", false)!!

        assertNull(recoverySplash(listOf(splash), null, true))
        assertNull(recoverySplash(listOf(splash, Screen.GoogleLogin()), "other.app", true))
    }

    @Test
    fun `a rejected refreshed session requires explicit retry instead of another login loop`() {
        val splash = recoverySplash(listOf(Screen.AppDetails("org.example.app")), null, true)!!

        assertTrue(splash.recoveryBlocked)
        assertEquals(listOf(Screen.AppDetails("org.example.app")), splash.resumeBackStack)
    }

    @Test
    fun `existing details destination is not duplicated`() {
        val screens = listOf(Screen.Main(), Screen.AppDetails("org.example.app"))

        assertEquals(screens, recoverySplash(screens, "org.example.app", false)!!.resumeBackStack)
    }

    @Test
    fun `successful deep link login replaces splash rather than keeping all good behind details`() {
        val details = Screen.AppDetails("org.example.app")

        assertEquals(
            listOf(details),
            authenticatedBackStack(Screen.Splash(details.packageName), details)
        )
    }

    @Test
    fun `pending destination and retry limit survive saved navigation state`() {
        val splash: Screen = recoverySplash(listOf(Screen.Search), "org.example.app", true)!!

        assertEquals(splash, Json.decodeFromString<Screen>(Json.encodeToString(splash)))
        assertEquals(Screen.Splash(), Json.decodeFromString<Screen.Splash>("{}"))
    }

    @Test
    fun `empty recovery resumes at fallback without retaining login screens`() {
        val splash = recoverySplash(listOf(Screen.Splash(), Screen.GoogleLogin()), null, false)!!

        assertEquals(listOf(Screen.Main(2)), authenticatedBackStack(splash, Screen.Main(2)))
    }
}
