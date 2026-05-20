package ai.chaski.splashr.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for the Anthropic Messages API — https://docs.claude.com.
 *
 * Authentication is the `x-api-key` header plus a pinned `anthropic-version`.
 * Only the single `POST /v1/messages` endpoint is needed (see ClaudeAiService).
 */
interface AnthropicApi {

    @POST("v1/messages")
    suspend fun messages(
        @Header("x-api-key") apiKey: String,
        @Body body: AnthropicRequest,
        @Header("anthropic-version") version: String = ANTHROPIC_VERSION,
    ): AnthropicResponse

    companion object {
        const val BASE_URL = "https://api.anthropic.com/"
        const val ANTHROPIC_VERSION = "2023-06-01"

        /** Sonnet 4.6 — strong vision at roughly half the cost of Opus. */
        const val MODEL = "claude-sonnet-4-6"
    }
}

// ---- Request DTOs -----------------------------------------------------------

data class AnthropicRequest(
    val model: String,
    @SerializedName("max_tokens") val maxTokens: Int,
    val messages: List<AnthropicMessage>,
)

data class AnthropicMessage(
    val role: String,
    val content: List<ContentBlock>,
)

/**
 * One block of message content. [text] is populated for text blocks and
 * [source] for image blocks; Gson omits the null fields on the wire.
 */
data class ContentBlock(
    val type: String,
    val text: String? = null,
    val source: ImageSource? = null,
) {
    companion object {
        fun text(value: String) = ContentBlock(type = "text", text = value)

        fun imageUrl(url: String) =
            ContentBlock(type = "image", source = ImageSource(url = url))
    }
}

data class ImageSource(
    val url: String,
    val type: String = "url",
)

// ---- Response DTOs ----------------------------------------------------------

data class AnthropicResponse(
    val content: List<ResponseBlock> = emptyList(),
) {
    /** Text of the first text block — where the model's answer lives. */
    val text: String?
        get() = content.firstOrNull { it.type == "text" }?.text
}

data class ResponseBlock(
    val type: String,
    val text: String? = null,
)
