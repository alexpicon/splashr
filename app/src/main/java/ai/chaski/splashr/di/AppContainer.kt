package ai.chaski.splashr.di

import ai.chaski.splashr.BuildConfig
import ai.chaski.splashr.ai.AiSuggestionService
import ai.chaski.splashr.ai.ClaudeAiService
import ai.chaski.splashr.ai.RuleBasedAiService
import ai.chaski.splashr.data.local.SplashrDatabase
import ai.chaski.splashr.data.remote.AnthropicApi
import ai.chaski.splashr.data.remote.PexelsApi
import ai.chaski.splashr.data.repository.FakePhotoRepository
import ai.chaski.splashr.data.repository.PexelsPhotoRepository
import ai.chaski.splashr.data.repository.PhotoRepository
import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Minimal manual dependency container. Created once by [ai.chaski.splashr.SplashrApplication]
 * and handed to ViewModels via [ai.chaski.splashr.ui.AppViewModelProvider].
 *
 * No DI framework — at this size, a hand-written container is clearer and has
 * zero build cost.
 */
class AppContainer(context: Context) {

    private val database = SplashrDatabase.get(context)

    // Saved photos, collections, topics and photographer profiles always come
    // from this Room + SampleData repository.
    private val library: PhotoRepository =
        FakePhotoRepository(database.photoDao(), database.collectionDao())

    /**
     * The active repository. When a Pexels API key is configured the photo
     * catalogue is served from Pexels; otherwise the app runs entirely on the
     * built-in SampleData catalogue — so it still works with zero setup.
     */
    val photoRepository: PhotoRepository =
        if (BuildConfig.PEXELS_API_KEY.isNotBlank()) {
            PexelsPhotoRepository(
                api = createPexelsApi(),
                apiKey = BuildConfig.PEXELS_API_KEY,
                library = library,
            )
        } else {
            library
        }

    /**
     * The active AI service. With an Anthropic API key configured, Claude powers
     * tag suggestions, chat search and recommendations; otherwise the offline
     * rule-based service is used. Claude also falls back to it on any failure.
     */
    val aiService: AiSuggestionService =
        if (BuildConfig.ANTHROPIC_API_KEY.isNotBlank()) {
            ClaudeAiService(
                api = createAnthropicApi(),
                apiKey = BuildConfig.ANTHROPIC_API_KEY,
                aiCacheDao = database.aiCacheDao(),
                fallback = RuleBasedAiService(),
            )
        } else {
            RuleBasedAiService()
        }

    private fun createPexelsApi(): PexelsApi =
        Retrofit.Builder()
            .baseUrl(PexelsApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PexelsApi::class.java)

    private fun createAnthropicApi(): AnthropicApi {
        // Vision requests can take a while, so allow a generous timeout.
        val client = OkHttpClient.Builder()
            .callTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(AnthropicApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnthropicApi::class.java)
    }
}
