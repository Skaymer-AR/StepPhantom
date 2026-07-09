package com.stepphantom.xposed

import android.hardware.SensorEvent
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Constructor

/**
 * ⚠️ EXPERIMENTAL / DEPENDIENTE DE VERSIÓN ⚠️
 *
 * En AOSP, android.hardware.SensorEvent tiene un constructor PACKAGE-PRIVATE:
 *
 *     SensorEvent(int valueSize) { values = new float[valueSize]; }
 *
 * y sus campos son públicos:  float[] values (final), Sensor sensor,
 * int accuracy, long timestamp.
 *
 * No hay constructor público, así que la única forma de fabricar un SensorEvent
 * "desde cero" es por reflexión sobre ese constructor. Esto es necesario para
 * TYPE_STEP_DETECTOR y para TYPE_STEP_COUNTER cuando el device está quieto y el
 * sensor real NUNCA dispara (no habría evento real que modificar).
 *
 * Riesgos reales:
 *   - Es API oculta (non-SDK). Bajo LSPosed/Vector normalmente el proceso queda
 *     exento de la lista negra de hidden-API, pero no está garantizado en toda
 *     ROM. Si getDeclaredConstructor falla por restricción de hidden-API,
 *     agregá la dependencia org.lsposed.hiddenapibypass y envolvé la reflexión.
 *   - El layout del constructor/campos podría cambiar en una versión futura.
 *     Verificá contra el source de tu Android 16 concreto si deja de funcionar.
 *
 * Si esto devuelve null, el wrapper cae al plan B (reutilizar un evento real
 * cacheado), y si tampoco hay, no entrega evento (y lo dice en el log).
 */
object SensorEventFactory {

    private val ctor: Constructor<SensorEvent>? = try {
        SensorEvent::class.java
            .getDeclaredConstructor(Int::class.javaPrimitiveType)
            .also { it.isAccessible = true }
    } catch (t: Throwable) {
        XposedBridge.log("[StepPhantom] SensorEvent(int) no disponible por reflexión: $t")
        null
    }

    val available: Boolean get() = ctor != null

    fun tryCreate(valueSize: Int): SensorEvent? = try {
        ctor?.newInstance(valueSize)
    } catch (t: Throwable) {
        XposedBridge.log("[StepPhantom] Falló construir SensorEvent: $t")
        null
    }
}
