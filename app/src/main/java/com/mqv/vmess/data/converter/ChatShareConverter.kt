package com.mqv.vmess.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mqv.vmess.network.adapter.LocalDateTimeAdapter
import com.mqv.vmess.network.model.Chat
import java.time.LocalDateTime

class ChatShareConverter {
    @TypeConverter
    fun toShare(value: String): Chat.Share? {
        val type = object : TypeToken<Chat.Share?>() {}.type
        val gson = getGson()
        return if (value == "") null else gson.fromJson<Chat.Share>(value, type)
    }

    @TypeConverter
    fun fromShare(share: Chat.Share?): String {
        val type = object : TypeToken<Chat.Share?>() {}.type
        val gson = getGson()
        return if (share == null) "" else gson.toJson(share, type)
    }

    private fun getGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
    }
}