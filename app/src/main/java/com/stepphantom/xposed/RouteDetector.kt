package com.stepphantom.xposed

/**
 * Detección de VÍAS de lectura de pasos, SÓLO con fines de diagnóstico.
 *
 * Probamos si ciertas clases están disponibles en el classloader de la app. Es
 * una heurística, no una prueba de uso:
 *  - Clases de framework (android.health.connect.*) están disponibles en todo
 *    Android 14+, tenga o no la app código que las use. Por eso "framework HC
 *    disponible" != "la app usa HC framework".
 *  - Clases de librerías (androidx.health.connect.*, GMS fitness) sólo están si
 *    la app las empaquetó -> señal fuerte de que esa vía se usa.
 *
 * Este proyecto NO reescribe lecturas de Health Connect ni de Fit. Esta clase
 * sólo informa qué encontró, para que sepas si el hook de SensorManager va a
 * servir para esa app o no.
 */
object RouteDetector {

    data class Result(
        val sensorManagerAvailable: Boolean,
        val hcJetpack: Boolean,
        val hcFramework: Boolean,
        val fitOrRecording: Boolean,
        val classesFound: List<String>
    ) {
        fun primaryRouteLabel(): String = when {
            hcJetpack -> "Health Connect (Jetpack) — este módulo NO la reescribe"
            fitOrRecording -> "Google Fit / Recording API — este módulo NO la reescribe"
            hcFramework -> "Health Connect (framework) — disponible en el SO; verificá si la app la usa"
            sensorManagerAvailable -> "SensorManager (interceptable por este módulo)"
            else -> "desconocida"
        }
    }

    private val HC_JETPACK = listOf(
        "androidx.health.connect.client.HealthConnectClient",
        "androidx.health.connect.client.records.StepsRecord"
    )
    private val HC_FRAMEWORK = listOf(
        "android.health.connect.HealthConnectManager",
        "android.health.connect.datatypes.StepsRecord"
    )
    private val FIT = listOf(
        "com.google.android.gms.fitness.FitnessOptions",
        "com.google.android.gms.fitness.RecordingClient"
    )

    fun detect(classLoader: ClassLoader): Result {
        val found = mutableListOf<String>()
        val hcJetpack = anyLoadable(classLoader, HC_JETPACK, found)
        val hcFramework = anyLoadable(classLoader, HC_FRAMEWORK, found)
        val fit = anyLoadable(classLoader, FIT, found)
        // SensorManager es framework: siempre presente. La "usó realmente" se
        // sabe en runtime cuando registra un sensor de pasos (lo marca el hook).
        val sm = isLoadable(classLoader, "android.hardware.SensorManager", found)
        return Result(sm, hcJetpack, hcFramework, fit, found)
    }

    private fun anyLoadable(cl: ClassLoader, names: List<String>, acc: MutableList<String>): Boolean {
        var any = false
        for (n in names) if (isLoadable(cl, n, acc)) any = true
        return any
    }

    private fun isLoadable(cl: ClassLoader, name: String, acc: MutableList<String>): Boolean {
        return try {
            cl.loadClass(name)
            acc.add(name)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
