package com.mqv.vmess.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mqv.vmess.network.adapter.LocalDateTimeAdapter
import com.mqv.vmess.network.model.ConversationGroup
import java.time.LocalDateTime

class ConversationGroupConverter {
    @TypeConverter
    fun toParticipants(value: String): ConversationGroup? {
        val type = object : TypeToken<ConversationGroup?>() {}.type
        val gson = getGson()
        return if (value == "") null else gson.fromJson<ConversationGroup>(value, type)
    }

    @TypeConverter
    fun fromParticipants(conversationGroup: ConversationGroup?): String {
        val type = object : TypeToken<ConversationGroup?>() {}.type
        val gson = getGson()
        return if (conversationGroup == null) "" else gson.toJson(conversationGroup, type)
    }

    private fun getGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
    }
}