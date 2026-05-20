package ai.chaski.splashr.data.model

/**
 * The full set of criteria a search can use. The Search screen builds one of
 * these directly; the Chat search feature produces one from natural language.
 */
data class SearchFilter(
    val query: String = "",
    val topic: String? = null,
    val orientation: Orientation = Orientation.ANY,
    val color: String? = null,
    val photographer: String? = null,
    val tags: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = query.isBlank() &&
            topic == null &&
            orientation == Orientation.ANY &&
            color == null &&
            photographer == null &&
            tags.isEmpty()

    val activeFilterCount: Int
        get() = listOf(
            topic != null,
            orientation != Orientation.ANY,
            color != null,
            photographer != null,
            tags.isNotEmpty(),
        ).count { it }
}
