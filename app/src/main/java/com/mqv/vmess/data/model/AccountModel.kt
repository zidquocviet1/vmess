package com.mqv.vmess.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.signal.libsignal.protocol.IdentityKeyPair

@Entity(tableName = "accounts")
data class AccountModel(
    @PrimaryKey
    val id: String,
    val registrationId: Int,
    val identityKeyPair: IdentityKeyPair
)
