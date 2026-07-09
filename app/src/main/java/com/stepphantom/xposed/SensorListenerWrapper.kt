package com.stepphantom.xposed

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import com.stepphantom.config.ModuleConfig
import com.stepphantom.engine.StepSimulationEngine
import de.robv.android.xposed.XposedBridge
import java.util.concurrent.ConcurrentHashMap

/**
 * Envuelve al SensorEventListener original de la app objetivo.
 *
 * Dos estrategias distintas según el sensor, porque la física del problema
 * es distinta:
 *
 *  - ACELERÓMETRO / GIROSCOPIO: el sensor real dispara CONTINUAMENTE mientras
 *    está registrado, así que interceptamos cada evento real y MODIFICAMOS
 *    event.values[] in situ antes de reenviarlo. No hace falta fabricar eventos.
 *    (El SensorEvent que recibe cada listener sale de una cola propia por
 *    listener en SystemSensorManager, así que mutarlo no contamina a otras apps.)
 *
 *  - STEP_COUNTER / STEP_DETECTOR: si el usuario está quieto, el sensor real
 *    NO dispara nunca. No hay evento que modificar. Por eso acá NO alcanza con
 *    envolver: montamos un scheduler propio que FABRICA eventos a la cadencia
 *    configurada y se los entrega al listener original. Fabricar un SensorEvent
 *    es la parte experimental (ver SensorEventFactory).
 */
class SensorListenerWrapper(
    private val original: SensorEventListener,
    private val engine: StepSimulationEngine,
    private val configProvider: () -> ModuleConfig
) : SensorEventListener {

    // Sensores actualmente "vivos" para este listener (para refcount y limpieza).
    private val liveSensors = ConcurrentHashMap.newKeySet<Int>()

    // Handler que la app pasó al registrar, por tipo de sensor (puede ser null).
    private val handlersByType = ConcurrentHashMap<Int, Handler>()

    // Tipos que estamos generando sintéticamente (counter/detector).
    private val synthActive = ConcurrentHashMap.newKeySet<Int>()

    // Cache de un evento real por tipo, como plan B para fabricar eventos.
    private val seedEvents = ConcurrentHashMap<Int, SensorEvent>()

    private val schedulerThread: HandlerThread by lazy {
        HandlerThread("StepPhantom-sim").also { it.start() }
    }
    private val scheduler: Handler by lazy { Handler(schedulerThread.looper) }

    val isEmpty: Boolean get() = liveSensors.isEmpty()

    // ---------------------------------------------------------------------
    // Registro / desregistro (llamado por SensorHook)
    // ---------------------------------------------------------------------

    fun onRegistered(sensor: Sensor, handler: Handler?) {
        val type = sensor.type
        liveSensors.add(type)
        if (handler != null) handlersByType[type] = handler
        if (type == Sensor.TYPE_STEP_COUNTER || type == Sensor.TYPE_STEP_DETECTOR) {
            startSynthetic(sensor)
        }
    }

    fun onUnregistered(type: Int) {
        liveSensors.remove(type)
        synthActive.remove(type)
        handlersByType.remove(type)
    }

    fun stopAll() {
        liveSensors.clear()
        synthActive.clear()
        handlersByType.clear()
        runCatching { scheduler.removeCallbacksAndMessages(null) }
        runCatching { schedulerThread.quitSafely() }
    }

    // ---------------------------------------------------------------------
    // Eventos reales entrantes
    // ---------------------------------------------------------------------

    override fun onSensorChanged(event: SensorEvent) {
        try {
            val cfg = configProvider()
            val type = event.sensor.type
            seedEvents.putIfAbsent(type, event) // seed para plan B

            when (type) {
                Sensor.TYPE_STEP_COUNTER ->
                    if (cfg.simStepCounter) return else original.onSensorChanged(event)

                Sensor.TYPE_STEP_DETECTOR ->
                    if (cfg.simStepDetector) return else original.onSensorChanged(event)

                Sensor.TYPE_ACCELEROMETER -> {
                    if (cfg.simAccelerometer) engine.fillAccelerometer(event.values)
                    original.onSensorChanged(event)
                }

                Sensor.TYPE_GYROSCOPE -> {
                    if (cfg.simGyroscope) engine.fillGyroscope(event.values)
                    original.onSensorChanged(event)
                }

                else -> original.onSensorChanged(event)
            }
        } catch (t: Throwable) {
            // Nunca crashear la app objetivo por culpa del hook.
            XposedBridge.log("[StepPhantom] onSensorChanged error: $t")
            runCatching { original.onSensorChanged(event) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        runCatching { original.onAccuracyChanged(sensor, accuracy) }
    }

    // ---------------------------------------------------------------------
    // Generación sintética (counter / detector)
    // ---------------------------------------------------------------------

    private fun startSynthetic(sensor: Sensor) {
        val type = sensor.type
        if (!synthActive.add(type)) return // ya corriendo
        scheduleNext(sensor)
    }

    private fun scheduleNext(sensor: Sensor) {
        val type = sensor.type
        if (!synthActive.contains(type)) return
        val delay = when (type) {
            Sensor.TYPE_STEP_COUNTER -> 1000L                       // ~1 Hz basta para un contador
            Sensor.TYPE_STEP_DETECTOR -> engine.stepDetectorIntervalMs()
            else -> return
        }.coerceIn(50L, 60_000L)

        scheduler.postDelayed({
            deliver(sensor)
            scheduleNext(sensor)
        }, delay)
    }

    private fun deliver(sensor: Sensor) {
        try {
            val value = when (sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> engine.currentStepCount().toFloat()
                Sensor.TYPE_STEP_DETECTOR -> 1.0f
                else -> return
            }
            val ev = buildEvent(sensor, value) ?: return
            val h = handlersByType[sensor.type]
            if (h != null) h.post { safeDeliver(ev) } else safeDeliver(ev)
        } catch (t: Throwable) {
            XposedBridge.log("[StepPhantom] deliver error: $t")
        }
    }

    private fun safeDeliver(ev: SensorEvent) {
        runCatching { original.onSensorChanged(ev) }
            .onFailure { XposedBridge.log("[StepPhantom] listener lanzó: $it") }
    }

    /** Fabrica (o reutiliza) un SensorEvent. Ver notas de riesgo en SensorEventFactory. */
    private fun buildEvent(sensor: Sensor, value0: Float): SensorEvent? {
        SensorEventFactory.tryCreate(1)?.let { fresh ->
            fresh.values[0] = value0
            fresh.sensor = sensor
            fresh.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
            fresh.timestamp = SystemClock.elapsedRealtimeNanos()
            return fresh
        }
        // Plan B: reutilizar un evento real cacheado del mismo tipo (si alguno llegó).
        seedEvents[sensor.type]?.let { seed ->
            return runCatching {
                seed.values[0] = value0
                seed.timestamp = SystemClock.elapsedRealtimeNanos()
                seed
            }.getOrNull()
        }
        return null
    }
}
