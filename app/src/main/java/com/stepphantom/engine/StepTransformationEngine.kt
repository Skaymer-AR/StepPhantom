package com.stepphantom.engine

import android.os.SystemClock
import com.stepphantom.config.PackageConfig
import com.stepphantom.config.TransformMode
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Motor de transformación de pasos. Puro: no sabe de sensores ni de Xposed.
 * Mantiene estado de sesión POR PAQUETE (aunque en la práctica cada proceso
 * hookeado es un paquete, guardamos por clave para no asumir).
 *
 * Garantías del contador:
 *  - Monotónico dentro de la sesión (nunca baja).
 *  - No negativo.
 *  - Sin doble aplicación (transformamos el valor REAL una sola vez por evento).
 *  - Sin overflow silencioso (clamp a Long).
 *  - Coherencia temporal con SystemClock.elapsedRealtimeNanos().
 */
class StepTransformationEngine {

    private class Session {
        var realBaseline: Long = -1L      // primer valor real observado
        var lastEmitted: Long = -1L       // último valor entregado (para monotonicidad)
        var startNanos: Long = SystemClock.elapsedRealtimeNanos()
        var pausedAccumNanos: Long = 0L
        var pauseStartNanos: Long = -1L
        var seed: Long = Random.nextLong()
        var lastResetToken: Long = Long.MIN_VALUE
    }

    private val sessions = ConcurrentHashMap<String, Session>()

    private fun session(pkg: String): Session =
        sessions.getOrPut(pkg) { Session() }

    private fun maybeReset(s: Session, cfg: PackageConfig) {
        if (cfg.resetToken != s.lastResetToken) {
            s.lastResetToken = cfg.resetToken
            s.realBaseline = -1L
            s.lastEmitted = -1L
            s.startNanos = SystemClock.elapsedRealtimeNanos()
            s.pausedAccumNanos = 0L
            s.pauseStartNanos = -1L
        }
    }

    private fun activeNanos(s: Session, cfg: PackageConfig, now: Long): Long {
        var paused = s.pausedAccumNanos
        if (cfg.paused) {
            if (s.pauseStartNanos < 0) s.pauseStartNanos = now
            paused += (now - s.pauseStartNanos)
        } else if (s.pauseStartNanos >= 0) {
            s.pausedAccumNanos += (now - s.pauseStartNanos)
            s.pauseStartNanos = -1L
            paused = s.pausedAccumNanos
        }
        val active = (now - s.startNanos) - paused
        return if (active < 0) 0L else active
    }

    private fun applyLimitsAndJitter(cfg: PackageConfig, s: Session, value: Long): Long {
        var v = value
        if (cfg.jitterPercent > 0f) {
            // Jitter determinístico por sesión (no cambia en cada lectura sin avanzar tiempo).
            val rnd = Random(s.seed xor (v * 2654435761L))
            val delta = (v * (cfg.jitterPercent / 100.0) * (rnd.nextDouble() - 0.5) * 2).roundToLong()
            v += delta
        }
        if (cfg.minLimit > 0 && v < cfg.minLimit) v = cfg.minLimit
        if (cfg.maxLimit >= 0 && v > cfg.maxLimit) v = cfg.maxLimit
        if (v < 0) v = 0
        return v
    }

    private fun enforceMonotonic(s: Session, value: Long): Long {
        var v = value
        if (v < 0) v = 0
        if (s.lastEmitted >= 0 && v < s.lastEmitted) v = s.lastEmitted
        s.lastEmitted = v
        return v
    }

    // -----------------------------------------------------------------
    // Contador con valor real disponible (evento real de TYPE_STEP_COUNTER)
    // -----------------------------------------------------------------

    /**
     * Transforma un valor real de contador según la config del paquete.
     * Devuelve el valor a entregar al listener. Monotónico y no negativo.
     */
    @Synchronized
    fun transformCounter(pkg: String, cfg: PackageConfig, realValue: Long): Long {
        val s = session(pkg)
        maybeReset(s, cfg)
        if (s.realBaseline < 0) s.realBaseline = realValue
        val sessionSteps = (realValue - s.realBaseline).coerceAtLeast(0)
        val now = SystemClock.elapsedRealtimeNanos()

        val raw: Long = when (cfg.mode) {
            TransformMode.ORIGINAL -> realValue
            TransformMode.SUMAR -> safeAdd(realValue, cfg.offset)
            TransformMode.MULTIPLICAR -> safeMul(realValue, cfg.multiplier)
            TransformMode.REEMPLAZAR -> cfg.replaceValue
            TransformMode.RITMO_SIMULADO -> rhythmValue(cfg, s, now)
            TransformMode.PERSONALIZADO -> {
                val ritmo = if (cfg.stepsPerMinute > 0) rhythmDelta(cfg, s, now) else 0L
                val base = safeAdd(cfg.initialSteps, safeMul(sessionSteps, cfg.multiplier))
                applyLimitsAndJitter(cfg, s, safeAdd(safeAdd(base, cfg.offset), ritmo))
            }
        }
        return enforceMonotonic(s, raw)
    }

    // -----------------------------------------------------------------
    // Contador SIN valor real (tick del scheduler: REEMPLAZAR / RITMO_SIMULADO)
    // -----------------------------------------------------------------

    /** Valor sintético para modos que deben empujar aunque no haya pasos reales. */
    @Synchronized
    fun syntheticCounter(pkg: String, cfg: PackageConfig): Long {
        val s = session(pkg)
        maybeReset(s, cfg)
        val now = SystemClock.elapsedRealtimeNanos()
        val raw = when (cfg.mode) {
            TransformMode.REEMPLAZAR -> cfg.replaceValue
            TransformMode.RITMO_SIMULADO -> rhythmValue(cfg, s, now)
            else -> s.lastEmitted.coerceAtLeast(0) // otros modos no se sintetizan
        }
        return enforceMonotonic(s, raw)
    }

    /** ¿Este modo necesita empuje activo del scheduler aunque no haya movimiento? */
    fun needsScheduler(cfg: PackageConfig): Boolean =
        cfg.mode == TransformMode.REEMPLAZAR || cfg.mode == TransformMode.RITMO_SIMULADO

    /** Milisegundos entre pasos para TYPE_STEP_DETECTOR sintético (RITMO_SIMULADO). */
    fun detectorIntervalMs(cfg: PackageConfig): Long {
        if (cfg.paused || cfg.stepsPerMinute <= 0) return Long.MAX_VALUE / 4
        return (60_000.0 / cfg.stepsPerMinute).toLong().coerceAtLeast(50L)
    }

    // -----------------------------------------------------------------
    // Acelerómetro / giroscopio (experimental, opcional)
    // -----------------------------------------------------------------

    fun fillAccelerometer(pkg: String, cfg: PackageConfig, out: FloatArray) {
        val s = session(pkg)
        val now = SystemClock.elapsedRealtimeNanos()
        val g = 9.81f
        if (cfg.paused || cfg.stepsPerMinute <= 0) {
            out[0] = 0f; out[1] = 0f; out[2] = g; return
        }
        val freq = (cfg.stepsPerMinute / 60.0) * 2.0
        val t = activeNanos(s, cfg, now) / 1_000_000_000.0
        val phase = 2.0 * Math.PI * freq * t
        out[0] = (0.9 * Math.sin(phase + 0.5)).toFloat()
        out[1] = (0.6 * Math.sin(phase / 2.0)).toFloat()
        out[2] = g + (1.4 * Math.sin(phase)).toFloat()
    }

    fun fillGyroscope(pkg: String, cfg: PackageConfig, out: FloatArray) {
        val s = session(pkg)
        val now = SystemClock.elapsedRealtimeNanos()
        if (cfg.paused) { out[0] = 0f; out[1] = 0f; out[2] = 0f; return }
        val freq = (cfg.stepsPerMinute / 60.0).coerceAtLeast(0.5)
        val t = activeNanos(s, cfg, now) / 1_000_000_000.0
        val phase = 2.0 * Math.PI * freq * t
        out[0] = (0.04 * Math.sin(phase)).toFloat()
        out[1] = (0.03 * Math.sin(phase + 1.0)).toFloat()
        out[2] = (0.02 * Math.sin(phase + 2.0)).toFloat()
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private fun rhythmDelta(cfg: PackageConfig, s: Session, now: Long): Long {
        val minutes = activeNanos(s, cfg, now) / 60_000_000_000.0
        return (minutes * cfg.stepsPerMinute.coerceAtLeast(0)).toLong().coerceAtLeast(0)
    }

    private fun rhythmValue(cfg: PackageConfig, s: Session, now: Long): Long {
        val v = safeAdd(cfg.initialSteps, rhythmDelta(cfg, s, now))
        return applyLimitsAndJitter(cfg, s, v)
    }

    private fun safeAdd(a: Long, b: Long): Long {
        val r = a + b
        // Detección simple de overflow por signos.
        return if (((a xor r) and (b xor r)) < 0) {
            if (b > 0) Long.MAX_VALUE else 0L
        } else r
    }

    private fun safeMul(a: Long, m: Float): Long {
        if (m <= 0f) return 0L
        val r = a.toDouble() * m.toDouble()
        return when {
            r >= Long.MAX_VALUE.toDouble() -> Long.MAX_VALUE
            r <= 0.0 -> 0L
            else -> r.roundToLong()
        }
    }
}
