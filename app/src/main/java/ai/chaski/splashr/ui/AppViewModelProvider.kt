package ai.chaski.splashr.ui

import ai.chaski.splashr.SplashrApplication
import ai.chaski.splashr.ui.chat.ChatViewModel
import ai.chaski.splashr.ui.collections.CollectionDetailViewModel
import ai.chaski.splashr.ui.collections.LibraryViewModel
import ai.chaski.splashr.ui.detail.PhotoDetailViewModel
import ai.chaski.splashr.ui.foryou.ForYouViewModel
import ai.chaski.splashr.ui.home.HomeViewModel
import ai.chaski.splashr.ui.photographer.PhotographerViewModel
import ai.chaski.splashr.ui.search.SearchViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Single [androidx.lifecycle.ViewModelProvider.Factory] for every ViewModel in
 * the app. ViewModels that depend on navigation arguments read them from a
 * [androidx.lifecycle.SavedStateHandle] obtained via [createSavedStateHandle].
 */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer { HomeViewModel(app().container.photoRepository) }
        initializer {
            SearchViewModel(createSavedStateHandle(), app().container.photoRepository)
        }
        initializer {
            ChatViewModel(app().container.photoRepository, app().container.aiService)
        }
        initializer {
            ForYouViewModel(app().container.photoRepository, app().container.aiService)
        }
        initializer { LibraryViewModel(app().container.photoRepository) }
        initializer {
            PhotoDetailViewModel(
                createSavedStateHandle(),
                app().container.photoRepository,
                app().container.aiService,
            )
        }
        initializer {
            PhotographerViewModel(createSavedStateHandle(), app().container.photoRepository)
        }
        initializer {
            CollectionDetailViewModel(createSavedStateHandle(), app().container.photoRepository)
        }
    }
}

private fun CreationExtras.app(): SplashrApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SplashrApplication
