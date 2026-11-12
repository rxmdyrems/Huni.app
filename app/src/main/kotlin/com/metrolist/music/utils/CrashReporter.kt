package com.metrolist.music.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import com.metrolist.music.BuildConfig
import com.metrolist.music.constants.CrashReportingEnabledKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import java.io.PrintWriter
import java.io.StringWriter

/** Prepares redacted crash reports locally. The app never posts crash data itself. */
object CrashReporter {
    private const val FILE_NAME = "pending_crash_report.txt"
    private const val MAX_STACK_CHARS = 4000

    fun init(context: Context) = Unit

    // Non-fatal and watchdog paths remain local-only; fatal reports are shown next launch.
    fun reportNonFatal(throwable: Throwable) = Unit
    fun reportAnr(mainThreadStack: Array<StackTraceElement>, blockedMs: Long) = Unit

    fun saveFatal(context: Context, throwable: Throwable) {
        runCatching { if (isEnabled(context)) context.filesDir.resolve(FILE_NAME).writeText(buildReport(throwable)) }
    }

    fun isEnabled(context: Context): Boolean = runCatching {
        context.dataStore.get(CrashReportingEnabledKey, defaultValue = true)
    }.getOrDefault(true)

    fun readPending(context: Context): String? = runCatching {
        context.filesDir.resolve(FILE_NAME).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
    }.getOrNull()

    fun deletePending(context: Context) = runCatching { context.filesDir.resolve(FILE_NAME).delete() }

    fun fakeReport(): String = buildString {
        appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("Device model: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Build flavor: ${BuildConfig.FLAVOR}")
        appendLine()
        appendLine("java.lang.IllegalStateException: Preview crash report")
        appendLine("    at com.remycamiguel.huni.Preview.example(Preview.kt:1)")
    }

    fun githubUri(report: String): Uri = Uri.parse("https://github.com/rxmdyrems/Huni.app/issues/new").buildUpon()
        .appendQueryParameter("title", "Crash: Huni ${BuildConfig.VERSION_NAME}")
        .appendQueryParameter("body", externalBody(report)).build()

    fun emailUri(report: String): Uri = Uri.parse("mailto:camiguel.remy@gmail.com").buildUpon()
        .appendQueryParameter("subject", "Huni crash report ${BuildConfig.VERSION_NAME}")
        .appendQueryParameter("body", externalBody(report)).build()

    fun needsClipboard(report: String) = report.length > 6000
    fun shortExternalBody() = "The full redacted crash report is on the clipboard. Please paste it here."
    private fun externalBody(report: String) = if (report.length <= 6000) report else shortExternalBody()

    private fun buildReport(throwable: Throwable): String = buildString {
        appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("Device model: ${sanitize("${Build.MANUFACTURER} ${Build.MODEL}")}")
        appendLine("Build flavor: ${BuildConfig.FLAVOR}")
        appendLine()
        val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }
        append(sanitize(trace.toString()).takeLast(MAX_STACK_CHARS))
    }

    internal fun sanitize(input: String): String {
        var result = input
        result = Regex("/storage(?:/[^\\s\\\"')\\]]*)?").replace(result, "<storage>")
        result = Regex("/data/user(?:/[^\\s\\\"')\\]]*)?").replace(result, "<data>")
        result = Regex("/data/data(?:/[^\\s\\\"')\\]]*)?").replace(result, "<data>")
        result = Regex("https?://[^\\s\\\"')\\]]+").replace(result) { it.value.substringBefore('?').substringBefore('#') }
        result = Regex("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}").replace(result, "<email>")
        return result
    }
}
