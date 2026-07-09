package com.stepphantom.config

import org.json.JSONArray
import org.json.JSONObject

enum class WalkMode { SOFT, CONSTANT, PAUSE }

/**
 * Modelo de configuración. Es intencionalmente plano y serializable a JSON
 * "a mano" (org.json) para que el MISMO archivo pueda:
 *   - guardarse desde la app (ConfigRepository),
 *   - servirse por el ContentProvider,
 *   - y parsearse dentro del proceso hookeado (ModuleConfig.fromJson)
 * sin arrastrar dependencias de serialización al classpath del hook.
 *
 * No usamos kotlinx.serialization aquí a propósito: dentro del proceso víctima
 * cuanto menos classpath toquemos, menos cosas se pueden romper.
 */
data class ModuleConfig(
    val moduleEnabled: Boolean = false,

    val targetPackages: List<String> = emptyList(),

    // Qué sensores simular
    val simStepCounter: Boolean = true,
    val simStepDetector: Boolean = true,
    val simAccelerometer: Boolean = false, // apagado por defecto: puede romper apps
    val simGyroscope: Boolean = false,

    // Parámetros del motor
    val stepsPerMinute: Int = 100,
    val initialSteps: Long = 0L,
    val jitterPercent: Float = 8f,

    val walkMode: WalkMode = WalkMode.CONSTANT,
    val paused: Boolean = false,

    // Cambia cada vez que la app pide "reset". El hook compara este valor contra
    // el último visto y, si difiere, reinicia el motor. Es la forma de mandar un
    // "reset" cruzando procesos sin canal inverso.
    val resetToken: Long = 0L
) {

    fun toJson(): JSONObject = JSONObject().apply {
        put("moduleEnabled", moduleEnabled)
        put("targetPackages", JSONArray(targetPackages))
        put("simStepCounter", simStepCounter)
        put("simStepDetector", simStepDetector)
        put("simAccelerometer", simAccelerometer)
        put("simGyroscope", simGyroscope)
        put("stepsPerMinute", stepsPerMinute)
        put("initialSteps", initialSteps)
        put("jitterPercent", jitterPercent.toDouble())
        put("walkMode", walkMode.name)
        put("paused", paused)
        put("resetToken", resetToken)
    }

    fun toJsonString(): String = toJson().toString(2)

    companion object {
        const val PACKAGE_NAME = "com.stepphantom"          // applicationId de la app/módulo
        const val AUTHORITY = "com.stepphantom.config"      // authority del ContentProvider
        const val PREFS_NAME = "stepphantom_prefs"
        const val PREFS_KEY_JSON = "config_json"

        fun fromJson(obj: JSONObject): ModuleConfig {
            val pkgs = mutableListOf<String>()
            obj.optJSONArray("targetPackages")?.let { arr ->
                for (i in 0 until arr.length()) pkgs.add(arr.getString(i))
            }
            val mode = runCatching { WalkMode.valueOf(obj.optString("walkMode", "CONSTANT")) }
                .getOrDefault(WalkMode.CONSTANT)

            return ModuleConfig(
                moduleEnabled = obj.optBoolean("moduleEnabled", false),
                targetPackages = pkgs,
                simStepCounter = obj.optBoolean("simStepCounter", true),
                simStepDetector = obj.optBoolean("simStepDetector", true),
                simAccelerometer = obj.optBoolean("simAccelerometer", false),
                simGyroscope = obj.optBoolean("simGyroscope", false),
                stepsPerMinute = obj.optInt("stepsPerMinute", 100),
                initialSteps = obj.optLong("initialSteps", 0L),
                jitterPercent = obj.optDouble("jitterPercent", 8.0).toFloat(),
                walkMode = mode,
                paused = obj.optBoolean("paused", false),
                resetToken = obj.optLong("resetToken", 0L)
            )
        }

        fun fromJsonString(s: String): ModuleConfig =
            runCatching { fromJson(JSONObject(s)) }.getOrDefault(ModuleConfig())
    }
}
