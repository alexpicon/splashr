package ai.chaski.splashr.data.model

/**
 * A user-created collection of photos. Persisted in the local Room database;
 * [photoIds] is assembled from the collection/photo cross-reference table.
 */
data class Collection(
    val id: String,
    val name: String,
    val description: String,
    val photoIds: List<String>,
    val createdAt: Long,
) {
    val photoCount: Int get() = photoIds.size
}
