package ai.chaski.splashr.data.model

/** A trending topic / category used on the Home screen and as a search filter. */
data class Topic(
    val id: String,
    val title: String,
    val description: String,
    val coverImageUrl: String,
    val photoCount: Int,
)
