package com.metrolist.music.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconUtils {
    private val paletteAliases = mapOf(
        "amber" to "MainActivityAlias",
        "violet" to "MainActivityIconViolet",
        "green" to "MainActivityIconGreen",
        "maroon" to "MainActivityIconMaroon",
        "ocean" to "MainActivityIconOcean",
        "teal" to "MainActivityIconTeal",
        "rose" to "MainActivityIconRose",
        "orange" to "MainActivityIconOrange",
        "indigo" to "MainActivityIconIndigo",
        "slate" to "MainActivityIconSlate",
    )

    fun applyPalette(context: Context, paletteId: String) {
        val packageManager = context.packageManager
        val chosenId = paletteId.lowercase().takeIf(paletteAliases::containsKey) ?: "amber"
        val chosenAlias = paletteAliases.getValue(chosenId)
        packageManager.setComponentEnabledSetting(
            ComponentName(context, "com.metrolist.music.$chosenAlias"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        paletteAliases.filterValues { it != chosenAlias }.values.forEach { alias ->
            packageManager.setComponentEnabledSetting(
                ComponentName(context, "com.metrolist.music.$alias"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        // Kept disabled as an upgrade-compatibility alias from earlier builds.
        packageManager.setComponentEnabledSetting(
            ComponentName(context, "com.metrolist.music.MainActivityStatic"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
