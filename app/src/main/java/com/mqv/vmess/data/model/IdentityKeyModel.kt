package com.mqv.vmess.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "identity_key", indices = [Index(value = ["address"], unique = true)])
data class IdentityKeyModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val address: String,
    val identityKey: String,
    val timestamp: Long,
)
