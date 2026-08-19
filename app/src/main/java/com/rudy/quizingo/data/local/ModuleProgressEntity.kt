package com.rudy.quizingo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rudy.quizingo.data.model.ModuleStatus

/** One row per module - only the latest attempt is kept, overwritten on every
 *  question advance (PAUSED) and on completion (FINISHED). */
@Entity(tableName = "module_progress")
data class ModuleProgressEntity(
    @PrimaryKey val moduleId: String,
    val status: ModuleStatus,
    val correctCount: Int,
    val totalQuestions: Int,
    val bestStreak: Int,
    val skippedCount: Int,
    val currentQuestionIndex: Int,
    val currentStreak: Int,
    val lastAttemptTimestamp: Long
)
