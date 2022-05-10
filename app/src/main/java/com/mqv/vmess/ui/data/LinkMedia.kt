package com.mqv.vmess.ui.data

data class LinkMedia(
    val id: String,
    val url: String,
    val thumbnail: String?,
    val title: String,
    val description: String,
    override val timestamp: Long
) : MessageMedia(timestamp, 0L) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LinkMedia

        if (id != other.id) return false
        if (url != other.url) return false
        if (thumbnail != other.thumbnail) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + thumbnail.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}