package com.rameez.hel.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromList(list: List<String>?): String {
        val normalized = list
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        return normalized.joinToString(",")
    }

    @TypeConverter
    fun toList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()

        return data
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun fromIntList(list: List<Int>?): String {
        return list?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toIntList(data: String?): List<Int> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}
