package com.stepphantom.engine

import android.os.SystemClock
import com.stepphantom.config.ModuleConfig
import com.stepphantom.config.WalkMode
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Motor de simulación puro. No sabe nada de sensores ni de Xposed: sólo
 * transforma "tiempo transcurrido" + "configuración" en valores.
 *
 * Está pensado para ser instanciado por [com.stepphantom.xposed.SensorListenerWrapper],
 * uno por proceso hookeado. Toda la coherencia temporal se apoya en
 * [SystemClock.elapsedRealtimeNanos], que es monotónico y no salta con
 * cambios de hora ni con el device dormido/despierto (es la MISMA fuente
 * que usa el framework para timestamp de eventos de sensores).
 *
 * Garantías que ofrece:
 *  - El contador de pasos NUNCA decrece (monotónico).
 *  - El contador NUNCA es negativo.
 *  - El tiempo en pausa no cuenta para acumular pasos.
 *
 * Lo que NO garantiza (a propósito): física realista. Es "razonable y variable",
 * como pediste, no un modelo biomecánico.
 */
class StepSimulationEngine(
    @Volatile private var config: ModuleConfig
) {

    // t0 de la simulación (referencia monotónica).
    private var startNanos: Long = SystemClock.elapsedRealtimeNanos()

    // Nanosegundos totales acumulados en estado de pausa (no cuentan para pasos).
    private var pausedAccumNanos: Long = 0L

    // Momento en que arrancó la pausa activa; -1 si no estamos en pausa.
    private var pauseStartNanos: Long = -1L

    // Último valor entregado del contador. Sirve para forzar monotonicidad
    // aun cuando el jitter genere un candidato menor.
    private var lastCounterValue: Long = -1L

    // Semilla estable para el patrón, así el ruido no "salta" entre lecturas.
    private val seed: Long = Random.nextLong()

    // ---------------------------------------------------------------------
    // Configuración / ciclo de vida
    // ---------------------------------------------------------------------

    @Synchronized
    fun updateConfig(newConfig: ModuleConfig) {
        this.config = newConfig
    }

    /** Reinicia el contador a los pasos iniciales configurados y resetea el reloj. */
    @Synchronized
    fun reset() {
        startNanos = SystemClock.elapsedRealtimeNanos()
        pausedAccumNanos = 0L
        pauseStartNanos = if (isPaused()) SystemClock.elapsedRealtimeNanos() else -1L
        lastCounterValue = -1L
    }

    private fun isPaused(): Boolean =
        config.paused || config.walkMode == WalkMode.PAUSE

    // Nanosegundos "activos": tiempo total menos el tiempo pasado en pausa.
    @Synchronized
    private fun activeNanos(now: Long): Long {
        var paused = pausedAccumNanos
        if (isPaused()) {
            if (pauseStartNanos < 0) pauseStartNanos = now
            paused += (now - pauseStartNanos)
        } else if (pauseStartNanos >= 0) {
            // Salimos de una pausa: consolidamos el tiempo pausado.
            pausedAccumNanos += (now - pauseStartNanos)
            pauseStartNanos = -1L
            paused = pausedAccumNanos
        }
        val active = (now - startNanos) - paused
        return if (active < 0) 0L else active
    }

    // ---------------------------------------------------------------------
    // TYPE_STEP_COUNTER  ->  contador monotónico acumulado
    // ---------------------------------------------------------------------

    /**
     * Devuelve el total de pasos acumulados en este instante.
     * Monotónico y no-negativo por construcción.
     */
    @Synchronized
    fun currentStepCount(): Long {
        val now = SystemClock.elapsedRealtimeNanos()
        val activeMinutes = activeNanos(now) / 60_000_000_000.0
        val spm = config.stepsPerMinute.coerceAtLeast(0)

        var base = config.initialSteps + (activeMinutes * spm)

        if (config.walkMode == WalkMode.SOFT && config.jitterPercent > 0f) {
            // "Caminar suave": pequeña variación pseudo-aleatoria pero suave
            // (una onda lenta), nunca una sierra brusca.
            val wobble = sin((now + seed) / 3_000_000_000.0) * spm * (config.jitterPercent / 100.0)
            base += wobble
        }

        var value = base.toLong()
        if (value < 0) value = 0
        if (value < config.initialSteps) value = config.initialSteps
        // Monotonicidad estricta.
        if (value < lastCounterValue) value = lastCounterValue
        lastCounterValue = value
        return value
    }

    // ---------------------------------------------------------------------
    // TYPE_STEP_DETECTOR  ->  eventos discretos (valor 1.0f)
    // ---------------------------------------------------------------------

    /** Milisegundos entre pasos según pasos/minuto. Devuelve un valor grande si spm<=0. */
    fun stepDetectorIntervalMs(): Long {
        val spm = config.stepsPerMinute
        if (spm <= 0 || isPaused()) return Long.MAX_VALUE / 4
        val base = 60_000.0 / spm
        val jitter = if (config.jitterPercent > 0f) {
            base * (config.jitterPercent / 100.0) * (Random.nextDouble() - 0.5) * 2
        } else 0.0
        return (base + jitter).toLong().coerceAtLeast(50L)
    }

    // ---------------------------------------------------------------------
    // TYPE_ACCELEROMETER  ->  patrón de caminata simple (m/s^2)
    // ---------------------------------------------------------------------

    /**
     * Rellena [out] (tamaño >= 3) con x, y, z simulando una caminata.
     * En pausa devuelve gravedad estática con ruido mínimo.
     */
    @Synchronized
    fun fillAccelerometer(out: FloatArray) {
        val now = SystemClock.elapsedRealtimeNanos()
        val g = 9.81f

        if (isPaused() || config.stepsPerMinute <= 0) {
            out[0] = noise(0.02f)
            out[1] = noise(0.02f)
            out[2] = g + noise(0.02f)
            return
        }

        // Dos "rebotes" verticales por paso -> freq = (spm/60) * 2 Hz
        val freqHz = (config.stepsPerMinute / 60.0) * 2.0
        val t = activeNanos(now) / 1_000_000_000.0
        val phase = 2.0 * PI * freqHz * t
        val amp = 1.5f + config.jitterPercent / 100f

        out[0] = (amp * 0.6f * sin(phase + 0.5)).toFloat() + noise(0.1f)
        out[1] = (amp * 0.4f * sin(phase / 2.0)).toFloat() + noise(0.1f)
        out[2] = g + (amp * sin(phase)).toFloat() + noise(0.1f)
    }

    // ---------------------------------------------------------------------
    // TYPE_GYROSCOPE  ->  microvariaciones suaves (rad/s)
    // ---------------------------------------------------------------------

    @Synchronized
    fun fillGyroscope(out: FloatArray) {
        val now = SystemClock.elapsedRealtimeNanos()
        if (isPaused()) {
            out[0] = noise(0.005f); out[1] = noise(0.005f); out[2] = noise(0.005f)
            return
        }
        val freqHz = (config.stepsPerMinute / 60.0).coerceAtLeast(0.5)
        val t = activeNanos(now) / 1_000_000_000.0
        val phase = 2.0 * PI * freqHz * t
        out[0] = (0.04 * sin(phase)).toFloat() + noise(0.01f)
        out[1] = (0.03 * sin(phase + 1.0)).toFloat() + noise(0.01f)
        out[2] = (0.02 * sin(phase + 2.0)).toFloat() + noise(0.01f)
    }

    private fun noise(scale: Float): Float =
        ((Random.nextFloat() - 0.5f) * 2f) * scale
}
