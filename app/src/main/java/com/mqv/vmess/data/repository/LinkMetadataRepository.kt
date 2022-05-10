package com.mqv.vmess.data.repository

import com.mqv.vmess.data.model.LinkMetadata
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface LinkMetadataRepository {
    fun findById(id: String): Single<LinkMetadata>

    fun insert(link: LinkMetadata): Completable

    fun refreshLinkMetadata(id: String, url: String, sendTime: Long): Single<LinkMetadata>
}