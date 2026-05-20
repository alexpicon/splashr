package ai.chaski.splashr.ai

import ai.chaski.splashr.data.model.AiTagSuggestion
import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.model.PhotoCritique
import ai.chaski.splashr.data.model.SearchFilter

/** Result of interpreting a natural-language chat query into a structured search. */
data class ChatSearchResult(
    val filter: SearchFilter,
    val interpretation: String,
)

/**
 * Abstraction for the app's AI-assisted features. The current implementation
 * ([RuleBasedAiService]) is rule-based and runs fully offline, but this seam
 * means it could later be backed by an on-device or hosted language model
 * without changing any screen.
 */
interface AiSuggestionService {

    /** Suggests additional discovery tags for a photo, with a short rationale. */
    suspend fun suggestTags(photo: Photo): AiTagSuggestion

    /** Converts a free-text request (e.g. "dark city photos") into a [SearchFilter]. */
    suspend fun parseChatQuery(message: String): ChatSearchResult

    /** Ranks photos for the "For You" feed from saved photos and chosen interests. */
    suspend fun recommendForYou(
        savedPhotos: List<Photo>,
        interests: List<String>,
        allPhotos: List<Photo>,
    ): List<Photo>

    /**
     * Produces a short, mentor-style critique of [photo] — composition, light,
     * colour, and one takeaway. Shown on the photo detail view.
     */
    suspend fun critique(photo: Photo): PhotoCritique
}
