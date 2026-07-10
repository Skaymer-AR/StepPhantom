package com.stepphantom.config

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Lado APP. Persiste toda la config (mapa paquete->PackageConfig) como un único
 * JSON en SharedPreferences. Un solo blob = consistencia trivial y export/import
 * directo. Cada guardado notifica al ConfigProvider por si hay observers.
 */
class ConfigRepository(private val appContext: Context) {

    private val prefs = appContext.getSharedPreferences(
        StepPhantomConfig.PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _config = MutableStateFlow(load())
    val config: StateFlow<StepPhantomConfig> = _config

    fun load(): StepPhantomConfig {
        val json = prefs.getString(StepPhantomConfig.PREFS_KEY_JSON, null)
            ?: return StepPhantomConfig()
        return StepPhantomConfig.fromJsonString(json)
    }

    private fun save(cfg: StepPhantomConfig) {
        prefs.edit().putString(StepPhantomConfig.PREFS_KEY_JSON, cfg.toJsonString()).apply()
        _config.value = cfg
        runCatching { appContext.contentResolver.notifyChange(ConfigProvider.URI, null) }
    }

    // ----- operaciones por paquete -----

    fun addPackage(pkg: String) {
        val clean = pkg.trim()
        if (clean.isEmpty() || clean in _config.value.packages) return
        val next = _config.value.packages + (clean to PackageConfig())
        save(_config.value.copy(packages = next))
    }

    fun removePackage(pkg: String) {
        if (pkg !in _config.value.packages) return
        save(_config.value.copy(packages = _config.value.packages - pkg))
    }

    fun updatePackage(pkg: String, transform: (PackageConfig) -> PackageConfig) {
        val current = _config.value.packages[pkg] ?: PackageConfig()
        val next = _config.value.packages + (pkg to transform(current))
        save(_config.value.copy(packages = next))
    }

    fun packageConfig(pkg: String): PackageConfig =
        _config.value.packages[pkg] ?: PackageConfig()

    // ----- export / import -----

    fun exportJson(): String = _config.value.toJsonString()

    fun importJson(json: String): Boolean = runCatching {
        save(StepPhantomConfig.fromJson(org.json.JSONObject(json)))
        true
    }.getOrDefault(false)
}
