package com.mqv.vmess.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "link_metadata")
data class LinkMetadata(
    @PrimaryKey
    val id: String,
    val url: String,
    val thumbnail: String?,
    val title: String = "",
    val description: String = "",
    @ColumnInfo(name = "max_age")
    val maxAge: Long = 0L,
    @ColumnInfo(name = "send_time")
    val sendTime: Long = 0L
)