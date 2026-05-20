package ai.chaski.splashr.data.model

/**
 * Orientation of a photo, derived from its width/height. [ANY] is used as a
 * "no preference" value inside search filters.
 */
enum class Orientation(val label: String) {
    ANY("Any"),
    LANDSCAPE("Landscape"),
    PORTRAIT("Portrait"),
    SQUARE("Square"),
}

/**
 * A single photo. This is the core domain model used across every screen.
 *
 * [isSaved] is not part of the photo's intrinsic data — it is folded in by the
 * repository from the local Room database so the UI always reflects offline state.
 */
data class Photo(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val photographerId: String,
    val photographerName: String,
    val tags: List<String>,
    val topic: String,
    val width: Int,
    val height: Int,
    val dominantColor: String = "neutral",
    /** Link to the photographer's profile on the source site, when applicable. */
    val photographerUrl: String = "",
    val isSaved: Boolean = false,
) {
    /** Width / height. Used to lay photos out in the staggered (masonry) grid. */
    val aspectRatio: Float
        get() = if (height == 0) 1f else width.toFloat() / height.toFloat()

    val orientation: Orientation
        get() = when {
            aspectRatio > 1.15f -> Orientation.LANDSCAPE
            aspectRatio < 0.87f -> Orientation.PORTRAIT
            else -> Orientation.SQUARE
        }
}
