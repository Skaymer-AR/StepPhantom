package com.stepphantom.xposed

import android.hardware.SensorEvent
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Constructor

/**
 * ⚠️ EXPERIMENTAL / DEPENDIENTE DE VERSIÓN ⚠️
 *
 * android.hardware.SensorEvent tiene un constructor PACKAGE-PRIVATE en AOSP:
 *     SensorEvent(int valueSize)
 * y campos públicos: float[] values (final), Sensor sensor, int accuracy, long timestamp.
 *
 * No hay constructor público, así que fabricar un SensorEvent "desde cero" (para
 * empujar TYPE_STEP_COUNTER/DETECTOR cuando el sensor real no dispara) requiere
 * reflexión. Es API oculta: bajo LSPosed/Vector el proceso suele quedar exento de
 * la lista negra hidden-API, pero no está garantizado en toda ROM. Si falla,
 * el wrapper cae a reutilizar un evento real cacheado; si tampoco hay, no entrega.
 */
object SensorEventFactory {

    private val ctor: Constructor<SensorEvent>? = try {
        SensorEvent::class.java
            .getDeclaredConstructor(Integer.TYPE)
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
