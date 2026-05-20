package ai.chaski.splashr

import ai.chaski.splashr.di.AppContainer
import android.app.Application

/** Application entry point — owns the single [AppContainer] instance. */
class SplashrApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
