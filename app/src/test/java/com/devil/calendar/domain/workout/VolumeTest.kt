package com.devil.calendar.domain.workout

import com.devil.calendar.data.local.workout.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeTest {

    @Test
    fun `set volume multiplies reps by weight`() {
        assertEquals(800.0, Volume.setVolume(8, 100.0), 0.001)
    }

    @Test
    fun `negative input is clamped to zero`() {
        assertEquals(0.0, Volume.setVolume(-5, 100.0), 0.001)
        assertEquals(0.0, Volume.setVolume(5, -100.0), 0.001)
    }

    @Test
    fun `total volume sums every set`() {
        val sets = listOf(8 to 100.0, 8 to 100.0, 6 to 90.0)
        assertEquals(2140.0, Volume.totalVolume(sets), 0.001)
    }

    @Test
    fun `pounds convert to kilograms and back`() {
        val kg = Volume.toKilograms(220.0, WeightUnit.LB)
        assertEquals(99.79, kg, 0.05)
        assertEquals(220.0, Volume.fromKilograms(kg, WeightUnit.LB), 0.05)
    }

    @Test
    fun `kilograms pass through unchanged`() {
        assertEquals(100.0, Volume.toKilograms(100.0, WeightUnit.KG), 0.001)
    }

    @Test
    fun `format groups thousands and keeps one decimal when needed`() {
        assertEquals("1250 kg", Volume.format(1250.0, WeightUnit.KG))
        assertEquals("82.5 kg", Volume.format(82.5, WeightUnit.KG))
        assertEquals("45 lb", Volume.format(45.0, WeightUnit.LB))
    }
}
