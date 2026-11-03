package com.devil.calendar.domain.coach

/**
 * The fallback configs. These are what the coach uses with no key, no network,
 * or when Sarvam returns something that will not validate - so the feature
 * always works. Angles are elbow or knee flexion in degrees.
 */
object BuiltInCoachTemplates {

    private fun reps(
        key: String,
        movement: MovementType,
        joint: String,
        down: Float,
        up: Float,
        angle: String,
        distance: String,
        tip: String,
    ) = CoachConfig(
        exerciseKey = key,
        movement = movement,
        mode = CoachMode.REPS,
        cameraAngle = angle,
        cameraDistance = distance,
        tip = tip,
        primaryJoint = joint,
        downAngle = down,
        upAngle = up,
        tolerance = 12f,
        minRepMillis = 700L,
        fromTemplate = true,
    )

    private fun hold(
        key: String,
        movement: MovementType,
        joint: String,
        angle: String,
        tip: String,
    ) = CoachConfig(
        exerciseKey = key,
        movement = movement,
        mode = CoachMode.HOLD,
        cameraAngle = angle,
        cameraDistance = "2 to 3 m, side on",
        tip = tip,
        primaryJoint = joint,
        downAngle = 0f,
        upAngle = 0f,
        tolerance = 20f,
        minRepMillis = 0L,
        fromTemplate = true,
    )

    val all: List<CoachConfig> = listOf(
        reps("push up", MovementType.PUSH_UP, "elbow", 95f, 160f,
            "Side on, phone at chest height", "2 m, side on",
            "Keep the body in one line; the chest should reach the elbow."),
        reps("pushup", MovementType.PUSH_UP, "elbow", 95f, 160f,
            "Side on, phone at chest height", "2 m, side on",
            "Keep the body in one line."),
        reps("pull up", MovementType.PULL_UP, "elbow", 70f, 165f,
            "Side on if possible", "2 to 3 m",
            "Pull-ups are the hardest to track - a clear side view helps most."),
        reps("dip", MovementType.DIP, "elbow", 80f, 165f,
            "Side on", "2 m, side on",
            "Dips need a side view; the arms hide the torso from the front."),
        reps("squat", MovementType.SQUAT, "knee", 95f, 170f,
            "Front or side on", "2 to 3 m",
            "Break at the hips and knees together; the knee tracks the toe."),
        reps("lunge", MovementType.LUNGE, "knee", 95f, 170f,
            "Side on", "2 to 3 m, side on",
            "Both knees to about 90 degrees at the bottom."),
        reps("pistol", MovementType.PISTOL, "knee", 80f, 170f,
            "Side on", "2 m, side on",
            "One leg out front; the standing knee bends to about 80 degrees."),
        reps("sit up", MovementType.SIT_UP, "hip", 70f, 140f,
            "Side on, low", "2 m, side on",
            "Lift from the abs, not the neck."),
        reps("leg raise", MovementType.LEG_RAISE, "hip", 70f, 160f,
            "Side on, low", "2 m, side on",
            "Keep the lower back flat on the floor."),
        reps("pike push up", MovementType.PIKE_PUSH_UP, "elbow", 100f, 165f,
            "Side on", "2 m, side on",
            "Hips high; the head travels towards the floor."),
        reps("burpee", MovementType.BURPEE, "knee", 90f, 170f,
            "Side on, further back", "3 m, side on",
            "Full body movement - keep the whole body in frame."),
        reps("curl", MovementType.DB_CURL, "elbow", 60f, 160f,
            "Side on", "2 m, side on",
            "Elbow stays pinned to the side."),
        reps("press", MovementType.DB_PRESS, "elbow", 80f, 170f,
            "Side on or front", "2 to 3 m",
            "Press straight overhead without arching the back."),
        reps("row", MovementType.DB_ROW, "elbow", 70f, 160f,
            "Side on", "2 m, side on",
            "Hinge at the hips; pull the elbow past the ribs."),
        reps("goblet", MovementType.GOBLET_SQUAT, "knee", 95f, 170f,
            "Front on", "2 to 3 m",
            "Chest up, elbows inside the knees."),
        hold("plank", MovementType.PLANK, "hip", "Side on, low", "Hips level with the shoulders."),
        hold("l sit", MovementType.L_SIT, "hip", "Side on", "Legs parallel to the floor."),
        hold("handstand", MovementType.HANDSTAND, "shoulder", "Side on, full body", "Stack the shoulders over the wrists."),
        hold("front lever", MovementType.FRONT_LEVER, "hip", "Side on", "Body straight, hips level."),
        hold("planche", MovementType.PLANCHE, "shoulder", "Side on", "Lean forward until the feet leave the floor."),
        hold("human flag", MovementType.HUMAN_FLAG, "hip", "Side on", "Stack the body sideways, hips high."),
        hold("hollow", MovementType.PLANK, "hip", "Side on, low", "Lower back pressed down."),
        hold("wall sit", MovementType.SQUAT, "knee", "Side on", "Thighs parallel to the floor."),
    )

    /** Normalises a typed name so "Push-Ups" and "push ups" hit the same template. */
    fun keyFor(name: String): String =
        name.lowercase()
            .replace('-', ' ')
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

    fun find(name: String): CoachConfig? {
        val key = keyFor(name)
        if (key.isBlank()) return null
        all.firstOrNull { it.exerciseKey == key }?.let { return it }
        // Partial match: "diamond push up" still finds the push-up template.
        return all.firstOrNull { key.contains(it.exerciseKey) || it.exerciseKey.contains(key) }
    }
}
