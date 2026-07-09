package com.stepphantom.config

import android.content.Context
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge

/**
 * Lado HOOK (corre dentro del proceso objetivo). Resuelve la config con una
 * cascada de fuentes, de la más robusta a la más frágil:
 *
 *   1) ContentProvider exportado (content://com.stepphantom.config/config)
 *      -> Robusto e independiente de versión. Necesita un Context, que el hook
 *         consigue del SystemSensorManager. Es la fuente PRINCIPAL.
 *
 *   2) XSharedPreferences -> Alternativa "idiomática" de Xposed. En Android
 *      moderno (>= 7) el viejo MODE_WORLD_READABLE está muerto, así que esto sólo
 *      funciona si LSPosed/Vector provee su bridge de preferencias remotas para
 *      este módulo. Puede devolver null; por eso es fallback, no principal.
 *
 *   3) Defaults -> Si todo falla, ModuleConfig() (módulo efectivamente inactivo).
 */
object HookConfigSource {

    private const val TAG = "[StepPhantom]"

    fun load(ctx: Context?): ModuleConfig {
        queryProvider(ctx)?.let { return it }
        queryPrefs()?.let { return it }
        return ModuleConfig()
    }

    private fun queryProvider(ctx: Context?): ModuleConfig? {
        val context = ctx ?: return null
        return try {
            context.contentResolver.query(
                ConfigProvider.CONFIG_URI, null, null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(ConfigProvider.COLUMN_JSON)
                    if (idx >= 0) return ModuleConfig.fromJsonString(c.getString(idx))
                }
                null
            }
        } catch (t: Throwable) {
            XposedBridge.log("$TAG Provider no accesible: $t")
            null
        }
    }

    private fun queryPrefs(): ModuleConfig? {
        return try {
            @Suppress("DEPRECATION")
            val xsp = XSharedPreferences(ModuleConfig.PACKAGE_NAME, ModuleConfig.PREFS_NAME)
            xsp.reload()
            if (!xsp.file.canRead()) return null
            val json = xsp.getString(ModuleConfig.PREFS_KEY_JSON, null) ?: return null
            ModuleConfig.fromJsonString(json)
        } catch (t: Throwable) {
            XposedBridge.log("$TAG XSharedPreferences no disponible: $t")
            null
        }
    }
}
