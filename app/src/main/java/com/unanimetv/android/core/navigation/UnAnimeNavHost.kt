package com.unanimetv.android.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unanimetv.android.feature.catalog.ui.CatalogScreen
import com.unanimetv.android.feature.episode.ui.EpisodeScreen
import com.unanimetv.android.feature.favorites.ui.FavoritesScreen
import com.unanimetv.android.feature.home.ui.HomeScreen
import com.unanimetv.android.feature.settings.ui.SettingsScreen
import com.unanimetv.android.feature.title.ui.TitleScreen

private const val EPISODE_ROUTE = "episode?url={url}"
private const val TITLE_ROUTE = "title?url={url}"
private const val CATALOG_ROUTE = "catalog?url={url}&title={title}"
private const val FAVORITES_ROUTE = "favorites"

private fun episodeRoute(url: String) = "episode?url=${Uri.encode(url)}"
private fun titleRoute(url: String) = "title?url=${Uri.encode(url)}"
private fun catalogRoute(url: String, title: String) =
    "catalog?url=${Uri.encode(url)}&title=${Uri.encode(title)}"

@Composable
fun UnAnimeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.HOME.route,
        modifier = modifier,
        enterTransition = {
            androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(280)) +
                androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(280)) { it / 24 }
        },
        exitTransition = {
            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))
        },
        popEnterTransition = {
            androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(280))
        },
        popExitTransition = {
            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220)) +
                androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(220)) { it / 24 }
        }
    ) {
        composable(TopLevelDestination.HOME.route) {
            HomeScreen(
                onEpisodeClick = { navController.navigate(episodeRoute(it)) },
                onTitleClick = { navController.navigate(titleRoute(it)) },
                onSeeAll = { url, title -> navController.navigate(catalogRoute(url, title)) },
                onFavoritesClick = { navController.navigate(FAVORITES_ROUTE) }
            )
        }
        composable(TopLevelDestination.SETTINGS.route) {
            SettingsScreen()
        }
        composable(FAVORITES_ROUTE) {
            FavoritesScreen(
                onTitleClick = { navController.navigate(titleRoute(it)) },
                onEpisodeClick = { navController.navigate(episodeRoute(it)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(CATALOG_ROUTE) { entry ->
            val url = entry.arguments?.getString("url").orEmpty()
            val title = entry.arguments?.getString("title").orEmpty()
            CatalogScreen(
                path = url,
                title = title,
                onTitleClick = { navController.navigate(titleRoute(it)) },
                onCatalogClick = { url2, title2 -> navController.navigate(catalogRoute(url2, title2)) },
                onFavoritesClick = { navController.navigate(FAVORITES_ROUTE) }
            )
        }
        composable(TITLE_ROUTE) { entry ->
            val url = entry.arguments?.getString("url").orEmpty()
            TitleScreen(
                url = url,
                onEpisodeClick = { navController.navigate(episodeRoute(it)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(EPISODE_ROUTE) { entry ->
            val url = entry.arguments?.getString("url").orEmpty()
            EpisodeScreen(
                url = url,
                onTitleClick = { navController.navigate(titleRoute(it)) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
