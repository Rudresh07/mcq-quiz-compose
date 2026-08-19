package com.rudy.quizingo

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rudy.quizingo.di.databaseModule
import com.rudy.quizingo.di.networkModule
import com.rudy.quizingo.di.repositoryModule
import com.rudy.quizingo.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class QuizingoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Crash reports from a debug build on a developer's own device are noise, not
        // signal - only collect them for real (non-debug) builds.
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        startKoin {
            androidLogger()
            androidContext(this@QuizingoApplication)
            modules(networkModule, databaseModule, repositoryModule, viewModelModule)
        }
    }
}
