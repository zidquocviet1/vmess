package com.mqv.vmess.crypto.storage

import org.signal.libsignal.protocol.state.SignalProtocolStore

interface VMessProtocolStore : SignalProtocolStore {
    fun clearAll()
}