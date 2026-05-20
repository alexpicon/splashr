package ai.chaski.splashr.ai

import ai.chaski.splashr.data.local.AiCacheDao
import ai.chaski.splashr.data.local.AiCritiqueEntity
import ai.chaski.splashr.data.local.AiSuggestionEntity
import ai.chaski.splashr.data.model.AiTagSuggestion
import ai.chaski.splashr.data.model.Orientation
import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.model.PhotoCritique
import ai.chaski.splashr.data.model.SearchFilter
import ai.chaski.splashr.data.remote.AnthropicApi
import ai.chaski.splashr.data.remote.AnthropicMessage
import ai.chaski.splashr.data.remote.AnthropicRequest
import ai.chaski.splashr.data.remote.ContentBlock
import ai.chaski.splashr.data.sample.SampleData
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * An [AiSuggestionService] backed by the Claude API (Anthropic Messages API).
 *
 * It powers every AI feature in the app: vision-based tag suggestions, natural-
 * language chat search, and the "For You" ranking. Tag suggestions are cached in
 * Room so each photo is only ever sent to the model once.
 *
 * Every call is wrapped so that any network, auth, or parsing failure falls back
 * to [fallback] (the rule-based service) — the feature keeps working offline or
 * without a key, just less smartly.
 */
class ClaudeAiService(
    private val api: AnthropicApi,
    private val apiKey: String,
    private val aiCacheDao: AiCacheDao,
    private val fallback: AiSuggestionService,
) : AiSuggestionService {

    private val gson = Gson()

    // ---- Tag suggestions (vision + cached) ----------------------------------

    override suspend fun suggestTags(photo: Photo): AiTagSuggestion {
        aiCacheDao.getSuggestion(photo.id)?.let { cached ->
            return AiTagSuggestion(
                photoId = photo.id,
                suggestedTags = cached.tags.split(TAG_SEPARATOR).filter { it.isNotBlank() },
                explanation = cached.explanation,
            )
        }
        return runCatching {
            val prompt = buildString {
                append("Look at this photo and suggest 5-6 short discovery tags ")
                append("(single words or short phrases) that would help people find it. ")
                if (photo.tags.isNotEmpty()) {
                    append("Do not repeat these existing tags: ")
                    append(photo.tags.joinToString(", "))
                    append(". ")
                }
                append("Also write one sentence explaining why these tags fit. ")
                append("Respond with ONLY a JSON object, no markdown: ")
                append("""{"tags":["..."],"explanation":"..."}""")
            }
            val response = api.messages(
                apiKey = apiKey,
                body = AnthropicRequest(
                    model = AnthropicApi.MODEL,
                    maxTokens = 400,
                    messages = listOf(
                        AnthropicMessage(
                            role = "user",
                            content = listOf(
                                ContentBlock.imageUrl(photo.imageUrl),
                                ContentBlock.text(prompt),
                            ),
                        ),
                    ),
                ),
            )
            val parsed = gson.fromJson(extractJson(response.text), TagResponse::class.java)
            val suggestion = AiTagSuggestion(
                photoId = photo.id,
                suggestedTags = parsed.tags.orEmpty(),
                explanation = parsed.explanation.orEmpty(),
            )
            aiCacheDao.insertSuggestion(
                AiSuggestionEntity(
                    photoId = photo.id,
                    tags = suggestion.suggestedTags.joinToString(TAG_SEPARATOR),
                    explanation = suggestion.explanation,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            suggestion
        }.getOrElse { fallback.suggestTags(photo) }
    }

    // ---- Natural-language chat search ---------------------------------------

    override suspend fun parseChatQuery(message: String): ChatSearchResult {
        return runCatching {
            val topics = SampleData.topics.joinToString(", ") { it.title }
            val prompt = buildString {
                append("Convert this photo-search request into structured filters: ")
                append("\"$message\".\n")
                append("Available topics: $topics. ")
                append("orientation must be one of ANY, LANDSCAPE, PORTRAIT, SQUARE.\n")
                append("Respond with ONLY a JSON object, no markdown: ")
                append("""{"query":"free-text keywords or empty string",""")
                append(""""topic":"one topic from the list or null",""")
                append(""""orientation":"ANY|LANDSCAPE|PORTRAIT|SQUARE",""")
                append(""""color":"a colour word or null","photographer":"a name or null",""")
                append(""""interpretation":"one friendly sentence describing the search"}""")
            }
            val response = api.messages(
                apiKey = apiKey,
                body = AnthropicRequest(
                    model = AnthropicApi.MODEL,
                    maxTokens = 400,
                    messages = listOf(
                        AnthropicMessage("user", listOf(ContentBlock.text(prompt))),
                    ),
                ),
            )
            val parsed = gson.fromJson(extractJson(response.text), ChatResponse::class.java)
            val filter = SearchFilter(
                query = parsed.query.orEmpty(),
                topic = parsed.topic.cleanOrNull(),
                orientation = runCatching {
                    Orientation.valueOf(parsed.orientation.orEmpty().uppercase())
                }.getOrDefault(Orientation.ANY),
                color = parsed.color.cleanOrNull(),
                photographer = parsed.photographer.cleanOrNull(),
            )
            ChatSearchResult(
                filter = filter,
                interpretation = parsed.interpretation.orEmpty()
                    .ifBlank { "Here's what I found." },
            )
        }.getOrElse { fallback.parseChatQuery(message) }
    }

    // ---- "For You" ranking --------------------------------------------------

    override suspend fun recommendForYou(
        savedPhotos: List<Photo>,
        interests: List<String>,
        allPhotos: List<Photo>,
    ): List<Photo> {
        if (savedPhotos.isEmpty() && interests.isEmpty()) return emptyList()
        val savedIds = savedPhotos.map { it.id }.toSet()
        val candidates = allPhotos.filterNot { it.id in savedIds }
        if (candidates.isEmpty()) return emptyList()

        return runCatching {
            val saved = savedPhotos.joinToString("; ") { "${it.title} (${it.topic})" }
                .ifBlank { "none yet" }
            val candidateLines = candidates.joinToString("\n") {
                "${it.id}: ${it.title} — topic ${it.topic}, tags ${it.tags.joinToString(", ")}"
            }
            val prompt = buildString {
                append("A user's saved photos: $saved.\n")
                append("Their interests: ${interests.joinToString(", ").ifBlank { "none" }}.\n")
                append("Rank the photos below by relevance to this user, best first:\n")
                append(candidateLines)
                append("\nRespond with ONLY a JSON object, no markdown: ")
                append("""{"photoIds":["id",...]} — up to 24 ids from the list above.""")
            }
            val response = api.messages(
                apiKey = apiKey,
                body = AnthropicRequest(
                    model = AnthropicApi.MODEL,
                    maxTokens = 1000,
                    messages = listOf(
                        AnthropicMessage("user", listOf(ContentBlock.text(prompt))),
                    ),
                ),
            )
            val parsed = gson.fromJson(extractJson(response.text), RecommendResponse::class.java)
            val byId = candidates.associateBy { it.id }
            val ranked = parsed.photoIds.orEmpty().mapNotNull { byId[it] }.take(24)
            ranked.ifEmpty { fallback.recommendForYou(savedPhotos, interests, allPhotos) }
        }.getOrElse { fallback.recommendForYou(savedPhotos, interests, allPhotos) }
    }

    // ---- Photo critique (vision + cached) -----------------------------------

    override suspend fun critique(photo: Photo): PhotoCritique {
        aiCacheDao.getCritique(photo.id)?.let { cached ->
            return PhotoCritique(
                photoId = photo.id,
                composition = cached.composition,
                light = cached.light,
                color = cached.color,
                takeaway = cached.takeaway,
            )
        }
        return runCatching {
            val prompt = buildString {
                append("Look at this photo and write a short, encouraging critique like a ")
                append("photography mentor. Cover four aspects — each ONE short sentence, ")
                append("max 25 words, plain text:\n")
                append("- composition: how the frame is built (subject placement, lines, balance)\n")
                append("- light: the quality and direction of light\n")
                append("- color: the palette and how it contributes\n")
                append("- takeaway: the single thing that makes this shot work\n")
                append("Respond with ONLY JSON, no markdown: ")
                append("""{"composition":"...","light":"...","color":"...","takeaway":"..."}""")
            }
            val response = api.messages(
                apiKey = apiKey,
                body = AnthropicRequest(
                    model = AnthropicApi.MODEL,
                    maxTokens = 400,
                    messages = listOf(
                        AnthropicMessage(
                            role = "user",
                            content = listOf(
                                ContentBlock.imageUrl(photo.imageUrl),
                                ContentBlock.text(prompt),
                            ),
                        ),
                    ),
                ),
            )
            val parsed = gson.fromJson(extractJson(response.text), CritiqueResponse::class.java)
            val critique = PhotoCritique(
                photoId = photo.id,
                composition = parsed.composition.orEmpty(),
                light = parsed.light.orEmpty(),
                color = parsed.color.orEmpty(),
                takeaway = parsed.takeaway.orEmpty(),
            )
            aiCacheDao.insertCritique(
                AiCritiqueEntity(
                    photoId = photo.id,
                    composition = critique.composition,
                    light = critique.light,
                    color = critique.color,
                    takeaway = critique.takeaway,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            critique
        }.getOrElse { fallback.critique(photo) }
    }

    // ---- Helpers ------------------------------------------------------------

    /** Extracts the first {...} JSON object from a model reply, ignoring any prose. */
    private fun extractJson(raw: String?): String {
        val text = raw.orEmpty()
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }

    private fun String?.cleanOrNull(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

    private data class TagResponse(
        val tags: List<String>?,
        val explanation: String?,
    )

    private data class ChatResponse(
        val query: String?,
        val topic: String?,
        val orientation: String?,
        val color: String?,
        val photographer: String?,
        val interpretation: String?,
    )

    private data class RecommendResponse(
        @SerializedName("photoIds") val photoIds: List<String>?,
    )

    private data class CritiqueResponse(
        val composition: String?,
        val light: String?,
        val color: String?,
        val takeaway: String?,
    )

    private companion object {
        const val TAG_SEPARATOR = "|"
    }
}
