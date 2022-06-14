package com.mqv.vmess.crypto.storage

import com.mqv.vmess.dependencies.AppDependencies

class PreKeyMetadataStoreImpl : PreKeyMetadataStore {
    override var nextSignedPreKeyId: Int
        get() = AppDependencies.getAppPreferences().nextSignedPreKeyId
        set(value) {
            AppDependencies.getAppPreferences().nextSignedPreKeyId = value
        }
    override var activeSignedPreKeyId: Int
        get() = AppDependencies.getAppPreferences().activeSignedPreKeyId
        set(value) {
            AppDependencies.getAppPreferences().activeSignedPreKeyId = value
        }
    override var isSignedPreKeyRegistered: Boolean
        get() = AppDependencies.getAppPreferences().signedPreKeyRegistered
        set(value) {
            AppDependencies.getAppPreferences().signedPreKeyRegistered = value
        }
    override var signedPreKeyFailureCount: Int
        get() = AppDependencies.getAppPreferences().signedPreKeyFailureCount
        set(value) {
            AppDependencies.getAppPreferences().signedPreKeyFailureCount = value
        }
    override var nextOneTimePreKeyId: Int
        get() = AppDependencies.getAppPreferences().nextOneTimePreKeyId
        set(value) {
            AppDependencies.getAppPreferences().nextOneTimePreKeyId = value
        }
}