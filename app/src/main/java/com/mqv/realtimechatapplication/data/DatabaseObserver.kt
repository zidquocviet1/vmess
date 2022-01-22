package com.mqv.realtimechatapplication.data

class DatabaseObserver {
    private var messageListener: MutableMap<String, MutableSet<MessageListener>> = HashMap()
    private var conversationListener: MutableSet<ConversationListener> = HashSet()

    interface ConversationListener {
        fun onConversationInserted(conversationId: String)
        fun onConversationUpdated(conversationId: String)
    }

    interface MessageListener {
        fun onMessageInserted(messageId: String)
        fun onMessageUpdated(messageId: String)
    }

    fun registerConversationListener(listener: ConversationListener) {
        conversationListener.add(listener)
    }

    fun unregisterConversationListener(listener: ConversationListener) {
        conversationListener.remove(listener)
    }

    fun registerMessageListener(conversationId: String, listener: MessageListener) {
        registerMapListener(messageListener, conversationId, listener)
    }

    fun unregisterMessageListener(listener: MessageListener) {
        unregisterMapListener(messageListener, listener)
    }

    fun notifyConversationInserted(conversationId: String) {
        conversationListener.forEach { it.onConversationInserted(conversationId) }
    }

    fun notifyConversationUpdated(conversationId: String) {
        conversationListener.forEach { it.onConversationUpdated(conversationId) }
    }

    fun notifyMessageInserted(conversationId: String, messageId: String) {
        messageListener[conversationId]?.forEach { it.onMessageInserted(messageId) }
    }

    fun notifyMessageUpdated(conversationId: String, messageId: String) {
        messageListener[conversationId]?.forEach { it.onMessageUpdated(messageId) }
    }

    private fun <K, V> registerMapListener(map: MutableMap<K, MutableSet<V>>, key: K, listener: V) {
        var listeners = map[key]
        if (listeners == null) {
            listeners = HashSet()
        }
        listeners.add(listener)
        map[key] = listeners
    }

    private fun <K, V> unregisterMapListener(map: MutableMap<K, MutableSet<V>>, listener: V) {
        map.entries.forEach { entry ->
            entry.value.remove(listener)
        }
    }
}