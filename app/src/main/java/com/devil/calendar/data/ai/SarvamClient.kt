package com.devil.calendar.data.ai

import com.devil.calendar.data.security.AiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<OutMessage>,
    val temperature: Double = SarvamConfig.TEMPERATURE,
    @SerialName("max_tokens") val maxTokens: Int = SarvamConfig.MAX_TOKENS,
)

@Serializable
private data class OutMessage(val role: String, val content: String)

@Serializable
private data class ChatResponse(
    val choices: List<Choice> = emptyList(),
)

@Serializable
private data class Choice(
    val message: Message? = null,
)

@Serializable
private data class Message(
    val content: String? = null,
    @SerialName("reasoning_content") val reasoning: String? = null,
)

/** One chat turn. */
data class ChatMessage(val role: String, val content: String) {
    companion object {
        fun system(text: String) = ChatMessage("system", text)
        fun user(text: String) = ChatMessage("user", text)
    }
}

/**
 * The only place that talks to Sarvam.
 *
 * Every failure is mapped to an [AiError] with user-readable copy; nothing here
 * throws to the UI and nothing logs the key or the response body.
 */
@Singleton
class SarvamClient @Inject constructor(
    private val keyStore: AiKeyStore,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(SarvamConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(SarvamConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(SarvamConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    suspend fun complete(
        messages: List<ChatMessage>,
        maxTokens: Int = SarvamConfig.MAX_TOKENS,
    ): AiResult<String> = withContext(Dispatchers.IO) {
        val key = keyStore.read() ?: return@withContext AiResult.Failure(AiError.NoKey)

        var last: AiError = AiError.Unknown("no model tried")
        for (model in SarvamConfig.MODELS) {
            when (val attempt = runModel(model, key, messages, maxTokens)) {
                is AiResult.Success -> return@withContext attempt
                is AiResult.Failure -> {
                    last = attempt.error
                    // Only a model problem is worth another model; anything
                    // else (auth, offline, rate limit) would fail the same way.
                    if (attempt.error !is AiError.ModelRejected) {
                        return@withContext attempt
                    }
                }
            }
        }
        AiResult.Failure(last)
    }

    private suspend fun runModel(
        model: String,
        key: String,
        messages: List<ChatMessage>,
        maxTokens: Int,
    ): AiResult<String> {
        var attempt = 0
        var emptyRetried = false
        var serverRetries = 0

        while (true) {
            val outcome = attemptCall(model, key, messages, maxTokens)
            when (outcome) {
                is AiResult.Success -> {
                    val text = outcome.value.trim()
                    if (text.isEmpty() && !emptyRetried) {
                        // A thinking model can spend the whole budget on
                        // reasoning and return nothing. One retry, then give up.
                        emptyRetried = true
                        continue
                    }
                    return if (text.isEmpty()) {
                        AiResult.Failure(AiError.EmptyAnswer)
                    } else {
                        AiResult.Success(text)
                    }
                }
                is AiResult.Failure -> {
                    attempt++
                    when (val error = outcome.error) {
                        is AiError.RateLimited -> {
                            if (attempt > SarvamConfig.MAX_RETRIES) return outcome
                            delay(backoffMillis(attempt, lastRetryAfter))
                        }
                        is AiError.ServerError, is AiError.Timeout -> {
                            if (serverRetries >= 2) return outcome
                            serverRetries++
                            delay(600L * serverRetries)
                        }
                        is AiError.BadRequest -> {
                            // 422: fix nothing, just try once more as the brief asks.
                            if (attempt > 1) return outcome
                            delay(200)
                        }
                        else -> return outcome
                    }
                }
            }
        }
    }

    private var lastRetryAfter: Long? = null

    private suspend fun attemptCall(
        model: String,
        key: String,
        messages: List<ChatMessage>,
        maxTokens: Int,
    ): AiResult<String> {
        val body = json.encodeToString(
            ChatRequest.serializer(),
            ChatRequest(
                model = model,
                messages = messages.map { OutMessage(it.role, it.content) },
                maxTokens = maxTokens,
            ),
        )

        val request = Request.Builder()
            .url(SarvamConfig.chatUrl)
            .addHeader(SarvamConfig.AUTH_HEADER, key)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON_MEDIA))
            .build()

        return try {
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                when (response.code) {
                    in 200..299 -> {
                        val parsed = runCatching {
                            json.decodeFromString(ChatResponse.serializer(), raw)
                        }.getOrNull()
                        val content = parsed?.choices?.firstOrNull()?.message?.content
                        AiResult.Success(content.orEmpty())
                    }
                    401, 403 -> AiResult.Failure(AiError.InvalidKey)
                    429 -> {
                        lastRetryAfter = response.header("Retry-After")?.toLongOrNull()
                        AiResult.Failure(AiError.RateLimited)
                    }
                    400, 404 -> {
                        // Sarvam reports an unknown model as 400 or 404.
                        if (raw.contains("model", ignoreCase = true)) {
                            AiResult.Failure(AiError.ModelRejected(model))
                        } else {
                            AiResult.Failure(AiError.BadRequest)
                        }
                    }
                    422 -> AiResult.Failure(AiError.BadRequest)
                    in 500..599 -> AiResult.Failure(AiError.ServerError)
                    else -> AiResult.Failure(AiError.Unknown("HTTP ${response.code}"))
                }
            }
        } catch (timeout: SocketTimeoutException) {
            AiResult.Failure(AiError.Timeout)
        } catch (io: IOException) {
            AiResult.Failure(AiError.Offline)
        }
    }

    /** Exponential backoff with jitter, honouring Retry-After when present. */
    private fun backoffMillis(attempt: Int, retryAfterSeconds: Long?): Long {
        retryAfterSeconds?.let { if (it in 1..60) return it * 1_000L }
        val base = 1_000L shl (attempt - 1).coerceIn(0, 4)
        return base + Random.nextLong(0, 400)
    }

    private companion object {
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
