package com.mqv.vmess.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_search_people")
data class RecentSearchPeople(
    @PrimaryKey
    @ColumnInfo(name = "user_id", defaultValue = "-1")
    val userId: String,
    @ColumnInfo(defaultValue = "0")
    val timestamp: Long
)
