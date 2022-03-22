package com.mqv.vmess.ui.data

data class Member(
    val uid: String,
    val displayName: String,
    val nickname: String?,
    val photoUrl: String?,
    val isAdmin: Boolean
)
