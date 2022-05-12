package com.mqv.vmess.ui.data

import com.google.gson.annotations.Expose

data class PhoneContact(
    val uid: String,
    val displayName: String,
    @Expose(serialize = false, deserialize = false)
    var contactName: String,
    @Expose(serialize = false, deserialize = false)
    var contactNumber: String,
    val photoUrl: String? = null,
    var isFriend: Boolean = false,
    var isPendingFriendRequest: Boolean = false
) {
    companion object {
        val NOT_FOUND = PhoneContact(uid = "-1", displayName = "", contactName = "", contactNumber = "")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PhoneContact

        if (uid != other.uid) return false
        if (displayName != other.displayName) return false
        if (contactName != other.contactName) return false
        if (contactNumber != other.contactNumber) return false
        if (photoUrl != other.photoUrl) return false
        if (isFriend != other.isFriend) return false
        if (isPendingFriendRequest != other.isPendingFriendRequest) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + contactName.hashCode()
        result = 31 * result + contactNumber.hashCode()
        result = 31 * result + (photoUrl?.hashCode() ?: 0)
        result = 31 * result + isFriend.hashCode()
        result = 31 * result + isPendingFriendRequest.hashCode()
        return result
    }

}
