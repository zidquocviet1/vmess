package com.mqv.vmess.ui.data

data class RemoteMedia(
    val id: Int,
    val path: String,
    val type: String,
    override val timestamp: Long,
    override val size: Long
) : MessageMedia(timestamp, size) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemoteMedia

        if (id != other.id) return false
        if (path != other.path) return false
        if (type != other.type) return false
        if (timestamp != other.timestamp) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }
}
