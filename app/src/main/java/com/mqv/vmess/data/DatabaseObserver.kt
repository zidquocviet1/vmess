package com.mqv.vmess.data

import org.webrtc.IceCandidate

class DatabaseObserver {
    private var messageListener: MutableMap<String, MutableSet<MessageListener>> = HashMap()
    private var conversationListener: MutableSet<ConversationListener> = HashSet()
    private var friendRequestListener: MutableSet<FriendRequestListener> = HashSet()
    private var noneFriendRequestListener: MutableSet<NoneFriendRequestListener> = HashSet()
    private var webRtcCallListener: WebRtcCallListener? = null
    private var offerSdp: String? = null
    private var candidates: MutableList<IceCandidate> = mutableListOf()

    interface WebRtcCallListener {
        fun onOffer(sdp: String)
        fun onAnswer(sdp: String)
        fun onIceCandidate(candidate: IceCandidate)
        fun onUserDenyCall()
        fun onUserIsInCall()
        fun onClose()
    }

    interface ConversationListener {
        fun onConversationInserted(conversationId: String)
        fun onConversationUpdated(conversationId: String)
    }

    interface MessageListener {
        fun onMessageInserted(messageId: String)
        fun onMessageUpdated(messageId: String)
    }

    interface FriendRequestListener {
        fun onRequest(userId: String)
        fun onConfirm(userId: String)
        fun onUnfriend(userId: String)
        fun onCancel(userId: String)
    }

    interface NoneFriendRequestListener : FriendRequestListener {
        override fun onUnfriend(userId: String) {
            throw RuntimeException()
        }
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

    fun registerNoneFriendRequestListener(listener: NoneFriendRequestListener) {
        noneFriendRequestListener.add(listener)
    }

    fun unregisterNoneFriendRequestListener(listener: NoneFriendRequestListener) {
        noneFriendRequestListener.remove(listener)
    }

    fun registerFriendRequestListener(listener: FriendRequestListener) {
        friendRequestListener.add(listener)
    }

    fun unregisterFriendRequestListener(listener: FriendRequestListener) {
        friendRequestListener.remove(listener)
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

    fun notifyRequestFriend(userId: String) {
        noneFriendRequestListener.forEach { it.onRequest(userId) }
        friendRequestListener.forEach { it.onRequest(userId) }
    }

    fun notifyConfirmFriend(userId: String) {
        noneFriendRequestListener.forEach { it.onConfirm(userId) }
        friendRequestListener.forEach { it.onConfirm(userId) }
    }

    fun notifyUnfriend(userId: String) {
        friendRequestListener.forEach { it.onUnfriend(userId) }
    }

    fun notifyCancelRequest(userId: String) {
        noneFriendRequestListener.forEach { it.onCancel(userId) }
        friendRequestListener.forEach { it.onCancel(userId) }
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

    // Section to send and receive all about WebRTC call data. If it is continued develop in the future, suggest migrate to webSocket.
    fun registerWebRtcCallListener(callback: WebRtcCallListener) {
        webRtcCallListener = callback
        offerSdp?.let { notifyRtcOffer(it) }
        candidates.forEach { notifyIceCandidate(it) }
    }

    fun unregisterWebRtcCallListener() {
        webRtcCallListener = null
    }

    fun notifyRtcAnswer(sdp: String) {
        webRtcCallListener?.onAnswer(sdp)
    }

    fun notifyRtcOffer(sdp: String) {
        if (webRtcCallListener == null) {
            offerSdp = sdp
        }
        webRtcCallListener?.onOffer(sdp)
    }

    fun notifyIceCandidate(candidate: IceCandidate) {
        if (webRtcCallListener == null) {
            candidates.add(candidate)
        }
        webRtcCallListener?.onIceCandidate(candidate)
    }

    fun notifyRtcSessionClose() {
        webRtcCallListener?.onClose()
    }

    fun notifyRtcDenyCall() {
        webRtcCallListener?.onUserDenyCall()
    }

    fun notifyRtcUserIsInCall() {
        webRtcCallListener?.onUserIsInCall()
    }
}