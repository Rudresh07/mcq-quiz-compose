package com.rudy.quizingo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ModuleProgressEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class QuizingoDatabase : RoomDatabase() {

    abstract fun moduleProgressDao(): ModuleProgressDao
}
