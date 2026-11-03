package com.devil.calendar.data.ai

import com.devil.calendar.data.local.workout.ExerciseMode

/**
 * Parses hand-written routines, offline:
 *
 *   Push-ups 4x12 rest 60s
 *   Plank 3 x 45s hold
 *   Pull-ups 3x8 @90s
 *
 * A trailing `s`/`sec` on the count makes it a hold; anything else is reps.
 */
object RoutineTextParser {

    private val lineRegex = Regex(
        """^\s*[-*\u2022]?\s*(.+?)\s+(\d{1,2})\s*[x\u00d7]\s*(\d{1,3})\s*(s|sec|secs|seconds)?\b(.*)$""",
        RegexOption.IGNORE_CASE,
    )
    private val restRegex = Regex("""(?:rest|@)\s*(\d{1,3})\s*(?:s|sec|secs|seconds)?""", RegexOption.IGNORE_CASE)
    private val nameLineRegex = Regex("""^\s*(?:routine|workout|name)\s*[:\-]\s*(.+)$""", RegexOption.IGNORE_CASE)

    fun parse(text: String): RoutineDraft? {
        if (text.isBlank()) return null
        val lines = text.lines()
        val title = lines.firstNotNullOfOrNull { nameLineRegex.find(it)?.groupValues?.get(1)?.trim() }

        val exercises = lines.mapNotNull { parseLine(it) }.take(30)
        if (exercises.isEmpty()) return null

        return RoutineDraft(
            name = title?.take(40)?.ifBlank { null } ?: "Imported routine",
            exercises = exercises,
        )
    }

    private fun parseLine(line: String): DraftExercise? {
        val match = lineRegex.find(line) ?: return null
        val name = match.groupValues[1].trim().trim('-', '*', '\u2022').trim()
        if (name.isEmpty()) return null
        val sets = match.groupValues[2].toIntOrNull() ?: return null
        val count = match.groupValues[3].toIntOrNull() ?: return null
        val unit = match.groupValues[4].lowercase()
        val tail = match.groupValues[5]

        val isHold = unit.startsWith("s") || tail.contains("hold", ignoreCase = true)
        val rest = restRegex.find(tail)?.groupValues?.get(1)?.toIntOrNull()
            ?: restRegex.find(line)?.groupValues?.get(1)?.toIntOrNull()
            ?: 60

        return DraftExercise(
            name = name.take(40),
            sets = sets.coerceIn(1, 20),
            mode = if (isHold) ExerciseMode.HOLD else ExerciseMode.REPS,
            reps = if (isHold) 0 else count.coerceIn(1, 100),
            holdSeconds = if (isHold) count.coerceIn(5, 600) else 0,
            restSeconds = rest.coerceIn(0, 600),
        )
    }
}
