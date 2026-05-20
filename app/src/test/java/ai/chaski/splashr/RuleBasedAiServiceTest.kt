package ai.chaski.splashr

import ai.chaski.splashr.ai.RuleBasedAiService
import ai.chaski.splashr.data.model.Orientation
import ai.chaski.splashr.data.sample.SampleData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the rule-based AI service. These verify that the chat-search
 * parser and tag suggester actually behave as the UI promises.
 */
class RuleBasedAiServiceTest {

    private val ai = RuleBasedAiService()

    @Test
    fun chatQuery_parsesColourAndTopic() = runBlocking {
        val result = ai.parseChatQuery("show me dark city photos")
        assertEquals("dark", result.filter.color)
        assertEquals("City", result.filter.topic)
    }

    @Test
    fun chatQuery_parsesOrientationAndTopic() = runBlocking {
        val result = ai.parseChatQuery("show me vertical travel photos")
        assertEquals(Orientation.PORTRAIT, result.filter.orientation)
        assertEquals("Travel", result.filter.topic)
    }

    @Test
    fun chatQuery_parsesPhotographer() = runBlocking {
        val result = ai.parseChatQuery("photos by Alex")
        assertEquals("Alex", result.filter.photographer)
    }

    @Test
    fun suggestTags_returnsNewTagsNotAlreadyOnThePhoto() = runBlocking {
        val photo = SampleData.photos.first()
        val suggestion = ai.suggestTags(photo)
        assertTrue("expected at least one suggested tag", suggestion.suggestedTags.isNotEmpty())
        assertTrue(
            "suggested tags should not duplicate existing ones",
            suggestion.suggestedTags.none { it.lowercase() in photo.tags.map(String::lowercase) },
        )
    }

    @Test
    fun recommendForYou_isEmptyWithNoSignal() = runBlocking {
        val recommendations = ai.recommendForYou(emptyList(), emptyList(), SampleData.photos)
        assertTrue(recommendations.isEmpty())
    }

    @Test
    fun recommendForYou_ranksChosenInterestsFirst() = runBlocking {
        val recommendations = ai.recommendForYou(
            savedPhotos = emptyList(),
            interests = listOf("Ocean"),
            allPhotos = SampleData.photos,
        )
        assertTrue(recommendations.isNotEmpty())
        assertEquals("Ocean", recommendations.first().topic)
    }
}
