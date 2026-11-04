package com.devil.calendar.domain.walk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

class WalkMathTest {

    // ---- step baseline ----

    @Test
    fun `steps today is the delta from the baseline`() {
        val r = StepBaseline.stepsToday(rawSensor = 1500, storedBaseline = 1000, storedSteps = 0, isNewDay = false)
        assertEquals(500, r.stepsToday)
    }

    @Test
    fun `a reboot re-baselines instead of going negative`() {
        val r = StepBaseline.stepsToday(rawSensor = 40, storedBaseline = 9000, storedSteps = 300, isNewDay = false)
        assertEquals(0, r.stepsToday)
        assertEquals(40L, r.baseline)
        assertTrue(r.needsPersist)
    }

    @Test
    fun `a new day resets the count`() {
        val r = StepBaseline.stepsToday(rawSensor = 9000, storedBaseline = 1000, storedSteps = 8000, isNewDay = true)
        assertEquals(0, r.stepsToday)
        assertEquals(9000L, r.baseline)
    }

    // ---- dead reckoning ----

    @Test
    fun `steps north accumulate on the y axis`() {
        val pdr = DeadReckoning(stepLengthMetres = 1.0)
        pdr.onHeading(0.0)
        repeat(10) { pdr.onStep() }
        val (x, y) = pdr.path.last()
        assertTrue("expected to move north", y > 9.0)
        assertTrue("expected no sideways drift", abs(x) < 0.5)
    }

    @Test
    fun `steps east accumulate on the x axis`() {
        val pdr = DeadReckoning(stepLengthMetres = 1.0)
        pdr.onHeading(90.0)
        repeat(10) { pdr.onStep() }
        val (x, y) = pdr.path.last()
        assertTrue("expected to move east", x > 9.0)
        assertTrue(abs(y) < 0.5)
    }

    @Test
    fun `heading smoothing moves toward the new bearing`() {
        val pdr = DeadReckoning()
        pdr.onHeading(0.0)
        pdr.onHeading(90.0, alpha = 0.2)
        pdr.onStep()
        val (x, _) = pdr.path.last()
        // One smoothed step: some eastward movement, but nowhere near a full 90.
        assertTrue("expected partial turn", x > 0.0 && x < 0.5)
    }

    @Test
    fun `polyline round trips`() {
        val points = listOf(0.0 to 0.0, 1.5 to 2.25, -3.0 to 4.75)
        val decoded = DeadReckoning.decode(DeadReckoning.encode(points))
        assertEquals(points.size, decoded.size)
        assertEquals(1.5, decoded[1].first, 0.01)
        assertEquals(4.75, decoded[2].second, 0.01)
    }

    @Test
    fun `step length follows height and stays sane`() {
        assertEquals(0.705, DeadReckoning.stepLengthForHeight(170), 0.01)
        assertTrue(DeadReckoning.stepLengthForHeight(50) >= 0.3)
        assertTrue(DeadReckoning.stepLengthForHeight(250) <= 1.2)
    }

    // ---- word of the day ----

    @Test
    fun `there are at least 366 distinct phrases`() {
        assertTrue("got ${WordOfDay.all.size}", WordOfDay.all.size >= 366)
        assertEquals(WordOfDay.all.size, WordOfDay.all.distinct().size)
    }

    @Test
    fun `the word is stable for a date and varies across days`() {
        val jan1 = LocalDate.of(2026, 1, 1)
        assertEquals(WordOfDay.forDate(jan1), WordOfDay.forDate(jan1))
        assertNotEquals(WordOfDay.forDate(jan1), WordOfDay.forDate(jan1.plusDays(1)))
    }

    @Test
    fun `a whole year never repeats a word`() {
        val start = LocalDate.of(2026, 1, 1)
        val words = (0 until 365).map { WordOfDay.forDate(start.plusDays(it.toLong())) }
        assertEquals("a year must not repeat", 365, words.distinct().size)
    }

    @Test
    fun `progress is clamped`() {
        assertEquals(0f, WordOfDay.progress(0, 1000))
        assertEquals(0.5f, WordOfDay.progress(500, 1000))
        assertEquals(1f, WordOfDay.progress(2000, 1000))
        assertEquals(0f, WordOfDay.progress(500, 0))
    }
}
