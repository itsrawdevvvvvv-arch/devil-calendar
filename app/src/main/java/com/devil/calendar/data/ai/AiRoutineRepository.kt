package com.devil.calendar.data.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns text or a goal into a [RoutineDraft]. Parsing is fully offline; only
 * [generate] and [testConnection] touch the network, and both degrade to a
 * clear error rather than crashing.
 */
@Singleton
class AiRoutineRepository @Inject constructor(
    private val client: SarvamClient,
) {
    /** JSON first, then the plain-text grammar. Null means "could not read it". */
    fun parsePasted(text: String): RoutineDraft? =
        RoutineJsonParser.parse(text) ?: RoutineTextParser.parse(text)

    suspend fun generate(goal: String): AiResult<RoutineDraft> {
        if (goal.isBlank()) return AiResult.Failure(AiError.BadRequest)

        val first = client.complete(
            listOf(
                ChatMessage.system(AiPrompts.SYSTEM),
                ChatMessage.user(AiPrompts.generate(goal)),
            ),
        )
        val text = when (first) {
            is AiResult.Success -> first.value
            is AiResult.Failure -> return first
        }
        RoutineJsonParser.parse(text)?.let { return AiResult.Success(it) }

        // One repair attempt, exactly as specified.
        val repaired = client.complete(
            listOf(
                ChatMessage.system(AiPrompts.SYSTEM),
                ChatMessage.user(AiPrompts.generate(goal)),
                ChatMessage("assistant", text.take(2_000)),
                ChatMessage.user(AiPrompts.REPAIR),
            ),
        )
        return when (repaired) {
            is AiResult.Success ->
                RoutineJsonParser.parse(repaired.value)
                    ?.let { AiResult.Success(it) }
                    ?: AiResult.Failure(AiError.EmptyAnswer)
            is AiResult.Failure -> repaired
        }
    }

    suspend fun testConnection(): AiResult<String> = client.complete(
        messages = listOf(ChatMessage.user(AiPrompts.TEST)),
        maxTokens = 512,
    )
}
