package com.devil.calendar.data.ai

/** Prompts live in one place so the schema and the copy stay in step. */
object AiPrompts {

    /** Shown behind the "Copy AI prompt" button - paste it into any chatbot. */
    val COPY_PROMPT: String = """
        You are a calisthenics coach. Build me a routine and reply with JSON only,
        no explanation, no markdown fences.

        Use exactly this shape:
        {"name":"Push Day","exercises":[
          {"name":"Push-ups","sets":4,"reps":12,"rest_seconds":60},
          {"name":"Plank","sets":3,"hold_seconds":45,"rest_seconds":45}
        ]}

        Rules:
        - "name" is the routine name, under 40 characters.
        - Each exercise has "name" (under 40 characters) and "sets" (1-20).
        - For rep exercises use "reps" (1-100). Omit "hold_seconds".
        - For timed holds use "hold_seconds" (5-600) and set "reps" to 0.
        - "rest_seconds" is 0-600, usually 30, 60, 90 or 120.
        - Only bodyweight and dumbbell exercises. No machines.
        - Maximum 12 exercises.
    """.trimIndent()

    const val SYSTEM: String =
        "You are a calisthenics coach. Reply with valid JSON only. " +
            "No markdown, no code fences, no commentary before or after."

    fun generate(goal: String): String =
        "Build a routine for: ${goal.trim()}. Reply with JSON only, using the schema " +
            "{\"name\":string,\"exercises\":[{\"name\":string,\"sets\":int,\"reps\":int," +
            "\"hold_seconds\":int,\"rest_seconds\":int}]}. Bodyweight and dumbbells only."

    const val REPAIR: String =
        "That was not valid JSON. Reply again with ONLY the JSON object, no fences, " +
            "no explanation, matching the schema exactly."

    const val TEST: String = "Reply with the single word OK."
}
