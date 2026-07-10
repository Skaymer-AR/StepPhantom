package com.stepphantom.config

import org.json.JSONObject

/**
 * Modos de transformación del contador de pasos, POR PAQUETE.
 *
 * Todos operan sobre el valor REAL del step counter (salvo RITMO_SIMULADO, que
 * genera desde tiempo). Se aplican una sola vez por evento, en el wrapper.
 */
enum class TransformMode {
    ORIGINAL,        // devuelve el valor real sin tocar
    SUMAR,           // real + offset
    MULTIPLICAR,     // round(real * multiplier)
    REEMPLAZAR,      // valor objetivo fijo (p.ej. 87)
    RITMO_SIMULADO,  // initialSteps + minutosActivos * stepsPerMinute
    PERSONALIZADO    // combina mult + offset + inicial + ritmo + límites + jitter
}

/**
 * Configuración de UNA aplicación objetivo. Es plana y serializable a JSON a
 * mano (org.json) para no arrastrar dependencias de serialización al proceso
 * hookeado.
 */
data class PackageConfig(
    val enabled: Boolean = false,
    val mode: TransformMode = TransformMode.ORIGINAL,

    val offset: Long = 0L,
    val multiplier: Float = 1f,
    val replaceValue: Long = 87L,

    val stepsPerMinute: Int = 100,
    val initialSteps: Long = 0L,
    val jitterPercent: Float = 0f,

    val minLimit: Long = 0L,
    val maxLimit: Long = -1L,     // -1 = sin límite superior

    val paused: Boolean = false,
    val resetToken: Long = 0L,

    // Ruta de sensores (implementada). Las de HC son sólo diagnóstico en esta versión.
    val hookSensorManager: Boolean = true,
    val detectHcJetpack: Boolean = true,
    val detectHcFramework: Boolean = true,

    // Experimentales, apagados por defecto (pueden romper apps).
    val simAccelerometer: Boolean = false,
    val simGyroscope: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("enabled", enabled)
        put("mode", mode.name)
        put("offset", offset)
        put("multiplier", multiplier.toDouble())
        put("replaceValue", replaceValue)
        put("stepsPerMinute", stepsPerMinute)
        put("initialSteps", initialSteps)
        put("jitterPercent", jitterPercent.toDouble())
        put("minLimit", minLimit)
        put("maxLimit", maxLimit)
        put("paused", paused)
        put("resetToken", resetToken)
        put("hookSensorManager", hookSensorManager)
        put("detectHcJetpack", detectHcJetpack)
        put("detectHcFramework", detectHcFramework)
        put("simAccelerometer", simAccelerometer)
        put("simGyroscope", simGyroscope)
    }

    companion object {
        fun fromJson(o: JSONObject): PackageConfig {
            val mode = runCatching { TransformMode.valueOf(o.optString("mode", "ORIGINAL")) }
                .getOrDefault(TransformMode.ORIGINAL)
            return PackageConfig(
                enabled = o.optBoolean("enabled", false),
                mode = mode,
                offset = o.optLong("offset", 0L),
                multiplier = o.optDouble("multiplier", 1.0).toFloat(),
                replaceValue = o.optLong("replaceValue", 87L),
                stepsPerMinute = o.optInt("stepsPerMinute", 100),
                initialSteps = o.optLong("initialSteps", 0L),
                jitterPercent = o.optDouble("jitterPercent", 0.0).toFloat(),
                minLimit = o.optLong("minLimit", 0L),
                maxLimit = o.optLong("maxLimit", -1L),
                paused = o.optBoolean("paused", false),
                resetToken = o.optLong("resetToken", 0L),
                hookSensorManager = o.optBoolean("hookSensorManager", true),
                detectHcJetpack = o.optBoolean("detectHcJetpack", true),
                detectHcFramework = o.optBoolean("detectHcFramework", true),
                simAccelerometer = o.optBoolean("simAccelerometer", false),
                simGyroscope = o.optBoolean("simGyroscope", false)
            )
        }
    }
}

/**
 * Configuración raíz: un mapa paquete -> PackageConfig. La ausencia de un paquete
 * en el mapa significa "no seleccionado" => el hook lo deja pasar sin tocar nada.
 */
data class StepPhantomConfig(
    val schema: Int = 1,
    val packages: Map<String, PackageConfig> = emptyMap()
) {
    fun forPackage(pkg: String): PackageConfig? = packages[pkg]

    fun toJson(): JSONObject = JSONObject().apply {
        put("schema", schema)
        val pkgs = JSONObject()
        packages.forEach { (name, cfg) -> pkgs.put(name, cfg.toJson()) }
        put("packages", pkgs)
    }

    fun toJsonString(): String = toJson().toString(2)

    companion object {
        const val PACKAGE_NAME = "com.stepphantom"
        const val CONFIG_AUTHORITY = "com.stepphantom.config"
        const val DIAG_AUTHORITY = "com.stepphantom.diagnostics"
        const val PREFS_NAME = "stepphantom_prefs"
        const val PREFS_KEY_JSON = "config_json"

        fun fromJson(o: JSONObject): StepPhantomConfig {
            val map = mutableMapOf<String, PackageConfig>()
            o.optJSONObject("packages")?.let { pkgs ->
                val it = pkgs.keys()
                while (it.hasNext()) {
                    val name = it.next()
                    pkgs.optJSONObject(name)?.let { c ->
                        map[name] = PackageConfig.fromJson(c)
                    }
                }
            }
            return StepPhantomConfig(schema = o.optInt("schema", 1), packages = map)
        }

        fun fromJsonString(s: String): StepPhantomConfig =
            runCatching { fromJson(JSONObject(s)) }.getOrDefault(StepPhantomConfig())
    }
}
