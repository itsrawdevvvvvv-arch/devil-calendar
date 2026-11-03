package com.devil.calendar.data.repository

import com.devil.calendar.data.ai.AiResult
import com.devil.calendar.data.ai.ChatMessage
import com.devil.calendar.data.ai.SarvamClient
import com.devil.calendar.data.local.coach.CoachConfigEntity
import com.devil.calendar.data.local.coach.CoachDao
import com.devil.calendar.domain.coach.BuiltInCoachTemplates
import com.devil.calendar.domain.coach.CoachConfig
import com.devil.calendar.domain.coach.CoachMode
import com.devil.calendar.domain.coach.MovementType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class CoachJson(
    val movement: String = "custom",
    val mode: String = "reps",
    @SerialName("camera_angle") val cameraAngle: String = "",
    @SerialName("camera_distance") val cameraDistance: String = "",
    val tip: String = "",
    @SerialName("primary_joint") val primaryJoint: String = "elbow",
    @SerialName("down_angle") val downAngle: Float = 95f,
    @SerialName("up_angle") val upAngle: Float = 160f,
    val tolerance: Float = 12f,
    @SerialName("min_rep_ms") val minRepMillis: Long = 700L,
)

/**
 * Resolves a coach config for an exercise: cache, then Sarvam, then the
 * built-in template. Always returns something usable - the camera coach must
 * work with no key and no network.
 */
@Singleton
class CoachConfigRepository @Inject constructor(
    private val dao: CoachDao,
    private val sarvam: SarvamClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    suspend fun configFor(exerciseName: String): CoachConfig {
        val key = BuiltInCoachTemplates.keyFor(exerciseName)
        dao.config(key)?.let { return it.toModel() }

        val fetched = fetchFromSarvam(exerciseName, key)
        if (fetched != null) {
            dao.save(fetched.toEntity())
            return fetched
        }

        // Offline, no key, or the model returned junk: use the template and do
        // not cache it, so a later attempt can still upgrade the config.
        return BuiltInCoachTemplates.find(exerciseName)
            ?: BuiltInCoachTemplates.find("squat")!!.copy(exerciseKey = key, movement = MovementType.CUSTOM)
    }

    private suspend fun fetchFromSarvam(name: String, key: String): CoachConfig? {
        val prompt = """
            Pick the closest built-in movement for the exercise "$name" and tune it.
            Reply with JSON only:
            {"movement":"push_up|pull_up|dip|squat|lunge|pistol|sit_up|leg_raise|pike_push_up|burpee|plank|l_sit|handstand|front_lever|planche|human_flag|db_curl|db_press|db_row|goblet_squat|custom",
             "mode":"reps|hold",
             "camera_angle":"short guidance",
             "camera_distance":"short guidance",
             "tip":"one sentence of form advice",
             "primary_joint":"elbow|knee|hip|shoulder",
             "down_angle":95,"up_angle":160,"tolerance":12,"min_rep_ms":700}
        """.trimIndent()

        val result = sarvam.complete(
            messages = listOf(
                ChatMessage.system("You are a calisthenics coach. Reply with valid JSON only."),
                ChatMessage.user(prompt),
            ),
            maxTokens = 768,
        )
        val text = (result as? AiResult.Success)?.value ?: return null

        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val parsed = runCatching {
            json.decodeFromString(CoachJson.serializer(), text.substring(start, end + 1))
        }.getOrNull() ?: return null

        return parsed.toModel(key)
    }

    /** Every number is clamped: a model that returns 400 degrees is ignored. */
    private fun CoachJson.toModel(key: String): CoachConfig {
        val movement = MovementType.entries
            .firstOrNull { it.name.equals(this.movement, ignoreCase = true) }
            ?: MovementType.CUSTOM
        val isHold = mode.equals("hold", ignoreCase = true) || movement.isHold
        return CoachConfig(
            exerciseKey = key,
            movement = movement,
            mode = if (isHold) CoachMode.HOLD else CoachMode.REPS,
            cameraAngle = cameraAngle.take(80).ifBlank { "Side on" },
            cameraDistance = cameraDistance.take(80).ifBlank { "2 to 3 m" },
            tip = tip.take(160),
            primaryJoint = primaryJoint.take(20).ifBlank { "elbow" },
            downAngle = downAngle.coerceIn(20f, 175f),
            upAngle = upAngle.coerceIn(30f, 180f),
            tolerance = tolerance.coerceIn(5f, 30f),
            minRepMillis = minRepMillis.coerceIn(300L, 5_000L),
            fromTemplate = false,
        )
    }

    private fun CoachConfigEntity.toModel(): CoachConfig = CoachConfig(
        exerciseKey = exerciseKey,
        movement = MovementType.entries.firstOrNull { it.name == movement } ?: MovementType.CUSTOM,
        mode = if (mode == CoachMode.HOLD.name) CoachMode.HOLD else CoachMode.REPS,
        cameraAngle = cameraAngle,
        cameraDistance = cameraDistance,
        tip = tip,
        primaryJoint = primaryJoint,
        downAngle = downAngle,
        upAngle = upAngle,
        tolerance = tolerance,
        minRepMillis = minRepMillis,
        fromTemplate = fromTemplate,
    )

    private fun CoachConfig.toEntity(): CoachConfigEntity = CoachConfigEntity(
        exerciseKey = exerciseKey,
        movement = movement.name,
        mode = mode.name,
        cameraAngle = cameraAngle,
        cameraDistance = cameraDistance,
        tip = tip,
        primaryJoint = primaryJoint,
        downAngle = downAngle,
        upAngle = upAngle,
        tolerance = tolerance,
        minRepMillis = minRepMillis,
        fromTemplate = fromTemplate,
        createdAt = System.currentTimeMillis(),
    )
}
