package com.mqv.vmess.data.repository.impl

import com.mqv.vmess.data.dao.LinkMetadataDao
import com.mqv.vmess.data.model.LinkMetadata
import com.mqv.vmess.data.repository.LinkMetadataRepository
import com.mqv.vmess.ui.components.linkpreview.LinkPreviewMetadata
import com.mqv.vmess.ui.components.linkpreview.LinkPreviewMetadata.Companion.resolveHttpUrl
import com.mqv.vmess.util.DateTimeHelper.toLong
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.LocalDateTime

class LinkMetadataRepositoryImpl(
    private val mLinkMetadataDao: LinkMetadataDao
) : LinkMetadataRepository {
    override fun findById(id: String): Single<LinkMetadata> =
        mLinkMetadataDao.getById(id)

    override fun insert(link: LinkMetadata): Completable =
        mLinkMetadataDao.insert(link)

    override fun refreshLinkMetadata(id: String, url: String, sendTime: Long): Single<LinkMetadata> {
        return Single.create<Document> { emitter ->
            try {
                val doc = Jsoup.connect(resolveHttpUrl(url))
                    .timeout(30 * 1000)
                    .get()
                emitter.onSuccess(doc)
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }.subscribeOn(Schedulers.io())
            .map { doc -> LinkPreviewMetadata.parseFromDocument(url, doc) }
            .map { linkPreview ->
                with(linkPreview) {
                    LinkMetadata(
                        id,
                        url,
                        imageUrl,
                        title,
                        description,
                        LocalDateTime.now().plusDays(7).toLong(),
                        sendTime
                    )
                }
            }
            .flatMap { metadata -> insert(metadata).toSingleDefault(metadata) }
            .onErrorReturnItem(LinkMetadata(id, url, thumbnail = null))
    }
}