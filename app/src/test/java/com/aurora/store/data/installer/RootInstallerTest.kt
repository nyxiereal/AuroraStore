/*
 * SPDX-FileCopyrightText: 2026 alltechdev
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.installer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RootInstallerTest {

    @Test
    fun testCommandRunsAsGivenUid() {
        assertThat(RootInstaller.asUid(10123, "pm install-commit 42"))
            .isEqualTo("su 10123 -c 'pm install-commit 42'")
    }

    @Test
    fun testCommandRunsAsRootWithoutUid() {
        assertThat(RootInstaller.asUid(null, "pm install-commit 42"))
            .isEqualTo("pm install-commit 42")
    }
}
