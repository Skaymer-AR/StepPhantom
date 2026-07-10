package com.stepphantom.config

import android.content.Context
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge

object HookConfigSource {
    private const val TAG = "[StepPhantom]"

    fun load(ctx: Context?): StepPhantomConfig {
        queryProvider(ctx)?.let { return it }
        queryPrefs()?.let { return it }
        XposedBridge.log("$TAG Sin config accesible: no se simula (passthrough).")
        return StepPhantomConfig()
    }

    private fun queryProvider(ctx: Context?): StepPhantomConfig? {
        val context = ctx ?: return null
        return try {
            context.contentResolver.query(ConfigProvider.URI, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(ConfigProvider.COL_JSON)
                    if (idx >= 0) {
                        val json = c.getString(idx)
                        if (!json.isNullOrEmpty()) return StepPhantomConfig.fromJsonString(json)
                    }
                }
                null
            }
        } catch (t: Throwable) {
            XposedBridge.log("$TAG Provider de config no accesible: $t")
            null
        }
    }

    private fun queryPrefs(): StepPhantomConfig? = try {
        @Suppress("DEPRECATION")
        val xsp = XSharedPreferences(StepPhantomConfig.PACKAGE_NAME, StepPhantomConfig.PREFS_NAME)
        xsp.reload()
        if (!xsp.file.canRead()) null else xsp.getString(StepPhantomConfig.PREFS_KEY_JSON, null)?.let { StepPhantomConfig.fromJsonString(it) }
    } catch (t: Throwable) {
        XposedBridge.log("$TAG XSharedPreferences no disponible: $t")
        null
    }
}
