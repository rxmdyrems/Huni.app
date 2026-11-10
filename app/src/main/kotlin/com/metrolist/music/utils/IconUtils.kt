package com.metrolist.music.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconUtils {
    private const val DEFAULT_PALETTE = "amber"

    private val paletteAliases = linkedMapOf(
        "amber" to "com.metrolist.music.MainActivityAlias",
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

    /** Enable the alias matching [paletteId], disabling every other launcher alias. */
    fun setIcon(context: Context, paletteId: String) {
        val targetClass = paletteAliases[paletteId] ?: paletteAliases.getValue(DEFAULT_PALETTE)
        val packageManager = context.packageManager

        paletteAliases.values
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
