package com.mqv.vmess.ui.data

import com.mqv.vmess.data.model.FriendNotification

object FriendNotificationMapper {
    @JvmStatic
    fun fromFriendNotification(friendNotification: FriendNotification, peopleList: List<People>) =
        with(friendNotification) {
            FriendNotificationState(id ?: -1L, peopleList.stream()
                .filter { p: People -> p.uid == senderId }
                .findFirst()
                .orElse(People.NOT_FOUND), type, hasRead, createdAt)
        }

    @JvmStatic
    fun fromFriendNotificationState(state: FriendNotificationState) =
        with(state) {
            FriendNotification(id, sender.uid, type, hasRead, createdAt)
        }
}