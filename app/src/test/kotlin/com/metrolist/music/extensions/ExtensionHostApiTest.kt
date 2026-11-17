package com.metrolist.music.extensions

import com.metrolist.music.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtensionHostApiTest {
    @Test
    fun `Mihon extension AppInfo binary contract is present`() {
        val hostApi = Class.forName("eu.kanade.tachiyomi.AppInfo")
        val instance = hostApi.getField("INSTANCE").get(null)
        val versionMethod = hostApi.getMethod("getVersionName")

        assertEquals(String::class.java, versionMethod.returnType)
        assertEquals(BuildConfig.BASE_VERSION_NAME, versionMethod.invoke(instance))
    }
}
