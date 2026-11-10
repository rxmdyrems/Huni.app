package com.metrolist.music.utils

import android.content.Context
import timber.log.Timber

/** Saves a redacted fatal report and then delegates to Android's normal crash handler. */
class CrashHandler private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching { CrashReporter.saveFatal(context, throwable) }
        Timber.e(throwable, "App crashed")
        defaultHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        const val EXTRA_CRASH_LOG = "crash_log"

        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext))
        }
    }
}
