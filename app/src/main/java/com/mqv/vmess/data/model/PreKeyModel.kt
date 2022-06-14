package com.mqv.vmess.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pre_key", indices = [Index(value = ["userId", "keyId"], unique = true)])
data class PreKeyModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val userId: String,
    val keyId: Int,
    val publicKey: String,
    val privateKey: String,
    val timestamp: Long,
)