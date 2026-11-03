package com.devil.calendar.data.ai

import com.devil.calendar.data.local.workout.ExerciseMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineJsonParserTest {

    @Test
    fun `parses a clean object`() {
        val raw = """
            {"name":"Push Day","exercises":[
              {"name":"Push-ups","sets":4,"reps":12,"rest_seconds":60}
            ]}
        """.trimIndent()

        val draft = RoutineJsonParser.parse(raw)
        assertNotNull(draft)
        assertEquals("Push Day", draft!!.name)
        assertEquals(1, draft.exercises.size)
        assertEquals(4, draft.exercises[0].sets)
        assertEquals(12, draft.exercises[0].reps)
        assertEquals(ExerciseMode.REPS, draft.exercises[0].mode)
    }

    @Test
    fun `strips markdown fences and prose around the json`() {
        val raw = """
            Sure! Here is your routine:

            ```json
            {"name":"Pull","exercises":[{"name":"Pull-ups","sets":3,"reps":8,"rest_seconds":90}]}
            ```

            Let me know if you want it harder.
        """.trimIndent()

        val draft = RoutineJsonParser.parse(raw)
        assertNotNull(draft)
        assertEquals("Pull", draft!!.name)
        assertEquals("Pull-ups", draft.exercises[0].name)
    }

    @Test
    fun `tolerates trailing commas`() {
        val raw = """{"name":"Legs","exercises":[{"name":"Squat","sets":3,"reps":20,"rest_seconds":60,},],}"""
        val draft = RoutineJsonParser.parse(raw)
        assertNotNull(draft)
        assertEquals("Squat", draft!!.exercises[0].name)
    }

    @Test
    fun `detects a hold from hold_seconds`() {
        val raw = """{"name":"Core","exercises":[{"name":"Plank","sets":3,"hold_seconds":45,"rest_seconds":45}]}"""
        val draft = RoutineJsonParser.parse(raw)
        assertNotNull(draft)
        val plank = draft!!.exercises[0]
        assertEquals(ExerciseMode.HOLD, plank.mode)
        assertEquals(45, plank.holdSeconds)
    }

    @Test
    fun `clamps absurd numbers into human ranges`() {
        val raw = """{"name":"X","exercises":[{"name":"Burpees","sets":999,"reps":5000,"rest_seconds":9999}]}"""
        val draft = RoutineJsonParser.parse(raw)
        assertNotNull(draft)
        val burpee = draft!!.exercises[0]
        assertEquals(20, burpee.sets)
        assertEquals(100, burpee.reps)
        assertEquals(600, burpee.restSeconds)
    }

    @Test
    fun `returns null for prose with no object`() {
        assertNull(RoutineJsonParser.parse("Here is a routine: do some push-ups."))
    }

    @Test
    fun `returns null when every exercise has no name`() {
        assertNull(RoutineJsonParser.parse("""{"name":"X","exercises":[{"sets":3,"reps":10}]}"""))
    }

    @Test
    fun `text parser reads a simple line`() {
        val draft = RoutineTextParser.parse("Push-ups 4x12 rest 60s")
        assertNotNull(draft)
        assertEquals(4, draft!!.exercises[0].sets)
        assertEquals(12, draft.exercises[0].reps)
        assertEquals(60, draft.exercises[0].restSeconds)
    }

    @Test
    fun `text parser reads a hold`() {
        val draft = RoutineTextParser.parse("Plank 3 x 45s hold")
        assertNotNull(draft)
        assertEquals(ExerciseMode.HOLD, draft!!.exercises[0].mode)
        assertEquals(45, draft.exercises[0].holdSeconds)
    }

    @Test
    fun `text parser reads several lines`() {
        val draft = RoutineTextParser.parse(
            """
            Push-ups 4x12 rest 60s
            Pull-ups 3x8 @90s
            Plank 3x45s hold
            """.trimIndent(),
        )
        assertNotNull(draft)
        assertEquals(3, draft!!.exercises.size)
        assertTrue(draft.exercises[1].restSeconds == 90)
    }
}
