package com.stepphantom.xposed

import android.hardware.SensorEvent
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Constructor

object SensorEventFactory {
    private val ctor: Constructor<SensorEvent>? = try {
        SensorEvent::class.java.getDeclaredConstructor(Integer.TYPE).also { it.isAccessible = true }
    } catch (t: Throwable) {
        XposedBridge.log("[StepPhantom] SensorEvent(int) no disponible por reflexión: $t"); null
    }
    val available: Boolean get() = ctor != null
    fun tryCreate(valueSize: Int): SensorEvent? = try { ctor?.newInstance(valueSize) } catch (t: Throwable) {
        XposedBridge.log("[StepPhantom] Falló construir SensorEvent: $t"); null
    }
}
