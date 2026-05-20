package ai.chaski.splashr.ai

import ai.chaski.splashr.data.model.AiTagSuggestion
import ai.chaski.splashr.data.model.Orientation
import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.model.PhotoCritique
import ai.chaski.splashr.data.model.SearchFilter
import kotlinx.coroutines.delay

/**
 * A rule-based implementation of [AiSuggestionService].
 *
 * It is intentionally simple — keyword maps, scoring, and light parsing — but
 * every feature works end to end from the user's point of view. The small
 * [delay] calls simulate model latency so the UI's loading states are visible
 * and realistic.
 */
class RuleBasedAiService : AiSuggestionService {

    // ---- Tag suggestions ----------------------------------------------------

    private val topicTags = mapOf(
        "Nature" to listOf("landscape", "outdoors", "scenic", "wilderness", "natural light"),
        "City" to listOf("urban", "street", "skyline", "city life", "metropolitan"),
        "Travel" to listOf("wanderlust", "destination", "explore", "journey", "culture"),
        "Architecture" to listOf("design", "structure", "geometry", "modern", "lines"),
        "Animals" to listOf("wildlife", "nature", "creature", "fauna", "habitat"),
        "Food" to listOf("culinary", "fresh", "tasty", "kitchen", "ingredients"),
        "Technology" to listOf("digital", "innovation", "gadget", "hardware", "future"),
        "People" to listOf("portrait", "human", "lifestyle", "candid", "expression"),
        "Abstract" to listOf("texture", "minimal", "pattern", "conceptual", "form"),
        "Ocean" to listOf("sea", "coastal", "water", "marine", "horizon"),
    )

    private val colorTags = mapOf(
        "dark" to listOf("moody", "low-key", "dramatic"),
        "blue" to listOf("cool tones", "serene"),
        "green" to listOf("fresh", "lush"),
        "warm" to listOf("warm tones", "cozy"),
        "white" to listOf("bright", "high-key", "clean"),
        "monochrome" to listOf("black and white", "minimal"),
        "teal" to listOf("calm", "muted tones"),
        "orange" to listOf("vibrant", "sunlit"),
        "red" to listOf("bold", "vivid"),
        "neutral" to listOf("understated", "soft tones"),
    )

    private val orientationTag = mapOf(
        Orientation.PORTRAIT to "vertical",
        Orientation.LANDSCAPE to "wide shot",
        Orientation.SQUARE to "square crop",
    )

    override suspend fun suggestTags(photo: Photo): AiTagSuggestion {
        delay(700) // simulate model latency for a realistic loading state
        val existing = photo.tags.map { it.lowercase() }.toSet()
        val candidates = buildList {
            addAll(topicTags[photo.topic].orEmpty())
            addAll(colorTags[photo.dominantColor].orEmpty())
            orientationTag[photo.orientation]?.let { add(it) }
        }
        val suggested = candidates
            .filter { it.lowercase() !in existing }
            .distinct()
            .take(6)
        val explanation = "Based on this photo's ${photo.topic.lowercase()} subject and its " +
            "${photo.dominantColor} tones, these tags should help it surface in more searches."
        return AiTagSuggestion(photo.id, suggested, explanation)
    }

    // ---- Chat-based search --------------------------------------------------

    private val colorKeywords: List<Pair<String, String>> = listOf(
        "dark" to "dark", "moody" to "dark", "night" to "dark",
        "blue" to "blue", "green" to "green",
        "warm" to "warm", "golden" to "warm",
        "white" to "white", "bright" to "white",
        "monochrome" to "monochrome", "black" to "monochrome",
        "teal" to "teal", "orange" to "orange", "red" to "red",
    )

    private val topicKeywords: List<Pair<String, String>> = listOf(
        "nature" to "Nature", "forest" to "Nature", "mountain" to "Nature", "mountains" to "Nature",
        "city" to "City", "urban" to "City", "street" to "City",
        "travel" to "Travel", "trip" to "Travel",
        "architecture" to "Architecture", "building" to "Architecture", "buildings" to "Architecture",
        "animal" to "Animals", "animals" to "Animals", "wildlife" to "Animals",
        "food" to "Food",
        "tech" to "Technology", "technology" to "Technology",
        "people" to "People", "portrait" to "People", "portraits" to "People",
        "abstract" to "Abstract",
        "ocean" to "Ocean", "sea" to "Ocean", "beach" to "Ocean", "wave" to "Ocean", "waves" to "Ocean",
    )

    private val portraitWords = setOf("vertical", "portrait", "tall")
    private val landscapeWords = setOf("horizontal", "landscape", "wide")

    private val stopWords = setOf(
        "show", "me", "find", "get", "give", "the", "a", "an", "some", "of", "with",
        "photos", "photo", "images", "image", "pictures", "picture", "please", "for",
        "i", "want", "looking", "look", "search", "and", "in", "on", "to", "that", "are",
    )

    override suspend fun parseChatQuery(message: String): ChatSearchResult {
        delay(500)
        val lower = message.lowercase().trim()
        val words = lower.split(Regex("[^a-z']+")).filter { it.isNotBlank() }

        val color = colorKeywords.firstOrNull { (key, _) -> key in words }?.second
        val orientation = when {
            words.any { it in portraitWords } -> Orientation.PORTRAIT
            words.any { it in landscapeWords } -> Orientation.LANDSCAPE
            "square" in words -> Orientation.SQUARE
            else -> Orientation.ANY
        }
        val photographer = Regex("\\bby ([a-z]+)").find(lower)
            ?.groupValues?.get(1)
            ?.replaceFirstChar { it.uppercase() }
        val topic = topicKeywords.firstOrNull { (key, _) -> key in words }?.second

        val recognized = buildSet {
            color?.let { c -> addAll(colorKeywords.filter { it.second == c }.map { it.first }) }
            addAll(portraitWords)
            addAll(landscapeWords)
            add("square")
            topic?.let { t -> addAll(topicKeywords.filter { it.second == t }.map { it.first }) }
            photographer?.let { add("by"); add(it.lowercase()) }
        }
        val leftover = words.filter { it !in stopWords && it !in recognized }
        val query = leftover.joinToString(" ")

        val filter = SearchFilter(
            query = query,
            topic = topic,
            orientation = orientation,
            color = color,
            photographer = photographer,
        )
        return ChatSearchResult(filter, describe(filter, message.trim()))
    }

    private fun describe(filter: SearchFilter, original: String): String {
        if (filter.isEmpty) {
            return "I couldn't pick out specific filters, so here is a broad match for " +
                "\"$original\"."
        }
        val descriptors = mutableListOf<String>()
        filter.color?.let { descriptors.add(it) }
        when (filter.orientation) {
            Orientation.PORTRAIT -> descriptors.add("vertical")
            Orientation.LANDSCAPE -> descriptors.add("wide")
            Orientation.SQUARE -> descriptors.add("square")
            Orientation.ANY -> Unit
        }
        val subject = filter.topic?.let { "${it.lowercase()} photos" } ?: "photos"
        return buildString {
            append("Showing ")
            if (descriptors.isNotEmpty()) append(descriptors.joinToString(", ")).append(" ")
            append(subject)
            if (filter.query.isNotBlank()) append(" matching \"${filter.query}\"")
            filter.photographer?.let { append(" by $it") }
            append(".")
        }
    }

    // ---- "For You" recommendations ------------------------------------------

    override suspend fun recommendForYou(
        savedPhotos: List<Photo>,
        interests: List<String>,
        allPhotos: List<Photo>,
    ): List<Photo> {
        delay(400)
        if (savedPhotos.isEmpty() && interests.isEmpty()) return emptyList()

        val interestSet = interests.map { it.lowercase() }.toSet()
        val savedTopicWeight = savedPhotos.groupingBy { it.topic.lowercase() }.eachCount()
        val savedTags = savedPhotos.flatMap { it.tags }.map { it.lowercase() }.toSet()
        val savedIds = savedPhotos.map { it.id }.toSet()

        fun score(photo: Photo): Int {
            var score = 0
            if (photo.topic.lowercase() in interestSet) score += 6
            score += (savedTopicWeight[photo.topic.lowercase()] ?: 0) * 3
            score += photo.tags.count { it.lowercase() in savedTags } * 2
            return score
        }

        val ranked = allPhotos
            .filterNot { it.id in savedIds }
            .map { it to score(it) }
            .sortedByDescending { it.second }

        val positive = ranked.filter { it.second > 0 }
        return (positive.ifEmpty { ranked }).take(24).map { it.first }
    }

    // ---- Photo critique -----------------------------------------------------

    override suspend fun critique(photo: Photo): PhotoCritique {
        delay(600) // simulate analysis latency
        val compositionLine = when (photo.orientation) {
            Orientation.LANDSCAPE ->
                "The wide framing gives the scene room to breathe across the horizontal."
            Orientation.PORTRAIT ->
                "The vertical crop pulls the eye up through the frame."
            Orientation.SQUARE ->
                "The square crop keeps the eye centred on the subject."
            Orientation.ANY ->
                "The framing feels balanced and intentional."
        }
        val color = photo.dominantColor
        return PhotoCritique(
            photoId = photo.id,
            composition = compositionLine,
            light = "Light reads as $color in tone, which sets the photo's mood.",
            color = "The $color palette gives the image a cohesive feel.",
            takeaway = "A strong ${photo.topic.lowercase()} shot — the $color tones and " +
                "${photo.orientation.name.lowercase()} framing work together.",
        )
    }
}
