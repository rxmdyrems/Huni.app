package com.metrolist.music.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    @Test
    fun parsesVersionCodeFromKotlinGradleConfig() {
        val buildFile = """
            android {
                defaultConfig {
                    versionCode = 40
                    versionName = "0.9.14"
                }
            }
        """.trimIndent()

        assertTrue(Updater.isUpdateAvailable(39, Updater.parseVersionCode(buildFile)))
        assertFalse(Updater.isUpdateAvailable(40, Updater.parseVersionCode(buildFile)))
    }

    @Test
    fun rejectsBuildFileWithoutVersionCode() {
        try {
            Updater.parseVersionCode("versionName = \"0.9.14\"")
            throw AssertionError("Expected a missing versionCode to fail")
        } catch (_: IllegalStateException) {
            // Expected: failing closed prevents a release-name-only comparison.
        }
    }
}
