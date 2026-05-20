package ai.chaski.splashr.data.model

/** A photographer whose work appears in the app. */
data class Photographer(
    val id: String,
    val name: String,
    val username: String,
    val bio: String,
    val profileImageUrl: String,
    val location: String,
    val totalPhotos: Int,
)
