package ai.chaski.splashr.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

/** Route strings, argument keys, and route builders for the whole app. */
object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val CHAT = "chat"
    const val FOR_YOU = "foryou"
    const val LIBRARY = "library"

    // Search accepts two optional arguments so Home topics and detail tags can
    // deep-link into it.
    const val SEARCH_PATTERN = "search?topic={topic}&query={query}"
    const val PHOTO_DETAIL = "photo/{photoId}"
    const val PHOTOGRAPHER = "photographer/{photographerId}"
    const val COLLECTION_DETAIL = "collection/{collectionId}"

    const val ARG_TOPIC = "topic"
    const val ARG_QUERY = "query"
    const val ARG_PHOTO_ID = "photoId"
    const val ARG_PHOTOGRAPHER_ID = "photographerId"
    const val ARG_COLLECTION_ID = "collectionId"

    fun photo(id: String): String = "photo/$id"
    fun photographer(id: String): String = "photographer/$id"
    fun collection(id: String): String = "collection/$id"
    fun searchByTopic(topic: String): String = "search?topic=${Uri.encode(topic)}"
    fun searchByQuery(query: String): String = "search?query=${Uri.encode(query)}"
}

/** The five bottom-navigation destinations. */
enum class TopLevelDestination(
    val routePattern: String,
    val navRoute: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, Routes.HOME, "Home", Icons.Outlined.Home),
    SEARCH(Routes.SEARCH_PATTERN, Routes.SEARCH, "Search", Icons.Outlined.Search),
    CHAT(Routes.CHAT, Routes.CHAT, "Chat", Icons.AutoMirrored.Outlined.Chat),
    FOR_YOU(Routes.FOR_YOU, Routes.FOR_YOU, "For You", Icons.Outlined.Favorite),
    LIBRARY(Routes.LIBRARY, Routes.LIBRARY, "Library", Icons.Outlined.PhotoLibrary),
}
