package ai.chaski.splashr.data.model

/**
 * AI-generated tag suggestions for a photo, surfaced in the full-screen detail
 * view. Produced by an [ai.chaski.splashr.ai.AiSuggestionService].
 */
data class AiTagSuggestion(
    val photoId: String,
    val suggestedTags: List<String>,
    val explanation: String,
)
