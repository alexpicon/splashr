package ai.chaski.splashr.data.remote

import ai.chaski.splashr.data.model.Photo
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Pexels photo API — https://www.pexels.com/api/.
 *
 * Every call carries the API key in an `Authorization` header. Pexels uses the
 * raw key with no "Bearer" prefix.
 */
interface PexelsApi {

    /** Curated photos — used for the Home "Featured" and "Trending" rails. */
    @GET("v1/curated")
    suspend fun curated(
        @Header("Authorization") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 40,
    ): PexelsPhotosResponse

    /** Free-text search, optionally constrained by orientation and colour. */
    @GET("v1/search")
    suspend fun search(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("orientation") orientation: String? = null,
        @Query("color") color: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 40,
    ): PexelsPhotosResponse

    /** A single photo by its Pexels id. */
    @GET("v1/photos/{id}")
    suspend fun photo(
        @Header("Authorization") apiKey: String,
        @Path("id") id: Long,
    ): PexelsPhoto

    companion object {
        const val BASE_URL = "https://api.pexels.com/"
    }
}

// ---- Response DTOs ----------------------------------------------------------

data class PexelsPhotosResponse(
    @SerializedName("photos") val photos: List<PexelsPhoto> = emptyList(),
    @SerializedName("total_results") val totalResults: Int = 0,
    @SerializedName("page") val page: Int = 1,
)

data class PexelsPhoto(
    @SerializedName("id") val id: Long,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int,
    @SerializedName("url") val url: String,
    @SerializedName("photographer") val photographer: String,
    @SerializedName("photographer_url") val photographerUrl: String,
    @SerializedName("photographer_id") val photographerId: Long,
    @SerializedName("avg_color") val avgColor: String?,
    @SerializedName("alt") val alt: String?,
    @SerializedName("src") val src: PexelsSrc,
)

data class PexelsSrc(
    @SerializedName("large2x") val large2x: String,
    @SerializedName("large") val large: String,
    @SerializedName("medium") val medium: String,
    @SerializedName("portrait") val portrait: String,
    @SerializedName("landscape") val landscape: String,
)

// ---- Mapping to the domain model --------------------------------------------

/** Stable prefix so Pexels photo ids never collide with SampleData ids. */
private const val PEXELS_ID_PREFIX = "px-"

/** True if [id] refers to a Pexels-sourced photo. */
fun isPexelsId(id: String): Boolean = id.startsWith(PEXELS_ID_PREFIX)

/** Extracts the numeric Pexels id from a domain photo id (e.g. "px-123" -> 123). */
fun pexelsNumericId(id: String): Long? = id.removePrefix(PEXELS_ID_PREFIX).toLongOrNull()

/**
 * Maps a Pexels photo to the app's domain [Photo].
 *
 * Pexels supplies no title, description, or tags — [alt] is the only text it
 * provides, so it seeds the title. Description and tags are left empty here and
 * filled in later by the Claude-backed AI service (see ClaudeAiService).
 */
fun PexelsPhoto.toPhoto(topic: String = ""): Photo {
    val cleanedAlt = alt?.trim()?.takeIf { it.isNotEmpty() }
    return Photo(
        id = "$PEXELS_ID_PREFIX$id",
        title = cleanedAlt?.replaceFirstChar { it.uppercase() } ?: "Untitled photo",
        description = "",
        imageUrl = src.large2x,
        photographerId = "$PEXELS_ID_PREFIX$photographerId",
        photographerName = photographer,
        photographerUrl = photographerUrl,
        tags = emptyList(),
        topic = topic,
        width = width,
        height = height,
        dominantColor = "neutral",
    )
}
