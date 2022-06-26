package com.mqv.vmess.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
* Entity model store local readable message content after that message had encrypted
* */
@Entity(
    tableName = "local_plaintext_content",
    indices = [Index(value = ["messageId", "conversationId"], unique = true)]
)
data class LocalPlaintextContentModel(
    @PrimaryKey
    val messageId: String,
    val conversationId: String,
    val content: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalPlaintextContentModel

        if (messageId != other.messageId) return false
        if (conversationId != other.conversationId) return false
        if (content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        var result = messageId.hashCode()
        result = 31 * result + conversationId.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }
}
