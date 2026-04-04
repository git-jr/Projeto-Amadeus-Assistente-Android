package com.paradoxo.amadeus.dao.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paradoxo.amadeus.enums.AcaoEnum

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String?): MutableList<String> {
        if (value == null) return mutableListOf()
        val type = object : TypeToken<MutableList<String>>() {}.type
        return gson.fromJson(value, type) ?: mutableListOf()
    }

    @TypeConverter
    fun toStringList(list: MutableList<String>?): String =
        gson.toJson(list ?: emptyList<String>())

    @TypeConverter
    fun fromAcaoEnum(value: String?): AcaoEnum? =
        value?.let { runCatching { gson.fromJson(it, AcaoEnum::class.java) }.getOrNull() }

    @TypeConverter
    fun toAcaoEnum(acao: AcaoEnum?): String? =
        gson.toJson(acao)
}
