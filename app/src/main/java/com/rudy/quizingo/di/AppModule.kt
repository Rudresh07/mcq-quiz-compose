package com.rudy.quizingo.di

import androidx.room.Room
import com.rudy.quizingo.data.ModuleRepository
import com.rudy.quizingo.data.local.QuizingoDatabase
import com.rudy.quizingo.data.remote.QuizApiService
import com.rudy.quizingo.ui.modules.ModuleListViewModel
import com.rudy.quizingo.ui.quiz.QuizViewModel
import com.rudy.quizingo.ui.results.ResultsViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val networkModule = module {
    single {
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
    }
    single {
        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .build()
    }
    single {
        Retrofit.Builder()
            .baseUrl(QuizApiService.BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    single<QuizApiService> { get<Retrofit>().create(QuizApiService::class.java) }
}

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), QuizingoDatabase::class.java, "quizingo.db")
            // Schema is still evolving pre-release - no user data worth migrating yet.
            .fallbackToDestructiveMigration(true)
            .build()
    }
    single { get<QuizingoDatabase>().moduleProgressDao() }
}

val repositoryModule = module {
    single { ModuleRepository(get(), get()) }
}

val viewModelModule = module {
    viewModel { ModuleListViewModel(get()) }
    viewModel { (moduleId: String) -> QuizViewModel(moduleId, get()) }
    viewModel { (moduleId: String) -> ResultsViewModel(moduleId, get()) }
}
