package org.youschool.app.core.update

import org.junit.Assert.*
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun testVersionComparison_newerVersions() {
        assertTrue(AppUpdateChecker.isNewerVersion("0.2.0", "v0.2.1"))
        assertTrue(AppUpdateChecker.isNewerVersion("0.2.0", "0.3.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("0.2.0", "1.0.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("0.1.9", "0.2.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("0.2.0", "v0.2.0.1"))
    }

    @Test
    fun testVersionComparison_sameOrOlderVersions() {
        assertFalse(AppUpdateChecker.isNewerVersion("0.2.0", "0.2.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("0.2.0", "v0.2.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("0.2.1", "0.2.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.0.0", "0.9.9"))
        assertFalse(AppUpdateChecker.isNewerVersion("0.2.0", "v0.1.0"))
    }

    @Test
    fun testVersionComparison_withSuffixes() {
        assertFalse(AppUpdateChecker.isNewerVersion("0.2.0", "v0.2.0-beta"))
        assertTrue(AppUpdateChecker.isNewerVersion("0.2.0", "v0.2.1-rc1"))
    }

    @Test
    fun testNormalizeVersion() {
        assertEquals(listOf(0, 2, 0), AppUpdateChecker.normalizeVersion("0.2.0"))
        assertEquals(listOf(0, 2, 0), AppUpdateChecker.normalizeVersion("v0.2.0"))
        assertEquals(listOf(1, 0, 5), AppUpdateChecker.normalizeVersion("V1.0.5-preview"))
    }
}
