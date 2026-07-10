package com.stepphantom.xposed

import android.hardware.SensorEventListener
import java.util.concurrent.ConcurrentHashMap

class ListenerRegistry {
    private val map = ConcurrentHashMap<SensorEventListener, SensorListenerWrapper>()
    fun getOrCreate(original: SensorEventListener, factory: () -> SensorListenerWrapper): SensorListenerWrapper = map.getOrPut(original, factory)
    fun get(original: SensorEventListener): SensorListenerWrapper? = map[original]
    fun remove(original: SensorEventListener): SensorListenerWrapper? = map.remove(original)
    fun isWrapper(candidate: Any?): Boolean = candidate is SensorListenerWrapper
}
