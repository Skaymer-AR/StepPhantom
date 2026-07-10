package com.stepphantom.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val icon: Drawable?
)

object AppEnumerator {

    /**
     * Lista apps instaladas. Requiere QUERY_ALL_PACKAGES para ver todo en una
     * herramienta sideloaded. Cargar iconos es caro: hacelo en un hilo de fondo.
     */
    fun list(context: Context, includeSystem: Boolean): List<AppInfo> {
        val pm = context.packageManager
        val flags = 0
        return pm.getInstalledApplications(flags)
            .asSequence()
            .filter { includeSystem || (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map {
                AppInfo(
                    packageName = it.packageName,
                    label = runCatching { pm.getApplicationLabel(it).toString() }
                        .getOrDefault(it.packageName),
                    isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    icon = runCatching { pm.getApplicationIcon(it) }.getOrNull()
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun exists(context: Context, packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    }.getOrDefault(false)
}
