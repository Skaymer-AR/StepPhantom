package com.stepphantom.engine

import com.stepphantom.config.PackageConfig
import com.stepphantom.config.TransformMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class StepTransformationEngineTest {

    private var nowNanos = 0L
    private val engine = StepTransformationEngine { nowNanos }
    private var wallMillis = atUtc(2026, 10, 15, 16, 0)
    private val scheduledEngine = StepTransformationEngine(
        nowNanos = { nowNanos },
        nowMillis = { wallMillis },
        zoneId = { ZoneId.of("UTC") }
    )
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

    @Test
    fun scheduledGoalRampsAcrossWindowAndRepeatsWithoutResettingTheCounter() {
        val anchor = LocalDate.of(2026, 10, 15).toEpochDay()
        val cfg = PackageConfig(
            enabled = true,
            mode = TransformMode.OBJETIVO_PROGRAMADO,
            scheduledGoalSteps = 7_000L,
            scheduledStartMinute = 16 * 60,
            scheduledEndMinute = 20 * 60,
            scheduledAnchorEpochDay = anchor
        )

        assertEquals(0L, scheduledEngine.syntheticCounter("test.schedule", cfg))
        assertTrue(scheduledEngine.nextStepDelayMs("test.schedule", cfg) in 2_057L..2_058L)

        wallMillis = atUtc(2026, 10, 15, 18, 0)
        assertEquals(3_500L, scheduledEngine.syntheticCounter("test.schedule", cfg))
        wallMillis = atUtc(2026, 10, 15, 20, 0)
        assertEquals(7_000L, scheduledEngine.syntheticCounter("test.schedule", cfg))
        wallMillis = atUtc(2026, 10, 16, 15, 59)
        assertEquals(7_000L, scheduledEngine.syntheticCounter("test.schedule", cfg))
        wallMillis = atUtc(2026, 10, 16, 18, 0)
        assertEquals(10_500L, scheduledEngine.syntheticCounter("test.schedule", cfg))
    }

    @Test
    fun scheduledGoalSupportsOvernightWindowsAndPauseFreezesProgress() {
        val overnight = PackageConfig(
            enabled = true,
            mode = TransformMode.OBJETIVO_PROGRAMADO,
            scheduledGoalSteps = 100L,
            scheduledStartMinute = 22 * 60,
            scheduledEndMinute = 2 * 60,
            scheduledAnchorEpochDay = LocalDate.of(2026, 10, 15).toEpochDay()
        )
        wallMillis = atUtc(2026, 10, 15, 23, 0)
        assertEquals(25L, scheduledEngine.syntheticCounter("test.overnight", overnight))
        wallMillis = atUtc(2026, 10, 16, 1, 0)
        assertEquals(75L, scheduledEngine.syntheticCounter("test.overnight", overnight))

        val daily = overnight.copy(
            scheduledGoalSteps = 7_000L,
            scheduledStartMinute = 16 * 60,
            scheduledEndMinute = 20 * 60,
            scheduledAnchorEpochDay = LocalDate.of(2026, 10, 15).toEpochDay()
        )
        wallMillis = atUtc(2026, 10, 15, 17, 0)
        val countAtPause = scheduledEngine.syntheticCounter("test.pause-schedule", daily)
        assertEquals(1_750L, countAtPause)
        val paused = daily.copy(paused = true)
        assertEquals(countAtPause, scheduledEngine.syntheticCounter("test.pause-schedule", paused))
        wallMillis = atUtc(2026, 10, 15, 18, 0)
        assertEquals(countAtPause, scheduledEngine.syntheticCounter("test.pause-schedule", paused))
        assertEquals(countAtPause, scheduledEngine.syntheticCounter("test.pause-schedule", daily))
        wallMillis = atUtc(2026, 10, 15, 18, 30)
        assertEquals(2_625L, scheduledEngine.syntheticCounter("test.pause-schedule", daily))
    }

    @Test
    fun detectorEmitsOnlyWhenTheScheduledCounterAdvances() {
        val cfg = PackageConfig(
            enabled = true,
            mode = TransformMode.OBJETIVO_PROGRAMADO,
            scheduledGoalSteps = 7_000L,
            scheduledStartMinute = 16 * 60,
            scheduledEndMinute = 20 * 60,
            scheduledAnchorEpochDay = LocalDate.of(2026, 10, 15).toEpochDay()
        )
        wallMillis = atUtc(2026, 10, 15, 16, 0)
        val delay = scheduledEngine.nextStepDelayMs("test.detector", cfg)
        wallMillis += delay

        assertTrue(scheduledEngine.shouldEmitScheduledDetectorStep("test.detector", cfg))
        assertTrue(!scheduledEngine.shouldEmitScheduledDetectorStep("test.detector", cfg))
    }

    @Test
    fun scheduledModeUsesItsTargetInsteadOfRealCounterReadings() {
        val cfg = PackageConfig(
            enabled = true,
            mode = TransformMode.OBJETIVO_PROGRAMADO,
            scheduledGoalSteps = 7_000L,
            scheduledStartMinute = 16 * 60,
            scheduledEndMinute = 20 * 60,
            scheduledAnchorEpochDay = LocalDate.of(2026, 10, 15).toEpochDay()
        )
        wallMillis = atUtc(2026, 10, 15, 17, 0)
        assertEquals(1_750L, scheduledEngine.transformCounter("test.real-counter", cfg, 4_000L))
        wallMillis = atUtc(2026, 10, 15, 18, 0)
        assertEquals(3_500L, scheduledEngine.transformCounter("test.real-counter", cfg, 9_000L))
    }

    private fun atUtc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()
}
