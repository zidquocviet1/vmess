package com.mqv.vmess.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sender_key", indices = [Index(value = ["deviceId", "address", "distributionId"])])
data class SenderKeyModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val deviceId: Int,
    val address: String,
    val record: String,
    val distributionId: String,
    val timestamp: Long,
)