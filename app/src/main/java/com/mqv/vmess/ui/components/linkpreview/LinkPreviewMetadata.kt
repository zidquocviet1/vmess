package com.mqv.vmess.ui.components.linkpreview

import android.webkit.URLUtil
import org.jsoup.nodes.Document
import java.net.URI
import java.net.URISyntaxException

data class LinkPreviewMetadata(
    val url: String,
    var imageUrl: String = "",
    var title: String = "",
    var description: String= "",
    var siteName: String = "",
    var mediaType: String = "",
    var favicon: String = "",
    var isLoadComplete: Boolean = false,
    var isNoPreview: Boolean = false,
) {
    companion object {
        @JvmField
        val LOADING = LinkPreviewMetadata("")

        @JvmField
        val ERROR = LinkPreviewMetadata("", isNoPreview = true)

        @JvmStatic
        fun resolveHttpUrl(tempUrl: String): String {
            if (tempUrl.startsWith("https://") || tempUrl.startsWith("http://")) {
                return tempUrl
            }
            return "https://$tempUrl".lowercase()
        }

        @JvmStatic
        fun parseFromDocument(url: String, doc: Document): LinkPreviewMetadata {
            val metaData = LinkPreviewMetadata(url)
            val elements = doc.getElementsByTag("meta")

            // getTitle doc.select("meta[property=og:title]")
            var title = doc.select("meta[property=og:title]").attr("content")

            if (title == null || title.isEmpty()) {
                title = doc.title()
            }
            metaData.title = title

            //getDescription
            var description = doc.select("meta[name=description]").attr("content")
            if (description!!.isEmpty()) {
                description = doc.select("meta[name=Description]").attr("content")
            }
            if (description!!.isEmpty()) {
                description = doc.select("meta[property=og:description]").attr("content")
            }
            if (description!!.isEmpty()) {
                description = ""
            }
            metaData.description = description

            // getMediaType
            val mediaTypes = doc.select("meta[name=medium]")
            var type: String? = ""
            type = if (mediaTypes.size > 0) {
                val media = mediaTypes.attr("content")
                if (media == "image") "photo" else media
            } else {
                doc.select("meta[property=og:type]").attr("content")
            }
            metaData.mediaType = type

            //getImages
            val imageElements = doc.select("meta[property=og:image]")
            if (imageElements.size > 0) {
                val image = imageElements.attr("content")
                if (image.isNotEmpty()) {
                    resolveURL(url, image)?.apply {
                        metaData.imageUrl = this
                    }
                }
            }

            if (metaData.imageUrl.isEmpty()) {
                var src = doc.select("link[rel=image_src]").attr("href")
                if (src.isNotEmpty()) {
                    resolveURL(url, src)?.apply {
                        metaData.imageUrl = this
                    }
                } else {
                    src = doc.select("link[rel=apple-touch-icon]").attr("href")
                    if (src.isNotEmpty()) {
                        resolveURL(url, src)?.apply {
                            metaData.imageUrl = this
                        }
                        resolveURL(url, src)?.apply {
                            metaData.favicon = this
                        }
                    } else {
                        src = doc.select("link[rel=icon]").attr("href")
                        if (src.isNotEmpty()) {
                            resolveURL(url, src)?.apply {
                                metaData.imageUrl = this
                            }
                            resolveURL(url, src)?.apply {
                                metaData.favicon = this
                            }
                        }
                    }
                }
            }

            //Favicon
            var src = doc.select("link[rel=apple-touch-icon]").attr("href")
            if (src.isNotEmpty()) {
                resolveURL(url, src)?.apply {
                    metaData.favicon = this
                }
            } else {
                src = doc.select("link[rel=icon]").attr("href")
                if (src.isNotEmpty()) {
                    resolveURL(url, src)?.apply {
                        metaData.favicon = this
                    }
                }
            }

            for (element in elements) {
                if (element.hasAttr("property")) {
                    val str_property = element.attr("property").toString().trim { it <= ' ' }
                    if (str_property == "og:url") {
//                        metaData.setUrl(element.attr("content").toString())
                    }
                    if (str_property == "og:site_name") {
                        metaData.siteName = element.attr("content").toString()
                    }
                }
            }

//            if (metaData.getUrl() == "" || metaData.getUrl().isEmpty()) {
//                var uri: URI? = null
//                try {
//                    uri = URI(url)
//                } catch (e: URISyntaxException) {
//                    e.printStackTrace()
//                }
//                if (url == null) {
//                    metaData.setUrl(url)
//                } else {
//                    metaData.setUrl(uri!!.host)
//                }
//            }

            metaData.isLoadComplete = true

            return metaData
        }

        private fun resolveURL(url: String, part: String): String? {
            return if (URLUtil.isValidUrl(part)) {
                part
            } else {
                var base_uri: URI? = null
                try {
                    base_uri = URI(url)
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }
                base_uri = base_uri!!.resolve(part)
                base_uri.toString()
            }
        }
    }
}