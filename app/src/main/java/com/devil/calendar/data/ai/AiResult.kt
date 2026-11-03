package com.devil.calendar.data.ai

/** Every failure the UI can show, with copy that is safe for a user to read. */
sealed class AiError(val message: String) {
    data object NoKey : AiError("Add your Sarvam key in Profile first.")
    data object InvalidKey : AiError("Key invalid or not activated.")
    data object RateLimited : AiError("Rate limit or credits reached. Try again later.")
    data object Offline : AiError("You're offline.")
    data object Timeout : AiError("Sarvam took too long to answer.")
    data object ServerError : AiError("Sarvam is having trouble. Try again.")
    data object EmptyAnswer : AiError("Sarvam returned an empty answer.")
    data object BadRequest : AiError("Sarvam rejected the request.")
    data class ModelRejected(val model: String) : AiError("Model $model is unavailable.")
    data class Unknown(val detail: String) : AiError("Something went wrong.")
}

sealed class AiResult<out T> {
    data class Success<T>(val value: T) : AiResult<T>()
    data class Failure(val error: AiError) : AiResult<Nothing>()

    val successOrNull: T? get() = (this as? Success)?.value
    val errorOrNull: AiError? get() = (this as? Failure)?.error
}
