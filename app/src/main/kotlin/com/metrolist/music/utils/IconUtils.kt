package com.metrolist.music.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconUtils {
    const val DEFAULT_ICON_ID = "default"

    private const val DEFAULT_ALIAS = "com.metrolist.music.MainActivityAlias"

    private val paletteAliases = linkedMapOf(
        "amber" to "com.metrolist.music.MainActivityPaletteAmber",
        "violet" to "com.metrolist.music.MainActivityViolet",
        "green" to "com.metrolist.music.MainActivityGreen",
        "maroon" to "com.metrolist.music.MainActivityMaroon",
        "ocean" to "com.metrolist.music.MainActivityOcean",
        "teal" to "com.metrolist.music.MainActivityTeal",
        "rose" to "com.metrolist.music.MainActivityRose",
        "orange" to "com.metrolist.music.MainActivityOrange",
        "indigo" to "com.metrolist.music.MainActivityIndigo",
        "slate" to "com.metrolist.music.MainActivitySlate",
    )

    private val allAliases = listOf(DEFAULT_ALIAS) + paletteAliases.values

    /** Enable one palette alias, or restore the original transparent icon for [DEFAULT_ICON_ID]. */
    fun setIcon(context: Context, paletteId: String) {
        val targetClass =
            if (paletteId == DEFAULT_ICON_ID) {
                DEFAULT_ALIAS
            } else {
                paletteAliases[paletteId] ?: DEFAULT_ALIAS
            }
        val packageManager = context.packageManager

        allAliases
            .filterNot { it == targetClass }
            .forEach { aliasClass ->
                packageManager.setComponentEnabledSetting(
                    ComponentName(context, aliasClass),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }

        packageManager.setComponentEnabledSetting(
            ComponentName(context, targetClass),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
