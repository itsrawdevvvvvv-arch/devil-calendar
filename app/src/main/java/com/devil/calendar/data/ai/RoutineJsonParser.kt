package com.devil.calendar.data.ai

import com.devil.calendar.data.local.workout.ExerciseMode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class RoutineJson(
    val name: String = "",
    val exercises: List<ExerciseJson> = emptyList(),
)

@Serializable
internal data class ExerciseJson(
    val name: String = "",
    val sets: Int = 3,
    val reps: Int? = null,
    @SerialName("hold_seconds") val holdSeconds: Int? = null,
    @SerialName("holdSeconds") val holdSecondsCamel: Int? = null,
    @SerialName("rest_seconds") val restSeconds: Int? = null,
    @SerialName("restSeconds") val restSecondsCamel: Int? = null,
    val rest: Int? = null,
    val mode: String? = null,
    val hold: Boolean? = null,
)

/**
 * Turns whatever an AI returns into a [RoutineDraft].
 *
 * Models wrap JSON in code fences, add a sentence before it, leave trailing
 * commas, and sometimes answer with prose instead. Everything here is defensive:
 * fences are stripped, the first balanced object is extracted, unknown keys are
 * ignored, and every number is clamped to something a human could actually do.
 */
object RoutineJsonParser {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        allowTrailingComma = true
        explicitNulls = false
    }

    fun parse(raw: String): RoutineDraft? {
        val candidate = extractObject(raw) ?: return null
        val parsed = runCatching {
            json.decodeFromString(RoutineJson.serializer(), candidate)
        }.getOrNull() ?: return null

        val exercises = parsed.exercises
            .mapNotNull { it.toDraft() }
            .take(MAX_EXERCISES)
        if (exercises.isEmpty()) return null

        return RoutineDraft(
            name = parsed.name.trim().take(MAX_NAME).ifBlank { "Imported routine" },
            exercises = exercises,
        )
    }

    /** Strips fences and surrounding prose, then walks braces to the match. */
    internal fun extractObject(raw: String): String? {
        val start = raw.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until raw.length) {
            val c = raw[i]
            when {
                escaped -> escaped = false
                c == '\\' && inString -> escaped = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '{' -> depth++
                c == '}' -> {
                    depth--
                    if (depth == 0) return raw.substring(start, i + 1)
                }
            }
        }
        return null
    }

    private fun ExerciseJson.toDraft(): DraftExercise? {
        val cleanName = name.trim().take(MAX_NAME)
        if (cleanName.isEmpty()) return null

        val holdValue = holdSeconds ?: holdSecondsCamel
        val isHold = mode?.equals("hold", ignoreCase = true) == true ||
            hold == true ||
            (holdValue != null && holdValue > 0)

        val rest = restSeconds ?: restSecondsCamel ?: rest ?: 60

        return DraftExercise(
            name = cleanName,
            sets = sets.coerceIn(1, 20),
            mode = if (isHold) ExerciseMode.HOLD else ExerciseMode.REPS,
            reps = (reps ?: if (isHold) 0 else 10).coerceIn(0, 100),
            holdSeconds = if (isHold) (holdValue ?: 30).coerceIn(5, 600) else 0,
            restSeconds = rest.coerceIn(0, 600),
        )
    }

    private const val MAX_EXERCISES = 30
    private const val MAX_NAME = 40
}
