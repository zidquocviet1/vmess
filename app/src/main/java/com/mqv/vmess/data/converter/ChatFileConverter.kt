package com.mqv.vmess.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mqv.vmess.network.adapter.LocalDateTimeAdapter
import com.mqv.vmess.network.model.Chat
import java.time.LocalDateTime

class ChatFileConverter {
    @TypeConverter
    fun toListPhotos(value: String): List<Chat.File>? {
        val type = object : TypeToken<List<Chat.File>?>() {}.type
        val gson = getGson()
        return if (value == "") null else gson.fromJson<List<Chat.File>>(value, type)
    }

    @TypeConverter
    fun fromListPhotos(photos: List<Chat.File>?): String {
        val type = object : TypeToken<List<Chat.File>?>() {}.type
        val gson = getGson()
        return if (photos == null) "" else gson.toJson(photos, type)
    }

    private fun getGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
    }
}