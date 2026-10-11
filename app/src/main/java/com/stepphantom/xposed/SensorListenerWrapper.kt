package com.stepphantom.xposed

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import com.stepphantom.config.PackageConfig
import com.stepphantom.config.TransformMode
import com.stepphantom.engine.StepTransformationEngine
import de.robv.android.xposed.XposedBridge
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Envuelve el SensorEventListener original.
 *
 * TYPE_STEP_COUNTER: transforma event.values[0] EN EL LUGAR (una sola vez) según
 *   la config del paquete y lo delega. Para REEMPLAZAR/RITMO_SIMULADO además
 *   corre un scheduler que EMPUJA valores aunque no haya pasos reales (si estás
 *   quieto, el sensor real no dispara). Empujar requiere fabricar SensorEvent
 *   (experimental, ver SensorEventFactory).
 *
 * TYPE_STEP_DETECTOR: deja pasar los eventos reales; en RITMO_SIMULADO emite
 *   además eventos discretos 1.0f a la cadencia configurada.
 *
 * Acelerómetro/giroscopio: experimentales, sólo si el paquete los habilita;
 *   si no, passthrough intacto.
 */
class SensorListenerWrapper(
    private val original: SensorEventListener,
    private val engine: StepTransformationEngine,
    private val pkg: String,
    private val cfgProvider: () -> PackageConfig?,
    private val onCounterObserved: (real: Long, fake: Long) -> Unit,
    private val onSimulationError: (message: String) -> Unit
) : SensorEventListener {

    private val liveSensors = ConcurrentHashMap.newKeySet<Int>()
    private val handlersByType = ConcurrentHashMap<Int, Handler>()
    private val synthActive = ConcurrentHashMap.newKeySet<Int>()
    private val lastCounterValue = AtomicLong(-1L)

    private val thread: HandlerThread by lazy { HandlerThread("StepPhantom-sim").also { it.start() } }
    private val scheduler: Handler by lazy { Handler(thread.looper) }

    val isEmpty: Boolean get() = liveSensors.isEmpty()

    // -------------------- registro / desregistro --------------------

    fun onRegistered(sensor: Sensor, handler: Handler?) {
        val type = sensor.type
        liveSensors.add(type)
        if (handler != null) handlersByType[type] = handler
        if (type == Sensor.TYPE_STEP_COUNTER || type == Sensor.TYPE_STEP_DETECTOR) {
            if (type == Sensor.TYPE_STEP_COUNTER) {
                runCatching { cfgProvider() }.getOrNull()?.let { deliverSyntheticCounter(sensor, it) }
            }
            startSynthetic(sensor)
        }
    }

    fun onUnregistered(type: Int) {
        liveSensors.remove(type)
        synthActive.remove(type)
        handlersByType.remove(type)
        if (type == Sensor.TYPE_STEP_COUNTER) lastCounterValue.set(-1L)
    }

    fun stopAll() {
        liveSensors.clear(); synthActive.clear(); handlersByType.clear()
        runCatching { scheduler.removeCallbacksAndMessages(null) }
        runCatching { thread.quitSafely() }
    }

    // -------------------- eventos reales --------------------

    override fun onSensorChanged(event: SensorEvent) {
        val cfg = runCatching { cfgProvider() }.getOrElse {
            XposedBridge.log("[StepPhantom] config read error ($pkg): $it")
            deliverOriginal(event)
            return
        }
        if (cfg == null || !cfg.enabled || !cfg.hookSensorManager) {
            deliverOriginal(event)
            return
        }
        try {
            when (event.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    if (cfg.mode == TransformMode.ORIGINAL) {
                        val value = event.values[0].toLong()
                        deliverOriginal(event) { lastCounterValue.set(value) }
                    } else {
                        val real = event.values[0].toLong()
                        val fake = engine.transformCounter(pkg, cfg, real)
                        event.values[0] = fake.toFloat()
                        deliverOriginal(event) {
                            lastCounterValue.set(fake)
                            onCounterObserved(real, fake)
                        }
                    }
                }
                Sensor.TYPE_STEP_DETECTOR -> deliverOriginal(event)
                Sensor.TYPE_ACCELEROMETER -> {
                    if (cfg.simAccelerometer) engine.fillAccelerometer(pkg, cfg, event.values)
                    deliverOriginal(event)
                }
                Sensor.TYPE_GYROSCOPE -> {
                    if (cfg.simGyroscope) engine.fillGyroscope(pkg, cfg, event.values)
                    deliverOriginal(event)
                }
                else -> deliverOriginal(event)
            }
        } catch (t: Throwable) {
            XposedBridge.log("[StepPhantom] transform error ($pkg): $t")
            // A transformation failure must not turn one sensor callback into two.
            deliverOriginal(event)
        }
    }

    private fun deliverOriginal(event: SensorEvent, afterDelivery: (() -> Unit)? = null) {
        try {
            original.onSensorChanged(event)
            if (afterDelivery != null) runCatching(afterDelivery)
                .onFailure { XposedBridge.log("[StepPhantom] diagnostic callback error ($pkg): $it") }
        } catch (t: Throwable) {
            XposedBridge.log("[StepPhantom] listener threw ($pkg): $t")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        runCatching { original.onAccuracyChanged(sensor, accuracy) }
    }

    // -------------------- generación sintética --------------------

    private fun startSynthetic(sensor: Sensor) {
        if (!synthActive.add(sensor.type)) return
        scheduleNext(sensor)
    }

    private fun scheduleNext(sensor: Sensor) {
        val type = sensor.type
        if (!synthActive.contains(type)) return
        val cfg = cfgProvider()
        val delay = when (type) {
            Sensor.TYPE_STEP_COUNTER, Sensor.TYPE_STEP_DETECTOR ->
                if (cfg != null) engine.nextStepDelayMs(pkg, cfg) else 1000L
            else -> return
        }.coerceIn(50L, 60_000L)

        scheduler.postDelayed({
            deliverSynthetic(sensor)
            scheduleNext(sensor)
        }, delay)
    }

    private fun deliverSynthetic(sensor: Sensor) {
        try {
            val cfg = cfgProvider() ?: return
            if (!cfg.enabled || !cfg.hookSensorManager) return

            when (sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> deliverSyntheticCounter(sensor, cfg)
                Sensor.TYPE_STEP_DETECTOR -> {
                    if (cfg.mode != TransformMode.RITMO_SIMULADO && cfg.mode != TransformMode.PERSONALIZADO) return
                    if (cfg.paused || cfg.stepsPerMinute <= 0) return
                    pushEvent(sensor, 1.0f)
                }
            }
        } catch (t: Throwable) {
            XposedBridge.log("[StepPhantom] deliverSynthetic error ($pkg): $t")
        }
    }

    private fun deliverSyntheticCounter(sensor: Sensor, cfg: PackageConfig) {
        if (!cfg.enabled || !cfg.hookSensorManager || !engine.needsScheduler(cfg)) return
        if ((cfg.mode == TransformMode.RITMO_SIMULADO || cfg.mode == TransformMode.PERSONALIZADO) && cfg.paused) return

        val fake = engine.syntheticCounter(pkg, cfg)
        if (lastCounterValue.get() == fake) return
        pushEvent(sensor, fake.toFloat()) {
            lastCounterValue.set(fake)
            onCounterObserved(-1L, fake)
        }
    }

    private fun pushEvent(sensor: Sensor, value0: Float, afterDelivery: (() -> Unit)? = null): Boolean {
        val event = buildEvent(sensor, value0) ?: return false
        val deliver = Runnable {
            try {
                original.onSensorChanged(event)
                if (afterDelivery != null) runCatching(afterDelivery)
                    .onFailure { XposedBridge.log("[StepPhantom] diagnostic callback error ($pkg): $it") }
            } catch (t: Throwable) {
                XposedBridge.log("[StepPhantom] synthetic listener threw ($pkg): $t")
                onSimulationError("El listener rechazó el evento sintético: ${t.javaClass.simpleName}")
            }
        }
        val handler = handlersByType[sensor.type]
        return if (handler == null) {
            onSimulationError("No se encontró un hilo de entrega para el sensor")
            false
        } else {
            val accepted = handler.post(deliver)
            if (!accepted) onSimulationError("El hilo de la app ya no acepta eventos")
            accepted
        }
    }

    private fun buildEvent(sensor: Sensor, value0: Float): SensorEvent? {
        val fresh = SensorEventFactory.tryCreate(1) ?: return null
        return runCatching {
            fresh.values[0] = value0
            fresh.sensor = sensor
            fresh.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
            fresh.timestamp = SystemClock.elapsedRealtimeNanos()
            fresh
        }.getOrElse {
            onSimulationError("No se pudo preparar SensorEvent: ${it.javaClass.simpleName}")
            null
        }
    }
}
