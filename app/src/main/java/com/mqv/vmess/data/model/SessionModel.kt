package com.mqv.vmess.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions", indices = [Index(value = ["userId", "remoteAddress", "deviceId"])])
data class SessionModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val userId: String,
    val remoteAddress: String,
    val deviceId: Int,
    val record: String,
)
