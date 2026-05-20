package ai.chaski.splashr.ui.navigation

import ai.chaski.splashr.ui.chat.ChatScreen
import ai.chaski.splashr.ui.collections.CollectionDetailScreen
import ai.chaski.splashr.ui.collections.LibraryScreen
import ai.chaski.splashr.ui.detail.PhotoDetailScreen
import ai.chaski.splashr.ui.foryou.ForYouScreen
import ai.chaski.splashr.ui.home.HomeScreen
import ai.chaski.splashr.ui.photographer.PhotographerScreen
import ai.chaski.splashr.ui.search.SearchScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/** Root composable: bottom navigation scaffold wrapping the navigation graph. */
@Composable
fun SplashrApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevelRoutes = remember { TopLevelDestination.entries.map { it.routePattern } }
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.routePattern,
                            onClick = {
                                navController.navigate(destination.navRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(destination.icon, contentDescription = destination.label)
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        SplashrNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun SplashrNavHost(
    navController: NavHostController,
    modifier: Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onPhotoClick = { navController.navigate(Routes.photo(it)) },
                onTopicClick = { navController.navigate(Routes.searchByTopic(it)) },
            )
        }
        composable(
            route = Routes.SEARCH_PATTERN,
            arguments = listOf(
                navArgument(Routes.ARG_TOPIC) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Routes.ARG_QUERY) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            SearchScreen(onPhotoClick = { navController.navigate(Routes.photo(it)) })
        }
        composable(Routes.CHAT) {
            ChatScreen(onPhotoClick = { navController.navigate(Routes.photo(it)) })
        }
        composable(Routes.FOR_YOU) {
            ForYouScreen(onPhotoClick = { navController.navigate(Routes.photo(it)) })
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onPhotoClick = { navController.navigate(Routes.photo(it)) },
                onCollectionClick = { navController.navigate(Routes.collection(it)) },
            )
        }
        composable(
            route = Routes.PHOTO_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_PHOTO_ID) { type = NavType.StringType }),
        ) {
            PhotoDetailScreen(
                onBack = { navController.popBackStack() },
                onPhotographerClick = { navController.navigate(Routes.photographer(it)) },
                onTagClick = { navController.navigate(Routes.searchByQuery(it)) },
            )
        }
        composable(
            route = Routes.PHOTOGRAPHER,
            arguments = listOf(
                navArgument(Routes.ARG_PHOTOGRAPHER_ID) { type = NavType.StringType },
            ),
        ) {
            PhotographerScreen(
                onBack = { navController.popBackStack() },
                onPhotoClick = { navController.navigate(Routes.photo(it)) },
            )
        }
        composable(
            route = Routes.COLLECTION_DETAIL,
            arguments = listOf(
                navArgument(Routes.ARG_COLLECTION_ID) { type = NavType.StringType },
            ),
        ) {
            CollectionDetailScreen(
                onBack = { navController.popBackStack() },
                onPhotoClick = { navController.navigate(Routes.photo(it)) },
            )
        }
    }
}
