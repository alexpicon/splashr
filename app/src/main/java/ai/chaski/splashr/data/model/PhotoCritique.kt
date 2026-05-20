package ai.chaski.splashr.data.model

/**
 * AI-generated photo critique — a short, mentor-style read on why a photo
 * works. Surfaced in the photo detail view; produced by an
 * [ai.chaski.splashr.ai.AiSuggestionService].
 */
data class PhotoCritique(
    val photoId: String,
    val composition: String,
    val light: String,
    val color: String,
    /** The single thing that makes this shot work — shown as a closing thought. */
    val takeaway: String,
)
