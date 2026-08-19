package com.rudy.quizingo.data.local

import androidx.room.TypeConverter
import com.rudy.quizingo.data.model.ModuleStatus

class Converters {

    @TypeConverter
    fun fromModuleStatus(status: ModuleStatus): String = status.name

    @TypeConverter
    fun toModuleStatus(value: String): ModuleStatus = ModuleStatus.valueOf(value)
}
