package com.devil.calendar.camera

import com.devil.calendar.data.local.workout.ExerciseMode
import com.devil.calendar.domain.coach.CoachConfig
import com.devil.calendar.domain.coach.CoachMode
import com.devil.calendar.domain.coach.MovementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.random.Random

class RepCounterTest {

    private fun config() = CoachConfig(
        exerciseKey = "push up",
        movement = MovementType.PUSH_UP,
        mode = CoachMode.REPS,
        cameraAngle = "Side on",
        cameraDistance = "2 m",
        tip = "",
        primaryJoint = "elbow",
        downAngle = 95f,
        upAngle = 160f,
        tolerance = 12f,
        minRepMillis = 600L,
        fromTemplate = true,
    )

    /** Depth signal: 0 at the top, 1 at the bottom, one cycle per [periodMs]. */
    private fun reps(
        count: Int,
        periodMs: Int,
        fps: Int,
        amplitude: Float = 1f,
        noise: Float = 0f,
        dropoutFrames: Int = 0,
        random: Random = Random(7),
    ): List<Pair<Float, Long>> {
        val step = 1000L / fps
        val out = mutableListOf<Pair<Float, Long>>()
        var t = 0L
        var frameIndex = 0
        repeat(count) {
            var elapsed = 0
            while (elapsed < periodMs) {
                val phase = elapsed.toFloat() / periodMs
                val value = ((1 - cos(2 * PI * phase)) / 2).toFloat() * amplitude
                val jitter = if (noise > 0f) (random.nextFloat() - 0.5f) * noise else 0f
                val drop = dropoutFrames > 0 && frameIndex % (periodMs / (1000 / fps)) in 1..dropoutFrames
                if (!drop) out += (value + jitter) to t
                frameIndex++
                elapsed += step.toInt()
                t += step
            }
        }
        // Settle back at the top so the final up-crossing is seen.
        repeat(fps / 2) {
            out += 0f to t
            t += step
        }
        return out
    }

    private fun run(samples: List<Pair<Float, Long>>): RepCounter {
        val counter = RepCounter(config())
        samples.forEach { (value, time) -> counter.onSignal(value, time) }
        return counter
    }

    @Test
    fun `eight clean reps at 30 fps`() {
        val counter = run(reps(count = 8, periodMs = 2000, fps = 30, noise = 0.02f))
        assertEquals(8, counter.reps)
    }

    @Test
    fun `eight clean reps at 12 fps`() {
        val counter = run(reps(count = 8, periodMs = 2000, fps = 12, noise = 0.02f))
        assertEquals(8, counter.reps)
    }

    @Test
    fun `landmark dropouts do not lose reps`() {
        val counter = run(reps(count = 8, periodMs = 2000, fps = 30, noise = 0.01f, dropoutFrames = 5))
        assertEquals(8, counter.reps)
    }

    @Test
    fun `fast reps about one per second`() {
        val counter = run(reps(count = 6, periodMs = 1000, fps = 30, noise = 0.02f))
        assertEquals(6, counter.reps)
    }

    @Test
    fun `slow reps about four seconds each`() {
        val counter = run(reps(count = 3, periodMs = 4000, fps = 30, noise = 0.02f))
        assertEquals(3, counter.reps)
    }

    @Test
    fun `no movement counts nothing`() {
        val flat = (0 until 300).map { 0.5f to it * 33L }
        assertEquals(0, run(flat).reps)
    }

    @Test
    fun `a single spike does not add a rep`() {
        val samples = reps(count = 3, periodMs = 2000, fps = 30).toMutableList()
        samples[150] = 60f to samples[150].second
        assertEquals(3, run(samples).reps)
    }

    @Test
    fun `a shallow rep is counted and flagged`() {
        // Two full reps establish the range, then one at 50% depth.
        val deep = reps(count = 2, periodMs = 2000, fps = 30)
        val start = (deep.lastOrNull()?.second ?: 0L) + 33L
        val shallow = reps(count = 1, periodMs = 2000, fps = 30, amplitude = 0.5f)
            .map { (v, t) -> v to (t + start) }
        val counter = run(deep + shallow)
        assertEquals(3, counter.reps)
        assertTrue("expected the shallow rep to be flagged", counter.shallow >= 1)
    }
}
