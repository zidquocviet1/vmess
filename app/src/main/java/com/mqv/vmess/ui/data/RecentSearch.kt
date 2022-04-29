package com.mqv.vmess.ui.data

import com.mqv.vmess.data.model.RecentSearchPeople

data class RecentSearch(
    val uid: String,
    val photoUrl: String?,
    val displayName: String,
    var isOnline: Boolean = false,
    val timestamp: Long
) {
    fun toRecentSearchPeople(): RecentSearchPeople {
        return RecentSearchPeople(this.uid, this.timestamp)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecentSearch

        if (uid != other.uid) return false
        if (photoUrl != other.photoUrl) return false
        if (displayName != other.displayName) return false
        if (isOnline != other.isOnline) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid.hashCode()
        result = 31 * result + (photoUrl?.hashCode() ?: 0)
        result = 31 * result + displayName.hashCode()
        result = 31 * result + isOnline.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}