package com.mqv.vmess.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mqv.vmess.network.adapter.LocalDateTimeAdapter
import com.mqv.vmess.network.model.Chat
import java.time.LocalDateTime

class ChatVideoConverter {
    @TypeConverter
    fun toListVideos(value: String): List<Chat.Video>? {
        val type = object : TypeToken<List<Chat.Video>?>() {}.type
        val gson = getGson()
        return if (value == "") null else gson.fromJson<List<Chat.Video>>(value, type)
    }

    @TypeConverter
    fun fromListVideos(photos: List<Chat.Video>?): String {
        val type = object : TypeToken<List<Chat.Video>?>() {}.type
        val gson = getGson()
        return if (photos == null) "" else gson.toJson(photos, type)
    }

    private fun getGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
    }
}