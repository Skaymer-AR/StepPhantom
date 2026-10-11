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

    @Volatile private var failure: String? = null

    private val ctor: Constructor<SensorEvent>? = try {
        SensorEvent::class.java
            .getDeclaredConstructor(Integer.TYPE)
            .also { it.isAccessible = true }
    } catch (t: Throwable) {
        failure = "SensorEvent(int) no está disponible: ${t.javaClass.simpleName}"
        XposedBridge.log("[StepPhantom] ${failure}")
        null
    }

    val available: Boolean get() = ctor != null

    fun failureDescription(): String = failure ?: "No se pudo construir SensorEvent (API oculta)"

    fun tryCreate(valueSize: Int): SensorEvent? {
        return try {
            val constructor = ctor ?: return null
            constructor.newInstance(valueSize)
        } catch (t: Throwable) {
            failure = "No se pudo construir SensorEvent: ${t.javaClass.simpleName}"
            XposedBridge.log("[StepPhantom] ${failure}")
            null
        }
    }
}
