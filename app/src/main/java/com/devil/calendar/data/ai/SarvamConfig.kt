package com.devil.calendar.data.ai

/**
 * Everything Sarvam-specific lives here so it can be corrected in one place.
 *
 * Verified against https://docs.sarvam.ai/api/token-factory/chat-completions on
 * 2026-11-03:
 *  - base URL is https://api.sarvam.ai
 *  - auth header is `api-subscription-key` (also accepts `Authorization: Bearer`)
 *  - `POST /v1/chat/completions` serves `sarvam-105b` and `sarvam-105b-conversations`
 *  - Sarvam-M is deprecated and no longer served
 *
 * Note for whoever reads this next: the brief that commissioned this feature
 * listed `sarvam-30b`, but that model is no longer in the current docs, so it is
 * deliberately absent here. If it comes back, add it to [MODELS].
 */
object SarvamConfig {
    const val BASE_URL = "https://api.sarvam.ai"
    const val CHAT_PATH = "/v1/chat/completions"
    const val AUTH_HEADER = "api-subscription-key"

    /**
     * Tried in order. A 400/404 that mentions the model moves to the next one.
     * Both are served by the same /v1 endpoint, so no per-model URL is needed.
     */
    val MODELS: List<String> = listOf(
        "sarvam-105b",
        "sarvam-105b-conversations",
    )

    /**
     * Reasoning tokens are billed as completion tokens and count against this,
     * so it must be generous or a thinking model returns an empty answer.
     */
    const val MAX_TOKENS = 3072
    const val TEMPERATURE = 0.2

    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val MAX_RETRIES = 3

    val chatUrl: String get() = BASE_URL + CHAT_PATH
}
