package com.stepphantom.config

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Lado APP (proceso propio). Persiste la config en SharedPreferences como un
 * único blob JSON bajo [ModuleConfig.PREFS_KEY_JSON]. Guardar UN solo string
 * mantiene todo consistente y hace trivial el export/import.
 *
 * Cada vez que se guarda, además de escribir el prefs, notificamos al
 * ContentProvider para que quien esté cacheando del otro lado pueda invalidar.
 */
class ConfigRepository(private val appContext: Context) {

    private val prefs =
        appContext.getSharedPreferences(ModuleConfig.PREFS_NAME, Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(load())
    val config: StateFlow<ModuleConfig> = _config

    fun load(): ModuleConfig {
        val json = prefs.getString(ModuleConfig.PREFS_KEY_JSON, null)
            ?: return ModuleConfig()
        return ModuleConfig.fromJsonString(json)
    }

    fun save(config: ModuleConfig) {
        prefs.edit()
            .putString(ModuleConfig.PREFS_KEY_JSON, config.toJsonString())
            .apply()
        _config.value = config
        // Avisar al provider que el dato cambió (útil si algún observer está suscripto).
        runCatching {
            appContext.contentResolver.notifyChange(ConfigProvider.CONFIG_URI, null)
        }
    }

    fun update(transform: (ModuleConfig) -> ModuleConfig) {
        save(transform(_config.value))
    }

    // ------- Export / Import JSON -------

    fun exportJson(): String = _config.value.toJsonString()

    /** Devuelve true si el JSON era válido y se importó. */
    fun importJson(json: String): Boolean {
        return runCatching {
            val cfg = ModuleConfig.fromJson(org.json.JSONObject(json))
            save(cfg)
            true
        }.getOrDefault(false)
    }
}
