package com.mqv.vmess.crypto.storage

interface PreKeyMetadataStore {
    var nextSignedPreKeyId: Int
    var activeSignedPreKeyId: Int
    var isSignedPreKeyRegistered: Boolean
    var signedPreKeyFailureCount: Int
    var nextOneTimePreKeyId: Int
}