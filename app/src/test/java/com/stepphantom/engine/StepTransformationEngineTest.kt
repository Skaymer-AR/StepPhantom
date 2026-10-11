package com.stepphantom.engine

import com.stepphantom.config.PackageConfig
import com.stepphantom.config.TransformMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepTransformationEngineTest {

    private var nowNanos = 0L
    private val engine = StepTransformationEngine { nowNanos }
    private val config = PackageConfig(
        enabled = true,
        mode = TransformMode.RITMO_SIMULADO,
        stepsPerMinute = 100
    )

    @Test
    fun counterAdvancesOnceAtEachVariableCadenceBoundary() {
        var count = engine.syntheticCounter("test.target", config)
        val observedIntervals = mutableSetOf<Long>()

        repeat(200) {
            val delayMs = engine.nextStepDelayMs("test.target", config)
            assertTrue("unexpected step interval: ${delayMs}ms", delayMs in 550L..650L)
            observedIntervals += delayMs
            nowNanos += delayMs * 1_000_000L

            val next = engine.syntheticCounter("test.target", config)
            assertEquals(count + 1L, next)
            count = next
        }

        assertTrue("cadence should not be a fixed metronome", observedIntervals.size > 1)
    }

    @Test
    fun pauseFreezesTheStepScheduleAndResumesAtItsRemainingInterval() {
        val packageName = "test.pause"
        engine.syntheticCounter(packageName, config)
        val firstDelayMs = engine.nextStepDelayMs(packageName, config)
        nowNanos += (firstDelayMs / 2L) * 1_000_000L

        val pausedConfig = config.copy(paused = true)
        val countAtPause = engine.syntheticCounter(packageName, pausedConfig)
        nowNanos += 10L * 60L * 1_000_000_000L
        assertEquals(countAtPause, engine.syntheticCounter(packageName, pausedConfig))

        val remainingMs = engine.nextStepDelayMs(packageName, config)
        nowNanos += remainingMs * 1_000_000L
        assertEquals(countAtPause + 1L, engine.syntheticCounter(packageName, config))
    }
}
